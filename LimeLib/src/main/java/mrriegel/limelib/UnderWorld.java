package mrriegel.limelib;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class UnderWorld {
	static DimensionType type;
	static int dim = 12;

	public static void init() {
		type = DimensionType.register("under", "world", dim, Prov.class, false);
		DimensionManager.registerDimension(dim, type);
		MinecraftForge.EVENT_BUS.register(UnderWorld.class);
		GameRegistry.registerWorldGenerator(new IWorldGenerator() {
			@Override
			public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
				if (chunkX % 5 == 0 && chunkZ % 5 == 0 && !world.isSpawnChunk(chunkX, chunkZ) && world.provider.getDimension() == 0) {
					BlockPos a = new ChunkPos(chunkX, chunkZ).getBlock(0, 0, 0);
					a = a.add(random.nextInt(15), 0, random.nextInt(15));
					for (int y = 0; y <= world.getHeight(); y++) {
						a = new BlockPos(a.getX(), y, a.getZ());
						int k = 4;
						for (BlockPos p : BlockPos.getAllInBox(a.add(k, 0, k), a.add(-k, 0, -k))) {
							if (p.getDistance(a.getX(), a.getY(), a.getZ()) <= k + (random.nextDouble() - .5) * 2. /*|| random.nextBoolean()*/)
								world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
						}
					}
				}
			}
		}, 1001);
	}

	public static class Prov extends WorldProvider {

		public Prov() {
		}

		@Override
		public DimensionType getDimensionType() {
			return type;
		}

		@Override
		public IChunkGenerator createChunkGenerator() {
			// TODO Auto-generated method stub
			IChunkGenerator cg = new IChunkGenerator() {
				List<SpawnListEntry> lis = Lists.newArrayList(new SpawnListEntry(EntityGolem.class, 100, 1, 15));

				@Override
				public void recreateStructures(Chunk chunkIn, int x, int z) {
				}

				@Override
				public void populate(int x, int z) {
				}

				@Override
				public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
					return false;
				}

				@Override
				public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
					return lis;
				}

				@Override
				public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
					return null;
				}

				@Override
				public boolean generateStructures(Chunk chunkIn, int x, int z) {
					return false;
				}

				@Override
				public Chunk generateChunk(int x, int z) {
					ChunkPrimer chunkprimer = new ChunkPrimer();
					//					Biome.getBiome(39).generateBiomeTerrain(world, world.rand, chunkprimer, x, z, .2);
					for (int xx = 0; xx < 16; xx++)
						for (int zz = 0; zz < 16; zz++)
							for (int y = 0; y < 75 + world.rand.nextInt(6); y++)
								chunkprimer.setBlockState(xx, y, zz, Blocks.BEDROCK.getDefaultState());
					Chunk chunk = new Chunk(world, chunkprimer, x, z);

					byte[] biomeArray = chunk.getBiomeArray();
					for (int i = 0; i < biomeArray.length; ++i) {
						biomeArray[i] = (byte) Biome.getIdForBiome(Biome.getBiome(39));
					}
					chunk.generateSkylightMap();
					return chunk;
				}
			};
			if ("".isEmpty())
				return cg;
			return super.createChunkGenerator();
		}

		@Override
		public boolean isSurfaceWorld() {
			return false;
		}

		@Override
		public boolean canRespawnHere() {
			return false;
		}

		@Override
		public int getRespawnDimension(EntityPlayerMP player) {
			return 0;
		}

		@Override
		public Vec3d getSkyColor(Entity cameraEntity, float partialTicks) {
			// TODO Auto-generated method stub
			if ("".isEmpty())
				return new Vec3d(1.0, 1.0, 1.0);
			return super.getSkyColor(cameraEntity, partialTicks);
		}

		@Override
		public Vec3d getCloudColor(float partialTicks) {
			// TODO Auto-generated method stub
			return super.getCloudColor(partialTicks);
		}

		@Override
		public float getSunBrightness(float par1) {
			// TODO Auto-generated method stub
			if (!true)
				return 0;
			return super.getSunBrightness(par1);
		}

		@Override
		public float getStarBrightness(float par1) {
			// TODO Auto-generated method stub
			return super.getStarBrightness(par1);
		}
	}

	@SubscribeEvent
	public static void fall(LivingHurtEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayerMP && event.getSource() == DamageSource.OUT_OF_WORLD && event.getEntityLiving().posY < 0) {
			if (!event.getEntityLiving().world.isRemote && event.getAmount() + .1f > event.getEntityLiving().getHealth() && event.getEntityLiving().dimension == 0) {
				event.setAmount(0);
				event.getEntityLiving().setHealth(event.getEntityLiving().getMaxHealth());
				System.out.println("zap " + event.getEntityLiving().dimension + "  " + event.getEntityLiving().world.provider.getDimension());
				Entity p =
						//event.getEntityLiving().world.getMinecraftServer().getPlayerList().recreatePlayerEntity((EntityPlayerMP) event.getEntityLiving(), 12, false);
						event.getEntityLiving().changeDimension(dim);
				p.setPositionAndUpdate(p.posX, 200, p.posZ);
				p.fallDistance = 0;
			}
		}
	}

	@SubscribeEvent
	public static void load(WorldEvent.Load event) {
		if (event.getWorld().provider.getClass() == Prov.class && event.getWorld() instanceof WorldServer) {
			ReflectionHelper.setPrivateValue(WorldServer.class, (WorldServer) event.getWorld(), new Teleporter((WorldServer) event.getWorld()) {
				@Override
				public void placeInPortal(Entity entityIn, float rotationYaw) {
				}
			}, "worldTeleporter", "field_85177_Q");

		}
	}

}
