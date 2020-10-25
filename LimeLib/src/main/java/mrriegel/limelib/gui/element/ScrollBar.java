package mrriegel.limelib.gui.element;

import org.apache.commons.lang3.Validate;

import mrriegel.limelib.gui.GuiDrawer;
import net.minecraft.util.EnumFacing.Plane;

public class ScrollBar extends GuiElement {

	public final Plane orientation;
	public double status = 0.;

	public ScrollBar(int id, int x, int y, int width, int height, GuiDrawer drawer, Plane orientation) {
		super(id, x, y, width, height);
		this.orientation = Validate.notNull(orientation);
	}

	@Override
	public void draw(int mouseX, int mouseY) {
		drawer.drawFramedRectangle(x, y, width, height);
		int b = Math.min(height, width) - 2;
		if (orientation == Plane.VERTICAL)
			drawer.drawBackgroundTexture(x + 1, y + 1 + ((int) (status * (height - b - 2))), b, b);
		else
			drawer.drawBackgroundTexture(x + 1 + ((int) (status * (width - b - 2))), y + 1, b, b);

	}

}
