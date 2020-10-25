package mrriegel.limelib.network;

import mrriegel.limelib.datapart.DataPart;
import mrriegel.limelib.datapart.DataPartRegistry;
import mrriegel.limelib.helper.NBTHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

public class PlayerClickMessage extends AbstractMessage {

	public PlayerClickMessage() {
		super();
	}

	public PlayerClickMessage(BlockPos pos, EnumHand hand, boolean left) {
		NBTHelper.set(nbt, "pos", pos);
		NBTHelper.set(nbt, "left", left);
		NBTHelper.set(nbt, "mainhand", hand == EnumHand.MAIN_HAND);
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		DataPartRegistry reg = DataPartRegistry.get(player.world);
		DataPart part = reg.getDataPart(NBTHelper.get(nbt, "pos", BlockPos.class));
		EnumHand hand = NBTHelper.get(nbt, "mainhand", Boolean.class) ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
		if (part != null) {
			if (NBTHelper.get(nbt, "left", Boolean.class))
				part.onLeftClicked(player, hand);
			else
				part.onRightClicked(player, hand);
		}
	}

}
