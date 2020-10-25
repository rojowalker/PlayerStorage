package mrriegel.limelib.recipe;

import java.util.Collections;
import java.util.List;

import mrriegel.limelib.helper.InvHelper;
import mrriegel.limelib.util.FilterItem;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;

public class RecipeItemHandler extends AbstractRecipe<ItemStack, IItemHandler> {

	public RecipeItemHandler(NonNullList<ItemStack> output, boolean order, Object... input) {
		super(output, order, input);
	}

	@Override
	protected NonNullList<ItemStack> getIngredients(IItemHandler object) {
		NonNullList<ItemStack> lis = NonNullList.create();
		for (int i = 0; i < object.getSlots(); i++)
			lis.add(object.getStackInSlot(i));
		if (!order)
			lis.removeAll(Collections.singleton(ItemStack.EMPTY));
		return lis;
	}

	@Override
	public boolean removeIngredients(IItemHandler object, boolean simulate) {
		for (Object o : getInput()) {
			FilterItem f = null;
			if (o instanceof Item)
				f = new FilterItem((Item) o);
			if (o instanceof Block)
				f = new FilterItem((Block) o);
			if (o instanceof String)
				f = new FilterItem((String) o);
			if (o instanceof ItemStack)
				f = new FilterItem((ItemStack) o);
			if (InvHelper.extractItem(object, f, 1, simulate).isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public List<ItemStack> getResult(IItemHandler object) {
		return getOutput();
	}

}
