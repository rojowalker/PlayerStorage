package mrriegel.limelib;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

//@EventBusSubscriber(modid = LimeLib.MODID)
public class WorldAddition implements INBTSerializable<NBTTagCompound> {

	private World world;

	@Override
	public NBTTagCompound serializeNBT() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
	}

	@CapabilityInject(WorldAddition.class)
	public static Capability<WorldAddition> CAPA;
	private static final ResourceLocation LOCATION = new ResourceLocation(LimeLib.MODID, "addition");

	public static void register() {
		CapabilityManager.INSTANCE.register(WorldAddition.class, new IStorage<WorldAddition>() {

			@Override
			public NBTBase writeNBT(Capability<WorldAddition> capability, WorldAddition instance, EnumFacing side) {
				return instance.serializeNBT();
			}

			@Override
			public void readNBT(Capability<WorldAddition> capability, WorldAddition instance, EnumFacing side, NBTBase nbt) {
				if (nbt instanceof NBTTagCompound) {
					instance.deserializeNBT((NBTTagCompound) nbt);
				}
			}

		}, WorldAddition::new);
	}

	public static WorldAddition getAddition(World world) {
		return (world == null || !world.hasCapability(CAPA, null)) ? null : world.getCapability(CAPA, null);
	}

	@SubscribeEvent
	public static void attach(AttachCapabilitiesEvent<World> event) {
		event.addCapability(LOCATION, new Provider(event.getObject()));
	}

	private static class Provider implements ICapabilitySerializable<NBTTagCompound> {

		WorldAddition wa = CAPA.getDefaultInstance();

		public Provider(World world) {
			this.wa.world = world;
		}

		@Override
		public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
			return capability == CAPA;
		}

		@Override
		public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
			return hasCapability(capability, facing) ? CAPA.cast(wa) : null;
		}

		@Override
		public NBTTagCompound serializeNBT() {
			return (NBTTagCompound) CAPA.getStorage().writeNBT(CAPA, wa, null);
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			CAPA.getStorage().readNBT(CAPA, wa, null, nbt);
		}
	}

}
