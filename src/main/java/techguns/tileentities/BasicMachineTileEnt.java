package techguns.tileentities;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGItems;
import techguns.tileentities.operation.MachineOperation;

/**
 * Powered machine that runs {@link MachineOperation}s (1.12 BasicMachineTileEnt). Ticks on both sides like in 1.12:
 * the client advances progress for the animations, only the server starts and finishes operations.
 */
public abstract class BasicMachineTileEnt extends BasicPoweredTileEnt {

	@Nullable
	public MachineOperation currentOperation = null;
	public int totaltime = 0;
	public int progress = 0;

	public BasicMachineTileEnt(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize, boolean hasRotation, int maximumPower) {
		super(type, pos, state, inventorySize, hasRotation, maximumPower);
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.totaltime = tags.getInt("totaltime");
		this.progress = tags.getInt("progress");
		if (tags.contains("operation")) {
			this.currentOperation = this.createEmptyOperation();
			this.currentOperation.readFromNBT(tags, registries);
		} else {
			this.currentOperation = null;
		}
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putInt("totaltime", this.totaltime);
		tags.putInt("progress", this.progress);
		if (this.currentOperation != null) {
			this.currentOperation.writeToNBT(tags, registries);
		}
	}

	/** subclasses with own operation types override this */
	protected MachineOperation createEmptyOperation() {
		return new MachineOperation();
	}

	public int getProgressScaled(int limit) {
		return totaltime != 0 ? this.progress * limit / this.totaltime : 0;
	}

	public float getProgress() {
		return totaltime > 0 ? (float) this.progress / (float) this.totaltime : 0.0f;
	}

	public boolean isWorking() {
		return this.currentOperation != null;
	}

	protected abstract int getNeededPower();

	/** 1 + number of stack upgrades in the slot */
	public int getMaxMachineUpgradeMultiplier(int slot) {
		ItemStack upg = this.inventory.getStackInSlot(slot);
		if (TGItems.isMachineUpgrade(upg)) {
			return 1 + upg.getCount();
		}
		return 1;
	}

	/** used by multiblocks to disable the machine when broken */
	protected boolean enabled() {
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BasicMachineTileEnt tile) {
		tile.tick();
	}

	protected void tick() {
		if (this.level == null || !this.isRedstoneEnabled() || !this.enabled()) {
			return;
		}
		if (this.currentOperation != null) {
			if (this.consumePower(this.getNeededPower() * this.currentOperation.getStackMultiplier())) {
				this.progress++;
				if (this.level.isClientSide) {
					this.playAmbientSound();
				}
				if (progress >= totaltime) {
					if (!this.level.isClientSide) {
						this.finishedOperation();
					}
					this.progress = 0;
					this.totaltime = 0;
					this.currentOperation = null;
					if (!this.level.isClientSide) {
						this.checkAndStartOperation();
						this.needUpdate();
					}
				}
			}
		} else if (!this.level.isClientSide && this.contentsChanged) {
			this.checkAndStartOperation();
		}
	}

	protected abstract void checkAndStartOperation();

	protected abstract void finishedOperation();

	/** client side, called every working tick */
	protected abstract void playAmbientSound();

	protected void playLocalSound(net.minecraft.sounds.SoundEvent sound, float volume) {
		if (this.level != null) {
			this.level.playLocalSound(worldPosition.getX() + 0.5d, worldPosition.getY() + 0.5d, worldPosition.getZ() + 0.5d, sound,
					net.minecraft.sounds.SoundSource.BLOCKS, volume, 1.0f, true);
		}
	}
}
