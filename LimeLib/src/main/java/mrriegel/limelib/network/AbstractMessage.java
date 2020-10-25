package mrriegel.limelib.network;

import io.netty.buffer.ByteBuf;
import mrriegel.limelib.LimeLib;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public abstract class AbstractMessage implements IMessage, IMessageHandler<AbstractMessage, IMessage> {

	protected NBTTagCompound nbt = new NBTTagCompound();
	public boolean shallSend = true;

	public AbstractMessage() {
	}

	public AbstractMessage(NBTTagCompound nbt) {
		this.nbt = nbt;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		nbt = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeTag(buf, nbt);
	}

	//TODO remove side & nbt
	public abstract void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side);

	@Override
	public IMessage onMessage(final AbstractMessage message, final MessageContext ctx) {
		this.nbt = message.nbt.copy();
		//TODO
		//		if (false) {
		//			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
		//				//				handleMessage(ctx.side.isServer()?ctx.getServerHandler().player:FMLClientHandler.instance().getClientPlayerEntity(), null, null);
		//			});
		//		}
		Runnable run = () -> {
			EntityPlayer player = (ctx.side.isClient() ? LimeLib.proxy.getClientPlayer() : ctx.getServerHandler().player);
			message.handleMessage(player, message.nbt.copy(), ctx.side);
		};
		IThreadListener listener = (ctx.side.isClient() ? LimeLib.proxy.getClientListener() : ctx.getServerHandler().player.getServerWorld());
		listener.addScheduledTask(run);
		return null;
	}

	//	private EntityPlayer getPlayer(MessageContext ctx) {
	//		return (ctx.netHandler instanceof NetHandlerPlayServer) ? ctx.getServerHandler().player : FMLClientHandler.instance().getClientPlayerEntity();
	//	}

}
