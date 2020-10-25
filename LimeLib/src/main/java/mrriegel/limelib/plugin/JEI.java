package mrriegel.limelib.plugin;

import java.util.List;

import com.google.common.collect.Lists;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IFocus.Mode;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

@JEIPlugin
public class JEI implements IModPlugin {

	private static IJeiRuntime runtime;

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
	}

	public static IJeiRuntime getRuntime() {
		return runtime;
	}

	public static void showRecipes(ItemStack stack) {
		if (!stack.isEmpty())
			showRecipes((Object) stack);
	}

	public static void showUsage(ItemStack stack) {
		if (!stack.isEmpty())
			showUsage((Object) stack);
	}

	public static void showRecipes(FluidStack stack) {
		if (stack != null)
			showRecipes((Object) stack);
	}

	public static void showUsage(FluidStack stack) {
		if (stack != null)
			showUsage((Object) stack);
	}

	public static void showRecipes(Object stack) {
		runtime.getRecipesGui().show(runtime.getRecipeRegistry().createFocus(Mode.OUTPUT, stack));
	}

	public static void showUsage(Object stack) {
		runtime.getRecipesGui().show(runtime.getRecipeRegistry().createFocus(Mode.INPUT, stack));
	}

	public static void showCategories(List<String> strings) {
		runtime.getRecipesGui().showCategories(strings);
	}

	public static void showCategories(String string) {
		showCategories(Lists.newArrayList(string));
	}

	public static boolean hasKeyboardFocus() {
		if (runtime.getIngredientListOverlay() != null)
			return runtime.getIngredientListOverlay().hasKeyboardFocus();
		return false;
	}

	public static void setFilterText(String s) {
		if (runtime.getIngredientFilter() != null)
			runtime.getIngredientFilter().setFilterText(s);
	}

	public static String getFilterText() {
		if (runtime.getIngredientFilter() != null)
			return runtime.getIngredientFilter().getFilterText();
		return null;
	}

}
