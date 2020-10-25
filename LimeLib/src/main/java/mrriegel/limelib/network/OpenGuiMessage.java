package mrriegel.limelib.network;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;

public class OpenGuiMessage extends AbstractMessage {

	public OpenGuiMessage() {
	}

	public OpenGuiMessage(String modID, int guiID, @Nullable BlockPos pos) {
		nbt.setString("modid", modID);
		nbt.setInteger("guiid", guiID);
		nbt.setLong("pos", pos == null ? BlockPos.ORIGIN.toLong() : pos.toLong());
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		BlockPos p = BlockPos.fromLong(nbt.getLong("pos"));
		ModContainer mod = Loader.instance().getActiveModList().stream().filter(m -> m.getModId().equals(nbt.getString("modid"))).findAny().orElse(null);
		if (mod != null) {
			player.openGui(mod.getMod(), nbt.getInteger("guiid"), player.world, p.getX(), p.getY(), p.getZ());
		}
	}

}
