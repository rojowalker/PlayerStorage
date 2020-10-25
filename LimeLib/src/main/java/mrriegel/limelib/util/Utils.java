package mrriegel.limelib.util;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.util.TypeAdapters.ItemLizer;
import mrriegel.limelib.util.TypeAdapters.ItemStackLizer;
import mrriegel.limelib.util.TypeAdapters.NBTLizer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class Utils {

	private static GsonBuilder gsonBuilder;
	private static Gson gson;

	public static void init() {
		//dummy
		Utils.class.hashCode();
	}

	public static Gson getGSON() {
		if (gson != null)
			return gson;
		if (gsonBuilder == null)
			registerDefaultAdapters();
		return gson = gsonBuilder.create();
	}

	private static void registerDefaultAdapters() {
		gsonBuilder = new GsonBuilder().setPrettyPrinting().//
				registerTypeAdapter(NBTTagCompound.class, new NBTLizer()).//
				registerTypeAdapter(Item.class, new ItemLizer()).//
				//				registerTypeAdapter(IForgeRegistryEntry.class, new RegistryEntryLizer()).//
				registerTypeAdapter(ItemStack.class, new ItemStackLizer());
	}

	public static void registerGsonAdapter(Type type, Object adapter) {
		getGSON();
		gsonBuilder.registerTypeAdapter(type, adapter);
		gson = null;
	}

	public static String getCurrentModID() {
		return GameData.checkPrefix("", false).getNamespace();
	}

	public static List<Integer> split(int ii, int splits) {
		IntArrayList ints = new IntArrayList();
		for (int i = 0; i < splits; i++)
			ints.add(ii / splits);
		for (int i = 0; i < ii % splits; i++)
			ints.set(i, ints.getInt(i) + 1);
		return ints;
	}

	public static String formatNumber(long value) {
		return formatNumber(value, false);
	}

	public static String formatNumber(long value, boolean period) {
		if (value == Long.MIN_VALUE)
			value++;
		long abs = Math.abs(value);
		if (abs < 1000)
			return value + "";
		StringBuilder sb = new StringBuilder();
		if (value < 0)
			sb.append('-');
		String absString = abs + "";
		int digits = ((absString.length() - 1) % 3) + 1;
		sb.append(absString.substring(0, digits));
		if (period && absString.charAt(digits) != '0')
			sb.append("." + absString.charAt(digits));
		if (abs < 1_000_000)
			sb.append("K");
		else if (abs < 1_000_000_000)
			sb.append("M");
		else if (abs < 1_000_000_000_000L)
			sb.append("G");
		else if (abs < 1_000_000_000_000_000L)
			sb.append("T");
		else if (abs < 1_000_000_000_000_000_000L)
			sb.append("P");
		else
			sb.append("E");
		return sb.toString();
	}

	private static UUID fakePlayerUUID = new UUID(0x672ec31127a5449eL, 0x925c69a55980d378L);

	public static FakePlayer getFakePlayer(WorldServer world) {
		UUID uu = fakePlayerUUID;
		while (world.getEntityFromUuid(uu) != null)
			uu = UUID.randomUUID();
		return FakePlayerFactory.get(world, new GameProfile(uu, LimeLib.MODID + "_fake_player"));
	}

	public static FakePlayer getFakePlayerWithItem(WorldServer world, ItemStack stack) {
		FakePlayer player = getFakePlayer(world);
		player.inventory.mainInventory.set((player.inventory.currentItem = 0), stack);
		return player;
	}

	public static String getModID(IForgeRegistryEntry<?> registerable) {
		final String modID = registerable.getRegistryName().getNamespace();
		ModContainer mod = Loader.instance().getIndexedModList().get(modID);
		if (mod == null) {
			for (String s : Loader.instance().getIndexedModList().keySet()) {
				if (s.equalsIgnoreCase(modID)) {
					mod = Loader.instance().getIndexedModList().get(s);
					break;
				}
			}
		}
		return mod != null ? mod.getModId() : "minecraft";
	}

	public static String getModName(IForgeRegistryEntry<?> registerable) {
		ModContainer m = Loader.instance().getIndexedModList().get(getModID(registerable));
		if (m != null)
			return m.getName();
		else
			return "Minecraft";
	}

	public static EntityPlayerMP getRandomPlayer() {
		List<WorldServer> lis = Lists.newArrayList(FMLCommonHandler.instance().getMinecraftServerInstance().worlds);
		if (lis.isEmpty())
			return null;
		Collections.shuffle(lis);
		for (WorldServer world : lis) {
			EntityPlayerMP player = getRandomPlayer(world);
			if (player != null)
				return player;
		}
		return null;
	}

	public static EntityPlayerMP getRandomPlayer(WorldServer world) {
		if (world.playerEntities.isEmpty())
			return null;
		return (EntityPlayerMP) world.playerEntities.get(world.rand.nextInt(world.playerEntities.size()));
	}

	public static String toASCII(String text) {
		return Base64.getEncoder().encodeToString(text.getBytes());
	}

	public static String fromASCII(String ascii) {
		return new String(Base64.getDecoder().decode(ascii.getBytes()));
	}

	public static void runNextTick(Runnable run, IThreadListener itl) {
		new Thread(() -> itl.addScheduledTask(run)).start();
	}

	private static Cache<String, Logger> loggers = CacheBuilder.newBuilder().build();

	public static Logger logger() {
		try {
			return loggers.get(getCurrentModID(), () -> LogManager.getLogger(getCurrentModID()));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

}
