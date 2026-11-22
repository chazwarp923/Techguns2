package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import techguns.TGBlockEntities;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.blocks.machines.BlockMachineFacing;
import techguns.gui.containers.ChargingStationMenu;
import techguns.recipes.ChargingStationRecipe;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;
import techguns.tileentities.operation.MachineSlotItem;
import techguns.util.TGFXHooks;

/**
 * Charging station (1.12 ChargingStationTileEnt): charges items with an energy capability directly, other items run a
 * techguns:charging_station recipe (empty energy cell -> energy cell).
 */
public class ChargingStationTileEnt extends BasicMachineTileEnt {

	protected static final float SOUND_VOLUME = 0.35f;

	public static final int SLOT_INPUT = 0;
	public static final int SLOT_OUTPUT = 1;
	public static final int SLOT_UPGRADE = 2;

	public static final int CHARGERATE = 800;
	public static final int ITEMCHARGERATE = 1600;

	public final MachineSlotItem input = new MachineSlotItem(this, SLOT_INPUT);

	/** an energy item is charged, synced to the client for the renderer and sounds */
	public boolean charging = false;
	protected int lastsound = 0;

	public ChargingStationTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.CHARGING_STATION.get(), pos, state, 3, false, 100000);
	}

	@Override
	protected ItemStackHandlerPlus createInventory(int size) {
		return new ItemStackHandlerPlus(size) {
			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged(slot);
			}

			@Override
			protected boolean allowItemInSlot(int slot, ItemStack stack) {
				return switch (slot) {
					case SLOT_INPUT -> getValidSlotForItemInMachine(stack) == SLOT_INPUT;
					case SLOT_UPGRADE -> TGItems.isMachineUpgrade(stack);
					default -> false;
				};
			}

			@Override
			protected boolean allowExtractFromSlot(int slot, int amount) {
				return slot == SLOT_OUTPUT;
			}
		};
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new ChargingStationMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.chargingstation");
	}

	public static boolean isChargeable(ItemStack stack) {
		return !stack.isEmpty() && stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
	}

	public int getValidSlotForItemInMachine(ItemStack stack) {
		if (TGItems.isMachineUpgrade(stack)) {
			return SLOT_UPGRADE;
		}
		if (isChargeable(stack) || ChargingStationRecipe.isInput(level, stack)) {
			return SLOT_INPUT;
		}
		return -1;
	}

	/** per stack, the base class multiplies with the stack multiplier */
	@Override
	protected int getNeededPower() {
		if (this.currentOperation == null) {
			return 0;
		}
		return this.currentOperation.getPowerPerTick() / this.currentOperation.getStackMultiplier();
	}

	@Override
	protected void checkAndStartOperation() {
		this.setContentsChanged(false);
		var found = ChargingStationRecipe.find(level, input.get());
		if (found.isEmpty()) {
			return;
		}
		ChargingStationRecipe r = found.get().value();
		int n = r.input().count();
		if (!input.canConsume(n) || !canOutput(r.result(), SLOT_OUTPUT)) {
			return;
		}
		int maxStack = this.getMaxMachineUpgradeMultiplier(SLOT_UPGRADE);
		int multiplier = 1;
		for (int i = maxStack; i > 1; --i) {
			if (canOutput(r.result().copyWithCount(r.result().getCount() * i), SLOT_OUTPUT) && input.canConsume(n * i)) {
				multiplier = i;
				break;
			}
		}
		ItemStack in = input.getTypeWithSize(n);
		input.consume(n * multiplier);
		this.currentOperation = new MachineOperation(r.result().copy(), in).setPowerPerTick(CHARGERATE).setStackMultiplier(multiplier);
		this.progress = 0;
		this.totaltime = Math.max(1, r.chargeAmount() / CHARGERATE);
		this.needUpdate();
	}

	@Override
	protected void finishedOperation() {
		this.inventory.insertItemNoCheck(SLOT_OUTPUT, currentOperation.getItemOutput0(), false);
	}

	@Override
	protected void tick() {
		if (this.level == null || !this.isRedstoneEnabled()) {
			return;
		}
		if (this.currentOperation != null) {
			super.tick();
			return;
		}
		ItemStack stack = input.get();
		IEnergyStorage itemEnergy = stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
		if (this.level.isClientSide) {
			if (this.charging) {
				this.playAmbientSound();
			}
			return;
		}
		if (itemEnergy != null && itemEnergy.receiveEnergy(ITEMCHARGERATE, true) > 0) {
			int max = Math.min(this.energy.getEnergyStored(), ITEMCHARGERATE);
			int pwr = max > 0 ? itemEnergy.receiveEnergy(max, false) : 0;
			if (pwr > 0) {
				this.energy.extractEnergy(pwr, false);
				this.setChanged();
			}
			setCharging(pwr > 0);
		} else if (itemEnergy != null) {
			// full, move it to the output
			if (this.inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
				this.inventory.setStackInSlot(SLOT_OUTPUT, stack);
				this.inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
			}
			setCharging(false);
		} else {
			setCharging(false);
			if (this.contentsChanged) {
				this.checkAndStartOperation();
			}
		}
	}

	protected void setCharging(boolean value) {
		if (this.charging != value) {
			this.charging = value;
			this.needUpdate();
		}
	}

	/** the item shown inside the station */
	public ItemStack getDisplayedItem() {
		if (this.charging) {
			return this.inventory.getStackInSlot(SLOT_INPUT);
		}
		if (this.currentOperation != null) {
			return this.currentOperation.getItemInputI(0);
		}
		return ItemStack.EMPTY;
	}

	public Direction getFacing() {
		BlockState state = this.getBlockState();
		return state.hasProperty(BlockMachineFacing.FACING) ? state.getValue(BlockMachineFacing.FACING) : Direction.NORTH;
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.charging = tags.getBoolean("charging");
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putBoolean("charging", this.charging);
	}

	@Override
	protected void playAmbientSound() {
		if (this.level == null) {
			return;
		}
		double x = worldPosition.getX() + 0.5d;
		double y = worldPosition.getY() + 0.5d;
		double z = worldPosition.getZ() + 0.5d;
		if (level.getNearestPlayer(x, y, z, 10d, false) != null) {
			// flares on three sides like 1.12, none on the facing side
			Direction facing = getFacing();
			for (Direction d : Direction.Plane.HORIZONTAL) {
				if (d != facing) {
					TGFXHooks.createFX("ChargeFlare2", level, x + d.getStepX() * 0.2d, y, z + d.getStepZ() * 0.2d, 0, 0, 0);
				}
			}
		}
		if (this.lastsound > 0) {
			this.lastsound--;
		}
		if (this.lastsound <= 0) {
			this.lastsound = 20;
			level.playLocalSound(x, y, z, TGSounds.CHARGING_STATION_WORK.get(), SoundSource.BLOCKS, SOUND_VOLUME, 1.0f, false);
		}
	}
}
