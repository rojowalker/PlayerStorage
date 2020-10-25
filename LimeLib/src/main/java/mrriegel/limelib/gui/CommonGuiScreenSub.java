package mrriegel.limelib.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public abstract class CommonGuiScreenSub extends CommonGuiScreen {

	protected GuiScreen parent;

	public CommonGuiScreenSub() {
		super();
		this.parent = Minecraft.getMinecraft().currentScreen;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_ESCAPE && parent != null) {
			mc.currentScreen = parent;
			onClosed();
		} else
			super.keyTyped(typedChar, keyCode);
	}

	protected void onClosed() {
	};

}
