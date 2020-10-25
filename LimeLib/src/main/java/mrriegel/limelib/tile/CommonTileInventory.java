package mrriegel.limelib.tile;

import java.util.List;

import com.google.common.collect.Lists;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.helper.NBTHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class CommonTileInventory extends CommonTile implements IInventory {

	protected NonNullList<ItemStack> stacks;
	private int SIZE, STACKLIMIT;

	public CommonTileInventory(int size) {
		this(size, 64);
	}

	public CommonTileInventory(int size, int limit) {
		SIZE = size;
		STACKLIMIT = limit;
		stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	}

	@Override
	public String getName() {
		return "Null";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int getSizeInventory() {
		return SIZE;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		return stacks.get(index);
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		ItemStack itemstack = ItemStackHelper.getAndSplit(stacks, index, count);
		markDirty();
		return itemstack;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack stack = ItemStackHelper.getAndRemove(stacks, index);
		markDirty();
		return stack;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		stacks.set(index, stack);
		if (stack.getCount() > getInventoryStackLimit()) {
			stack.setCount(getInventoryStackLimit());
			LimeLib.log.warn("Size of itemstack is too high for inventory.");
		}
		markDirty();

	}

	@Override
	public int getInventoryStackLimit() {
		return STACKLIMIT;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		stacks.clear();
	}

	@Override
	public List<ItemStack> getDroppingItems() {
		return Lists.newArrayList(stacks);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			if (this instanceof ISidedInventory)
				return (T) new SidedInvWrapper((ISidedInventory) this, facing);
			return (T) new InvWrapper(this);
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		List<ItemStack> lis = NBTHelper.getList(compound, "Items", ItemStack.class);
		for (int i = 0; i < lis.size(); i++) {
			stacks.set(i, lis.get(i));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTHelper.setList(compound, "Items", stacks);
		return super.writeToNBT(compound);
	}

	@Override
	public boolean isEmpty() {
		return !stacks.stream().anyMatch(s -> !s.isEmpty());
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return isUsable(player);
	}

}
