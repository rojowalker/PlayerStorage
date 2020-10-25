package mrriegel.limelib.datapart;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.DataPartSyncMessage;
import mrriegel.limelib.network.PacketHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

//TODO rename
public class DataPartRegistry implements INBTSerializable<NBTTagCompound> {

	public static final ResourceLocation LOCATION = new ResourceLocation(LimeLib.MODID + ":datapart");
	public static final BiMap<String, Class<? extends DataPart>> PARTS = HashBiMap.create();

	private Map<BlockPos, DataPart> partMap = Maps.newHashMap();
	public World world;

	public static DataPartRegistry get(World world) {
		if (world == null)
			return null;
		DataPartRegistry reg = world.hasCapability(CapabilityDataPart.DATAPART, null) ? world.getCapability(CapabilityDataPart.DATAPART, null) : null;
		if (reg != null) {
			if (reg.world == null)
				reg.world = world;
			for (DataPart part : reg.partMap.values())
				if (part.world == null)
					part.world = world;
		}
		return reg;
	}

	public static void register(String name, Class<? extends DataPart> clazz) {
		Validate.isTrue(!PARTS.containsKey(name) && !PARTS.containsValue(clazz), "already registered");
		Validate.isTrue(Stream.of(clazz.getConstructors()).anyMatch(c -> c.getParameterCount() == 0), "empty constructor required");
		PARTS.put(name, clazz);
	}

	public DataPart getDataPart(BlockPos pos) {
		world.getChunk(pos);
		return partMap.get(pos);
	}

	public BlockPos nextPos(BlockPos pos) {
		if (getDataPart(pos) == null)
			return pos;
		Chunk chunk = world.getChunk(pos);
		List<BlockPos> posses = StreamSupport.stream(BlockPos.getAllInBox(pos.add(7, 7, 7), pos.add(-7, -7, -7)).spliterator(), false).//
				filter(p -> world.getChunk(p) == chunk).sorted((p1, p2) -> {
					int res = Double.compare(p1.distanceSq(pos), p2.distanceSq(pos));
					if (res != 0)
						return res;
					return Integer.compare(p2.getY(), p1.getY());
				}).collect(Collectors.toList());
		for (BlockPos p : posses) {
			if (getDataPart(p) == null)
				return p;
		}
		return null;
	}

	public boolean addDataPart(BlockPos pos, DataPart part, boolean force) {
		Validate.notNull(part);
		if (!PARTS.inverse().containsKey(part.getClass())) {
			LimeLib.log.error(part.getClass() + " not registered.");
			return false;
		}
		if (world.isRemote && !part.clientValid())
			return false;
		pos = pos.toImmutable();
		part.pos = pos;
		part.world = world;
		if (partMap.get(pos) == null || force) {
			partMap.put(pos, part);
			part.onAdded();
			sync(pos, true);
			return true;
		}
		return false;
	}

	public void removeDataPart(BlockPos pos) {
		if (partMap.containsKey(pos)) {
			partMap.get(pos).onRemoved();
			partMap.remove(pos);
			sync(pos, true);
		}
	}

	public void clearWorld() {
		partMap.clear();
	}

	public Collection<DataPart> getParts() {
		return Collections.unmodifiableCollection(partMap.values());
	}

	public void sync(BlockPos pos, boolean toAllPlayers) {
		DataPart dp = getDataPart(pos);
		if (world != null && !world.isRemote && (dp == null || dp.clientValid())) {
			DataPartSyncMessage message = new DataPartSyncMessage(dp, pos, partMap.values().stream().map(DataPart::getPos).collect(Collectors.toList()));
			if (toAllPlayers)
				PacketHandler.sendToDimension(message, world.provider.getDimension());
			else
				PacketHandler.sendToAllAround(message, new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 18));
		}
	}

	public void sync(BlockPos pos) {
		sync(pos, false);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		List<NBTTagCompound> nbts = Lists.newArrayList();
		for (DataPart entry : partMap.values())
			nbts.add(entry.writeDataToNBT(new NBTTagCompound()));
		return NBTHelper.setList(new NBTTagCompound(), "nbts", nbts);
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		clearWorld();
		List<NBTTagCompound> nbts = NBTHelper.getList(nbt, "nbts", NBTTagCompound.class);
		for (NBTTagCompound n : nbts) {
			createPart(n);
		}
	}

	public void createPart(NBTTagCompound n) {
		try {
			Class<?> clazz = PARTS.get(n.getString("id"));
			if (clazz != null && DataPart.class.isAssignableFrom(clazz)) {
				DataPart part = ((Class<? extends DataPart>) clazz).newInstance();
				if (part != null) {
					part.setWorld(world);
					part.readDataFromNBT(n);
					addDataPart(part.pos, part, true);
					return;
				}
			}
		} catch (ReflectiveOperationException e) {
		}
		LimeLib.log.error("Failed to create datapart " + n.getString("id"));
	}

}