package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGConfig;

/** Block entity with a Forge Energy storage (1.12 BasicPoweredTileEnt). */
public abstract class BasicPoweredTileEnt extends BasicRedstoneTileEnt {

	protected final EnergyStoragePlus energy;

	public BasicPoweredTileEnt(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize, boolean hasRotation, int maximumPower) {
		super(type, pos, state, inventorySize, hasRotation);
		this.energy = new EnergyStoragePlus(maximumPower).onChange(this::setChanged);
	}

	public EnergyStoragePlus getEnergyStorage() {
		return energy;
	}

	/** drain amount from the storage, returns if there was enough */
	protected boolean consumePower(int amount) {
		if (TGConfig.machinesNeedNoPower) {
			return true;
		}
		if (this.energy.extractEnergy(amount, true) >= amount) {
			return this.energy.extractEnergy(amount, false) >= amount;
		}
		return false;
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.energy.setEnergyStored(tags.getInt("powerStored"));
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putInt("powerStored", this.energy.getEnergyStored());
	}
}
