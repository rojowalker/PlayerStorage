package mrriegel.limelib.gui.slot;

import org.apache.commons.lang3.Validate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

public class CommonSlot extends Slot {

	protected Object newInventory;
	protected final SlotItemHandler back;

	private CommonSlot(Object inventory, int index, int xPosition, int yPosition) {
		super(inventory instanceof IInventory ? (IInventory) inventory : new InventoryBasic("Null", false, 0), index, xPosition, yPosition);
		Validate.isTrue(inventory instanceof IInventory || inventory instanceof IItemHandler);
		this.newInventory = inventory;
		this.back = isItemHandler() ? new SlotItemHandler(getItemHandler(), getSlotIndex(), xPos, yPos) : null;
	}

	public CommonSlot(IInventory inventory, int index, int xPosition, int yPosition) {
		this((Object) inventory, index, xPosition, yPosition);
	}

	public CommonSlot(IItemHandler inventory, int index, int xPosition, int yPosition) {
		this((Object) inventory, index, xPosition, yPosition);
	}

	public boolean isInventory() {
		return newInventory instanceof IInventory;
	}

	public boolean isItemHandler() {
		return !isInventory();
	}

	public IInventory getInventory() {
		return (IInventory) newInventory;
	}

	public IItemHandler getItemHandler() {
		return (IItemHandler) newInventory;
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		if (isInventory())
			return super.isItemValid(stack);
		else
			return back.isItemValid(stack);
	}

	@Override
	public ItemStack getStack() {
		if (isInventory())
			return super.getStack();
		else
			return back.getStack();
	}

	@Override
	public void putStack(ItemStack stack) {
		if (isInventory())
			super.putStack(stack);
		else {
			if (getItemHandler() instanceof IItemHandlerModifiable)
				((IItemHandlerModifiable) this.getItemHandler()).setStackInSlot(getSlotIndex(), stack);
			else
				putStack(stack, getItemHandler());
			this.onSlotChanged();
		}
	}

	protected void putStack(ItemStack stack, IItemHandler handler) {
		handler.extractItem(getSlotIndex(), 1000, false);
		handler.insertItem(getSlotIndex(), stack, false);
	}

	@Override
	public void onSlotChange(ItemStack p_75220_1_, ItemStack p_75220_2_) {
		if (isInventory())
			super.onSlotChange(p_75220_1_, p_75220_2_);
	}

	@Override
	public int getSlotStackLimit() {
		if (isInventory())
			return super.getSlotStackLimit();
		else
			return back.getSlotStackLimit();
	}

	@Override
	public int getItemStackLimit(ItemStack stack) {
		if (isInventory())
			return super.getItemStackLimit(stack);
		else
			return back.getItemStackLimit(stack);
	}

	@Override
	public boolean canTakeStack(EntityPlayer playerIn) {
		if (isInventory())
			return super.canTakeStack(playerIn);
		else
			return back.canTakeStack(playerIn);
	}

	@Override
	public ItemStack decrStackSize(int amount) {
		if (isInventory())
			return super.decrStackSize(amount);
		else
			return back.decrStackSize(amount);
	}

	@Override
	public boolean isHere(IInventory inv, int slotIn) {
		return isInventory() && getInventory() == inv && slotIn == getSlotIndex();
	}

	@Override
	public boolean isSameInventory(Slot other) {
		if (isInventory())
			return (getInventory() == other.inventory) || (other instanceof CommonSlot && ((CommonSlot) other).isInventory() && ((CommonSlot) other).getInventory() == getInventory());
		else
			return back.isSameInventory(other) || (other instanceof CommonSlot && ((CommonSlot) other).isItemHandler() && ((CommonSlot) other).getItemHandler() == getItemHandler());
	}

}
