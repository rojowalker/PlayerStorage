package mrriegel.limelib.util;

import com.google.common.base.Predicate;

import mrriegel.limelib.helper.StackHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

public class FilterItem implements Predicate<ItemStack> {
	ItemStack stack;
	boolean meta, ore, nbt;

	public FilterItem(ItemStack stack) {
		this(stack, !stack.isEmpty() ? stack.getItemDamage() != OreDictionary.WILDCARD_VALUE : true, false, false);
	}

	public FilterItem(String s) {
		this(OreDictionary.getOres(s).isEmpty() ? ItemStack.EMPTY : OreDictionary.getOres(s).get(0), false, true, false);
	}

	public FilterItem(Item i) {
		//TODO think
		this(new ItemStack(i), false, false, false);
	}

	public FilterItem(Block b) {
		this(new ItemStack(b, 1, OreDictionary.WILDCARD_VALUE));
	}

	public FilterItem(ItemStack stack, boolean meta, boolean ore, boolean nbt) {
		if (stack.isEmpty())
			throw new NullPointerException();
		this.stack = stack;
		this.meta = meta;
		this.ore = ore;
		this.nbt = nbt;
	}

	private FilterItem() {
	}

	public void readFromNBT(NBTTagCompound compound) {
		NBTTagCompound c = compound.getCompoundTag("stack");
		stack = new ItemStack(c);
		meta = compound.getBoolean("meta");
		ore = compound.getBoolean("ore");
		nbt = compound.getBoolean("nbt");
	}

	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound c = new NBTTagCompound();
		stack.writeToNBT(c);
		compound.setTag("stack", c);
		compound.setBoolean("meta", meta);
		compound.setBoolean("ore", ore);
		compound.setBoolean("nbt", nbt);
		return c;
	}

	public ItemStack getStack() {
		return stack;
	}

	public void setStack(ItemStack stack) {
		if (stack.isEmpty())
			throw new NullPointerException();
		this.stack = stack;
	}

	public boolean isMeta() {
		return meta;
	}

	public void setMeta(boolean meta) {
		this.meta = meta;
	}

	public boolean isOre() {
		return ore;
	}

	public void setOre(boolean ore) {
		this.ore = ore;
	}

	public boolean isNbt() {
		return nbt;
	}

	public void setNbt(boolean nbt) {
		this.nbt = nbt;
	}

	public static FilterItem loadFilterItemFromNBT(NBTTagCompound nbt) {
		FilterItem fil = new FilterItem();
		fil.readFromNBT(nbt);
		return !fil.getStack().isEmpty() ? fil : null;
	}

	public boolean match(ItemStack s) {
		if (s.isEmpty())
			return false;
		if (ore && StackHelper.equalOreDict(s, stack))
			return true;
		if (nbt && !ItemStack.areItemStackTagsEqual(stack, s))
			return false;
		if (meta && s.getItemDamage() != stack.getItemDamage())
			return false;
		return s.getItem() == stack.getItem();
	}

	@Override
	public boolean apply(ItemStack input) {
		return match(input);
	}

}
