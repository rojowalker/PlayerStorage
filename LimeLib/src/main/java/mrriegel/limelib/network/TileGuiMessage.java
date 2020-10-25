package mrriegel.limelib.network;

import mrriegel.limelib.tile.CommonTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

public class TileGuiMessage extends AbstractMessage {

	public TileGuiMessage() {
		super();
	}

	public TileGuiMessage(NBTTagCompound nbt) {
		super(nbt);
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		TileEntity tile = player.world.getTileEntity(BlockPos.fromLong(nbt.getLong("pos")));
		if (tile instanceof CommonTile)
			((CommonTile) tile).openGUI((EntityPlayerMP) player);
	}

}
