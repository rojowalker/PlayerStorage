package mrriegel.playerstorage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;
import org.cyclops.commoncapabilities.api.capability.itemhandler.DefaultSlotlessItemHandlerWrapper;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ISlotlessItemHandler;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.util.GlobalBlockPos;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.Enums.Sort;
import mrriegel.playerstorage.gui.ContainerExI;
import mrriegel.playerstorage.registry.Registry;
import mrriegel.playerstorage.registry.TileInterface;
import mrriegel.playerstorage.registry.TileKeeper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketCollectItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

@EventBusSubscriber(modid = PlayerStorage.MODID)
public class ExInventory implements INBTSerializable<NBTTagCompound> {

	@Interface(iface = "org.cyclops.commoncapabilities.api.capability.itemhandler.ISlotlessItemHandler", modid = "commoncapabilities")
	public static class Handler implements IItemHandler, ISlotlessItemHandler {

		private final static Stream<ItemStack> func(StackWrapper s) {
			final int max = s.getStack().getMaxStackSize(), size = (int) Math.ceil(s.getSize() / (double) max);
			List<ItemStack> lis = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				if (i == size - 1) {
					lis.add(ItemHandlerHelper.copyStackWithSize(s.getStack(), s.getSize() - max * (size - 1)));
				} else {
					lis.add(ItemHandlerHelper.copyStackWithSize(s.getStack(), max));
				}
			}
			return lis.stream();
		}

		ExInventory ei;
		TileInterface tile;

		public Handler(@Nullable TileInterface tile) {
			ei = getInventory(tile.getPlayer());
			this.tile = tile;
		}

		@Override
		public ItemStack extractItem(int amount, boolean simulate) {
			if (!isPlayerOn())
				return ItemStack.EMPTY;
			return ei.extractItem(s -> !s.isEmpty(), amount, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (!isPlayerOn())
				return ItemStack.EMPTY;
			return ei.extractItem(getStackInSlot(slot), amount, simulate);
		}

		@Override
		public ItemStack extractItem(ItemStack matchStack, int matchFlags, boolean simulate) {
			if (!isPlayerOn())
				return ItemStack.EMPTY;
			return new DefaultSlotlessItemHandlerWrapper(this).extractItem(matchStack, matchFlags, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public int getSlots() {
			if (!isPlayerOn())
				return 0;
			return ei.items.size() + 2;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			if (!isPlayerOn())
				return ItemStack.EMPTY;
			if (ei.items.size() < slot || slot == 0)
				return ItemStack.EMPTY;
			StackWrapper w = ei.items.get(slot - 1);
			return ItemHandlerHelper.copyStackWithSize(w.getStack(), MathHelper.clamp(w.getSize(), 1, w.getStack().getMaxStackSize()));
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!isPlayerOn())
				return stack;
			return ei.insertItem(stack, simulate);
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			if (!isPlayerOn())
				return stack;
			return ei.insertItem(stack, simulate);
		}

		private boolean isPlayerOn() {
			return tile == null || tile.isOn();
		}

		private void refresh() {
			if (ei.itemlist == null) {
				ei.itemlist = ei.items.stream().flatMap(Handler::func).filter(st -> !st.isEmpty()).collect(Collectors.toList());
			}
		}

	}

	static class Provider implements ICapabilitySerializable<NBTTagCompound> {

		ExInventory ei = EXINVENTORY.getDefaultInstance();

		public Provider(EntityPlayer player) {
			ei.player = player;
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			EXINVENTORY.getStorage().readNBT(EXINVENTORY, ei, null, nbt);
		}

		@Override
		public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
			return hasCapability(capability, facing) ? EXINVENTORY.cast(ei) : null;
		}

		@Override
		public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
			return capability == EXINVENTORY;
		}

