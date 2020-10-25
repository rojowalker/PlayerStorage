package mrriegel.limelib.network;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.tile.CommonTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

public class TileSyncMessage extends AbstractMessage {
	public TileSyncMessage() {
	}

	public TileSyncMessage(NBTTagCompound nbt) {
		super(nbt);
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		BlockPos pos = NBTHelper.get(nbt, "pos", BlockPos.class);
		if (side.isClient()) {
			if (player.world.getTileEntity(pos) instanceof CommonTile) {
				PacketHandler.sendToServer(new TileSyncMessage(nbt));
			}
		} else {
			if (player.world.getTileEntity(pos) instanceof CommonTile) {
				((CommonTile) player.world.getTileEntity(pos)).sync();
			}
		}
	}

}
