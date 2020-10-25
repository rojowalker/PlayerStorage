package mrriegel.limelib.gui;

import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import mrriegel.limelib.LimeLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class GuiDrawer {

	public static final ResourceLocation COMMON_TEXTURES = new ResourceLocation(LimeLib.MODID + ":textures/gui/base.png");

	public int guiLeft, guiTop, xSize, ySize;
	public float zLevel = 0;
	private static Minecraft mc = Minecraft.getMinecraft();

	public GuiDrawer(int guiLeft, int guiTop, int xSize, int ySize, float zLevel) {
		super();
		this.guiLeft = guiLeft;
		this.guiTop = guiTop;
		this.xSize = xSize;
		this.ySize = ySize;
		this.zLevel = zLevel;
	}

	public void drawSlot(int x, int y) {
		drawSizedSlot(x, y, 18);
	}

	public void drawSizedSlot(int x, int y, int size) {
		drawFramedRectangle(x, y, size, size);
	}

	public void drawPlayerSlots(int x, int y) {
		drawSlots(x, y + 58, 9, 1);
		drawSlots(x, y, 9, 3);
	}

	public void drawSlots(int x, int y, int width, int height) {
		for (int k = 0; k < height; ++k)
			for (int i = 0; i < width; ++i)
				drawSlot(x + i * 18, y + k * 18);
	}

	public void drawScrollbar(int x, int y, int length, float percent, Direction dir) {
		int width = dir.isHorizontal() ? length : 10;
		int height = dir.isHorizontal() ? 10 : length;
		drawFramedRectangle(x, y, width, height);
		if (!dir.isHorizontal())
			new GuiButtonExt(0, x + guiLeft + 1, y + guiTop + 1 + (int) (percent * (length - 10)), 8, 8, "").drawButton(mc, getMouseX(), getMouseY(), mc.getTickLength());
		else
			new GuiButtonExt(0, x + guiLeft + 1 + (int) (percent * (length - 10)), y + guiTop + 1, 8, 8, "").drawButton(mc, getMouseX(), getMouseY(), mc.getTickLength());
	}

	public void drawTextfield(int x, int y, int width) {
		drawFramedRectangle(x, y, width, 12);
	}

	public void drawTextfield(GuiTextField textfield) {
		if (!textfield.getEnableBackgroundDrawing())
			drawTextfield(textfield.x - guiLeft - 2, textfield.y - guiTop - 2, textfield.width + 9);
	}

	public void drawFramedRectangle(int x, int y, int width, int height) {
		bindTexture();
		GuiUtils.drawContinuousTexturedBox(x + guiLeft, y + guiTop, 0, 0, width, height, 18, 18, 1, zLevel);
	}

	public void drawBackgroundTexture(int x, int y, int width, int height) {
		bindTexture();
		GuiUtils.drawContinuousTexturedBox(x + guiLeft, y + guiTop, 0, 18, width, height, 18, 18, 4, zLevel);
	}

	public void drawBackgroundTexture(int x, int y) {
		drawBackgroundTexture(x, y, xSize, ySize);
	}

	public void drawBackgroundTexture() {
		drawBackgroundTexture(0, 0);
	}

	public void drawColoredRectangle(int x, int y, int width, int height, int color) {
		GuiUtils.drawGradientRect((int) zLevel, x + guiLeft, y + guiTop, x + width + guiLeft, y + height + guiTop, color, color);
	}

	public void drawFrame(int x, int y, int width, int height, int frame, int color) {
		drawColoredRectangle(x, y, width, frame, color);
		drawColoredRectangle(x, y + 1, frame, height, color);
		drawColoredRectangle(x + 1, y + height - (frame - 1), width, frame, color);
		drawColoredRectangle(x + width - (frame - 1), y, frame, height, color);
	}

	public void drawEnergyBarV(int x, int y, int height, float percent) {
		bindTexture();
		for (int i = 0; i < height + 1; i++)
			if (i % 2 == 0)
				GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop + i, 0, 36, 8, 1, zLevel);
			else
				GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop + i, 0, 37, 8, 1, zLevel);
		for (int i = 0; i < (height + 1) * (1f - percent); i++)
			if (i % 2 == 0)
				GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop + i, 0, 38, 8, 1, zLevel);
			else
				GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop + i, 0, 39, 8, 1, zLevel);
	}

	public void drawEnergyBarH(int x, int y, int width, float percent) {
		bindTexture();
		for (int i = 0; i < width + 1; i++)
			if (i % 2 == 0)
				GuiUtils.drawTexturedModalRect(x + guiLeft + i, y + guiTop, 8, 36, 1, 8, zLevel);
			else
				GuiUtils.drawTexturedModalRect(x + guiLeft + i, y + guiTop, 9, 36, 1, 8, zLevel);
		for (int i = 0; i < (width + 1) * (percent); i++)
			if (i % 2 == 0)
				GuiUtils.drawTexturedModalRect(x + guiLeft + i, y + guiTop, 10, 36, 1, 8, zLevel);
			else
				GuiUtils.drawTexturedModalRect(x + guiLeft + i, y + guiTop, 11, 36, 1, 8, zLevel);
	}

	public void drawFluidRect(int x, int y, int width, int height, FluidStack fluid) {
		if (fluid == null || fluid.getFluid() == null) {
			return;
		}
		TextureAtlasSprite icon = mc.getTextureMapBlocks().getTextureExtry(fluid.getFluid().getStill().toString());
		if (icon == null) {
			return;
		}
		x += guiLeft;
		y += guiTop;
		int renderAmount = Math.max(height, 1);
		int posY = y + height - renderAmount;

		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		int color = fluid.getFluid().getColor(fluid);
		GL11.glColor3ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF));

		GlStateManager.enableBlend();
		for (int i = 0; i < width; i += 16) {
			for (int j = 0; j < renderAmount; j += 16) {
				int drawWidth = Math.min(width - i, 16);
				int drawHeight = Math.min(renderAmount - j, 16);

				int drawX = x + i;
				int drawY = posY + j;

				double minU = icon.getMinU();
				double maxU = icon.getMaxU();
				double minV = icon.getMinV();
				double maxV = icon.getMaxV();

				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder tes = tessellator.getBuffer();
				tes.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
				tes.pos(drawX, drawY + drawHeight, 0).tex(minU, minV + (maxV - minV) * drawHeight / 16F).endVertex();
				tes.pos(drawX + drawWidth, drawY + drawHeight, 0).tex(minU + (maxU - minU) * drawWidth / 16F, minV + (maxV - minV) * drawHeight / 16F).endVertex();
				tes.pos(drawX + drawWidth, drawY, 0).tex(minU + (maxU - minU) * drawWidth / 16F, minV).endVertex();
				tes.pos(drawX, drawY, 0).tex(minU, minV).endVertex();
				tessellator.draw();
			}
		}
		GlStateManager.disableBlend();
	}

	public void drawItemStack(ItemStack stack, int x, int y) {
		GlStateManager.pushMatrix();
		RenderHelper.enableGUIStandardItemLighting();
		mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x + guiLeft, y + guiTop);
		GlStateManager.popMatrix();
	}

	public void drawProgressArrow(int x, int y, float percent, Direction d) {
		bindTexture();
		int totalLength = 22;
		int currentLength = (int) (totalLength * percent);
		switch (d) {
		case DOWN:
			GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop, 93, 0, 15, 22, zLevel);
			GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop, 108, 0, 16, currentLength, zLevel);
			break;
		case LEFT:
			GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop, 40, 0, 22, 15, zLevel);
			GuiUtils.drawTexturedModalRect(x + guiLeft + (totalLength - currentLength), y + guiTop, 40 + (totalLength - currentLength), 15, currentLength, 16, zLevel);
			break;
		case RIGHT:
			GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop, 18, 0, 22, 15, zLevel);
			GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop, 18, 15, currentLength, 16, zLevel);
			break;
		case UP:
			GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop, 78, 0, 15, 22, zLevel);
			GuiUtils.drawTexturedModalRect(x + guiLeft - 1, y + guiTop + (totalLength - currentLength), 62, 0 + (totalLength - currentLength), 16, currentLength, zLevel);
			break;
		}
	}

	public void drawFlame(int x, int y, float percent) {
		bindTexture();
		GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop, 24, 31, 13, 13, zLevel);
		int totalHeight = 13;
		int currentHeight = (int) (totalHeight * percent);
		GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop + (totalHeight - currentHeight), 37, 31 + (totalHeight - currentHeight), 13, 13, zLevel);
	}

	public void drawStopSign(int x, int y) {
		bindTexture();
		GuiUtils.drawTexturedModalRect(x + guiLeft, y + guiTop, 12, 36, 12, 12, zLevel);
	}

	private void bindTexture() {
		mc.getTextureManager().bindTexture(COMMON_TEXTURES);
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	public static int getMouseX() {
		return Mouse.getX() * new ScaledResolution(mc).getScaledWidth() / mc.displayWidth;
	}

	public static int getMouseY() {
		ScaledResolution sr = new ScaledResolution(mc);
		return sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
	}

	public static void renderToolTip(ItemStack stack, int x, int y) {
		List<String> list = getTooltip(stack);
		FontRenderer font = stack.getItem().getFontRenderer(stack);
		ScaledResolution sr = new ScaledResolution(mc);
		GuiUtils.drawHoveringText(list, x, y, sr.getScaledWidth(), sr.getScaledHeight(), -1, (font == null ? mc.fontRenderer : font));
	}

	public static void renderToolTip(List<String> list, int x, int y) {
		ScaledResolution sr = new ScaledResolution(mc);
		GuiUtils.drawHoveringText(list, x, y, sr.getScaledWidth(), sr.getScaledHeight(), -1, mc.fontRenderer);
	}

	public static List<String> getTooltip(ItemStack stack) {
		List<String> list = stack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
		for (int i = 0; i < list.size(); ++i) {
			if (i == 0) {
				list.set(i, stack.getRarity().color + list.get(i));
			} else {
				list.set(i, TextFormatting.GRAY + list.get(i));
			}
		}
		return list;
	}

	public static void openGui(GuiScreen screen) {
		if (FMLCommonHandler.instance().getSide().equals(Side.CLIENT)) {
			FMLCommonHandler.instance().showGuiScreen(screen);
		}
	}

	private static FontRenderer uni, notUni;

	public static FontRenderer getFontRenderer(boolean unicode) {
		if (unicode) {
			if (uni == null)
				return uni = new FontRenderer(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), mc.renderEngine, true);
			else
				return uni;
		} else {
			if (notUni == null)
				return notUni = new FontRenderer(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), mc.renderEngine, false);
			else
				return notUni;
		}
	}

	public enum Direction {
		UP, RIGHT, DOWN, LEFT;

		public boolean isHorizontal() {
			return this == RIGHT || this == LEFT;
		}
	}

}
