package mrriegel.playerstorage.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import baubles.common.container.SlotBauble;
import mrriegel.limelib.gui.CommonContainer;
import mrriegel.playerstorage.ClientProxy;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.PlayerStorage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public final class ContainerExI extends CommonContainer<EntityPlayer> {

	public static class SlotExtra extends Slot {

		EntityPlayer player;
		EntityEquipmentSlot slot;

		public SlotExtra(IInventory inventoryIn, int index, int xPosition, int yPosition, EntityPlayer player) {
			super(inventoryIn, index, xPosition, yPosition);
			this.player = player;
			if (armor()) {
				List<EntityEquipmentSlot> lis = new ArrayList<>(Arrays.asList(EntityEquipmentSlot.values()));
				lis.remove(0);
				lis.remove(0);
				slot = lis.get(getSlotIndex() - 36);
			}
		}

		boolean armor() {
			return getSlotIndex() > 35 && getSlotIndex() < 40;
		}

		@Override
		public boolean canTakeStack(EntityPlayer playerIn) {
			if (armor())
				return super.canTakeStack(playerIn);
			ItemStack itemstack = getStack();
			return !itemstack.isEmpty() && !playerIn.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false : super.canTakeStack(playerIn);
		}

		@Override
		public int getSlotStackLimit() {
			return armor() ? 1 : super.getSlotStackLimit();
		}

		@Override
		public String getSlotTexture() {
			return armor() ? ItemArmor.EMPTY_SLOT_NAMES[slot.getIndex()] : "minecraft:items/empty_armor_slot_shield";
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return armor() ? stack.getItem().isValidArmor(stack, slot, player) : super.isItemValid(stack);
		}

	}

	public static class SlotResult extends SlotCrafting {
		public SlotResult(EntityPlayer player, InventoryCrafting craftingInventory, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition) {
			super(player, craftingInventory, inventoryIn, slotIndex, xPosition, yPosition);
		}
	}

	public boolean space, shift, ctrl;
	protected boolean forceSync = true;

	IRecipe recipe;

	public ExInventory ei;

	public ContainerExI(InventoryPlayer invPlayer) {
		super(invPlayer, invPlayer.player, Pair.of("result", new InventoryCraftResult()));
		ei = ExInventory.getInventory(getPlayer());
		ei.markForSync();
		invs.put("matrix", new InventoryCrafting(this, 3, 3));
		ReflectionHelper.setPrivateValue(InventoryCrafting.class, getMatrix(), ei.matrix, 0);
		addSlotToContainer(new SlotResult(invPlayer.player, getMatrix(), invs.get("result"), 0, 44, 88 + 18 * ei.gridHeight));
		initSlots(getMatrix(), 8, 30 + 18 * ei.gridHeight, 3, 3, 0);
		initPlayerSlots(80, 30 + 18 * ei.gridHeight);
		initSlots(invPlayer, 152, 10 + 18 * ei.gridHeight, 5, 1, invPlayer.mainInventory.size(), SlotExtra.class, getPlayer());
		addSlotToContainer(new Slot(new InventoryBasic("", false, 1), 0, 114, 10 + 18 * ei.gridHeight) {
			@Override
			public TextureAtlasSprite getBackgroundSprite() {
				return ClientProxy.sprite;
			}

			@Override
			public ItemStack getStack() {
				return ItemStack.EMPTY;
			}
		});
		if (PlayerStorage.baubles) {
			EntityPlayer player = getPlayer();
			IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
			for (int i = 0; i < 7; i++) {
				addSlotToContainer(getSlot(baubles, i, 246, 8 + i * 18));
			}
		}
	}

	@Override
	protected List<Area> allowedSlots(ItemStack stack, IInventory inv, int index) {
		if (inv == getMatrix() || inv == invPlayer && index > 35)
			return Collections.singletonList(getAreaForEntireInv(invPlayer));
		if (ei.noshift && inv == invPlayer && index >= 0 && index <= 8)
			return Collections.singletonList(getAreaForInv(inv, 9, 27));
		if (ei.noshift && inv == invPlayer && index >= 9 && index <= 35)
			return Collections.singletonList(getAreaForInv(inv, 0, 9));
		return null;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

	@Override
	public boolean canMergeSlot(ItemStack stack, Slot slot) {
		return slot.inventory != invs.get("result") /*&& slot.inventory != getMatrix()*/ && super.canMergeSlot(stack, slot);
	}

	public void craftShift() {
		IInventory result = invs.get("result");
		SlotResult sl = (SlotResult) inventorySlots.stream().filter(s -> s instanceof SlotResult).findFirst().get();
		int crafted = 0;
		ItemStack res = result.getStackInSlot(0);
		IItemHandler inv = new PlayerMainInvWrapper(invPlayer);
		while (crafted + res.getCount() <= res.getMaxStackSize()) {
			if (!ItemHandlerHelper.insertItemStacked(inv, res.copy(), true).isEmpty()) {
				break;
			}
			ItemHandlerHelper.insertItemStacked(inv, res.copy(), false);
			sl.onTake(getPlayer(), res);
			crafted += res.getCount();
			//			onCraftMatrixChanged(null);
			if (!ItemHandlerHelper.canItemStacksStack(res, result.getStackInSlot(0))) {
				break;
			} else {
				res = result.getStackInSlot(0);
			}
		}
		detectAndSendChanges();
	}

	public InventoryCrafting getMatrix() {
		return (InventoryCrafting) invs.get("matrix");
	}

	@Optional.Method(modid = "baubles")
	private Slot getSlot(IBaublesItemHandler baubles, int slot, int x, int y) {
		return new SlotBauble(getPlayer(), baubles, slot, x, y);
	}

	@Override
	protected void initSlots() {
	}

	@Override
	public void onCraftMatrixChanged(IInventory inventoryIn) {
		//detectAndSendChanges();
		if (recipe == null || !recipe.matches(getMatrix(), getPlayer().world)) {
			recipe = CraftingManager.findMatchingRecipe(getMatrix(), getPlayer().world);
		}
		invs.get("result").setInventorySlotContents(0, recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(getMatrix()));
	}

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
		Slot slot = null;
		if (!player.world.isRemote && slotId >= 0 && slotId < inventorySlots.size() && (slot = getSlot(slotId)) != null) {
			if (slot.inventory == invs.get("result")) {
				onCraftMatrixChanged(null);
			}
			if (slot.getHasStack() && slot.inventory instanceof InventoryPlayer) {
				ItemStack stack = slot.getStack();
				boolean apply = false;
				for (Slot s : getSlotsFor(player.inventory)) {
					if (ctrl && shift) {
						apply = true;
						if (ei.mode == GuiMode.ITEM) {
							if (s.getHasStack() && s.getStack().isItemEqual(stack)) {
								ItemStack rest = ei.insertItem(s.getStack(), false);
								s.putStack(rest);
								if (!rest.isEmpty()) {
									break;
								}
							}
						}
					} else if (ctrl && space && slot.getSlotIndex() > 8) {
						apply = true;
						if (s.getSlotIndex() <= 8) {
							continue;
						}
						if (ei.mode == GuiMode.ITEM) {
							if (s.getHasStack()) {
								ItemStack rest = ei.insertItem(s.getStack(), false);
								s.putStack(rest);
							}
						}
					}
				}
				if (apply) {
					detectAndSendChanges();
					return ItemStack.EMPTY;
				}
				if (shift && !ei.noshift || ctrl && ei.noshift) {
					IInventory inv = slot.inventory;
					if (inv instanceof InventoryPlayer && slot.getSlotIndex() < 36) {
						boolean inserted = false;
						for (int i : new int[] { 36, 37, 38, 39 }) {
							if (stack.getCount() == 1 && inv.getStackInSlot(i).isEmpty() && getSlotFromInventory(inv, i).isItemValid(stack)) {
								getSlotFromInventory(inv, i).putStack(stack.copy());
								slot.putStack(ItemStack.EMPTY);
								inserted = true;
								break;
							}
						}
						if (!inserted) {
							slot.putStack(ei.insertItem(stack, false));
						}
						detectAndSendChanges();
						return ItemStack.EMPTY;
					}
				}
			}
		}
		detectAndSendChanges();
		return super.slotClick(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		Slot slot = inventorySlots.get(index);
		if (!player.world.isRemote && slot.getHasStack()) {
			if (slot.inventory == invs.get("result")) {
				craftShift();
				return ItemStack.EMPTY;
			}
		}
		return super.transferStackInSlot(player, index);
	}

}
