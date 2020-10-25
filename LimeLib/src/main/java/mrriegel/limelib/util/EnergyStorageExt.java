package mrriegel.limelib.util;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.energy.EnergyStorage;

public class EnergyStorageExt extends EnergyStorage {

	public EnergyStorageExt(int capacity) {
		super(capacity);
	}

	public EnergyStorageExt(int capacity, int maxTransfer) {
		super(capacity, maxTransfer);
	}

	public EnergyStorageExt(int capacity, int maxReceive, int maxExtract) {
		super(capacity, maxReceive, maxExtract);
	}

	public void setEnergyStored(int energy) {
		this.energy = MathHelper.clamp(energy, 0, capacity);
	}

	public void modifyEnergyStored(int energy) {
		this.energy += energy;
		this.energy = MathHelper.clamp(this.energy, 0, capacity);
	}

	public int getMaxReceive() {
		return maxReceive;
	}

	public void setMaxReceive(int maxReceive) {
		this.maxReceive = maxReceive;
	}

	public int getMaxExtract() {
		return maxExtract;
	}

	public void setMaxExtract(int maxExtract) {
		this.maxExtract = maxExtract;
	}

}
