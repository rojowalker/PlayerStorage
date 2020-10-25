package mrriegel.limelib.tile;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.common.collect.Sets;

import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.network.TileGuiMessage;
import mrriegel.limelib.network.TileMessage;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommonTile extends TileEntity {

	private boolean syncDirty;
	//TODO protected final boolean keepData
	//TODO rename
	public Set<EntityPlayer> activePlayers = Sets.newHashSet();
	public static Set<CommonTile> syncs = Collections.newSetFromMap(new WeakHashMap<>());

	@Override
	public NBTTagCompound getUpdateTag() {
		return serializeNBT();
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
		return oldState.getBlock() != newSate.getBlock();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound tag = writeToSyncNBT(new NBTTagCompound());
		return new SPacketUpdateTileEntity(this.pos, 1337, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound tag = pkt.getNbtCompound();
		readFromSyncNBT(tag);
	}

	//TODO make abstract
	public void readFromSyncNBT(NBTTagCompound compound) {
		readFromNBT(compound);
	}

	public NBTTagCompound writeToSyncNBT(NBTTagCompound compound) {
		return writeToNBT(compound);
	}

	public boolean needsSync() {
		return syncDirty;
	}

	public void markForSync() {
		this.syncDirty = true;
	}

	public void unmarkForSync() {
		this.syncDirty = false;
	}

	public void sync() {
		markDirty();
		if (onServer())
			for (EntityPlayerMP player : world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(pos.add(-11, -11, -11), pos.add(11, 11, 11)))) {
				Packet<?> p = getUpdatePacket();
				if (p != null)
					player.connection.sendPacket(p);
			}
	}

	public boolean isUsable(EntityPlayer player) {
		return this.world.getTileEntity(this.pos) != this || isInvalid() ? false : player.getDistanceSq(getX() + 0.5D, getY() + 0.5D, getZ() + 0.5D) <= 64.0D;
	}

	public List<ItemStack> getDroppingItems() {
		return NonNullList.create();
	}

	public boolean openGUI(EntityPlayerMP player) {
		return false;
	}

	public final void sendOpenGUI() {
		if (world.isRemote) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setLong("pos", pos.toLong());
			PacketHandler.sendToServer(new TileGuiMessage(nbt));
		}
	}

	//TODO both sides
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt) {
	}

	//TODO both sides
	public final void sendMessage(NBTTagCompound nbt) {
		nbt.setLong("pOs", pos.toLong());
		PacketHandler.sendToServer(new TileMessage(nbt));
	}

	public void neighborChanged(IBlockState state, Block block, BlockPos fromPos) {
	}

	//TODO add to commonblockcontainer
	public void writeDataToStack(ItemStack stack) {
	}

	public void readDataFromStack(ItemStack stack) {
	}

	public final int getX() {
		return pos.getX();
	}

	public final int getY() {
		return pos.getY();
	}

	public final int getZ() {
		return pos.getZ();
	}

	public IBlockState getBlockState() {
		return world.getBlockState(pos);
	}

	public boolean onServer() {
		return !world.isRemote;
	}

	public boolean onClient() {
		return !onServer();
	}

}
