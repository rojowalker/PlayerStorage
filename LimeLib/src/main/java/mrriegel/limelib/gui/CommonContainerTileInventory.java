package mrriegel.limelib.gui;

import org.apache.commons.lang3.tuple.Pair;

import mrriegel.limelib.tile.CommonTileInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public abstract class CommonContainerTileInventory<T extends CommonTileInventory> extends CommonContainerTile<T> {

	public CommonContainerTileInventory(InventoryPlayer invPlayer, T tile) {
		super(invPlayer, tile, new Pair[] { Pair.<String, IInventory> of("tile", tile) });
	}

}
