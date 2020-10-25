package mrriegel.limelib.gui.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

public class SlotImmutable extends CommonSlot {

	ItemStack stack;

	public SlotImmutable(int index, int xPosition, int yPosition, ItemStack stack) {
		super(new InventoryBasic("Null", false, 0), index, xPosition, yPosition);
		this.stack = stack;
	}

	@Override
	public boolean canTakeStack(EntityPlayer playerIn) {
		return false;
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack decrStackSize(int amount) {
		return null;
	}

	@Override
	public void putStack(ItemStack stack) {
	}

	@Override
	public ItemStack getStack() {
		return stack;
	}

	@Override
	public void onSlotChanged() {
	}

	@Override
	public int getSlotStackLimit() {
		return 64;
	}

	@Override
	public boolean isHere(IInventory inv, int slotIn) {
		return false;
	}

}
