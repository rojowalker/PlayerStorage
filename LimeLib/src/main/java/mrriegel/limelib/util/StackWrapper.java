package mrriegel.limelib.util;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.Lists;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.ItemHandlerHelper;

public final class StackWrapper {
	private ItemStack stack;
	private int size;

	public StackWrapper(ItemStack stack, int size) {
		super();
		Validate.isTrue(!stack.isEmpty());
		this.stack = stack.copy();
		this.size = size;
	}

	public StackWrapper(ItemStack stack) {
		this(stack, stack.getCount());
	}

	private StackWrapper() {
	}

	public void readFromNBT(NBTTagCompound compound) {
		NBTTagCompound c = compound.getCompoundTag("stack");
		stack = new ItemStack(c);
		size = compound.getInteger("size");
	}

	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound c = new NBTTagCompound();
		stack.writeToNBT(c);
		compound.setTag("stack", c);
		compound.setInteger("size", size);
		return compound;
	}

	@Override
	public String toString() {
		return "[" + size + "x" + stack.getItem().getRegistryName().getPath() + "@" + stack.getItemDamage() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StackWrapper))
			return false;
		StackWrapper o = (StackWrapper) obj;
		return ItemHandlerHelper.canItemStacksStack(stack, o.stack);
	}

	public ItemStack getStack() {
		return stack;
	}

	public void setStack(ItemStack stack) {
		Validate.isTrue(!stack.isEmpty());
		this.stack = stack.copy();
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public StackWrapper copy() {
		return new StackWrapper(stack, size);
	}

	public boolean canInsert(ItemStack stack) {
		return ItemHandlerHelper.canItemStacksStack(stack, this.stack);
	}

	public boolean insert(ItemStack stack) {
		if (canInsert(stack))
			size += stack.getCount();
		else
			return false;
		return true;
	}

	public ItemStack extract(int size) {
		if (size <= 0)
			return ItemStack.EMPTY;
		size = Math.min(size, this.size);
		this.size -= size;
		return ItemHandlerHelper.copyStackWithSize(stack, size);

	}

	public static StackWrapper loadStackWrapperFromNBT(NBTTagCompound nbt) {
		StackWrapper wrap = new StackWrapper();
		wrap.readFromNBT(nbt);
		return !wrap.getStack().isEmpty() ? wrap : null;
	}

	public static NonNullList<ItemStack> toStackList(List<StackWrapper> list) {
		NonNullList<ItemStack> lis = NonNullList.create();
		for (StackWrapper s : list) {
			if (s == null || s.getStack().isEmpty())
				continue;
			int value = s.size;
			int max = s.stack.getMaxStackSize();
			while (value > 0) {
				int f = Math.min(max, value);
				lis.add(ItemHandlerHelper.copyStackWithSize(s.stack, f));
				value -= f;
			}
		}
		return lis;
	}

	public static NonNullList<ItemStack> toStackList(StackWrapper wrap) {
		return toStackList(Collections.singletonList(wrap));
	}

	public static List<StackWrapper> toWrapperList(List<ItemStack> list) {
		List<StackWrapper> lis = Lists.newArrayList();
		for (ItemStack s : list) {
			if (s.isEmpty())
				continue;
			boolean added = false;
			for (int i = 0; i < lis.size(); i++) {
				ItemStack stack = lis.get(i).getStack();
				if (ItemHandlerHelper.canItemStacksStack(s, stack)) {
					lis.get(i).size += s.getCount();
					//					lis.get(i).setSize(lis.get(i).getSize() + s.getCount());
					added = true;
					break;
				}
			}
			if (!added)
				lis.add(new StackWrapper(s, s.getCount()));
		}
		return lis;
	}

	public static void add(StackWrapper wrap, List<StackWrapper> lis) {
		boolean added = false;
		for (StackWrapper w : lis)
			if (ItemHandlerHelper.canItemStacksStack(wrap.stack, w.stack)) {
				w.size += wrap.size;
				added = true;
				break;
			}
		if (!added)
			lis.add(wrap);
	}

}
