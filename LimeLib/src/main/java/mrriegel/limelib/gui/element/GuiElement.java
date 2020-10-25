package mrriegel.limelib.gui.element;

import mrriegel.limelib.gui.GuiDrawer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

public abstract class GuiElement extends Gui {
	protected GuiDrawer drawer;
	public int x, y, width, height, id;
	protected boolean visible;
	protected Minecraft mc;

	public GuiElement(int id, int x, int y, int width, int height) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.drawer = new GuiDrawer(0, 0, 0, 0, 0);
		visible = true;
		mc = Minecraft.getMinecraft();
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseX >= x && mouseY >= y && mouseX < x + this.width && mouseY < y + this.height;
	}

	public abstract void draw(int mouseX, int mouseY);

	public void onClick(int button) {
	}

	public void onRelease(int button) {
	}

	public void onScrolled(int scroll) {
	}

	public void onUpdate() {
	}

}
