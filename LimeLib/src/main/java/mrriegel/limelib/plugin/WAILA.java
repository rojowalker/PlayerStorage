package mrriegel.limelib.plugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.WailaPlugin;
import mrriegel.limelib.tile.IInfoProvider;
import net.minecraft.tileentity.TileEntity;

@WailaPlugin
public class WAILA implements IWailaPlugin {

	@Override
	public void register(IWailaRegistrar registrar) {
		for (Pair<IInfoProvider<?>, Class<? extends TileEntity>> e : IInfoProvider.getProviders()) {
			Class<? extends TileEntity> c = e.getValue();
			IInfoProvider<?> iip = e.getKey();
			Set<String> methods = Arrays.stream(iip.getClass().getMethods()).filter(m -> m.getDeclaringClass() == iip.getClass()).map(Method::getName).collect(Collectors.toSet());
			if (methods.contains("getHeadLines"))
				registrar.registerHeadProvider(iip, c);
			if (methods.contains("getBodyLines"))
				registrar.registerBodyProvider(iip, c);
			if (methods.contains("getTailLines"))
				registrar.registerTailProvider(iip, c);
			if (iip.readingSide().isServer())
				registrar.registerNBTProvider(iip, c);
		}
	}

}
