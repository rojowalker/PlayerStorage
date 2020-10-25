package mrriegel.limelib.recipe;

import mrriegel.limelib.helper.RecipeHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@Deprecated
public class ShapelessRecipeExt extends ShapelessOreRecipe {

	public ShapelessRecipeExt(ResourceLocation group, Block result, Object... recipe) {
		super(group, result, recipe);
	}

	public ShapelessRecipeExt(ResourceLocation group, Item result, Object... recipe) {
		super(group, result, recipe);
	}

	public ShapelessRecipeExt(ResourceLocation group, ItemStack result, Object... recipe) {
		super(group, result);
		this.group = group;
		output = result.copy();
		input = NonNullList.create();
		for (Object in : recipe) {
			Ingredient ing = RecipeHelper.getIngredient(in);
			if (ing != null) {
				input.add(ing);
			} else {
				String ret = "Invalid shapeless ore recipe: ";
				for (Object tmp : recipe) {
					ret += tmp + ", ";
				}
				ret += output;
				throw new RuntimeException(ret);
			}
		}
	}

	public ShapelessRecipeExt(ResourceLocation group, NonNullList<Ingredient> input, ItemStack result) {
		super(group, input, result);
	}

}