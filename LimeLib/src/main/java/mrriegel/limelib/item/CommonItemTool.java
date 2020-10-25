package mrriegel.limelib.item;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import mrriegel.limelib.helper.StackHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CommonItemTool extends CommonItem {

	protected Set<String> toolClasses;
	protected final Set<Block> effectiveBlocks;
	protected final Item.ToolMaterial toolMaterial;

	public CommonItemTool(String name, ToolMaterial material, String... toolClasses) {
		super(name);
		toolMaterial = material;
		this.toolClasses = toolClasses != null ? Sets.newHashSet(toolClasses) : Collections.EMPTY_SET;
		effectiveBlocks = effectives(this.toolClasses);
		setMaxStackSize(1);
		setMaxDamage(material.getMaxUses());
	}

	private static Set<Block> effectives(Set<String> toolClasses) {
		Set<Block> blocks = Sets.newHashSet();
		if (toolClasses.contains("pickaxe"))
			blocks.addAll(ReflectionHelper.getPrivateValue(ItemPickaxe.class, null, 0));
		if (toolClasses.contains("shovel"))
			blocks.addAll(ReflectionHelper.getPrivateValue(ItemSpade.class, null, 0));
		if (toolClasses.contains("axe"))
			blocks.addAll(ReflectionHelper.getPrivateValue(ItemAxe.class, null, 0));
		return blocks;
	}

	@Override
	public Set<String> getToolClasses(ItemStack stack) {
		return toolClasses;
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {
		return toolMaterial.getEnchantability();
	}

	public Item.ToolMaterial getToolMaterial() {
		return this.toolMaterial;
	}

	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
		stack.damageItem(2, attacker);
		return true;
	}

	@Override
	public boolean onBlockDestroyed(ItemStack stack, World worldIn, IBlockState state, BlockPos pos, EntityLivingBase entityLiving) {
		if (!worldIn.isRemote && state.getBlockHardness(worldIn, pos) != 0.0D) {
			stack.damageItem(1, entityLiving);
		}
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isFull3D() {
		return true;
	}

	protected final double getBaseDamage(ItemStack stack) {
		List<Double> lis = Lists.newArrayList();
		if (getToolClasses(stack).contains("axe"))
			lis.add(5.0);
		if (getToolClasses(stack).contains("shovel"))
			lis.add(1.5);
		if (getToolClasses(stack).contains("pickaxe"))
			lis.add(1.);
		return Math.round(lis.stream().mapToDouble(Double::doubleValue).sum() / lis.size() * 10.0) / 10.0;
	}

	protected final double getBaseSpeed(ItemStack stack) {
		List<Double> lis = Lists.newArrayList();
		if (getToolClasses(stack).contains("axe"))
			lis.add(-3.);
		if (getToolClasses(stack).contains("shovel"))
			lis.add(-3.);
		if (getToolClasses(stack).contains("pickaxe"))
			lis.add(-2.8);
		return Math.round(lis.stream().mapToDouble(Double::doubleValue).sum() / lis.size() * 10.0) / 10.0;
	}

	protected double getAttackDamage(ItemStack stack) {
		return getBaseDamage(stack) + toolMaterial.getAttackDamage();
	};

	protected double getAttackSpeed(ItemStack stack) {
		return getBaseSpeed(stack);
	};

	protected float getDigSpeed(ItemStack stack, float efficiencyOnProperMaterial) {
		return efficiencyOnProperMaterial;
	};

	@Override
	public float getDestroySpeed(ItemStack stack, IBlockState state) {
		float result = 1.0F;
		if (toolClasses.contains("pickaxe"))
			result = getDigSpeed(stack, Items.DIAMOND_PICKAXE.getDestroySpeed(stack, state));
		if (toolClasses.contains("axe"))
			result = Math.max(result, getDigSpeed(stack, Items.DIAMOND_AXE.getDestroySpeed(stack, state)));
		for (String type : getToolClasses(stack)) {
			if (state.getBlock().isToolEffective(type, state))
				result = Math.max(result, getDigSpeed(stack, toolMaterial.getEfficiency()));
		}
		return Math.max(result, this.effectiveBlocks.contains(state.getBlock()) ? getDigSpeed(stack, toolMaterial.getEfficiency()) : 1.0F);
	}

	@Override
	public int getHarvestLevel(ItemStack stack, String toolClass, EntityPlayer player, IBlockState blockState) {
		return getToolClasses(stack).contains(toolClass) ? toolMaterial.getHarvestLevel() : super.getHarvestLevel(stack, toolClass, player, blockState);
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		boolean sup = super.getIsRepairable(toRepair, repair);
		if (!sup) {
			sup = StackHelper.equalOreDict(toolMaterial.getRepairItemStack(), repair);
		}
		return sup;
	}

	@Override
	public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot equipmentSlot, ItemStack stack) {
		Multimap<String, AttributeModifier> multimap = HashMultimap.create();
		if (equipmentSlot == EntityEquipmentSlot.MAINHAND) {
			multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", getAttackDamage(stack), 0));
			multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", getAttackSpeed(stack), 0));
		}
		return multimap;
	}

	@Override
	public boolean canHarvestBlock(IBlockState state, ItemStack stack) {
		if (toolClasses.contains("pickaxe"))
			return Items.DIAMOND_PICKAXE.canHarvestBlock(state, stack);
		if (toolClasses.contains("shovel"))
			return Items.DIAMOND_SHOVEL.canHarvestBlock(state, stack);
		return super.canHarvestBlock(state, stack);
		// return state.getBlock().getMaterial(state).isToolNotRequired();
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!player.getHeldItem(hand).getItem().getToolClasses(player.getHeldItem(hand)).contains("shovel"))
			return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
		return Items.DIAMOND_SHOVEL.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
	}

}
