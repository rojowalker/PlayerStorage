package mrriegel.limelib.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.recipe.ShapedRecipeExt;
import mrriegel.limelib.recipe.ShapelessRecipeExt;
import mrriegel.limelib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.CraftingHelper.ShapedPrimer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class RecipeHelper {

	//TODO change location
	public static final boolean dev = (boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

	private static Map<String, RecipeHelper> helpers = Maps.newHashMap();

	private File DIR;
	private final Set<String> USED_OD_NAMES = Sets.newTreeSet();
	private boolean oldway = true;

	private static RecipeHelper getHelper() {
		String modid = Utils.getCurrentModID();
		RecipeHelper rh = helpers.get(modid);
		if (rh != null)
			return rh;
		rh = new RecipeHelper(modid);
		helpers.put(modid, rh);
		return rh;

	}

	private RecipeHelper(String modid) {
		DIR = new File("").toPath().resolve("../src/main/resources/assets/" + modid + "/recipes/").toFile();
		if (!DIR.exists() && dev)
			DIR.mkdirs();
		//		if (DIR.exists())
		//			Arrays.stream(DIR.listFiles()).forEach(File::delete);
		if (!dev) {
			File jar = Loader.instance().activeModContainer().getSource();
			try {
				JarInputStream jis = new JarInputStream(new FileInputStream(jar));
				JarEntry e = null;
				while ((e = jis.getNextJarEntry()) != null)
					if (e.getName().equals("assets/" + modid + "/recipes/")) {
						oldway = false;
						break;
					}
				jis.close();
			} catch (IOException e) {
			}
		} else
			oldway = false;
	}

	@Deprecated
	public static void addShapedOreRecipe(ItemStack stack, Object... input) {
		addShapedRecipe(stack, input);
	}

	public static void addShapedRecipe(ItemStack stack, Object... input) {
		RecipeHelper rh = getHelper();
		ResourceLocation rl = name(stack, input);
		if (Arrays.stream(input).anyMatch(o -> o instanceof Collection))
			addRecipe(rl, new ShapedRecipeExt(rl, stack, input));
		else if (Arrays.stream(input).anyMatch(o -> o instanceof String && OreDictionary.doesOreNameExist((String) o))) {
			if (rh.oldway)
				addRecipe(rl, new ShapedOreRecipe(rl, stack, input));
			else
				rh.addRecipe(rl, true, true, stack, input);
		} else {
			if (rh.oldway) {
				ShapedPrimer sp = CraftingHelper.parseShaped(input);
				addRecipe(rl, new ShapedRecipes("", sp.width, sp.height, sp.input, stack));
			} else
				rh.addRecipe(rl, true, false, stack, input);
		}
	}

	@Deprecated
	public static void addShapelessOreRecipe(ItemStack stack, Object... input) {
		addShapelessRecipe(stack, input);
	}

	public static void addShapelessRecipe(ItemStack stack, Object... input) {
		RecipeHelper rh = getHelper();
		ResourceLocation rl = name(stack, input);
		if (Arrays.stream(input).anyMatch(o -> o instanceof Collection))
			addRecipe(rl, new ShapelessRecipeExt(rl, stack, input));
		else if (Arrays.stream(input).anyMatch(o -> o instanceof String && OreDictionary.doesOreNameExist((String) o))) {
			if (rh.oldway)
				addRecipe(rl, new ShapelessOreRecipe(rl, stack, input));
			else
				rh.addRecipe(rl, false, true, stack, input);
		} else {
			if (rh.oldway)
				addRecipe(rl, new ShapelessRecipes("", stack, NonNullList.<Ingredient> from(Ingredient.EMPTY, Lists.newArrayList(input).stream().map(o -> CraftingHelper.getIngredient(o)).filter(o -> o != null).collect(Collectors.toList()).toArray(new Ingredient[0]))));
			else
				rh.addRecipe(rl, false, false, stack, input);
		}
	}

	public static void add(IRecipe recipe) {
		Validate.isTrue(!recipe.getClass().getName().startsWith("net.minecraft"), "Use JSON instead");
		ResourceLocation rl = name(recipe.getRecipeOutput(), (Object[]) recipe.getIngredients().toArray(new Ingredient[0]));
		addRecipe(rl, recipe);
	}

	private static void addRecipe(ResourceLocation rl, IRecipe recipe) {
		Validate.isTrue(!recipe.getRecipeOutput().isEmpty() /*&& !recipe.getClass().getName().startsWith("net.minecraft")*/);
		recipe.setRegistryName(rl);
		RegistryHelper.register(recipe);
	}

	/**
	 * @author williewillus (partly)
	 */
	private void addRecipe(ResourceLocation rl, boolean shaped, boolean ore, ItemStack stack, Object... input) {
		if (!dev)
			return;
		Map<String, Object> json = Maps.newHashMap();
		if (shaped) {
			List<String> pattern = Lists.newArrayList();
			int i = 0;
			while (i < input.length && input[i] instanceof String) {
				pattern.add((String) input[i]);
				i++;
			}
			json.put("pattern", pattern);

			Map<String, Map<String, Object>> key = Maps.newHashMap();
			Character curKey = null;
			for (; i < input.length; i++) {
				Object o = input[i];
				if (o instanceof Character) {
					if (curKey != null)
						throw new IllegalArgumentException("Provided two char keys in a row");
					curKey = (Character) o;
				} else {
					if (curKey == null)
						throw new IllegalArgumentException("Providing object without a char key");
					key.put(Character.toString(curKey), serializeItem(o));
					curKey = null;
				}
			}
			json.put("key", key);
		} else {
			json.put("ingredients", Arrays.stream(input).map(o -> serializeItem(o)).collect(Collectors.toList()));
		}
		json.put("type", shaped ? (ore ? "forge:ore_shaped" : "minecraft:crafting_shaped") : (ore ? "forge:ore_shapeless" : "minecraft:crafting_shapeless"));
		json.put("result", serializeItem(stack));

		//		String suffix = stack.getItem().getHasSubtypes() ? "_" + stack.getItemDamage() : "";
		//		File f = new File(DIR, stack.getItem().getRegistryName().getResourcePath() + suffix + ".json");
		if (!stack.isEmpty()) {
			File f = new File(DIR, rl.getPath().replace('/', '_') + ".json");
			writeToFile(f, json);
		} else
			LimeLib.log.warn("ItemStack is empty. Can't create a recipe. " + Arrays.toString(input));

	}

	private static ResourceLocation name(ItemStack stack, Object... input) {
		List<String> lis = Arrays.stream(input).map(o -> {
			if (o instanceof String)
				return o.toString();
			if (o instanceof Item)
				return ((Item) o).getRegistryName().getPath();
			if (o instanceof Block)
				return ((Block) o).getRegistryName().getPath();
			if (o instanceof ItemStack)
				return ((ItemStack) o).getItem().getRegistryName().getPath();
			if (o instanceof Ingredient)
				return Joiner.on(" ").join(Arrays.stream(((Ingredient) o).getMatchingStacks()).map(s -> s.getItem().getRegistryName().getPath()).sorted().collect(Collectors.toList()));
			return "";
		}).collect(Collectors.toList());
		return new ResourceLocation(Utils.getCurrentModID(), stack.getItem().getRegistryName().getPath() + "/" + stack.getItemDamage() + "_" + stack.getCount() + "_" + (Math.abs(lis.hashCode()) % 9999));
	}

	public static Ingredient getIngredient(Object obj) {
		Ingredient ret = CraftingHelper.getIngredient(obj);
		if (ret != null)
			return ret;
		List<Ingredient> lis = Lists.newArrayList();
		if (obj instanceof Collection) {
			for (Object o : (Collection<?>) obj)
				lis.add(CraftingHelper.getIngredient(o));

		}
		return new CompoundIngredient(lis);
	}

	/**
	 * @author williewillus
	 */
	private Map<String, Object> serializeItem(Object thing) {
		if (thing instanceof Item) {
			return serializeItem(new ItemStack((Item) thing));
		}
		if (thing instanceof Block) {
			return serializeItem(new ItemStack((Block) thing));
		}
		if (thing instanceof ItemStack) {
			ItemStack stack = (ItemStack) thing;
			Map<String, Object> ret = Maps.newHashMap();
			ret.put("item", stack.getItem().getRegistryName().toString());
			if (stack.getItem().getHasSubtypes() || stack.getItemDamage() != 0) {
				ret.put("data", stack.getItemDamage());
			}
			if (stack.getCount() > 1) {
				ret.put("count", stack.getCount());
			}
			if (stack.hasTagCompound()) {
				throw new IllegalArgumentException("nbt not implemented");
			}
			return ret;
		}
		if (thing instanceof String) {
			Map<String, Object> ret = Maps.newHashMap();
			USED_OD_NAMES.add((String) thing);
			ret.put("item", "#" + ((String) thing).toUpperCase());
			return ret;
		}

		throw new IllegalArgumentException("Not a block, item, stack, or od name");
	}

	/**
	 * @author williewillus
	 */
	public static void generateConstants() {
		if (!dev)
			return;
		for (RecipeHelper rh : helpers.values()) {
			List<Map<String, Object>> json = Lists.newArrayList();
			for (String s : rh.USED_OD_NAMES) {
				Map<String, Object> entry = new HashMap<>();
				entry.put("name", s.toUpperCase());
				entry.put("ingredient", ImmutableMap.of("type", "forge:ore_dict", "ore", s));
				json.add(entry);
			}
			if (!rh.USED_OD_NAMES.isEmpty()) {
				File file = new File(rh.DIR, "_constants.json");
				writeToFile(file, json);
			}
		}
	}

	private static void writeToFile(File file, Object o) {
		String newJson = Utils.getGSON().toJson(o).trim();
		String oldJson = null;
		try {
			oldJson = !file.exists() ? "" : Files.lines(file.toPath()).collect(Collectors.joining(Configuration.NEW_LINE)).trim();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!oldJson.equals(newJson)) {
			try (FileWriter fw = new FileWriter(file)) {
				Utils.getGSON().toJson(o, fw);
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static class CompoundIngredient extends net.minecraftforge.common.crafting.CompoundIngredient {

		protected CompoundIngredient(Collection<Ingredient> children) {
			super(children.stream().filter(i -> i != null).collect(Collectors.toList()));
		}

	}

	//1.13

	private static final Map<String, List<Pair<String, String>>> recipes = new HashMap<>();

	public static void generateFiles() {
		if (!dev)
			return;
		if (true)
			return;
		try {
			for (Entry<String, List<Pair<String, String>>> e : recipes.entrySet()) {
				File jar = Loader.instance().getIndexedModList().get(e.getKey()).getSource();
				if (!jar.getPath().endsWith(".jar")) {
					List<String> names = new ArrayList<>();
					for (Pair<String, String> p : e.getValue()) {
						String name = p.getLeft();
						int i = 1;
						while (names.contains(name)) {
							name = p.getLeft() + i++;
						}
						names.add(name);
					}
					File dir = new File("").toPath().resolve("../src/main/resources/data/" + e.getKey() + "/recipes/").toFile();
					if (!dir.exists())
						dir.mkdirs();
					for (int i = 0; i < names.size(); i++) {
						//						File file = new File(dir, names.get(i) + ".json");
						//						FileWriter fw = new FileWriter(file);
						//						fw.write(e.getValue().get(i).getRight());
						//						fw.close();
						Files.write(new File(dir, names.get(i) + ".json").toPath(), e.getValue().get(i).getRight().getBytes());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Object serializeItem2(Object o, boolean count) {
		Objects.requireNonNull(o);
		if (o instanceof String) {
			Map<String, Object> ret = new LinkedHashMap<>();
			ret.put("tag", o);
			return ret;
		}
		if (o instanceof ResourceLocation) {
			Map<String, Object> ret = new LinkedHashMap<>();
			ret.put("tag", o.toString());
			return ret;
		}
		if (o instanceof Item) {
			Map<String, Object> ret = new LinkedHashMap<>();
			ret.put("item", ((Item) o).getRegistryName().toString());
			return ret;
		}
		if (o instanceof Block) {
			Map<String, Object> ret = new LinkedHashMap<>();
			ret.put("item", ((Block) o).getRegistryName().toString());
			return ret;
		}
		if (o instanceof ItemStack) {
			ItemStack s = (ItemStack) o;
			Validate.isTrue(!s.isEmpty(), "ItemStack is empty.");
			Map<String, Object> ret = new LinkedHashMap<>();
			ret.put("item", s.getItem().getRegistryName().toString());
			if (count && s.getCount() > 1)
				ret.put("count", s.getCount());
			return ret;
		}
		if (o instanceof Collection) {
			return ((Collection<Object>) o).stream().map(oo -> serializeItem2(oo, count)).collect(Collectors.toList());
		}
		if (o instanceof Object[]) {
			return Arrays.stream((Object[]) o).map(oo -> serializeItem2(oo, count)).collect(Collectors.toList());
		}
		throw new IllegalArgumentException("Argument of type " + o.getClass().getName() + " is invalid.");

	}

	private static void validate(ItemStack stack) {
		Validate.isTrue(!stack.isEmpty(), "result must not be empty");
		//		Validate.isTrue(Loader.instance().hasReachedState(LoaderState.INITIALIZATION), "register after preInit");
	}

	private static boolean valid() {
		return dev && !Loader.instance().activeModContainer().getSource().getPath().endsWith(".jar");
	}

	public static void addCraftingRecipe(ItemStack result, @Nullable String group, boolean shaped, Object... input) {
		if (!dev)
			return;
		validate(result);
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("type", shaped ? "minecraft:crafting_shaped" : "minecraft:crafting_shapeless");
		if (!StringUtils.isNullOrEmpty(group))
			json.put("group", group);
		if (shaped) {
			List<String> pattern = new ArrayList<>();
			int i = 0;
			while (i < input.length && input[i] instanceof String) {
				pattern.add((String) input[i]);
				i++;
			}
			json.put("pattern", pattern);

			Map<String, Object> key = new LinkedHashMap<>();
			Character curKey = null;
			for (; i < input.length; i++) {
				Object o = input[i];
				if (o instanceof Character) {
					if (curKey != null)
						throw new IllegalArgumentException("Provided two char keys in a row");
					curKey = (Character) o;
				} else {
					if (curKey == null)
						throw new IllegalArgumentException("Providing object without a char key");
					key.put(Character.toString(curKey), serializeItem2(o, false));
					curKey = null;
				}
			}
			json.put("key", key);
		} else {
			json.put("ingredients", Arrays.stream(input).map(o -> serializeItem2(o, false)).collect(Collectors.toList()));
		}
		json.put("result", serializeItem2(result, true));
		addRecipe(result.getItem().getRegistryName().getPath(), json);
	}

	public static void addSmeltingRecipe(ItemStack result, Object input, double exp, int time) {
		if (!dev)
			return;
		validate(result);
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("type", "smelting");
		json.put("ingredient", serializeItem2(input, false));
		json.put("result", result.getItem().getRegistryName().toString());
		json.put("experience", exp);
		json.put("cookingtime", time);
		addRecipe(result.getItem().getRegistryName().getPath(), json);
	}

	public static void addRecipe(String name, Map<String, Object> json) {
		String id = Utils.getCurrentModID();
		recipes.computeIfAbsent(id, s -> {
			List<Pair<String, String>> list = new ArrayList<>();
			list.add(Pair.of(name, Utils.getGSON().toJson(json)));
			return list;
		});
		//		List<Pair<String, String>> recs = recipes.get(id);
		//		if (recs == null)
		//			recipes.put(id, recs = new ArrayList<>());
		//		recs.add(Pair.of(name, Utils.getGSON().toJson(json)));
	}

}
