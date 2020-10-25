package mrriegel.limelib.helper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.WorldGenMinable;

public class WorldHelper {

	public static void addOreSpawn(IBlockState state, World world, int veinPerChunk, int size, int chunkX, int chunkZ, int minY, int maxY) {
		addOreSpawn(state, world, veinPerChunk, size, chunkX, chunkZ, minY, maxY, BlockMatcher.forBlock(Blocks.STONE));
	}

	public static void addOreSpawn(IBlockState state, World world, int veinPerChunk, int size, int chunkX, int chunkZ, int minY, int maxY, Predicate<IBlockState> predicate) {
		for (int i = 0; i < veinPerChunk; i++) {
			int diffBtwnMinMaxY = maxY - minY;
			int x = (chunkX << 4) + world.rand.nextInt(16);
			int y = minY + world.rand.nextInt(diffBtwnMinMaxY);
			int z = (chunkZ << 4) + world.rand.nextInt(16);
			new WorldGenMinable(state, size, b -> predicate.test(b)).generate(world, world.rand, new BlockPos(x, y, z));
		}
	}

	public static double getDistance(BlockPos a, BlockPos b) {
		return Math.sqrt(a.distanceSq(b));
	}

	public static boolean spawnInRange(Entity entity, World world, BlockPos pos, int range) {
		List<BlockPos> lis = Lists.newArrayList();
		for (int x = pos.getX() - range; x <= pos.getX() + range; x++)
			for (int y = pos.getY() - range; y <= pos.getY() + range; y++)
				for (int z = pos.getZ() - range; z <= pos.getZ() + range; z++)
					lis.add(new BlockPos(x, y, z));
		List<BlockPos> end = Lists.newArrayList();
		for (BlockPos b : lis)
			if (canCreatureTypeSpawnAtLocation(entity, SpawnPlacementType.ON_GROUND, world, b))
				end.add(b);
		if (end.isEmpty())
			return false;
		Collections.shuffle(end);
		for (BlockPos fin : end) {
			entity.posX = fin.getX() + .5D;
			entity.posY = fin.getY() + .1D;
			entity.posZ = fin.getZ() + .5D;
			boolean spawned = entity.world.spawnEntity(entity);
			entity.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
			if (spawned)
				return true;
		}
		return false;
	}

	public static boolean canCreatureTypeSpawnAtLocation(Entity entity, EntityLiving.SpawnPlacementType spawnPlacementTypeIn, World worldIn, BlockPos pos) {
		if (!worldIn.getWorldBorder().contains(pos)) {
			return false;
		} else {
			IBlockState iblockstate = worldIn.getBlockState(pos);
			if (spawnPlacementTypeIn == EntityLiving.SpawnPlacementType.IN_WATER) {
				return iblockstate.getMaterial().isLiquid() && worldIn.getBlockState(pos.down()).getMaterial().isLiquid() && !worldIn.getBlockState(pos.up()).isNormalCube();
			} else {
				BlockPos blockpos = pos.down();
				IBlockState state = worldIn.getBlockState(blockpos);

				if (!state.getBlock().canCreatureSpawn(state, worldIn, blockpos, spawnPlacementTypeIn)) {
					return false;
				} else {
					Block block = worldIn.getBlockState(blockpos).getBlock();
					boolean flag = block != Blocks.BEDROCK && block != Blocks.BARRIER;
					boolean upFree = true;
					for (int i = 1; i <= MathHelper.floor(entity.height) && upFree; i++)
						if (!WorldEntitySpawner.isValidEmptySpawnBlock(worldIn.getBlockState(pos.up(i))))
							upFree = false;
					return flag && upFree && WorldEntitySpawner.isValidEmptySpawnBlock(iblockstate);
				}
			}
		}
	}

	public static boolean wayFree(World world, BlockPos pos1, BlockPos pos2) {
		return wayFree(world, pos1.getX() + .5, pos1.getY() + .5, pos1.getZ() + .5, pos2.getX() + .5, pos2.getY() + .5, pos2.getZ() + .5);
	}

	public static boolean wayFree(World world, double x1, double y1, double z1, double x2, double y2, double z2) {
		Vec3d p1 = new Vec3d(x1, y1, z1);
		Vec3d p2 = new Vec3d(x2, y2, z2);
		Vec3d d = new Vec3d(x2 - x1, y2 - y1, z2 - z1);
		d = d.normalize().scale(0.25);
		Set<BlockPos> set = Sets.newHashSet();
		while (p1.distanceTo(p2) > 0.5) {
			set.add(new BlockPos(p1));
			p1 = p1.add(d);
		}
		set.remove(new BlockPos(p1));
		set.remove(new BlockPos(p2));
		for (BlockPos p : set)
			if (!world.isAirBlock(p))
				return false;
		return true;
	}

	public static List<BlockPos> getChunk(World world, BlockPos pos) {
		Chunk chunk = world.getChunk(pos);
		List<BlockPos> lis = Lists.newLinkedList();
		for (int y = world.getActualHeight() - 1; y > 0; y--)
			for (int x = chunk.x * 16; x < chunk.x * 16 + 16; x++)
				for (int z = chunk.z * 16; z < chunk.z * 16 + 16; z++)
					lis.add(new BlockPos(x, y, z));
		return lis;
	}

	public static List<BlockPos> getNeighbors(BlockPos p) {
		return Lists.newArrayList(EnumFacing.VALUES).stream().map(f -> p.offset(f)).collect(Collectors.toList());
	}

}
