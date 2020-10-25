package mrriegel.playerstorage.registry;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.cyclops.commoncapabilities.capability.itemhandler.SlotlessItemHandlerConfig;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.tile.CommonTile;
import mrriegel.limelib.tile.IHUDProvider;
import mrriegel.limelib.util.GlobalBlockPos;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.PlayerStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileInterface extends CommonTile implements IHUDProvider {

	private static EntityPlayer player;

	public static void refresh() {
		for (World world : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
			for (TileEntity element : world.loadedTileEntityList) {
				TileEntity t = element;
				if (t instanceof TileInterface) {
					((TileInterface) t).refreshPlayer = true;
				}
			}
		}
	}

	public static void updateState(EntityPlayer player) {
		ExInventory.getInventory(player).tiles.stream().map(gp -> (TileInterface) gp.getTile()).//
				forEach(t -> t.setOn(true));
	}

	private String playerName;

	private boolean refreshPlayer = true, on;

	public EntityPlayer getActivePlayer() {
		return player;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		EntityPlayer p = getPlayer();
		if (p != null && on && (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || PlayerStorage.commonCaps && capability == SlotlessItemHandlerConfig.CAPABILITY))
			return (T) new ExInventory.Handler(this);
		return super.getCapability(capability, facing);
	}

	@Override
	public List<String> getData(boolean sneak, EnumFacing facing) {
		return Collections.singletonList(TextFormatting.GOLD + "Owner: " + (getPlayer() == null ? TextFormatting.RED : TextFormatting.GREEN) + playerName);
	}

	public EntityPlayer getPlayer() {
		if (player == null || refreshPlayer) {
			refreshPlayer = false;
			return player = ExInventory.getPlayerByName(playerName, world);
		}
		return player;
	}

	public String getPlayerName() {
		return playerName;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return on && getPlayer() != null && (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || PlayerStorage.commonCaps && capability == SlotlessItemHandlerConfig.CAPABILITY) || super.hasCapability(capability, facing);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (getPlayer() != null) {
			ExInventory.getInventory(getPlayer()).tiles.remove(GlobalBlockPos.fromTile(this));
		}
	}

	public boolean isOn() {
		return on;
	}

	@Override
	public boolean lineBreak(boolean sneak, EnumFacing facing) {
		return false;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		playerName = NBTHelper.get(compound, "player", String.class);
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			on = NBTHelper.get(compound, "on", Boolean.class);
		}
		super.readFromNBT(compound);
	}

	@Override
	public double scale(boolean sneak, EnumFacing facing) {
		return 1.0;
	}

	public void setOn(boolean on) {
		this.on = on;
		world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
		markForSync();
	}

	/*
	public EntityPlayer getPlayer() {
	    if (player == null || refreshPlayer) {
	        refreshPlayer = false;
	        return player = ExInventory.getPlayerByName(playerName, world);
	    }
	    return player;
	}
	 */
	public void setPlayer(@Nonnull EntityPlayer player) {
		TileInterface.player = player;
		playerName = player.getName();
		markForSync();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTHelper.set(compound, "player", playerName);
		NBTHelper.set(compound, "on", on);
		return super.writeToNBT(compound);
	}

}
