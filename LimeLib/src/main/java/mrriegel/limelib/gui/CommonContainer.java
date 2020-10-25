package mrriegel.limelib.gui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import mrriegel.limelib.gui.slot.CommonSlot;
import mrriegel.limelib.gui.slot.SlotGhost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;

public abstract class CommonContainer<T> extends Container {

	protected InventoryPlayer invPlayer;
	protected Map<String, IInventory> invs;
	protected T save;

	public CommonContainer(InventoryPlayer invPlayer, T save, Pair<String, IInventory>... invs) {
		this.invPlayer = invPlayer;
		this.invs = Maps.newHashMap();
		this.save = save;
		if (invs != null)
			for (Pair<String, IInventory> e : invs) {
				if (e != null) {
					this.invs.put(e.getLeft(), e.getRight());
				}
			}
		modifyInvs();
		initSlots();
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}

	public EntityPlayer getPlayer() {
		return invPlayer.player;
	}

	protected abstract void initSlots();

	protected void modifyInvs() {
	}

	protected abstract List<Area> allowedSlots(ItemStack stack, IInventory inv, int index);

	protected List<Area> allowedSlots(ItemStack stack, IItemHandler inv, int index) {
		return null;
	}

	public void update(EntityPlayer player) {
	}

	protected Area getAreaForEntireInv(IInventory inv) {
		return getAreaForInv(inv, 0, inv.getSizeInventory());
	}

	protected Area getAreaForEntireInv(IItemHandler inv) {
		return getAreaForInv(inv, 0, inv.getSlots());
	}

	protected Area getAreaForInv(Object inv, int start, int total) {
		List<Integer> l = Lists.newArrayList();
		for (Slot s : inventorySlots)
			if (sameInventory(inv, s) && s.getSlotIndex() >= start && s.getSlotIndex() < total + start)
				l.add(s.getSlotIndex());
		if (l.isEmpty())
			return null;
		Collections.sort(l);
		return new Area(inv, l.get(0), l.get(l.size() - 1));
	}

	protected Area getAreaForInv(IInventory inv, int start, int total) {
		return getAreaForInv((Object) inv, start, total);
	}

	protected Area getAreaForInv(IItemHandler inv, int start, int total) {
		return getAreaForInv((Object) inv, start, total);
	}

	protected List<Slot> getSlotsFor(Object inv) {
		return inventorySlots.stream().filter(s -> sameInventory(inv, s)).collect(Collectors.toList());
	}

	protected void initPlayerSlots(int x, int y) {
		initSlots(invPlayer, x, y + 58, 9, 1, 0);
		initSlots(invPlayer, x, y, 9, 3, 9);
	}

