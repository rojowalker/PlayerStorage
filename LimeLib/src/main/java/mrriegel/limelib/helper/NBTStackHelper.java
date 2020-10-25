package mrriegel.limelib.helper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class NBTStackHelper {

	public static void initNBTTagCompound(ItemStack stack) {
		if (!stack.hasTagCompound())
			stack.setTagCompound(new NBTTagCompound());
	}

	public static boolean hasTag(ItemStack stack, String keyName) {
		return NBTHelper.hasTag(stack.getTagCompound(), keyName);
	}

	public static ItemStack removeTag(ItemStack stack, String keyName) {
		NBTHelper.removeTag(stack.getTagCompound(), keyName);
		return stack;
	}

	public static <T> T get(ItemStack stack, String name, Class<T> clazz) {
		return NBTHelper.get(stack.getTagCompound(), name, clazz);
	}

	public static <T> Optional<T> getSafe(ItemStack stack, String name, Class<T> clazz) {
		return NBTHelper.getSafe(stack.getTagCompound(), name, clazz);
	}

	public static ItemStack set(ItemStack stack, String name, Object value) {
		initNBTTagCompound(stack);
		NBTHelper.set(stack.getTagCompound(), name, value);
		return stack;
	}

	public static <T> List<T> getList(ItemStack stack, String name, Class<T> clazz) {
		return NBTHelper.getList(stack.getTagCompound(), name, clazz);
	}

	public static <T> Optional<List<T>> getListSafe(ItemStack stack, String name, Class<T> clazz) {
		return NBTHelper.getListSafe(stack.getTagCompound(), name, clazz);
	}

	public static ItemStack setList(ItemStack stack, String name, List<?> values) {
		initNBTTagCompound(stack);
		NBTHelper.setList(stack.getTagCompound(), name, values);
		return stack;
	}

	public static <K, V> Map<K, V> getMap(ItemStack stack, String name, Class<K> keyClazz, Class<V> valClazz) {
		return NBTHelper.getMap(stack.getTagCompound(), name, keyClazz, valClazz);
	}

	public static <K, V> Optional<Map<K, V>> getMapSafe(ItemStack stack, String name, Class<K> keyClazz, Class<V> valClazz) {
		return NBTHelper.getMapSafe(stack.getTagCompound(), name, keyClazz, valClazz);
	}

	public static ItemStack setMap(ItemStack stack, String name, Map<?, ?> values) {
		initNBTTagCompound(stack);
		NBTHelper.setMap(stack.getTagCompound(), name, values);
		return stack;
	}

}
