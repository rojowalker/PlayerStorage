package mrriegel.limelib.util;

import java.util.Arrays;

import net.minecraftforge.energy.IEnergyStorage;

public class CombinedEnergyStorage implements IEnergyStorage {

	IEnergyStorage[] storages;

	public CombinedEnergyStorage(IEnergyStorage... storages) {
		this.storages = storages;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		int received = 0;
		int rest = maxReceive;
		for (IEnergyStorage e : storages) {
			if (received == maxReceive || rest == 0)
				break;
			int r = e.receiveEnergy(rest, simulate);
			received += r;
			rest -= r;
		}
		return received;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		int extracted = 0;
		int rest = maxExtract;
		for (IEnergyStorage e : storages) {
			if (extracted == maxExtract || rest == 0)
				break;
			int r = e.extractEnergy(rest, simulate);
			extracted += r;
			rest -= r;
		}
		return extracted;
	}

	@Override
	public int getEnergyStored() {
		return Arrays.stream(storages).mapToInt(IEnergyStorage::getEnergyStored).sum();
	}

	@Override
	public int getMaxEnergyStored() {
		return Arrays.stream(storages).mapToInt(IEnergyStorage::getMaxEnergyStored).sum();
	}

	// private static int getCapacity(EnergyStorage... storages) {
	// int i = 0;
	// for (EnergyStorage e : storages)
	// i += e.getMaxEnergyStored();
	// return i;
	// }
	//
	// private static int getReceive(EnergyStorageExt... storages) {
	// int i = 0;
	// for (EnergyStorageExt e : storages)
	// i += e.getMaxReceive();
	// return i;
	// }
	//
	// private static int getExtract(EnergyStorageExt... storages) {
	// int i = 0;
	// for (EnergyStorageExt e : storages)
	// i += e.getMaxExtract();
	// return i;
	// }

	@Override
	public boolean canExtract() {
		return extractEnergy(1, true) > 0;
		// return getExtract(storages) > 0;
	}

	@Override
	public boolean canReceive() {
		return receiveEnergy(1, true) > 0;
		// return getReceive(storages) > 0;
	}

}
