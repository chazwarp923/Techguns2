package techguns.tileentities;

import net.neoforged.neoforge.energy.EnergyStorage;

public class EnergyStoragePlus extends EnergyStorage {

	private Runnable onChange = () -> {};

	public EnergyStoragePlus(int capacity) {
		super(capacity);
	}

	public EnergyStoragePlus(int capacity, int maxReceive, int maxExtract) {
		super(capacity, maxReceive, maxExtract);
	}

	public EnergyStoragePlus onChange(Runnable r) {
		this.onChange = r;
		return this;
	}

	public void setEnergyStored(int value) {
		this.energy = Math.max(0, Math.min(this.capacity, value));
	}

	@Override
	public int receiveEnergy(int toReceive, boolean simulate) {
		int r = super.receiveEnergy(toReceive, simulate);
		if (r > 0 && !simulate) {
			onChange.run();
		}
		return r;
	}

	@Override
	public int extractEnergy(int toExtract, boolean simulate) {
		int r = super.extractEnergy(toExtract, simulate);
		if (r > 0 && !simulate) {
			onChange.run();
		}
		return r;
	}
}
