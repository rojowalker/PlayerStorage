package mrriegel.limelib.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

public class ContainerNull extends Container {

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
		return ItemStack.EMPTY;
	}

	@Override
	public void putStackInSlot(int slotID, ItemStack stack) {
	}

	@Override
	public void detectAndSendChanges() {
	}
}
