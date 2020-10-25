package mrriegel.limelib.helper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class BlockHelper {

	public static boolean isBlockBreakable(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		return !world.isAirBlock(pos) && !state.getBlock().getMaterial(state).isLiquid() && state.getBlock().getBlockHardness(state, world, pos) > -1F;
	}

	private static Field harvesters = ReflectionHelper.findField(Block.class, "harvesters");
	private static Reference2ObjectMap<Block, ThreadLocal<EntityPlayer>> players = new Reference2ObjectOpenHashMap<>();

	private static ThreadLocal<EntityPlayer> getPlayer(Block block) {
		ThreadLocal<EntityPlayer> harvesters = players.get(block);
		if (harvesters == null)
			try {
				players.put(block, harvesters = (ThreadLocal<EntityPlayer>) BlockHelper.harvesters.get(block));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		return harvesters;
	}

	public static NonNullList<ItemStack> breakBlock(World world, BlockPos pos, IBlockState state, @Nullable EntityPlayer player, boolean silk, int fortune, boolean dropXP, boolean particle) {
		if (!isBlockBreakable(world, pos))
			return NonNullList.create();
		Block block = state.getBlock();
		int exp = block.getExpDrop(state, world, pos, fortune);
		if (player != null) {
			BreakEvent event = new BreakEvent(world, pos, state, player);
			event.setExpToDrop(exp);
			if (MinecraftForge.EVENT_BUS.post(event))
				return NonNullList.create();
			exp = event.getExpToDrop();
		}
		if (particle)
			world.playEvent(2001, pos, Block.getStateId(state));
		NonNullList<ItemStack> lis = null;
		if (silk && block.canSilkHarvest(world, pos, state, player)) {
			lis = NonNullList.create();
			ItemStack drop = getSilkDrop(world, pos, player);
			if (!drop.isEmpty())
				lis.add(drop);
		} else
			lis = getFortuneDrops(world, pos, player, fortune);
		if (player != null && !ForgeHooks.canHarvestBlock(block, player, world, pos))
			lis.clear();
		world.setBlockToAir(pos);
		if (block instanceof BlockShulkerBox)
			return NonNullList.create();
		if (dropXP && !silk && exp > 0)
			block.dropXpOnBlockBreak(world, pos, exp);
		return lis;
	}

	public static NonNullList<ItemStack> getFortuneDrops(World world, BlockPos pos, EntityPlayer player, int fortune) {
		IBlockState state = world.getBlockState(pos);
		ThreadLocal<EntityPlayer> harvesters = getPlayer(state.getBlock());
		if (player != null)
			harvesters.set(player);
		List<ItemStack> tmp = NonNullList.create();
		state.getBlock().getDrops((NonNullList<ItemStack>) tmp, world, pos, state, fortune);
		try {
			Method m = state.getBlock().getClass().getMethod("getDrops", IBlockAccess.class, BlockPos.class, IBlockState.class, int.class);
			if (m.getDeclaringClass() != Block.class)
				tmp = state.getBlock().getDrops(world, pos, state, fortune);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		float chance = ForgeEventFactory.fireBlockHarvesting(tmp, world, pos, state, fortune, 1.0f, false, player);
		NonNullList<ItemStack> lis = NonNullList.create();
		for (ItemStack item : tmp) {
			if (!item.isEmpty() && world.rand.nextFloat() <= chance) {
				lis.add(item);
			}
		}
		harvesters.set(null);
		return lis;
	}

	private static Map<Class<?>, Method> methodMap = Maps.newHashMap();

	public static ItemStack getSilkDrop(World world, BlockPos pos, EntityPlayer player) {
		IBlockState state = world.getBlockState(pos);
		ThreadLocal<EntityPlayer> harvesters = getPlayer(state.getBlock());
		if (player != null)
			harvesters.set(player);
		NonNullList<ItemStack> tmp = NonNullList.create();
		if (state.getBlock().canSilkHarvest(world, pos, state, player)) {
			Method m = null;
			Class<?> clazz = state.getBlock().getClass();
			Set<Class<?>> clazzes = Sets.newHashSet(clazz);
			while (m == null) {
				if (methodMap.containsKey(clazz))
					m = methodMap.get(clazz);
				else
					try {
						m = ReflectionHelper.findMethod(clazz, "getSilkTouchDrop", "func_180643_i", IBlockState.class);
					} catch (Exception e) {
						clazz = clazz.getSuperclass();
						if (clazz != null)
							clazzes.add(clazz);
						else
							break;
					}
			}
			for (Class<?> c : clazzes)
				methodMap.put(c, m);
			ItemStack silked = ItemStack.EMPTY;
			try {
				silked = (ItemStack) m.invoke(state.getBlock(), state);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			}
			if (!silked.isEmpty())
				tmp.add(silked);
		}
		ForgeEventFactory.fireBlockHarvesting(tmp, world, pos, state, 0, 1.0f, true, player);
		harvesters.set(null);
		return tmp.isEmpty() ? ItemStack.EMPTY : tmp.get(0);
	}

	public static boolean isOre(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		if (!state.getBlock().isFullCube(state))
			return false;
		ItemStack stack = ItemStack.EMPTY;
		try {
			EntityPlayer player = world.isRemote ? LimeLib.proxy.getClientPlayer() : Utils.getFakePlayer((WorldServer) world);
			stack = getSilkDrop(world, pos, player);
			if (stack.isEmpty())
				stack = state.getBlock().getPickBlock(state, new RayTraceResult(new Vec3d(0, 0, 0), EnumFacing.UP), world, pos, player);
			return StackHelper.isOre(stack);
		} catch (Exception e) {
			stack = new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state));
			return StackHelper.isOre(stack);
		}
	}

	public static boolean isToolEffective(ItemStack tool, World world, BlockPos pos, boolean reallyEffective) {
		if (ForgeHooks.isToolEffective(world, pos, tool)) {
			return true;
		}
		IBlockState state = world.getBlockState(pos);
		state = state.getBlock().getActualState(state, world, pos);
		if (state.getBlockHardness(world, pos) < 0)
			return false;
		return (!reallyEffective && state.getBlock().getHarvestTool(state) == null) || tool.getItem().getToolClasses(tool).contains(state.getBlock().getHarvestTool(state));
	}

	public static boolean canToolHarvestBlock(IBlockAccess world, BlockPos pos, @Nonnull ItemStack stack) {
		IBlockState state = world.getBlockState(pos);
		state = state.getBlock().getActualState(state, world, pos);
		if (world instanceof World && state.getBlockHardness((World) world, pos) < 0)
			return false;
		String tool = state.getBlock().getHarvestTool(state);
		if (state.getBlock().getMaterial(state).isToolNotRequired())
			return true;
		if (stack.isEmpty())
			return false;
		return stack.getItem().getHarvestLevel(stack, tool, null, null) >= state.getBlock().getHarvestLevel(state);
	}

}
