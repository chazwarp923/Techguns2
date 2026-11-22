package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Block entity with a redstone setting: 0 ignore, 1 needs signal, 2 needs no signal (1.12 BasicRedstoneTileEnt). */
public abstract class BasicRedstoneTileEnt extends BasicOwnedTileEnt {

	protected byte redstoneBehaviour = 0;
	protected boolean hasSignal = false;

	public BasicRedstoneTileEnt(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize, boolean hasRotation) {
		super(type, pos, state, inventorySize, hasRotation);
	}

	public void onNeighborBlockChange() {
		if (this.level != null && !this.level.isClientSide) {
			boolean signal = this.level.hasNeighborSignal(this.worldPosition);
			if (signal != hasSignal) {
				hasSignal = signal;
				this.needUpdate();
			}
		}
	}

	public boolean isRedstoneEnabled() {
		return this.redstoneBehaviour == 0 || (this.hasSignal && this.redstoneBehaviour == 1) || (!this.hasSignal && this.redstoneBehaviour == 2);
	}

	public boolean hasSignal() {
		return hasSignal;
	}

	public void setHasSignal(boolean hasSignal) {
		this.hasSignal = hasSignal;
	}

	public byte getRedstoneBehaviour() {
		return redstoneBehaviour;
	}

	public void setRedstoneBehaviour(byte redstoneBehaviour) {
		this.redstoneBehaviour = redstoneBehaviour;
	}

	public String getRedstoneModeText() {
		return switch (this.redstoneBehaviour) {
			case 1 -> "techguns.redstonemode.high";
			case 2 -> "techguns.redstonemode.low";
			default -> "techguns.redstonemode.ignore";
		};
	}

	public String getSignalStateText() {
		return this.hasSignal ? "techguns.redstonemode.signal.high" : "techguns.redstonemode.signal.low";
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.hasSignal = tags.getBoolean("hasSignal");
		this.redstoneBehaviour = tags.getByte("redstoneBehaviour");
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putBoolean("hasSignal", this.hasSignal);
		tags.putByte("redstoneBehaviour", this.redstoneBehaviour);
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id == BUTTON_ID_REDSTONE) {
			if (this.isUseableByPlayer(ply)) {
				this.redstoneBehaviour = (byte) ((this.redstoneBehaviour + 1) % 3);
				this.needUpdate();
			}
		} else {
			super.buttonClicked(id, ply);
		}
	}
}
