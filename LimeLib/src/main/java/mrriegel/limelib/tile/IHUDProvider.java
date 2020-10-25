package mrriegel.limelib.tile;

import java.util.List;

import javax.annotation.Nullable;

import mrriegel.limelib.util.LimeCapabilities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;

public interface IHUDProvider {

	public static final String SHADOWFONT = "?~_\u6F2B~%z";

	//TODO add player, remove sneak
	@Nullable
	List<String> getData(boolean sneak, EnumFacing facing);

	default int getBackgroundColor(boolean sneak, EnumFacing facing) {
		return 0x44CCCCFF;
	}

	default Side readingSide() {
		return Side.CLIENT;
	}

	default boolean center(boolean sneak, EnumFacing facing) {
		return true;
	}

	//TODO rename to fontscale
	default double scale(boolean sneak, EnumFacing facing) {
		return .8;
	}

	default double totalScale(EntityPlayer player) {
		return 1.;
	}

	default boolean lineBreak(boolean sneak, EnumFacing facing) {
		return true;
	}

	default boolean requireFocus() {
		return true;
	}

	static boolean isHUDProvider(TileEntity t) {
		if (t != null)
			return t.hasCapability(LimeCapabilities.hudproviderCapa, null) || t instanceof IHUDProvider;
		return false;
	}

	static IHUDProvider getHUDProvider(TileEntity t) {
		IHUDProvider dk = null;
		if (t != null)
			dk = t.getCapability(LimeCapabilities.hudproviderCapa, null);
		if (dk == null)
			dk = (IHUDProvider) t;
		return dk;
	}

}
