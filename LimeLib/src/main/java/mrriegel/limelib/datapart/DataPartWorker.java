package mrriegel.limelib.datapart;

import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

public abstract class DataPartWorker extends DataPart {

	//TODO remove sides
	protected abstract boolean workDone(World world, Side side);

	protected abstract boolean canWork(World world, Side side);

	protected abstract void work(World world, Side side);

	@Override
	public void updateServer(World world) {
		if (world.getTotalWorldTime() % everyXtick(Side.SERVER) != 0)
			return;
		if (canWork(world, Side.SERVER))
			work(world, Side.SERVER);
		if (workDone(world, Side.SERVER)) {
			getRegistry().removeDataPart(pos);
		}
	}

	@Override
	public void updateClient(World world) {
		if (world.getTotalWorldTime() % everyXtick(Side.CLIENT) != 0)
			return;
		if (canWork(world, Side.CLIENT))
			work(world, Side.CLIENT);
		if (workDone(world, Side.CLIENT)) {
			getRegistry().removeDataPart(pos);
		}
	}

	protected int everyXtick(Side side) {
		return 1;
	}
}
