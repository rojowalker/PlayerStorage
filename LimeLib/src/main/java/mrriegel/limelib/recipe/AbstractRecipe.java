package mrriegel.limelib.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import mrriegel.limelib.helper.RecipeHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

/**
 * @param <S>
 *            type of output ()
 * @param <T>
 *            type of interacting object (world, inventory)
 */
public abstract class AbstractRecipe<S, T> {
	protected final List<S> output;
	protected final boolean order;
	protected final List<Ingredient> input;

	public AbstractRecipe(List<S> output, boolean order, Object... input) {
		if (output.contains(null))
			throw new IllegalArgumentException("output contains null");
		this.output = Collections.unmodifiableList(output);
		this.order = order;
		this.input = Collections.unmodifiableList(Arrays.asList(input).stream().map(o -> getIngredient(o)).collect(Collectors.toList()));
	}

	protected abstract List<ItemStack> getIngredients(T world);

	public abstract boolean removeIngredients(T object, boolean simulate);

	public abstract List<ItemStack> getResult(T object);

	protected Ingredient getIngredient(Object obj) {
		return RecipeHelper.getIngredient(obj);
	}

	public List<S> getOutput() {
		return output;
	}

	public boolean isOrder() {
		return order;
	}

	public List<Ingredient> getInput() {
		return input;
	}

	public boolean match(T object) {
		List<ItemStack> list = getIngredients(object);
		if (list.size() != input.size())
			return false;
		if (order) {
			for (int i = 0; i < input.size(); i++)
				if (!match(list.get(i), input.get(i)))
					return false;
			return true;
		} else {
			List<Ingredient> foo = Lists.newArrayList(input);
			for (ItemStack stack : list) {
				if (!stack.isEmpty()) {
					boolean flag = false;
					for (int i = 0; i < foo.size(); i++) {
						Ingredient o = foo.get(i);
						if (match(stack, o)) {
							flag = true;
							foo.remove(i);
							break;
						}
					}
					if (!flag) {
						return false;
					}
				}
			}
			return foo.isEmpty();
		}
	}

	protected boolean match(ItemStack stack, Ingredient o) {
		return o.apply(stack);
	}
}
