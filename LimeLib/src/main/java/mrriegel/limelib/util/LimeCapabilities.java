package mrriegel.limelib.util;

import mrriegel.limelib.tile.IHUDProvider;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class LimeCapabilities {

	@CapabilityInject(IHUDProvider.class)
	public static Capability<IHUDProvider> hudproviderCapa = null;

	public static void register() {
		CapabilityManager.INSTANCE.register(IHUDProvider.class, new NullStorage<IHUDProvider>(), () -> {
			throw new UnsupportedOperationException();
		});
	}

	private static class NullStorage<T> implements IStorage<T> {

		@Override
		public NBTBase writeNBT(Capability<T> capability, T instance, EnumFacing side) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void readNBT(Capability<T> capability, T instance, EnumFacing side, NBTBase nbt) {
			throw new UnsupportedOperationException();
		}

	}

}
