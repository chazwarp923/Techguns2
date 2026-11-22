package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.gui.containers.FabricatorMenu;
import techguns.recipes.FabricatorRecipe;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;
import techguns.tileentities.operation.MachineSlotItem;

/** Fabricator controller (1.12 FabricatorTileEntMaster). */
public class FabricatorTileEntMaster extends MultiBlockMachineTileEntMaster {

	protected static final float SOUND_VOLUME = 0.5f;

	public static final int SLOT_INPUT1 = 0;
	public static final int SLOT_WIRES = 1;
	public static final int SLOT_POWDER = 2;
	public static final int SLOT_PLATE = 3;
	public static final int SLOT_OUTPUT = 4;
	public static final int SLOT_UPGRADE = 5;

	public static final int POWER_PER_TICK = 80;

	public final MachineSlotItem input = new MachineSlotItem(this, SLOT_INPUT1);
	public final MachineSlotItem wireslot = new MachineSlotItem(this, SLOT_WIRES);
	public final MachineSlotItem powderslot = new MachineSlotItem(this, SLOT_POWDER);
	public final MachineSlotItem plateslot = new MachineSlotItem(this, SLOT_PLATE);

	public FabricatorTileEntMaster(BlockPos pos, BlockState state) {
		super(TGBlockEntities.FABRICATOR.get(), pos, state, 6, 100000);
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
					case SLOT_INPUT1, SLOT_WIRES, SLOT_POWDER, SLOT_PLATE, SLOT_UPGRADE -> getValidSlotForItemInMachine(stack) == slot;
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
		return new FabricatorMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.fabricator");
	}

	public int getValidSlotForItemInMachine(ItemStack stack) {
		if (stack.is(FabricatorRecipe.WIRES_SLOT)) {
			return SLOT_WIRES;
		}
		if (stack.is(FabricatorRecipe.POWDER_SLOT)) {
			return SLOT_POWDER;
		}
		if (stack.is(FabricatorRecipe.PLATES_SLOT)) {
			return SLOT_PLATE;
		}
		if (TGItems.isMachineUpgrade(stack)) {
			return SLOT_UPGRADE;
		}
		return SLOT_INPUT1;
	}

	protected static final net.minecraft.world.phys.shapes.VoxelShape GLASS_FORMED = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 14.4, 16);

	/** housing blocks take items and energy (1.12 FabricatorTileEntSlave) */
	@Override
	public boolean forwardsCapability(int slaveType, net.neoforged.neoforge.capabilities.BlockCapability<?, ?> cap) {
		return slaveType == techguns.blocks.machines.multiblocks.FabricatorSchematic.TYPE_HOUSING
				&& (cap == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK || cap == net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK);
	}

	@Override
	public net.minecraft.world.phys.shapes.VoxelShape getSlaveShape(int slaveType, BlockPos slavePos) {
		return slaveType == techguns.blocks.machines.multiblocks.FabricatorSchematic.TYPE_GLASS ? GLASS_FORMED : super.getSlaveShape(slaveType, slavePos);
	}

	@Override
	protected int getNeededPower() {
		return POWER_PER_TICK;
	}

	public FabricatorRecipe.Input recipeInput() {
		return new FabricatorRecipe.Input(input.get(), wireslot.get(), powderslot.get(), plateslot.get());
	}

	@Override
	protected void checkAndStartOperation() {
		this.setContentsChanged(false);
		FabricatorRecipe.Input in = recipeInput();
		var found = FabricatorRecipe.find(level, in);
		if (found.isEmpty()) {
			return;
		}
		FabricatorRecipe r = found.get().value();
		int multi = r.getMaxMultiplier(in, this.inventory.getStackInSlot(SLOT_OUTPUT), this.getMaxMachineUpgradeMultiplier(SLOT_UPGRADE));
		if (multi <= 0) {
			return;
		}
		int nIn = r.input().count();
		int nWires = r.wires().map(i -> i.count()).orElse(0);
		int nPowder = r.powder().map(i -> i.count()).orElse(0);
		int nPlate = r.plate().map(i -> i.count()).orElse(0);

		MachineOperation op = new MachineOperation(r.result().copy(), input.getTypeWithSize(nIn), nWires > 0 ? wireslot.getTypeWithSize(nWires) : ItemStack.EMPTY,
				nPowder > 0 ? powderslot.getTypeWithSize(nPowder) : ItemStack.EMPTY, nPlate > 0 ? plateslot.getTypeWithSize(nPlate) : ItemStack.EMPTY);
		op.setStackMultiplier(multi);
		op.setPowerPerTick(POWER_PER_TICK * multi);

		input.consume(nIn * multi);
		if (nWires > 0) {
			wireslot.consume(nWires * multi);
		}
		if (nPowder > 0) {
			powderslot.consume(nPowder * multi);
		}
		if (nPlate > 0) {
			plateslot.consume(nPlate * multi);
		}
		this.currentOperation = op;
		this.totaltime = FabricatorRecipe.TIME;
		this.progress = 0;
		this.needUpdate();
	}

	@Override
	protected void finishedOperation() {
		this.inventory.insertItemNoCheck(SLOT_OUTPUT, currentOperation.getItemOutput0(), false);
	}

	@Override
	protected void playAmbientSound() {
		if (this.progress == 1) {
			this.playLocalSound(TGSounds.FABRICATOR_WORK.get(), SOUND_VOLUME);
		}
	}
}
