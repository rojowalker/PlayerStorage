package mrriegel.limelib.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;

import mrriegel.limelib.LimeConfig;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.datapart.DataPart;
import mrriegel.limelib.datapart.DataPartRegistry;
import mrriegel.limelib.datapart.RenderRegistry;
import mrriegel.limelib.datapart.RenderRegistry.RenderDataPart;
import mrriegel.limelib.helper.ParticleHelper;
import mrriegel.limelib.tile.IHUDProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(modid = LimeLib.MODID, value = Side.CLIENT)
public class ClientEventHandler {

	@SubscribeEvent
	public static void onTextureStitch(TextureStitchEvent event) {
		event.getMap().registerSprite(ParticleHelper.roundParticle);
		event.getMap().registerSprite(ParticleHelper.sparkleParticle);
		event.getMap().registerSprite(ParticleHelper.squareParticle);
	}

	private static Minecraft mc = null;

	private static Minecraft getMC() {
		if (mc == null)
			mc = Minecraft.getMinecraft();
		return mc;
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent event) {
		Minecraft mc = getMC();
		if (event.phase == Phase.END && mc.world != null && !mc.isGamePaused()) {
			DataPartRegistry reg = DataPartRegistry.get(mc.world);
			if (reg != null) {
				Iterator<DataPart> it = reg.getParts().stream().filter(p -> p != null && mc.world.isBlockLoaded(p.getPos())).collect(Collectors.toList()).iterator();
				while (it.hasNext()) {
					DataPart part = it.next();
					part.updateClient(mc.world);
					part.ticksExisted++;
				}
			}
			if (mc.player != null && mc.player.ticksExisted % 2 == 0)
				rayTrace = DataPart.rayTrace(mc.player);
		}
	}

	public static Map<BlockPos, Map<EnumFacing, List<String>>> supplierTexts = Maps.newHashMap();