	//TODO rename addslots
	protected void initSlots(IInventory inv, int x, int y, int width, int height, int startIndex, Class<? extends Slot> clazz, Object... args) {
		if (inv == null)
			return;
		for (int k = 0; k < height; ++k) {
			for (int i = 0; i < width; ++i) {
				int id = i + k * width + startIndex;
				if (id >= inv.getSizeInventory())
					break;
				Slot slot = null;
				try {
					List<Object> lis = Lists.newArrayList(inv, id, x + i * 18, y + k * 18);
					for (Object o : args)
						lis.add(o);
					slot = ConstructorUtils.invokeConstructor(clazz, lis.toArray(new Object[0]), lis.stream().map(o -> o.getClass()).toArray(Class[]::new));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (slot != null)
					this.addSlotToContainer(slot);
			}
		}
	}

	protected void initSlots(IInventory inv, int x, int y, int width, int height, int startIndex) {
		initSlots(inv, x, y, width, height, startIndex, Slot.class);
	}

	protected void initSlots(IInventory inv, int x, int y, int width, int height) {
		initSlots(inv, x, y, width, height, 0);
	}

	protected void initSlots(IItemHandler inv, int x, int y, int width, int height, int startIndex, Class<? extends Slot> clazz, Object... args) {
		if (inv == null)
			return;
		for (int k = 0; k < height; ++k) {
			for (int i = 0; i < width; ++i) {
				int id = i + k * width + startIndex;
				if (id >= inv.getSlots())
					break;
				Slot slot = null;
				try {
					List<Object> lis = Lists.newArrayList(inv, id, x + i * 18, y + k * 18);
					for (Object o : args)
						lis.add(o);
					slot = ConstructorUtils.invokeConstructor(clazz, lis.toArray(new Object[0]), lis.stream().map(o -> o.getClass()).toArray(Class[]::new));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (slot != null)
					this.addSlotToContainer(slot);
			}
		}
	}

	protected void initSlots(IItemHandler inv, int x, int y, int width, int height, int startIndex) {
		initSlots(inv, x, y, width, height, startIndex, Slot.class);
	}

	protected void initSlots(IItemHandler inv, int x, int y, int width, int height) {
		initSlots(inv, x, y, width, height, 0);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);
		if (playerIn.world.isRemote)
			return ItemStack.EMPTY;
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();

			Object inv = getInv(slot);
			List<Area> ar = inv instanceof IInventory ? allowedSlots(itemstack1, (IInventory) inv, slot.getSlotIndex()) : inv instanceof IItemHandler ? allowedSlots(itemstack1, (IInventory) inv, index) : null;
			if (ar == null)
				return ItemStack.EMPTY;
			ar.removeAll(Collections.singleton(null));
			boolean merged = false;
			for (Area p : ar) {
				// if (slot.inventory == p.inv)
				// continue;
				Slot minSlot = getSlotFromInv(p.inv, p.min);
				// while (minSlot == null && p.min < p.inv.getSizeInventory())
				// minSlot = getSlotFromInventory(p.inv, ++p.min);
				Slot maxSlot = getSlotFromInv(p.inv, p.max);
				// while (maxSlot == null && p.max > 0)
				// minSlot = getSlotFromInventory(p.inv, --p.max);
				if (minSlot == null || maxSlot == null)
					continue;
				if (hasGhost(p)) {
					for (int i = p.min; i <= p.max; i++)
						if (!getSlotFromInv(p.inv, i).getHasStack() && getSlotFromInv(p.inv, i) instanceof SlotGhost) {
							getSlotFromInv(p.inv, i).putStack(ItemHandlerHelper.copyStackWithSize(itemstack1, 1));
							detectAndSendChanges();
							return ItemStack.EMPTY;
						}
				}
				if (this.mergeItemStack(itemstack1, minSlot.slotNumber, maxSlot.slotNumber + 1, false)) {
					merged = true;
					slot.onSlotChange(itemstack1, itemstack);
					break;
				}

			}
			if (!merged)
				return ItemStack.EMPTY;
			if (itemstack1.getCount() == 0) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}

			if (itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}
			//			ItemStack s = ItemHandlerHelper.copyStackWithSize(itemstack, Math.max(itemstack.getCount() - itemstack1.getCount(), 0));
			slot.onTake(playerIn, itemstack1);
			detectAndSendChanges();
		}
		return itemstack;
	}

	private boolean merge(ItemStack stack, int startindex, int endindex, boolean reverse) {
		boolean merged = false;
		for (int i = reverse ? endindex - 1 : startindex; !stack.isEmpty() && (reverse ? i >= startindex : i < endindex); i += reverse ? -1 : 1) {
			Slot slot = inventorySlots.get(i);
			ItemStack slotstack = slot.getStack();
			if (!slotstack.isEmpty() && slot.isItemValid(stack) && ItemHandlerHelper.canItemStacksStack(slotstack, stack)) {
				int fit = Math.min(slot.getItemStackLimit(stack), slotstack.getMaxStackSize()) - slotstack.getCount();
				if (fit == 0)
					continue;
				fit = Math.min(stack.getCount(), fit);
				slotstack.grow(fit);
				slot.onSlotChanged();
				stack.shrink(fit);
				merged = true;
			}
		}
		for (int i = reverse ? endindex - 1 : startindex; !stack.isEmpty() && (reverse ? i >= startindex : i < endindex); i += reverse ? -1 : 1) {
			Slot slot = inventorySlots.get(i);
			if (!slot.getHasStack() && slot.isItemValid(stack)) {
				int fit = Math.min(slot.getItemStackLimit(stack), stack.getMaxStackSize());
				if (fit == 0)
					continue;
				fit = Math.min(stack.getCount(), fit);
				ItemStack copy = ItemHandlerHelper.copyStackWithSize(stack, fit);
				slot.putStack(copy);
				stack.shrink(fit);
				merged = true;
			}
		}
		return merged;
	}

	@Override
	public boolean mergeItemStack(ItemStack stack, int startindex, int endindex, boolean reverse) {
		return merge(stack, startindex, endindex, reverse);
	}

	public Slot getSlotFromInv(Object inv, int slotIn) {
		if (inv instanceof IInventory)
			return getSlotFromInventory((IInventory) inv, slotIn);
		if (!(inv instanceof IItemHandler))
			return null;
		for (int i = 0; i < this.inventorySlots.size(); ++i) {
			Slot slot = this.inventorySlots.get(i);
			if (slot.getSlotIndex() == slotIn)
				if ((slot instanceof SlotItemHandler && ((SlotItemHandler) slot).getItemHandler() == inv) || (slot instanceof CommonSlot && ((CommonSlot) slot).getItemHandler() == inv)) {
					return slot;
				}
		}

		return null;
	}

	private final boolean hasGhost(Area area) {
		for (int i = area.min; i <= area.max; i++)
			if (getSlotFromInv(area.inv, i) instanceof SlotGhost)
				return true;
		return false;
	}

	protected static class Area {
		Object inv;
		int min, max;

		public Area(Object inv, int min, int max) {
			super();
			this.inv = inv;
			this.min = min;
			this.max = max;
		}

		public Area(IInventory inv, int min, int max) {
			this((Object) inv, min, max);
		}

		public Area(IItemHandler inv, int min, int max) {
			this((Object) inv, min, max);
		}

		@Override
		public String toString() {
			return "Area [inv=" + inv + ", min=" + min + ", max=" + max + "]";
		}
	}

	public static boolean sameInventory(Object inv, Slot slot) {
		return inv == getInv(slot);
	}

	public static Object getInv(Slot slot) {
		if (slot instanceof SlotItemHandler)
			return ((SlotItemHandler) slot).getItemHandler();
		if (slot instanceof CommonSlot && ((CommonSlot) slot).isItemHandler())
			return ((CommonSlot) slot).getItemHandler();
		return slot.inventory;
	}

}