		@Override
		public NBTTagCompound serializeNBT() {
			return (NBTTagCompound) EXINVENTORY.getStorage().writeNBT(EXINVENTORY, ei, null);
		}
	}

	@CapabilityInject(ExInventory.class)
	public static Capability<ExInventory> EXINVENTORY = null;
	public static final ResourceLocation LOCATION = new ResourceLocation(PlayerStorage.MODID, "inventory");
	public static Strategy<ItemStack> itemStrategy = new Strategy<ItemStack>() {

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			return a == null || b == null ? false : a.isItemEqual(b);
		}

		@Override
		public int hashCode(ItemStack o) {
			return (o.getItem().getRegistryName().toString() + o.getItemDamage()).hashCode();
		}

	};
	private static Map<UUID, ItemStack> usedStacks = new HashMap<>();

	@SubscribeEvent
	public static void attach(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(LOCATION, new Provider((EntityPlayer) event.getObject()));
			if (!event.getObject().world.isRemote) {
				TileInterface.refresh();
			}
		}
	}

	@SubscribeEvent
	public static void clone(Clone event) {
		if (!event.getEntityPlayer().world.isRemote) {
			ExInventory old, neww;
			if ((old = getInventory(event.getOriginal())) != null && (neww = getInventory(event.getEntityPlayer())) != null) {
				neww.deserializeNBT(old.serializeNBT());
				sync((EntityPlayerMP) event.getEntityPlayer());
			}
		}
	}

	@SubscribeEvent
	public static void death(LivingDeathEvent event) {
		if (ConfigHandler.keeper && event.getEntityLiving() instanceof EntityPlayerMP) {
			BlockPos p = new BlockPos(event.getEntityLiving()).down();
			World world = event.getEntityLiving().world;
			while (p.getY() < world.getActualHeight()) {
				if (world.isValid(p) && world.isAirBlock(p)) {
					boolean placed = world.setBlockState(p, Registry.keeper.getDefaultState());
					if (placed) {
						((TileKeeper) world.getTileEntity(p)).create(ExInventory.getInventory((EntityPlayer) event.getEntityLiving()));
						event.getEntityLiving().sendMessage(new TextComponentString(TextFormatting.GOLD + "You lost your entire player storage."));
					}
					break;
				}
				p = p.up();
			}
		}
	}

	@SubscribeEvent
	public static void destroy(PlayerDestroyItemEvent event) {
		if (event.getHand() == null)
			return;
		ExInventory ei = ExInventory.getInventory(event.getEntityPlayer());
		if (ei.refill && !event.getEntityPlayer().world.isRemote) {
			new Thread(() -> event.getEntityPlayer().world.getMinecraftServer().addScheduledTask(() -> {
				if (event.getEntityPlayer().getHeldItem(event.getHand()).isEmpty()) {
					ItemStack ss = ei.extractItem(s -> s.isItemEqualIgnoreDurability(event.getOriginal()), event.getOriginal().getMaxStackSize(), false);
					event.getEntityPlayer().setHeldItem(event.getHand(), ss);
					event.getEntityPlayer().openContainer.detectAndSendChanges();
				}
			})).start();
		}
	}

	@SubscribeEvent
	public static void finish(Finish event) {
		if (!event.getEntityLiving().world.isRemote && event.getEntityLiving() instanceof EntityPlayer && event.getResultStack().isEmpty()) {
			ExInventory ei = ExInventory.getInventory((EntityPlayer) event.getEntityLiving());
			if (ei.refill) {
				ItemStack stack = usedStacks.get(event.getEntityLiving().getUniqueID());
				usedStacks.remove(event.getEntityLiving().getUniqueID());
				event.setResultStack(ei.extractItem(s -> s.isItemEqual(stack), stack.getMaxStackSize(), false));
				ei.player.openContainer.detectAndSendChanges();
			}
		}
	}

	public static ExInventory getInventory(EntityPlayer player) {
		return player == null || !player.hasCapability(EXINVENTORY, null) ? null : player.getCapability(EXINVENTORY, null);
	}

	public static EntityPlayer getPlayerByName(String name, @Nullable World world) {
		if (name == null)
			return null;
		if (world != null ? world.isRemote : FMLCommonHandler.instance().getEffectiveSide().isClient())
			return world.getPlayerEntityByName(name);
		else
			return Arrays.stream(FMLCommonHandler.instance().getMinecraftServerInstance().worlds).flatMap(w -> w.playerEntities.stream()).filter(p -> p.getName().equals(name)).findFirst().orElse(null);
	}

	@SubscribeEvent
	public static void join(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof EntityPlayerMP) {
			sync((EntityPlayerMP) event.getEntity());
			TileInterface.updateState((EntityPlayer) event.getEntity());
		}
	}

	@SubscribeEvent
	public static void logout(PlayerLoggedOutEvent event) {
		ExInventory.getInventory(event.player).markForSync();
		if (!event.player.world.isRemote) {
			TileInterface.refresh();
			TileInterface.updateState(event.player);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void pickup(EntityItemPickupEvent event) {
		ExInventory ei = ExInventory.getInventory(event.getEntityPlayer());
		EntityItem e = event.getItem();
		if (ei.autoPickup != ei.autopickupInverted && !e.getItem().isEmpty()) {
			int count = e.getItem().getCount();
			ItemStack rest = ei.insertItem(e.getItem(), false);
			if (rest.getCount() != count) {
				EntityTracker entitytracker = ((WorldServer) event.getEntityPlayer().world).getEntityTracker();
				entitytracker.sendToTracking(e, new SPacketCollectItem(e.getEntityId(), event.getEntityPlayer().getEntityId(), count - rest.getCount()));
			}
			e.getItem().setCount(rest.getCount());
		}
	}

	public static void register() {
		CapabilityManager.INSTANCE.register(ExInventory.class, new IStorage<ExInventory>() {

			@Override
			public void readNBT(Capability<ExInventory> capability, ExInventory instance, EnumFacing side, NBTBase nbt) {
				if (nbt instanceof NBTTagCompound) {
					instance.deserializeNBT((NBTTagCompound) nbt);
					instance.init();
				}
			}

			@Override
			public NBTBase writeNBT(Capability<ExInventory> capability, ExInventory instance, EnumFacing side) {
				return instance.serializeNBT();
			}

		}, ExInventory::new);
	}

	@SubscribeEvent
	public static void start(Start event) {
		if (!event.getEntityLiving().world.isRemote && event.getEntityLiving() instanceof EntityPlayer) {
			ExInventory ei = ExInventory.getInventory((EntityPlayer) event.getEntityLiving());
			if (ei.refill) {
				usedStacks.put(event.getEntityLiving().getUniqueID(), event.getItem().copy());
			}
		}
	}

	/*
	public static ExInventory getInventory(EntityPlayer player) {
	    return (player == null || !player.hasCapability(EXINVENTORY, null)) ? null : player.getCapability(EXINVENTORY, null);
	}
	 */
	public static void sync(EntityPlayerMP player) {
		PacketHandler.sendTo(new MessageCapaSync(player), player);
	}

	@SubscribeEvent
	public static void tick(PlayerTickEvent event) {
		if (event.phase == Phase.END && !event.player.world.isRemote) {
			ExInventory exi;
			if ((exi = getInventory(event.player)) != null) {
				exi.update();
				//exi.craftupdate();
			}
		}
	}

	public EntityPlayer player;

	public List<StackWrapper> items = new ArrayList<>(), itemsPlusTeam = new ArrayList<>();

	public int itemLimit = ConfigHandler.itemCapacity, gridHeight = 4;

	public boolean needSync = true, defaultGUI = true, autoPickup, infiniteWater, noshift, refill;

	public NonNullList<ItemStack> matrix = NonNullList.withSize(9, ItemStack.EMPTY);

	private List<ItemStack> itemlist = null;

	public Set<String> members = new HashSet<>();

	public Set<GlobalBlockPos> tiles = new HashSet<>();

	public Object2ObjectMap<ItemStack, Limit> itemLimits = new Object2ObjectOpenCustomHashMap<>(itemStrategy);

	public boolean jeiSearch = false, topdown = true, autofocus = true, autopickupInverted = false;

	public Sort sort = Sort.NAME;

	public GuiMode mode = GuiMode.ITEM;

	public List<CraftingRecipe> recipes = new ArrayList<>();

	public ReferenceSet<StackWrapper> dirtyStacks = new ReferenceOpenHashSet<>();

	public ObjectSet<ItemStack> highlightItems = new ObjectOpenCustomHashSet<>(itemStrategy);

	public int itemCount;

	public boolean enabled = false;

	Field raw = ReflectionHelper.findField(ItemStack.class, "item", "field_151002_e");

	public boolean ignoreMin = false;

	public ExInventory() {
		itemLimits.defaultReturnValue(Limit.defaultValue);
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		int size = NBTHelper.get(nbt, "itemsize", Integer.class);
		items.clear();
		for (int i = 0; i < size; i++) {
			StackWrapper sw = StackWrapper.loadStackWrapperFromNBT(NBTHelper.get(nbt, "item" + i, NBTTagCompound.class));
			if (sw != null) {
				items.add(sw);
			}
		}
		itemLimit = NBTHelper.get(nbt, "itemLimit", Integer.class);
		List<ItemStack> tmp = NBTHelper.getList(nbt, "matrix", ItemStack.class);
		for (int i = 0; i < matrix.size(); i++) {
			matrix.set(i, tmp.get(i));
		}
		jeiSearch = NBTHelper.get(nbt, "jei", Boolean.class);
		topdown = NBTHelper.get(nbt, "top", Boolean.class);
		autofocus = NBTHelper.getSafe(nbt, "autofocus", Boolean.class).orElse(true);
		sort = NBTHelper.get(nbt, "sort", Sort.class);
		mode = NBTHelper.get(nbt, "mode", GuiMode.class);
		gridHeight = NBTHelper.get(nbt, "gridHeight", Integer.class);
		needSync = NBTHelper.get(nbt, "dirty", Boolean.class);
		defaultGUI = NBTHelper.get(nbt, "defaultGUI", Boolean.class);
		autoPickup = NBTHelper.get(nbt, "autoPickup", Boolean.class);
		infiniteWater = NBTHelper.get(nbt, "infiniteWater", Boolean.class);
		noshift = NBTHelper.get(nbt, "noshift", Boolean.class);
		refill = NBTHelper.get(nbt, "refill", Boolean.class);
		members = new HashSet<>(NBTHelper.getList(nbt, "members", String.class));
		List<Integer> ints = NBTHelper.getList(nbt, "tilesInt", Integer.class);
		List<BlockPos> poss = NBTHelper.getList(nbt, "tilesPos", BlockPos.class);
		tiles.clear();
		for (int i = 0; i < ints.size(); i++) {
			tiles.add(new GlobalBlockPos(poss.get(i), ints.get(i)));
		}
		itemLimits.clear();
		List<ItemStack> lisIK = NBTHelper.getList(nbt, "itemLimitsKey", ItemStack.class);
		List<BlockPos> lisIV = NBTHelper.getList(nbt, "itemLimitsValue", BlockPos.class);
		for (int i = 0; i < lisIK.size(); i++) {
			itemLimits.put(lisIK.get(i), new Limit(lisIV.get(i)));
		}
		size = NBTHelper.get(nbt, "recipesize", Integer.class);
		recipes.clear();
		for (int i = 0; i < size; i++) {
			NBTTagCompound n = NBTHelper.get(nbt, "recipe" + i, NBTTagCompound.class);
			CraftingRecipe cr = CraftingRecipe.deserialize(n);
			if (cr != null) {
				recipes.add(cr);
			}
		}
		highlightItems.clear();
		highlightItems.addAll(NBTHelper.getList(nbt, "hItems", NBTTagCompound.class).stream().map(n -> new ItemStack(n)).collect(Collectors.toList()));
	}

	public ItemStack extractItem(ItemStack stack, int size, boolean simulate) {
		return extractItem(s -> ItemHandlerHelper.canItemStacksStack(s, stack), size, simulate);
	}

	public ItemStack extractItem(Predicate<ItemStack> pred, int size, boolean simulate) {
		return extractItem(pred, size, true, simulate);
	}

	private ItemStack extractItem(Predicate<ItemStack> pred, int size, boolean useMembers, boolean simulate) {
		if (size <= 0 || pred == null || pred.test(ItemStack.EMPTY))
			return ItemStack.EMPTY;
		for (int i = 0; i < items.size(); i++) {
			StackWrapper s = items.get(i);
			if (pred.test(s.getStack())) {
				size = MathHelper.clamp(s.getSize() - (ignoreMin ? 0 : itemLimits.get(s.getStack()).min), 0, size);
				if (size <= 0)
					return ItemStack.EMPTY;
				if (!simulate) {
					markForSync();
					ItemStack res = s.extract(size);
					if (s.getSize() == 0) {
						items.remove(i);
					} else if (i > 0) {
						Collections.swap(items, i, i - 1);
					}
					return res;
				} else
					return ItemHandlerHelper.copyStackWithSize(s.getStack(), Math.min(size, s.getSize()));
			}
		}
		ItemStack ret = ItemStack.EMPTY;
		for (ExInventory ei : getMembers()) {
			if (useMembers && !(ret = ei.extractItem(pred, size, false, simulate)).isEmpty()) {
				markForSync();
				break;
			}
		}
		return ret;
	}

	public int getAmountItem(Predicate<ItemStack> pred) {
		return items.stream().filter(s -> pred.test(s.getStack())).mapToInt(s -> s.getSize()).findAny().orElse(0);
	}

	public boolean getEnabled() {
		return enabled;
	}

	public int getItemCount() {
		return MathHelper.clamp(items.stream().mapToInt(s -> s.getSize()).sum(), 0, Integer.MAX_VALUE);
	}

	public List<StackWrapper> getItems() {
		List<StackWrapper> lis = new ArrayList<>(itemsPlusTeam);
		return lis;
	}

	public int getMaxItemCount() {
		return itemLimit;
	}

	private List<ExInventory> getMembers() {
		Validate.isTrue(!player.world.isRemote);
		return members.stream().map(s -> getInventory(getPlayerByName(s, player.world))).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private void init() {
		if (!player.world.isRemote) {
			tiles.removeIf(gb -> !(gb.getTile() instanceof TileInterface) || ((TileInterface) gb.getTile()).getPlayerName() != null && !((TileInterface) gb.getTile()).getPlayerName().equals(player.getName()));
		}
	}

	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		return insertItem(stack, false, simulate);
	}

	private ItemStack insertItem(ItemStack stack, boolean ignoreLimit, boolean simulate) {
		if (stack.isEmpty())
			return stack;
		ItemStack copy = stack.copy();
		String stackstring1 = string(stack);
		int absLimit = ConfigHandler.infiniteSpace || ignoreLimit ? Integer.MAX_VALUE : itemLimit;
		int limit = itemLimits.get(stack).max;
		boolean voidd = itemLimits.get(stack).voidd;
		String stackstring2 = string(stack);
		ItemStack rest = ItemHandlerHelper.copyStackWithSize(stack, Math.max(0, Math.max(stack.getCount() + getAmountItem(s -> ItemHandlerHelper.canItemStacksStack(stack, s)) - limit, stack.getCount() + getItemCount() - absLimit)));
		rest.setCount(Math.min(stack.getCount(), rest.getCount()));
		if (rest.getCount() == stack.getCount())
			return voidd ? ItemStack.EMPTY : rest;
		String stackstring3 = string(stack);
		for (StackWrapper s : items) {
			if (s.canInsert(stack)) {
				if (!simulate) {
					s.insert(ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - rest.getCount()));
					//					dirtyStacks.add(s);
					markForSync();
				}
				return voidd ? ItemStack.EMPTY : rest;
			}
		}
		if (!simulate) {
			String stackstring4 = string(stack);
			ItemStack s = ItemHandlerHelper.copyStackWithSize(stack, 1);
			String stackstring5 = string(stack);
			if (s.isEmpty()) {
				String fin = System.lineSeparator() + "Why does this bug happen?" + System.lineSeparator();
				fin += "this: " + string(s) + System.lineSeparator();
				fin += "copy: " + string(copy) + System.lineSeparator();
				fin += "1: " + stackstring1 + System.lineSeparator();
				fin += "2: " + stackstring2 + System.lineSeparator();
				fin += "3: " + stackstring3 + System.lineSeparator();
				fin += "4: " + stackstring4 + System.lineSeparator();
				fin += "5: " + stackstring5;
				throw new RuntimeException(fin);
			}
			items.add(new StackWrapper(s, stack.getCount() - rest.getCount()));
			markForSync();
		}
		return voidd ? ItemStack.EMPTY : rest;
	}

	public void markForSync() {
		needSync = true;
		itemlist = null;
		if (!player.world.isRemote) {
			getMembers().forEach(e -> e.needSync = true);
		}
	}

	public void readSyncOnlyNBT(NBTTagCompound nbt) {
		itemsPlusTeam.clear();
		itemsPlusTeam.addAll(StreamSupport.stream(nbt.getTagList("items+", 10).spliterator(), false).map(n -> StackWrapper.loadStackWrapperFromNBT((NBTTagCompound) n)).collect(Collectors.toList()));
		itemCount = nbt.getInteger("itemCount");
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTHelper.set(nbt, "itemsize", items.size());
		for (int i = 0; i < items.size(); i++) {
			NBTHelper.set(nbt, "item" + i, items.get(i).writeToNBT(new NBTTagCompound()));
		}
		//TODO use NBTHelper.setlist
		NBTHelper.set(nbt, "itemLimit", itemLimit);
		NBTHelper.setList(nbt, "matrix", matrix);
		NBTHelper.set(nbt, "jei", jeiSearch);
		NBTHelper.set(nbt, "top", topdown);
		NBTHelper.set(nbt, "autofocus", autofocus);
		NBTHelper.set(nbt, "sort", sort);
		NBTHelper.set(nbt, "mode", mode);
		NBTHelper.set(nbt, "gridHeight", gridHeight);
		NBTHelper.set(nbt, "dirty", needSync);
		NBTHelper.set(nbt, "defaultGUI", defaultGUI);
		NBTHelper.set(nbt, "autoPickup", autoPickup);
		NBTHelper.set(nbt, "infiniteWater", infiniteWater);
		NBTHelper.set(nbt, "noshift", noshift);
		NBTHelper.set(nbt, "refill", refill);
		NBTHelper.setList(nbt, "members", new ArrayList<>(members));
		NBTHelper.setList(nbt, "tilesInt", tiles.stream().map(g -> g.getDimension()).collect(Collectors.toList()));
		NBTHelper.setList(nbt, "tilesPos", tiles.stream().map(g -> g.getPos()).collect(Collectors.toList()));
		NBTHelper.setList(nbt, "itemLimitsKey", new ArrayList<>(itemLimits.keySet()));
		NBTHelper.setList(nbt, "itemLimitsValue", itemLimits.values().stream().map(l -> l.toPos()).collect(Collectors.toList()));
		NBTHelper.set(nbt, "recipesize", recipes.size());
		for (int i = 0; i < recipes.size(); i++) {
			NBTHelper.set(nbt, "recipe" + i, CraftingRecipe.serialize(recipes.get(i)));
		}
		NBTHelper.setList(nbt, "hItems", highlightItems.stream().map(s -> s.writeToNBT(new NBTTagCompound())).collect(Collectors.toList()));
		return nbt;
	}

	public void setEnabled(boolean value) {
		enabled = value;
	}

	private String string(ItemStack stack) {
		Item ra = null;
		try {
			ra = (Item) raw.get(stack);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		String size = stack.getCount() + "", item = stack.getItem() == null ? "null" : stack.getItem().getRegistryName().toString() + "", rawitem = ra == null ? "null" : ra.getRegistryName().toString(), nbt = String.valueOf(stack.getTagCompound()), meta = stack.getMetadata() + "";
		return "[size=" + size + ", item=" + item + ", rawitem=" + rawitem + ", nbt=" + nbt + ", meta=" + meta + "]";
	}

	private void update() {
		if (needSync && player.openContainer instanceof ContainerExI) {
			itemsPlusTeam = items.stream().map(StackWrapper::copy).collect(Collectors.toList());
			for (ExInventory ei : getMembers()) {
				ei.items.forEach(sw -> {
					boolean merged = false;
					for (StackWrapper w : itemsPlusTeam) {
						if (w.canInsert(sw.getStack())) {
							w.setSize(w.getSize() + sw.getSize());
							merged = true;
							break;
						}
					}
					if (!merged) {
						itemsPlusTeam.add(sw.copy());
					}
				});
			}
			sync((EntityPlayerMP) player);
			needSync = false;
		}
	}

	public void writeSyncOnlyNBT(NBTTagCompound nbt) {
		//		Collector<NBTBase, NBTTagList, NBTTagList> collector = Collector.of(NBTTagList::new, NBTTagList::appendTag, (l, r) -> {
		//			r.forEach(l::appendTag);
		//			return l;
		//		});
		NBTTagList list = new NBTTagList();
		itemsPlusTeam.stream().map(sw -> sw.writeToNBT(new NBTTagCompound())).forEachOrdered(list::appendTag);
		nbt.setTag("items+", list);
		list = new NBTTagList();
		nbt.setInteger("itemCount", getItemCount());
	}

}
