package mrriegel.limelib.network;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.tile.CommonTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

public class TileMessage extends AbstractMessage {

	public TileMessage() {
		super();
	}

	public TileMessage(NBTTagCompound nbt) {
		super(nbt);
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		TileEntity tile = player.world.getTileEntity(BlockPos.fromLong(nbt.getLong("pOs")));
		if (tile instanceof CommonTile) {
			((CommonTile) tile).handleMessage(player, nbt);
			tile.markDirty();
		} else {
			LimeLib.log.warn("Tile entity on server is missing at " + BlockPos.fromLong(nbt.getLong("pos")));
		}
	}

}
