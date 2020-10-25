package mrriegel.limelib.gui.button;

import mrriegel.limelib.gui.GuiDrawer;
import mrriegel.limelib.gui.GuiDrawer.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiButtonExt;

public class GuiButtonArrow extends GuiButtonExt {

	Direction dir;

	public GuiButtonArrow(int id, int xPos, int yPos, Direction dir) {
		super(id, xPos, yPos, getWidth(dir), getHeight(dir), "");
		this.dir = dir;
	}

	private static int getWidth(Direction dir) {
		switch (dir) {
		case DOWN:
		case UP:
			return 18;
		case LEFT:
		case RIGHT:
			return 10;
		}
		return 0;
	}

	private static int getHeight(Direction dir) {
		switch (dir) {
		case DOWN:
		case UP:
			return 10;
		case LEFT:
		case RIGHT:
			return 18;
		}
		return 0;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partial) {
		if (this.visible) {
			this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			int k = this.getHoverState(this.hovered);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			int tx = dir == Direction.DOWN || dir == Direction.UP ? 124 : dir == Direction.LEFT ? 142 : 152;
			int ty = dir == Direction.UP || dir == Direction.LEFT || dir == Direction.RIGHT ? 0 : 10;

			mc.getTextureManager().bindTexture(GuiDrawer.COMMON_TEXTURES);
			drawTexturedModalRect(x, y, tx, ty + k * 20, width, height);
			// GuiUtils.drawContinuousTexturedBox(xPosition, yPosition, 0, 0,
			// 18, 18, 18, 18, 1, zLevel);
			this.mouseDragged(mc, mouseX, mouseY);
		}
	}

}
