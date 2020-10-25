package mrriegel.limelib.gui;

import java.io.IOException;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.collect.Lists;

import mrriegel.limelib.gui.element.GuiElement;
import mrriegel.limelib.gui.element.ITooltip;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

public class CommonGuiContainer extends GuiContainer {

	protected GuiDrawer drawer;
	protected List<GuiElement> elementList = Lists.newArrayList();

	public CommonGuiContainer(Container inventorySlotsIn) {
		super(inventorySlotsIn);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		for (GuiElement e : elementList)
			if (e.isVisible())
				e.draw(mouseX, mouseY);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		renderHoveredToolTip(mouseX, mouseY);
		for (GuiElement e : elementList)
			if (e.isMouseOver(mouseX, mouseY) && e instanceof ITooltip && e.isVisible())
				((ITooltip) e).drawTooltip(mouseX, mouseY);
		for (GuiButton e : buttonList)
			if (e instanceof ITooltip)
				if (e.isMouseOver())
					((ITooltip) e).drawTooltip(mouseX, mouseY);
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);
		drawer = new GuiDrawer(guiLeft, guiTop, xSize, ySize, zLevel);
		elementList.clear();
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		for (GuiElement e : elementList)
			if (e.isMouseOver(mouseX, mouseY) && e.isVisible()) {
				e.onClick(mouseButton);
				if (mouseButton == 0)
					elementClicked(e);
			}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		for (GuiElement e : elementList)
			if (e.isMouseOver(mouseX, mouseY) && e.isVisible())
				e.onRelease(state);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
		int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
		for (GuiElement e : elementList)
			if (e.isMouseOver(mouseX, mouseY) && e.isVisible())
				e.onScrolled(Mouse.getEventDWheel());
	}

	protected void elementClicked(GuiElement element) {
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		for (GuiElement e : elementList)
			e.onUpdate();
	}

}
