package mrriegel.limelib.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.util.GlobalBlockPos;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.limelib.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class NBTHelper {

	private static Set<INBTable<?>> iNBTs = new ReferenceOpenHashSet<>();

	static {
		//TODO init;
		NBTType.BLOCKPOS.classValid(Object.class);
	}

	public static boolean hasTag(NBTTagCompound nbt, String keyName) {
		return nbt != null && nbt.hasKey(keyName);
	}

	public static NBTTagCompound removeTag(NBTTagCompound nbt, String keyName) {
		if (nbt == null)
			return nbt;
		nbt.removeTag(keyName);
		return nbt;
	}

	public static <T extends NBTBase> Optional<T> getTagOptional(NBTTagCompound nbt, String name) {
		return hasTag(nbt, name) ? (Optional<T>) Optional.of(nbt.getTag(name)) : Optional.empty();
	}

	public static <T extends NBTBase> Optional<T> getTagOptional(NBTTagCompound nbt, String name, Class<T> clazz) {
		if (!hasTag(nbt, name))
			return Optional.empty();
		NBTBase n = nbt.getTag(name);
		if (clazz.isAssignableFrom(n.getClass()))
			return (Optional<T>) Optional.of(n);
		else
			return Optional.empty();
	}

	public static String toASCIIString(NBTTagCompound nbt) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			CompressedStreamTools.write(nbt, dos);
		} catch (IOException e) {
		}
		return Utils.toASCII(baos.toString());
	}

	public static NBTTagCompound fromASCIIString(String s) {
		ByteArrayInputStream bais = new ByteArrayInputStream(Utils.fromASCII(s).getBytes());
		DataInputStream dis = new DataInputStream(bais);
		try {
			return CompressedStreamTools.read(dis);
		} catch (IOException e) {
			return null;
		}
	}

	public static int getSize(NBTTagCompound nbt) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			CompressedStreamTools.write(nbt, dos);
		} catch (IOException e) {
		}
		return baos.size();
	}

	@Deprecated
	public static void register(INBTable<?> n) {
		Validate.isTrue(!Loader.instance().hasReachedState(LoaderState.AVAILABLE), "register before");
		if (!iNBTs.contains(n)) {
			iNBTs.add(n);
		}
	}

	public static interface INBTable<T> {

		boolean classValid(Class<?> clazz);

		NBTBase toNBT(T value);

		T toValue(NBTBase nbt, Class<? extends T> clazz);

		default T defaultValue(Class<? extends T> clazz) {
			return null;
		}

	}

	private static Map<Class<?>, INBTable<?>> cache = new IdentityHashMap<>();

	private static INBTable<?> getINBT(Class<?> clazz) {
		if (cache.containsKey(clazz))
			return cache.get(clazz);
		Set<INBTable<?>> ns = new ReferenceOpenHashSet<>();
		for (INBTable<?> n : iNBTs) {
			if (n.classValid(clazz))
				ns.add(n);
		}

		Validate.isTrue(ns.size() < 2, "too many ways found " + ns);
		INBTable<?> ret = null;
		ret = iNBTs.stream().filter(n -> n.classValid(clazz)).findFirst().orElse(null);
		//		for (INBTable<?> n : iNBTs)
		//			if (n.classValid(clazz)) {
		//				ret = n;
		//				break;
		//			}
		cache.put(clazz, ret);
		return ret;
	}

	static {
		register(new INBTable<Object>() {
			private Object2ByteOpenHashMap<Object> class2id = new Object2ByteOpenHashMap<>();
			{
				byte id = 1;
				class2id.put(boolean.class, id++);
				class2id.put(Boolean.class, id++);
				class2id.put(byte.class, id++);
				class2id.put(Byte.class, id++);
				class2id.put(char.class, id++);
				class2id.put(Character.class, id++);
				class2id.put(short.class, id++);
				class2id.put(Short.class, id++);
				class2id.put(int.class, id++);
				class2id.put(Integer.class, id++);
				class2id.put(float.class, id++);
				class2id.put(Float.class, id++);
				class2id.put(long.class, id++);
				class2id.put(Long.class, id++);
				class2id.put(double.class, id++);
				class2id.put(Double.class, id++);
			}

			@Override
			public boolean classValid(Class<?> clazz) {
				return class2id.containsKey(clazz);
			}

			@Override
			public NBTBase toNBT(Object value) {
				switch (class2id.getByte(value.getClass())) {
				case 0:
					throw new RuntimeException("primitives1");
				case 1:
				case 2:
					return new NBTTagByte((byte) (((boolean) value) ? 1 : 0));
				case 3:
				case 4:
					return new NBTTagByte((byte) value);
				case 5:
				case 6:
					return new NBTTagShort((short) (((int) value) + Short.MIN_VALUE));
				case 7:
				case 8:
					return new NBTTagShort((short) value);
				case 9:
				case 10:
					return new NBTTagInt((int) value);
				case 11:
				case 12:
					return new NBTTagInt(Float.floatToRawIntBits((float) value));
				case 13:
				case 14:
					return new NBTTagLong((long) value);
				case 15:
				case 16:
					return new NBTTagLong(Double.doubleToRawLongBits((double) value));
				default:
					throw new RuntimeException("primitives2");
				}
			}

			@Override
			public Object toValue(NBTBase nbt, Class<? extends Object> clazz) {
				switch (class2id.getByte(clazz)) {
				case 0:
					throw new RuntimeException("primitives1");
				case 1:
				case 2:
					return ((NBTTagByte) nbt).getByte() != 0;
				case 3:
				case 4:
					return ((NBTTagByte) nbt).getByte();
				case 5:
				case 6:
					return (char) ((((NBTTagShort) nbt).getShort()) - Short.MIN_VALUE);
				case 7:
				case 8:
					return ((NBTTagShort) nbt).getShort();
				case 9:
				case 10:
					return ((NBTTagInt) nbt).getInt();
				case 11:
				case 12:
					return Float.intBitsToFloat(((NBTTagInt) nbt).getInt());
				case 13:
				case 14:
					return ((NBTTagLong) nbt).getLong();
				case 15:
				case 16:
					return Double.longBitsToDouble(((NBTTagLong) nbt).getLong());
				default:
					throw new RuntimeException("primitives2");
				}
			}

			@Override
			public Object defaultValue(Class<? extends Object> clazz) {
				switch (class2id.getByte(clazz)) {
				case 0:
					throw new RuntimeException("primitives1");
				case 1:
				case 2:
					return false;
				case 3:
				case 4:
					return (byte) 0;
				case 5:
				case 6:
					return (char) 0;
				case 7:
				case 8:
					return (short) 0;
				case 9:
				case 10:
					return 0;
				case 11:
				case 12:
					return 0F;
				case 13:
				case 14:
					return 0L;
				case 15:
				case 16:
					return 0D;
				default:
					throw new RuntimeException("primitives2");
				}
			}

		});
		INBTable<Object> nn;
		register(nn = new INBTable<Object>() {
			private Object2ByteOpenHashMap<Object> class2id = new Object2ByteOpenHashMap<>();
			{
				byte id = 1;
				class2id.put(boolean[].class, id++);
				class2id.put(byte[].class, id++);
				class2id.put(char[].class, id++);
				class2id.put(short[].class, id++);
				class2id.put(int[].class, id++);
				class2id.put(float[].class, id++);
				class2id.put(long[].class, id++);
				class2id.put(double[].class, id++);
			}

			@Override
			public boolean classValid(Class<?> clazz) {
				return class2id.containsKey(clazz);
			}

			@Override
			public NBTBase toNBT(Object value) {
				switch (class2id.getByte(value.getClass())) {
				case 0:
					throw new RuntimeException("arrays1");
				case 1:
					boolean[] arrB = (boolean[]) value;
					BitSet bs = new BitSet(arrB.length);
					for (int i = 0; i < arrB.length; i++)
						bs.set(i, arrB[i]);
					NBTTagCompound nbt = new NBTTagCompound();
					if (arrB.length % 8 != 0)
						if (arrB.length <= Byte.MAX_VALUE) {
							nbt.setByte("s", (byte) arrB.length);
						} else if (arrB.length <= Short.MAX_VALUE) {
							nbt.setShort("s", (short) arrB.length);
						} else {
							nbt.setInteger("s", arrB.length);
						}
					nbt.setTag("v", new NBTTagByteArray(bs.toByteArray()));
					return nbt;
				case 2:
					return new NBTTagByteArray((byte[]) value);
				case 3:
					return new NBTTagString(new String((char[]) value));
				case 4:
					if (!"".isEmpty()) {
						short[] arrS = (short[]) value;
						ByteBuffer bb = ByteBuffer.allocate((int) Math.ceil(arrS.length / 2.));
						for (short s : arrS)
							bb.putShort(s);
						NBTTagCompound nbtt = new NBTTagCompound();
						nbtt.setByteArray("v", bb.array());
						nbtt.setInteger("s", arrS.length);
						return nbtt;
					}
					short[] arrS = (short[]) value;
					int[] retS = new int[arrS.length];
					for (int i = 0; i < arrS.length; i++)
						retS[i] = arrS[i];
					return new NBTTagIntArray(retS);
				case 5:
					return new NBTTagIntArray((int[]) value);
				case 6:
					float[] arrF = (float[]) value;
					int[] retF = new int[arrF.length];
					for (int i = 0; i < arrF.length; i++)
						retF[i] = Float.floatToRawIntBits(arrF[i]);
					return new NBTTagIntArray(retF);
				case 7:
					return new NBTTagLongArray((long[]) value);
				case 8:
					double[] arrD = (double[]) value;
					long[] retD = new long[arrD.length];
					for (int i = 0; i < arrD.length; i++)
						retD[i] = Double.doubleToRawLongBits(arrD[i]);
					return new NBTTagLongArray(retD);
				default:
					throw new RuntimeException("arrays2");
				}
			}

			@Override
			public Object toValue(NBTBase nbt, Class<? extends Object> clazz) {
				switch (class2id.getByte(clazz)) {
				case 0:
					throw new RuntimeException("arrays1");
				case 1:
					NBTTagCompound tag = (NBTTagCompound) nbt;
					byte[] arr = tag.getByteArray("v");
					int num = tag.hasKey("s") ? tag.getInteger("s") : arr.length << 3;
					BitSet bs = BitSet.valueOf(arr);
					boolean[] ret = new boolean[num];
					for (int i = 0; i < num; i++) {
						ret[i] = bs.get(i);
						//TODO test
					}
					return ret;
				case 2:
					return ((NBTTagByteArray) nbt).getByteArray();
				case 3:
					return ((NBTTagString) nbt).getString().toCharArray();
				case 4:
					int[] arrS = ((NBTTagIntArray) nbt).getIntArray();
					short[] retS = new short[arrS.length];
					for (int i = 0; i < arrS.length; i++)
						retS[i] = (short) arrS[i];
					return retS;
				case 5:
					return ((NBTTagIntArray) nbt).getIntArray();
				case 6:
					int[] arrF = ((NBTTagIntArray) nbt).getIntArray();
					float[] retF = new float[arrF.length];
					for (int i = 0; i < arrF.length; i++)
						retF[i] = Float.intBitsToFloat(arrF[i]);
					return retF;
				case 7:
					return getLongArray((NBTTagLongArray) nbt);
				case 8:
					long[] arrD = getLongArray((NBTTagLongArray) nbt);
					double[] retD = new double[arrD.length];
					for (int i = 0; i < arrD.length; i++)
						retD[i] = Double.longBitsToDouble(arrD[i]);
					return retD;
				default:
					throw new RuntimeException("arrays2");
				}
			}

			@Override
			public Object defaultValue(Class<? extends Object> clazz) {
				switch (class2id.getByte(clazz)) {
				case 0:
					throw new RuntimeException("arrays1");
				case 1:
					return new boolean[0];
				case 2:
					return new byte[0];
				case 3:
					return new char[0];
				case 4:
					return new short[0];
				case 5:
					return new int[0];
				case 6:
					return new float[0];
				case 7:
					return new long[0];
				case 8:
					return new double[0];
				default:
					throw new RuntimeException("arrays2");
				}
			}
		});
		Random ran = new Random();
		for (int i = 0; i < 20 && false; i++) {
			int[] a1 = ran.ints((ran.nextInt(3) + 1) * 8).toArray();
			boolean[] r1 = new boolean[a1.length];
			for (int j = 0; j < a1.length; j++)
				r1[j] = a1[j] % 2 == 0;
			System.out.println("-------------------------");
			System.out.println(Arrays.toString(r1));
			NBTBase nb = nn.toNBT(r1);
			boolean[] r2 = (boolean[]) nn.toValue(nb, boolean[].class);
			System.out.println(Arrays.toString(r2));
			System.out.println(nb);
		}
		if (!true)
			System.out.println(5 / 0);
		register(of(null, (n, c) -> {
			Object[] enums = c.getEnumConstants();
			int index = ((NBTTagShort) n).getShort();
			if (index < 0 || index >= enums.length) {
				LimeLib.log.warn("index " + index + " is out of range for " + c.getName());
				return null;
			}
			return enums[index];
		}, v -> new NBTTagShort((short) ((Enum<?>) v).ordinal()), c -> c.isEnum()));
		register(of(null, (n, c) -> {
			NBTTagCompound entry = (NBTTagCompound) n;
			String id = entry.getString("id"), clas = entry.getString("class");
			try {
				@SuppressWarnings("rawtypes")
				IForgeRegistry<?> reg = GameRegistry.findRegistry((Class<IForgeRegistryEntry>) Class.forName(clas));
				if (reg != null) {
					return reg.getValue(new ResourceLocation(id));
				}
			} catch (ClassNotFoundException e) {
				return null;
			}
			return null;
		}, v -> {
			NBTTagCompound entry = new NBTTagCompound();
			entry.setString("id", v.getRegistryName().toString());
			entry.setString("class", v.getRegistryType().getCanonicalName());
			return entry;
		}, c -> IForgeRegistryEntry.class.isAssignableFrom(c)));
		//		register(of(null, (n, s) -> n.getCompoundTag(s), v -> v, NBTTagCompound.class));
		register(of(null, (n, c) -> n, v -> v, c -> NBTBase.class.isAssignableFrom(c)));
		register(of(null, (n, c) -> BlockPos.fromLong(((NBTTagLong) n).getLong()), v -> new NBTTagLong(v.toLong()), BlockPos.class, MutableBlockPos.class));
		register(of(null, (n, c) -> {
			long[] arr = getLongArray((NBTTagLongArray) n);
			return new GlobalBlockPos(BlockPos.fromLong(arr[0]), (int) arr[1]);
		}, v -> new NBTTagLongArray(new long[] { v.getPos().toLong(), v.getDimension() }), GlobalBlockPos.class));
		register(of(c -> ItemStack.EMPTY, (n, c) -> new ItemStack((NBTTagCompound) n), v -> v.writeToNBT(new NBTTagCompound()), ItemStack.class));
		//TODO remove (oder nich wegen list)
		register(of(null, (n, c) -> {
			long[] l = getLongArray((NBTTagLongArray) n);
			return new UUID(l[0], l[1]);
		}, v -> new NBTTagLongArray(new long[] { v.getMostSignificantBits(), v.getLeastSignificantBits() }), UUID.class));
		register(of(null, (n, c) -> new ResourceLocation(((NBTTagString) n).getString()), v -> new NBTTagString(v.toString()), ResourceLocation.class));
		register(of(null, (n, c) -> FluidStack.loadFluidStackFromNBT((NBTTagCompound) n), v -> v.writeToNBT(new NBTTagCompound()), FluidStack.class));
		register(of(c -> "", (n, c) -> ((NBTTagString) n).getString(), v -> new NBTTagString(v), String.class));
		register(of(null, (n, c) -> StackWrapper.loadStackWrapperFromNBT((NBTTagCompound) n), v -> v.writeToNBT(new NBTTagCompound()), StackWrapper.class));
		//		for (NBTType t : NBTType.values())
		//			register(of(t.defaultValue, t.getter, t.setter, t.classes));
	}

	public static <T> INBTable<T> of(@Nullable Function<Class<? extends T>, T> defaultValue, BiFunction<NBTBase, Class<?>, T> getter, Function<T, NBTBase> setter, Class<?>... classes) {
		return of(defaultValue, getter, setter, clazz -> ArrayUtils.contains(classes, clazz));
	}

	public static <T> INBTable<T> of(@Nullable Function<Class<? extends T>, T> defaultValue, BiFunction<NBTBase, Class<?>, T> getter, Function<T, NBTBase> setter, Predicate<Class<?>> pred) {
		return new INBTable<T>() {

			@Override
			public boolean classValid(Class<?> clazz) {
				return pred.apply(clazz);
			}

			@Override
			public T defaultValue(Class<? extends T> clazz) {
				return defaultValue != null ? defaultValue.apply(clazz) : null;
			}

			@Override
			public NBTBase toNBT(T value) {
				return setter.apply(value);
			}

			@Override
			public T toValue(NBTBase nbt, Class<? extends T> clazz) {
				try {
					return getter.apply(nbt, clazz);
				} catch (ClassCastException e) {
					return defaultValue(clazz);
				}
			}

		};
	}

	private enum NBTType/**/ {
		BOOLEAN(false, (n, s) -> n.getBoolean(s), (n, p) -> n.setBoolean(p.getKey(), (boolean) p.getValue()), Boolean.class, boolean.class), //
		BYTE((byte) 0, (n, s) -> n.getByte(s), (n, p) -> n.setByte(p.getKey(), (byte) p.getValue()), Byte.class, byte.class), //
		SHORT((short) 0, (n, s) -> n.getShort(s), (n, p) -> n.setShort(p.getKey(), (short) p.getValue()), Short.class, short.class), //
		INT(0, (n, s) -> n.getInteger(s), (n, p) -> n.setInteger(p.getKey(), (int) p.getValue()), Integer.class, int.class), //
		LONG(0L, (n, s) -> n.getLong(s), (n, p) -> n.setLong(p.getKey(), (long) p.getValue()), Long.class, long.class), //
		FLOAT(0F, (n, s) -> n.getFloat(s), (n, p) -> n.setFloat(p.getKey(), (float) p.getValue()), Float.class, float.class), //
		DOUBLE(0D, (n, s) -> n.getDouble(s), (n, p) -> n.setDouble(p.getKey(), (double) p.getValue()), Double.class, double.class), //
		STRING(null, (n, s) -> n.getString(s), (n, p) -> n.setString(p.getKey(), (String) p.getValue()), String.class), //
		NBT(null, (n, s) -> n.getCompoundTag(s), (n, p) -> n.setTag(p.getKey(), (NBTTagCompound) p.getValue()), NBTTagCompound.class), //
		ITEMSTACK(ItemStack.EMPTY, (n, s) -> new ItemStack(n.getCompoundTag(s)), (n, p) -> n.setTag(p.getKey(), ((ItemStack) p.getValue()).writeToNBT(new NBTTagCompound())), ItemStack.class), //
		BLOCKPOS(null, (n, s) -> BlockPos.fromLong(n.getLong(s)), (n, p) -> n.setLong(p.getKey(), ((BlockPos) p.getValue()).toLong()), BlockPos.class, MutableBlockPos.class), //
		FLUIDSTACK(null, (n, s) -> FluidStack.loadFluidStackFromNBT(n.getCompoundTag(s)), (n, p) -> n.setTag(p.getKey(), ((FluidStack) p.getValue()).writeToNBT(new NBTTagCompound())), FluidStack.class);
		Object defaultValue;
		Class<?>[] classes;
		BiFunction<NBTTagCompound, String, Object> getter;
		BiConsumer<NBTTagCompound, Pair<String, Object>> setter;

		private NBTType(Object defaultValue, BiFunction<NBTTagCompound, String, Object> getter, BiConsumer<NBTTagCompound, Pair<String, Object>> setter, Class<?>... classes) {
			this.defaultValue = defaultValue;
			this.classes = classes;
			this.getter = getter;
			this.setter = setter;
		}

		public static Map<Class<?>, NBTType> m = new HashMap<>();

		public static boolean validClass(Class<?> clazz) {
			return Enum.class.isAssignableFrom(clazz) || m.get(clazz) != null;
		}

		static {
			for (NBTType n : NBTType.values()) {
				for (Class<?> c : n.classes)
					m.put(c, n);
				//				register(n);
			}
		}

		public void set(NBTTagCompound nbt, String name, Object value) {
			setter.accept(nbt, Pair.of(name, value));
		}

		public Object get(NBTTagCompound nbt, String name, Class<?> clazz) {
			return getter.apply(nbt, name);
		}

		public boolean classValid(Class<?> clazz) {
			return m.get(clazz) != null;
		}

		public Object defaultValue() {
			return defaultValue;
		}
	}

	public static boolean getBoolean(NBTTagCompound nbt, String name) {
		return Objects.requireNonNull(nbt).getBoolean(name);
	}

	public static NBTTagCompound setBoolean(NBTTagCompound nbt, String name, boolean value) {
		Objects.requireNonNull(nbt).setBoolean(name, value);
		return nbt;
	}

	public static <T> T get(NBTTagCompound nbt, String name, Class<T> clazz) {
		//		if(!"".isEmpty())
		//			return getSafe(nbt, name, clazz).orElse(getINBT(clazz).defaultValue(clazz));
		if (Enum.class.isAssignableFrom(clazz)) {
			Optional<Integer> o = getSafe(nbt, name, Integer.class);
			return o.isPresent() ? clazz.getEnumConstants()[o.get()] : null;
		}
		NBTType type = NBTType.m.get(clazz);
		if (type == null)
			throw new IllegalArgumentException();
		if (nbt == null || !nbt.hasKey(name))
			return (T) type.defaultValue();
		return (T) type.get(nbt, name, clazz);
	}

	@SuppressWarnings("unused")
	@Deprecated
	//TODO add getoptional
	private static <T> T get(NBTTagCompound nbt, String name) {
		try {
			return (T) get(nbt, name, Class.forName(nbt.getString(name + "_clazz")));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	//TODO rename to getOptional
	public static <T> Optional<T> getSafe(NBTTagCompound nbt, String name, Class<T> clazz) {
		if (!"".isEmpty()) {
			if (!nbt.hasKey(name))
				return Optional.empty();
			if (clazz == UUID.class)
				return (Optional<T>) Optional.of(nbt.getUniqueId(name));
			INBTable n = getINBT(clazz);
			if (n == null)
				throw new IllegalArgumentException("No type found for " + clazz.getName());
			return (Optional<T>) Optional.of(n.toValue(nbt.getTag(name), clazz));
		}
		if (nbt == null || nbt.hasKey(name))
			return Optional.of(get(nbt, name, clazz));
		return Optional.empty();
	}

	public static NBTTagCompound set(NBTTagCompound nbt, String name, Object value) {
		if (nbt == null || value == null)
			return nbt;
		if (!"".isEmpty()) {
			if (value.getClass() == UUID.class) {
				nbt.setUniqueId(name, (UUID) value);
				return nbt;
			}
			INBTable n = getINBT(value.getClass());
			if (n == null)
				throw new IllegalArgumentException("No type found for " + value.getClass().getName());
			nbt.setTag(name, n.toNBT(value));
		}
		Class<?> clazz = value.getClass();
		if (Enum.class.isAssignableFrom(clazz))
			return set(nbt, name, ((Enum<?>) value).ordinal());
		NBTType type = NBTType.m.get(clazz);
		if (type == null)
			throw new IllegalArgumentException();
		type.set(nbt, name, value);
		//		nbt.setString(name + "_clazz", clazz.getName());
		return nbt;
	}

	@SuppressWarnings("unused")
	@Deprecated
	private static <T, C extends Collection<T>> C getCollection(NBTTagCompound nbt, String name, Class<T> clazz, Supplier<C> supp) {
		return (C) getList(nbt, name, clazz);
	}

	public static <T> List<T> getList(NBTTagCompound nbt, String name, Class<T> clazz) {
		if (!"".isEmpty()) {
			if (nbt.hasKey(name + "_n0llNum", 3)) {
				return new ArrayList<>(Collections.nCopies(nbt.getInteger(name + "_n0llNum"), null));
			}
			INBTable n = getINBT(clazz);
			if (n == null) {
				LimeLib.log.warn("no nbt way found.");
				return Collections.emptyList();
			}
			Collection<T> values = new ArrayList<>();
			if (!"".isEmpty()) {
				if (clazz == boolean.class || clazz == Boolean.class) {
					values = (List<T>) new BooleanArrayList();
				} else if (clazz == byte.class || clazz == Byte.class) {
					values = (List<T>) new ByteArrayList();
				} else if (clazz == char.class || clazz == Character.class) {
					values = (List<T>) new CharArrayList();
				} else if (clazz == short.class || clazz == Short.class) {
					values = (List<T>) new ShortArrayList();
				} else if (clazz == int.class || clazz == Integer.class) {
					values = (List<T>) new IntArrayList();
				} else if (clazz == float.class || clazz == Float.class) {
					values = (List<T>) new FloatArrayList();
				} else if (clazz == long.class || clazz == Long.class) {
					values = (List<T>) new LongArrayList();
				} else if (clazz == double.class || clazz == Double.class) {
					values = (List<T>) new DoubleArrayList();
				} else
					values = new ArrayList<>();
			}
			NBTBase coll = nbt.getTag(name);
			if (coll instanceof NBTTagByteArray) {
				byte[] arr = ((NBTTagByteArray) coll).getByteArray();
				for (byte b : arr)
					values.add((T) n.toValue(new NBTTagByte(b), clazz));
			} else if (coll instanceof NBTTagIntArray) {
				int[] arr = ((NBTTagIntArray) coll).getIntArray();
				byte type = nbt.getByte(name + "_type");
				for (int i : arr) {
					switch (type) {
					case 2:
						values.add((T) n.toValue(new NBTTagShort((short) i), clazz));
						break;
					case 3:
						values.add((T) n.toValue(new NBTTagInt(i), clazz));
						break;
					case 5:
						values.add((T) n.toValue(new NBTTagFloat(Float.intBitsToFloat(i)), clazz));
						break;
					default:
						throw new RuntimeException("call da police");
					}
				}
			} else if (coll instanceof NBTTagLongArray) {
				long[] arr = getLongArray((NBTTagLongArray) coll);
				byte type = nbt.getByte(name + "_type");
				for (long l : arr) {
					switch (type) {
					case 4:
						values.add((T) n.toValue(new NBTTagLong(l), clazz));
						break;
					case 6:
						values.add((T) n.toValue(new NBTTagDouble(Double.longBitsToDouble(l)), clazz));
						break;
					default:
						throw new RuntimeException("call da police");
					}
				}

			} else if (coll instanceof NBTTagList) {
				for (NBTBase nb : (NBTTagList) coll) {
					values.add((T) n.toValue(nb, clazz));
				}
			} else
				throw new RuntimeException("something went really wrong");
			if (nbt.hasKey(name + "_n0lls", 11)) {
				int[] nulls = nbt.getIntArray(name + "_n0lls");
				//TODO support queue
				if (values instanceof List)
					for (int i : nulls) {
						((List<T>) values).add(i, null);
					}
				else
					values.addAll(Collections.nCopies(nulls.length, null));
			}
			return (List<T>) values;

		}
		if (!NBTType.validClass(clazz))
			throw new IllegalArgumentException();
		List<T> values = new ObjectArrayList<>();
		if (nbt == null || !nbt.hasKey(name, 10))
			return values;
		NBTTagCompound lis = nbt.getCompoundTag(name);
		int size = lis.getInteger("size");
		for (int i = 0; i < size; i++)
			values.add(get(lis, "__" + i, clazz));
		return values;
	}

	public static <T> Optional<List<T>> getListSafe(NBTTagCompound nbt, String name, Class<T> clazz) {
		if (nbt == null || nbt.hasKey(name, 10))
			return Optional.of(getList(nbt, name, clazz));
		return Optional.empty();
	}

	//TODO change parameter to collection
	public static NBTTagCompound setList(NBTTagCompound nbt, String name, List<?> values) {
		if (nbt == null || values.isEmpty())
			return nbt;
		if (!"".isEmpty()) {
			List<INBTable> nbts = values.stream().filter(Objects::nonNull).map(o -> Objects.requireNonNull(getINBT(o.getClass()), "Cannot add " + o.getClass().getName() + " object to NBT.")).distinct().collect(Collectors.toList());
			if (nbts.isEmpty()) {
				//only nulls
				nbt.setInteger(name + "_n0llNum", values.size());
				return nbt;
			}
			if (nbts.size() != 1)
				throw new IllegalArgumentException("Cannot add list with multiple types " + values.stream().filter(Objects::nonNull).map(o -> o.getClass().getName()).distinct().collect(Collectors.toList()) + " to NBT.");
			IntArrayList nulls = new IntArrayList();
			int k = 0;
			for (Object o : values) {
				if (o == null)
					nulls.add(k);
				k++;
			}
			List<?> nonNullValues;
			if (!nulls.isEmpty()) {
				nbt.setIntArray(name + "_n0lls", nulls.toIntArray());
				nonNullValues = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
			} else
				nonNullValues = new ArrayList<>(values);
			INBTable n = nbts.get(0);
			List<NBTBase> nbtBases = nonNullValues.stream().map(v -> n.toNBT(v)).collect(Collectors.toList());
			NBTBase base = n.toNBT(nonNullValues.get(0));
			if (base instanceof NBTTagByte) {
				byte[] arr = new byte[nonNullValues.size()];
				for (int i = 0; i < nonNullValues.size(); i++)
					arr[i] = ((NBTTagByte) n.toNBT(nonNullValues.get(i))).getByte();
				nbt.setByteArray(name, arr);
			} else if (base instanceof NBTTagShort || base instanceof NBTTagInt || base instanceof NBTTagFloat) {
				int[] arr = new int[nonNullValues.size()];
				for (int i = 0; i < nonNullValues.size(); i++)
					if (base instanceof NBTTagShort || base instanceof NBTTagInt)
						arr[i] = ((NBTPrimitive) n.toNBT(nonNullValues.get(i))).getInt();
					else
						arr[i] = Float.floatToRawIntBits(((NBTTagFloat) n.toNBT(nonNullValues.get(i))).getFloat());
				nbt.setIntArray(name, arr);
				nbt.setByte(name + "_type", base.getId());
			} else if (base instanceof NBTTagLong || base instanceof NBTTagDouble) {
				long[] arr = new long[nonNullValues.size()];
				for (int i = 0; i < nonNullValues.size(); i++)
					if (base instanceof NBTTagLong)
						arr[i] = ((NBTPrimitive) n.toNBT(nonNullValues.get(i))).getLong();
					else
						arr[i] = Double.doubleToRawLongBits(((NBTTagDouble) n.toNBT(nonNullValues.get(i))).getDouble());
				nbt.setTag(name, new NBTTagLongArray(arr));
				nbt.setByte(name + "_type", base.getId());
			} else {
				NBTTagList arr = new NBTTagList();
				for (int i = 0; i < nonNullValues.size(); i++)
					arr.appendTag(n.toNBT(nonNullValues.get(i)));
				nbt.setTag(name, arr);
			}
			return nbt;
		}
		for (Object o : values)
			if (o != null) {
				if (!NBTType.validClass(o.getClass()))
					throw new IllegalArgumentException();
				break;
			}
		NBTTagCompound lis = new NBTTagCompound();
		lis.setInteger("size", values.size());
		for (int i = 0; i < values.size(); i++)
			set(lis, "__" + i, values.get(i));
		nbt.setTag(name, lis);
		return nbt;
	}

	@SuppressWarnings("unused")
	@Deprecated
	private static <K, V> Map<K, V> getMap(NBTTagCompound nbt, String name, Class<K> keyClazz, Class<V> valClazz, Supplier<Map<K, V>> supp) {
		return getMap(nbt, name, keyClazz, valClazz);
	}

	public static <K, V> Map<K, V> getMap(NBTTagCompound nbt, String name, Class<K> keyClazz, Class<V> valClazz) {
		if (!NBTType.validClass(keyClazz) || !NBTType.validClass(valClazz))
			throw new IllegalArgumentException();
		Map<K, V> values = new HashMap<>();
		if (!"".isEmpty()) {
			if (keyClazz == boolean.class || keyClazz == Boolean.class) {
				values = new Object2ObjectArrayMap<>(2);
			} else if (keyClazz == byte.class || keyClazz == Byte.class) {
				values = (Map<K, V>) new Byte2ObjectOpenHashMap<>();
			} else if (keyClazz == char.class || keyClazz == Character.class) {
				values = (Map<K, V>) new Char2ObjectOpenHashMap<>();
			} else if (keyClazz == short.class || keyClazz == Short.class) {
				values = (Map<K, V>) new Short2ObjectOpenHashMap<>();
			} else if (keyClazz == int.class || keyClazz == Integer.class) {
				values = (Map<K, V>) new Int2ObjectOpenHashMap<>();
			} else if (keyClazz == float.class || keyClazz == Float.class) {
				values = (Map<K, V>) new Float2ObjectOpenHashMap<>();
			} else if (keyClazz == long.class || keyClazz == Long.class) {
				values = (Map<K, V>) new Long2ObjectOpenHashMap<>();
			} else if (keyClazz == double.class || keyClazz == Double.class) {
				values = (Map<K, V>) new Double2ObjectOpenHashMap<>();
			} else
				values = new HashMap<>();
		}
		if (nbt == null || !nbt.hasKey(name, 10))
			return values;
		NBTTagCompound map = nbt.getCompoundTag(name);
		List<K> keys = getList(map, "key", keyClazz);
		List<V> vals = getList(map, "value", valClazz);
		Validate.isTrue(keys.size() == vals.size());
		for (int i = 0; i < keys.size(); i++)
			values.put(keys.get(i), vals.get(i));
		return values;
	}

	public static <K, V> Optional<Map<K, V>> getMapSafe(NBTTagCompound nbt, String name, Class<K> keyClazz, Class<V> valClazz) {
		if (nbt == null || nbt.hasKey(name, 10))
			return Optional.of(getMap(nbt, name, keyClazz, valClazz));
		return Optional.empty();
	}

	public static NBTTagCompound setMap(NBTTagCompound nbt, String name, Map<?, ?> values) {
		if (nbt == null || values.isEmpty())
			return nbt;
		List<Entry<?, ?>> entries = Lists.newArrayList();
		for (Entry<?, ?> o : values.entrySet()) {
			if (!NBTType.validClass(o.getKey().getClass()) || !NBTType.validClass(o.getValue().getClass()))
				throw new IllegalArgumentException();
			entries.add(o);
		}
		NBTTagCompound map = new NBTTagCompound();
		if (!"".isEmpty()) {
			setList(map, "key", new ArrayList<>(values.keySet()));
			setList(map, "value", new ArrayList<>(values.values()));
		}
		setList(map, "key", entries.stream().map(e -> e.getKey()).collect(Collectors.toList()));
		setList(map, "value", entries.stream().map(e -> e.getValue()).collect(Collectors.toList()));
		nbt.setTag(name, map);
		return nbt;
	}

	private static Field dataField = ReflectionHelper.findField(NBTTagLongArray.class, "data", "field_193587_b");

	private static long[] getLongArray(NBTTagLongArray nbt) {
		try {
			return (long[]) dataField.get(nbt);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return new long[0];
		}

	}

}
