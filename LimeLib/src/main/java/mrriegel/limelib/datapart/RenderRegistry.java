package mrriegel.limelib.datapart;

import java.util.Map;

import com.google.common.collect.Maps;

public class RenderRegistry {

	public static Map<Class<? extends DataPart>, RenderDataPart<? extends DataPart>> map = Maps.newHashMap();

	public static void register(Class<? extends DataPart> clazz, RenderDataPart<?> render) {
		map.put(clazz, render);
	}

	public static abstract class RenderDataPart<T extends DataPart> {

		public abstract void render(T part, double x, double y, double z, float partialTicks);
	}

}
