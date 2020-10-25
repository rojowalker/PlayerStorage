package mrriegel.limelib.network;

import mrriegel.limelib.helper.NBTHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

public class RenderUpdateMessage extends AbstractMessage {

	public RenderUpdateMessage() {
	}

	public RenderUpdateMessage(BlockPos pos1, BlockPos pos2) {
		NBTHelper.set(nbt, "pos1", pos1);
		NBTHelper.set(nbt, "pos2", pos2);
	}

	public RenderUpdateMessage(int x1, int y1, int z1, int x2, int y2, int z2) {
		this(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		player.world.markBlockRangeForRenderUpdate(NBTHelper.get(nbt, "pos1", BlockPos.class), NBTHelper.get(nbt, "pos2", BlockPos.class));
	}

}