	@SubscribeEvent
	public static void render(RenderWorldLastEvent event) {
		//ihudprovider
		try {
			for (TileEntity t : getMC().world.loadedTileEntityList) {
				if (IHUDProvider.isHUDProvider(t) && t.getPos().getDistance((int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ) < 24) {
					IHUDProvider tile = IHUDProvider.getHUDProvider(t);
					RayTraceResult rtr = getMC().objectMouseOver;
					if (!tile.requireFocus() || (rtr != null && rtr.typeOfHit == Type.BLOCK && rtr.getBlockPos().equals(t.getPos()))) {
						boolean sneak = getMC().player.isSneaking();
						EnumFacing face = null;
						boolean playerhorizontal = !false;
						if (playerhorizontal || face.getAxis() == Axis.Y || !rtr.getBlockPos().equals(t.getPos())) {
							//						face = getMC().player.getHorizontalFacing();
							Vec3d v = new Vec3d(t.getPos().getX() + .5, mc.player.getPositionEyes(0).y, t.getPos().getZ() + .5);
							v = v.subtract(mc.player.getPositionEyes(0));
							face = EnumFacing.getFacingFromVector((float) v.x, (float) v.y, (float) v.z);
						}
						List<String> tmp = null;
						if (tile.readingSide().isServer()) {
							Map<EnumFacing, List<String>> faceMap = supplierTexts.get(t.getPos());
							if (faceMap != null) {
								List<String> foo = faceMap.get(face.getOpposite());
								tmp = foo;
							} else
								tmp = tile.getData(sneak, face.getOpposite());
						} else
							tmp = tile.getData(sneak, face.getOpposite());
						if (tmp != null && !tmp.isEmpty()) {
							double x = t.getPos().getX() - TileEntityRendererDispatcher.staticPlayerX;
							double y = t.getPos().getY() - TileEntityRendererDispatcher.staticPlayerY;
							double z = t.getPos().getZ() - TileEntityRendererDispatcher.staticPlayerZ;
							GlStateManager.pushMatrix();
							double dx = face.getAxis() == Axis.Z ? 0.5F : Math.max(-0.001, face.getAxisDirection().getOffset() * -1.001);
							double dz = face.getAxis() == Axis.X ? 0.5F : Math.max(-0.001, face.getAxisDirection().getOffset() * -1.001);
							GlStateManager.translate((float) x + dx, (float) y + 1F, (float) z + dz);
							float f1 = face.getHorizontalIndex() * 90f;
							if (face.getAxis() == Axis.Z)
								f1 += 180f;
							GlStateManager.rotate(f1, 0.0F, 1.0F, 0.0F);
							GlStateManager.enableRescaleNormal();
							FontRenderer fontrenderer = getMC().fontRenderer;
							float f3 = 0.010416667F;
							//					GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
							GlStateManager.scale(f3, -f3, f3);
							GlStateManager.glNormal3f(0.0F, 0.0F, -f3);
							GlStateManager.depthMask(false);
							final int maxWordLength = 93;
							boolean cutLongLines = tile.lineBreak(sneak, face.getOpposite());
							final double factor = MathHelper.clamp(tile.scale(sneak, face.getOpposite()), .1, 2.);
							List<String> text = true ? new ArrayList<>()
									: tmp.stream().filter(Objects::nonNull)//
											.flatMap(s -> (!cutLongLines ? Collections.singletonList(s) : fontrenderer.listFormattedStringToWidth(s, (int) (maxWordLength / factor))).stream()).collect(Collectors.toList());
							text.clear();
							for (String s : tmp) {
								boolean shad = s.contains(IHUDProvider.SHADOWFONT);
								if (!cutLongLines)
									text.add(s);
								else {
									for (String ss : fontrenderer.listFormattedStringToWidth(s, (int) (maxWordLength / factor)))
										text.add((shad ? IHUDProvider.SHADOWFONT : "") + ss);
								}
							}
							int lineHeight = fontrenderer.FONT_HEIGHT + 1;
							int oy = (int) -(lineHeight * text.size() * factor);
							int ysize = -oy;
							int color = tile.getBackgroundColor(sneak, face.getOpposite());
							double totalScale = MathHelper.clamp(tile.totalScale(getMC().player), .1, 2.);
							//						totalScale = mc.player.getPositionVector().distanceTo(new Vec3d(t.getPos())) * .75;
							GlStateManager.scale(totalScale, totalScale, totalScale);
							GuiUtils.drawGradientRect(0, -48, oy, -48 + 96, ysize + oy, color, color);
							GlStateManager.translate(0, -text.size() * lineHeight * factor, 0);
							GlStateManager.scale(factor, factor, factor);
							for (int j = 0; j < text.size(); ++j) {
								String s = text.get(j);
								boolean shadow = s.contains(IHUDProvider.SHADOWFONT);
								if (shadow)
									s = s.replace(IHUDProvider.SHADOWFONT, "");
								int width = fontrenderer.getStringWidth(s);
								boolean tooLong = !cutLongLines && width * factor > maxWordLength;
								double fac = maxWordLength / (width * factor);
								int xx = tile.center(sneak, face.getOpposite()) || tooLong ? -width / 2 : (int) (-46 / factor);
								if (tooLong)
									GlStateManager.scale(fac, 1, 1);
								fontrenderer.drawString(s, xx, j * 10 + 1, 0xFFFFFFFF, shadow);
								if (tooLong)
									GlStateManager.scale(1. / fac, 1, 1);
							}
							GlStateManager.scale(1. / factor, 1. / factor, 1. / factor);
							GlStateManager.scale(1. / totalScale, 1. / totalScale, 1. / totalScale);
							GlStateManager.depthMask(true);
							GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
							GlStateManager.popMatrix();

						}
					}
				}
			}
		} catch (ConcurrentModificationException e) {
		}
		//datapart
		DataPartRegistry reg = DataPartRegistry.get(getMC().world);
		if (reg != null) {
			reg.getParts().stream().filter(p -> p != null && getMC().player.getDistance(p.getX(), p.getY(), p.getZ()) < 64).//
					sorted((b, a) -> Double.compare(getMC().player.getDistance(a.getX(), a.getY(), a.getZ()), getMC().player.getDistance(b.getX(), b.getY(), b.getZ()))).forEach(p -> {
						@SuppressWarnings("rawtypes")
						RenderDataPart ren = RenderRegistry.map.get(p.getClass());
						if (ren != null)
							ren.render(p, p.getX() - TileEntityRendererDispatcher.staticPlayerX, p.getY() - TileEntityRendererDispatcher.staticPlayerY, p.getZ() - TileEntityRendererDispatcher.staticPlayerZ, event.getPartialTicks());
					});
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void itemToolTip(ItemTooltipEvent event) {
		Minecraft mc = getMC();
		if (LimeConfig.commandBlockCreativeTab && mc.currentScreen instanceof GuiContainerCreative && ((GuiContainerCreative) mc.currentScreen).getSelectedTabIndex() == CreativeTabs.REDSTONE.getIndex() && Block.getBlockFromItem(event.getItemStack().getItem()) instanceof BlockCommandBlock) {
			event.getToolTip().add(TextFormatting.YELLOW + LimeConfig.CONFIGHINT);
		}

	}

	public static DataPart rayTrace = null;

	@SubscribeEvent
	public static void draw(DrawBlockHighlightEvent event) {
		DataPart part = rayTrace;
		if (part != null && part.getHighlightBox() != null) {
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(2.0F);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			BlockPos blockpos = part.getPos();
			double d0 = TileEntityRendererDispatcher.staticPlayerX;
			double d1 = TileEntityRendererDispatcher.staticPlayerY;
			double d2 = TileEntityRendererDispatcher.staticPlayerZ;
			RenderGlobal.drawSelectionBoundingBox(part.getHighlightBox().offset(blockpos).grow(0.0020000000949949026D).offset(-d0, -d1, -d2), 0.0F, 0.0F, 0.0F, 0.4F);
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			event.setCanceled(true);
		}
	}

}
