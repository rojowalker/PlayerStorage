package mrriegel.limelib.plugin;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import mcjty.theoneprobe.api.ITheOneProbe;
import mrriegel.limelib.tile.IInfoProvider;
import net.minecraft.tileentity.TileEntity;

public class TOP implements Function<ITheOneProbe, Void> {

	@Override
	public Void apply(ITheOneProbe t) {
		for (Pair<IInfoProvider<?>, Class<? extends TileEntity>> e : IInfoProvider.getProviders()) {
			t.registerProvider(e.getKey());
		}
		return null;
	}

}
