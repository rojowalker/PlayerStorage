package mrriegel.limelib.gui;

import org.apache.commons.lang3.tuple.Pair;

import mrriegel.limelib.tile.CommonTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public abstract class CommonContainerTile<T extends CommonTile> extends CommonContainer<T> {

	public CommonContainerTile(InventoryPlayer invPlayer, T tile, Pair<String, IInventory>... invs) {
		super(invPlayer, tile, invs);
		tile.markForSync();
		tile.activePlayers.add(getPlayer());
	}

	public T getTile() {
		return save;
	}

	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		if (save != null) {
			save.markForSync();
			save.activePlayers.remove(playerIn);
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return save != null && save.isUsable(playerIn);
	}

}
