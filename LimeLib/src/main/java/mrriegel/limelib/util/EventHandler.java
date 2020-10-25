package mrriegel.limelib.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.datapart.CapabilityDataPart;
import mrriegel.limelib.datapart.DataPart;
import mrriegel.limelib.datapart.DataPartRegistry;
import mrriegel.limelib.gui.CommonContainer;
import mrriegel.limelib.network.HUDProviderMessage;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.network.PlayerClickMessage;
import mrriegel.limelib.tile.CommonTile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickEmpty;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(modid = LimeLib.MODID)
public class EventHandler {

	@SubscribeEvent
	public static void tick(WorldTickEvent event) {
		if (event.phase == Phase.END && event.side == Side.SERVER) {
			try {
				if (event.world.getTotalWorldTime() % 4 == 0) {
					for (int i = 0; i < event.world.loadedTileEntityList.size(); i++) {
						TileEntity t = event.world.loadedTileEntityList.get(i);
						CommonTile tile = t instanceof CommonTile ? (CommonTile) t : null;
						if (tile != null && tile.needsSync()) {
							tile.sync();
							tile.unmarkForSync();
						}
					}
				}
			} catch (ConcurrentModificationException e) {
			}
			DataPartRegistry reg = DataPartRegistry.get(event.world);
			if (reg != null) {
				Iterator<DataPart> it = reg.getParts().stream().filter(p -> p != null && event.world.isBlockLoaded(p.getPos())).collect(Collectors.toList()).iterator();
				while (it.hasNext()) {
					DataPart part = it.next();
					part.updateServer(event.world);
					part.ticksExisted++;
					if (event.world.getTotalWorldTime() % 200 == 0)
						reg.sync(part.getPos(), false);
				}
			}
		}
	}

	@SubscribeEvent
	public static void playerTick(PlayerTickEvent event) {
		if (event.phase == Phase.END && event.side == Side.SERVER) {
			if (event.player.world.getTotalWorldTime() % 5 == 0) {
				PacketHandler.sendTo(new HUDProviderMessage((EntityPlayerMP) event.player), (EntityPlayerMP) event.player);
			}
		}
		if (event.phase == Phase.END) {
			if (event.player.openContainer instanceof CommonContainer) {
				((CommonContainer<?>) event.player.openContainer).update(event.player);
			}
		}
	}

	@SubscribeEvent
	public static void attachWorld(AttachCapabilitiesEvent<World> event) {
		event.addCapability(DataPartRegistry.LOCATION, new CapabilityDataPart.CapaProvider(event.getObject()));
	}

	@SubscribeEvent
	public static void login(PlayerLoggedInEvent event) {
		if (!event.player.world.isRemote)
			((IThreadListener) event.player.world).addScheduledTask(() -> {
				DataPartRegistry reg = DataPartRegistry.get(event.player.world);
				if (reg != null)
					reg.getParts().forEach(p -> reg.sync(p.getPos(), true));
			});
	}

	private static Map<Side, Map<String, Long>> lastClicks = null;

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void interact(PlayerInteractEvent event) {
		boolean left = event instanceof LeftClickBlock || event instanceof LeftClickEmpty;
		boolean right = event instanceof RightClickBlock || event instanceof RightClickEmpty || event instanceof RightClickItem;
		boolean server = event instanceof RightClickBlock || event instanceof LeftClickBlock || event instanceof RightClickItem;
		if ((left || right) && !event.isCanceled() && !(event.getEntityPlayer() instanceof FakePlayer)) {
			DataPart part = DataPart.rayTrace(event.getEntityPlayer());
			Side side = event.getSide();
			if (part != null && part.clientValid() && !event.getEntityPlayer().isSneaking()) {
				if (lastClicks == null) {
					lastClicks = Maps.newTreeMap();
					lastClicks.put(Side.SERVER, Maps.newTreeMap());
					lastClicks.put(Side.CLIENT, Maps.newTreeMap());
				}
				boolean touch = false;
				if (!lastClicks.get(side).containsKey(event.getEntityPlayer().getName()) || System.currentTimeMillis() - lastClicks.get(side).get(event.getEntityPlayer().getName()) > (left ? 150 : 40)) {
					if (left)
						touch = part.onLeftClicked(event.getEntityPlayer(), event.getHand());
					else {
						touch = part.onRightClicked(event.getEntityPlayer(), event.getHand());
						event.getEntityPlayer().swingArm(event.getHand());
					}
					if (!server && event.getWorld().isRemote)
						PacketHandler.sendToServer(new PlayerClickMessage(part.getPos(), event.getHand(), left));
					lastClicks.get(side).put(event.getEntityPlayer().getName(), System.currentTimeMillis());
				}
				if (!touch)
					return;
				if (event.isCancelable())
					event.setCanceled(true);
				event.setResult(Result.DENY);
				if (event instanceof LeftClickBlock) {
					((LeftClickBlock) event).setUseBlock(Result.DENY);
					((LeftClickBlock) event).setUseItem(Result.DENY);
				} else if (event instanceof RightClickBlock) {
					((RightClickBlock) event).setUseBlock(Result.DENY);
					((RightClickBlock) event).setUseItem(Result.DENY);
				}
			}
		}
	}
}
