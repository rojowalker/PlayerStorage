package mrriegel.limelib.gui.slot;

import java.util.function.Predicate;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class SlotFilter extends CommonSlot {

	private Predicate<ItemStack> pred;

	public SlotFilter(IInventory inventoryIn, int index, int xPosition, int yPosition, Predicate<ItemStack> pred) {
		super(inventoryIn, index, xPosition, yPosition);
		this.pred = pred;
	}

	public SlotFilter(IItemHandler inventoryIn, int index, int xPosition, int yPosition, Predicate<ItemStack> pred) {
		super(inventoryIn, index, xPosition, yPosition);
		this.pred = pred;
	}

	public SlotFilter(IInventory inventoryIn, int index, int xPosition, int yPosition) {
		super(inventoryIn, index, xPosition, yPosition);
		this.pred = s -> inventoryIn.isItemValidForSlot(index, s);
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		return pred.test(stack);
	}

}
