package mrriegel.limelib.tile;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import com.google.common.collect.Lists;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.util.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;

@Optional.InterfaceList(value = { @Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "waila"), @Optional.Interface(iface = "mcjty.theoneprobe.api.IProbeInfoProvider", modid = "theoneprobe") })
public interface IInfoProvider<T extends TileEntity> extends IWailaDataProvider, IProbeInfoProvider {

	Class<T> getTileClass();

	default List<String> getHeadLines(T tile, IBlockState state, EntityPlayer player, ItemStack stack, boolean sneak) {
		return Collections.emptyList();
	}

	default List<String> getBodyLines(T tile, IBlockState state, EntityPlayer player, ItemStack stack, boolean sneak) {
		return Collections.emptyList();
	}

	default List<String> getTailLines(T tile, IBlockState state, EntityPlayer player, ItemStack stack, boolean sneak) {
		return Collections.emptyList();
	}

	default Side readingSide() {
		return Side.SERVER;
	}

	@Override
	default ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return ItemStack.EMPTY;
	}

	@Override
	default List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		List<String> niu = getHeadLines((T) (readingSide().isClient() ? accessor.getTileEntity() : getSyncedTile(accessor.getTileEntity(), accessor)), accessor.getBlockState(), accessor.getPlayer(), itemStack, Keyboard.isKeyDown(42) || accessor.getPlayer().isSneaking());
		if (niu != null && !niu.isEmpty())
			currenttip.addAll(niu);
		return currenttip;
	}

	@Override
	default List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		List<String> niu = getBodyLines((T) (readingSide().isClient() ? accessor.getTileEntity() : getSyncedTile(accessor.getTileEntity(), accessor)), accessor.getBlockState(), accessor.getPlayer(), itemStack, Keyboard.isKeyDown(42) || accessor.getPlayer().isSneaking());
		if (niu != null && !niu.isEmpty())
			currenttip.addAll(niu);
		return currenttip;
	}

	@Override
	default List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		List<String> niu = getTailLines((T) (readingSide().isClient() ? accessor.getTileEntity() : getSyncedTile(accessor.getTileEntity(), accessor)), accessor.getBlockState(), accessor.getPlayer(), itemStack, Keyboard.isKeyDown(42) || accessor.getPlayer().isSneaking());
		if (niu != null && !niu.isEmpty())
			currenttip.addAll(niu);
		return currenttip;
	}

	@Override
	default NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, BlockPos pos) {
		return te.getUpdateTag();
	}

	@Override
	default String getID() {
		return Utils.getCurrentModID();
	}

	@Override
	default void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
		if (probeInfo != null && world != null && blockState != null && data != null) {
			BlockPos pos = data.getPos();
			if (pos != null) {
				TileEntity tileEntity = world.getTileEntity(pos);
				if (tileEntity == null || tileEntity.getClass() != getTileClass())
					return;
				T t = (T) tileEntity;
				getHeadLines(t, blockState, player, data.getPickBlock(), mode == ProbeMode.EXTENDED).forEach(s -> probeInfo.text(s));
				getBodyLines(t, blockState, player, data.getPickBlock(), mode == ProbeMode.EXTENDED).forEach(s -> probeInfo.text(s));
				getTailLines(t, blockState, player, data.getPickBlock(), mode == ProbeMode.EXTENDED).forEach(s -> probeInfo.text(s));
			}
		}

	}

	static final List<Pair<IInfoProvider<?>, Class<? extends TileEntity>>> providers = Lists.newArrayList();

	public static void registerProvider(IInfoProvider<?> provider, Class<? extends TileEntity> clazz) {
		if (!providers.stream().map(p -> p.getRight()).anyMatch(c -> c == clazz))
			providers.add(Pair.of(provider, clazz));
		else
			LimeLib.log.warn(clazz + " is already registered.");
	}

	public static List<Pair<IInfoProvider<?>, Class<? extends TileEntity>>> getProviders() {
		return Collections.unmodifiableList(providers);
	}

	static TileEntity getSyncedTile(TileEntity t, IWailaDataAccessor accessor) {
		try {
			TileEntity tile = t.getClass().newInstance();
			tile.handleUpdateTag(accessor.getNBTData());
			tile.setWorld(t.getWorld());
			tile.setPos(t.getPos());
			return tile;
		} catch (IllegalAccessException | InstantiationException e) {
			return t;
		}
	}

}
