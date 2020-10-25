package mrriegel.limelib.helper;

import java.awt.Color;

import org.apache.commons.lang3.Validate;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.EnumDyeColor;

public class ColorHelper {

	public static int getRGB(EnumDyeColor color) {
		return color.getColorValue() | 0xFF000000;
	}

	public static int getRGB(EnumDyeColor color, int alpha) {
		return getRGB(getRGB(color), alpha);
	}

	public static int getRGB(int color, int alpha) {
		Validate.isTrue(alpha >= 0 && alpha <= 255, "alpha out of range " + alpha);
		return new Color(getRed(color), getGreen(color), getBlue(color), alpha).getRGB();
	}

	public static void glColor(int color) {
		GlStateManager.color(getRed(color) / 255f, getGreen(color) / 255f, getBlue(color) / 255f, getAlpha(color) / 255f);
	}

	public static int getRed(int color) {
		return ((0xFF000000 | color) >> 16) & 0xFF;
		// return new Color(color,true).getRed();
	}

	public static int getGreen(int color) {
		return ((0xFF000000 | color) >> 8) & 0xFF;
		// return new Color(color,true).getGreen();
	}

	public static int getBlue(int color) {
		return ((0xFF000000 | color) >> 0) & 0xFF;
		// return new Color(color,true).getBlue();
	}

	public static int getAlpha(int color) {
		return ((color) >> 24) & 0xFF;
		// return new Color(color,true).getAlpha();
	}

	public static int getRainbow(int frequence) {
		if (frequence <= 0)
			frequence = 1;
		return Color.getHSBColor(((System.currentTimeMillis() / frequence) % 360l) / 360f, 1, 1).getRGB();
	}

	public static int brighter(int color, double factor) {
		if (factor < 0D)
			factor = 0D;
		else if (factor > 1D)
			factor = 1D;
		int red = (int) Math.round(Math.min(255, getRed(color) + 255 * factor));
		int green = (int) Math.round(Math.min(255, getGreen(color) + 255 * factor));
		int blue = (int) Math.round(Math.min(255, getBlue(color) + 255 * factor));
		return new Color(red, green, blue, getAlpha(color)).getRGB();
	}

	public static int darker(int color, double factor) {
		if (factor < 0D)
			factor = 0D;
		else if (factor > 1D)
			factor = 1D;
		int red = (int) Math.round(Math.max(0, getRed(color) - 255 * factor));
		int green = (int) Math.round(Math.max(0, getGreen(color) - 255 * factor));
		int blue = (int) Math.round(Math.max(0, getBlue(color) - 255 * factor));
		return new Color(red, green, blue, getAlpha(color)).getRGB();
	}

}
