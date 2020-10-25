package mrriegel.limelib.network;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import mrriegel.limelib.datapart.DataPart;
import mrriegel.limelib.datapart.DataPartRegistry;
import mrriegel.limelib.helper.NBTHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

public class DataPartSyncMessage extends AbstractMessage {

	public DataPartSyncMessage() {
	}

	public DataPartSyncMessage(DataPart part, BlockPos pos, List<BlockPos> parts) {
		if (part == null) {
			nbt.setBoolean("removed", true);
			NBTHelper.set(nbt, "poS", pos);
		} else
			part.writeDataToNBT(nbt);
		NBTHelper.setList(nbt, "poss", parts);
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		BlockPos pos = NBTHelper.get(nbt, "poS", BlockPos.class);
		DataPartRegistry reg = DataPartRegistry.get(player.world);
		if (reg != null) {
			if (nbt.getBoolean("removed"))
				reg.removeDataPart(pos);
			else {
				DataPart p = reg.getDataPart(pos);
				if (p != null) {
					p.setWorld(player.world);
					p.readDataFromNBT(nbt);
				} else {
					reg.createPart(nbt);
				}
			}
			List<BlockPos> valids = NBTHelper.getList(nbt, "poss", BlockPos.class);
			Set<BlockPos> clients = reg.getParts().stream().map(DataPart::getPos).collect(Collectors.toSet());
			clients.removeAll(valids);
			clients.forEach(p -> reg.removeDataPart(p));
		}
	}
}
