package mrriegel.limelib.helper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.util.Utils;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryEntry.Impl;

@EventBusSubscriber(modid = LimeLib.MODID)
public class RegistryHelper {
	private static final List<IForgeRegistryEntry<?>> entries = Lists.newArrayList();
	private static final Map<Pair<Item, Integer>, ModelResourceLocation> models = Maps.newHashMap();

	@SubscribeEvent
	public static <T extends IForgeRegistryEntry<T>> void registerEvent(@SuppressWarnings("rawtypes") RegistryEvent.Register event) {
		Class<?> clazz = (Class<?>) event.getGenericType();
		Iterator<IForgeRegistryEntry<?>> it = entries.iterator();
		while (it.hasNext()) {
			IForgeRegistryEntry<?> entry = it.next();
			if (clazz.isAssignableFrom(entry.getClass())) {
				event.getRegistry().register(entry);
				it.remove();
			}
		}
	}

	@SubscribeEvent
	public static void registerModelEvent(ModelRegistryEvent event) {
		for (Map.Entry<Pair<Item, Integer>, ModelResourceLocation> e : models.entrySet()) {
			ModelLoader.setCustomModelResourceLocation(e.getKey().getKey(), e.getKey().getValue(), e.getValue());
		}
	}

	//TODO add return entry
	public static <T extends IForgeRegistryEntry<T>> void register(IForgeRegistryEntry<T> entry) {
		entries.add(entry);
	}

	public static <T extends IForgeRegistryEntry<T>> void unregister(IForgeRegistryEntry<T> entry) {
		entries.remove(entry);
	}

	public static void initModel(Item item, int meta, ModelResourceLocation mrl) {
		Validate.isTrue(item != null && item != Items.AIR);
		models.put(Pair.of(item, meta), mrl);
	}

	public static void setRegistryName(Impl<?> entry, ResourceLocation rl) {
		if (Utils.getCurrentModID().equals(rl.getNamespace()))
			entry.setRegistryName(rl);
		else
			ReflectionHelper.setPrivateValue(Impl.class, entry, rl, "registryName");
	}

}
