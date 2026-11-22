package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.TGBlockEntities;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.gui.containers.ChemLabMenu;
import techguns.recipes.ChemLabRecipe;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;
import techguns.tileentities.operation.MachineSlotItem;

/**
 * Chemical laboratory (1.12 ChemLabTileEnt): two inputs, a bottle slot and an input tank make an item and / or a fluid
 * in the output tank. Fluid pipes fill the input tank and drain the output tank, or the input tank in drain mode.
 */
public class ChemLabTileEnt extends BasicMachineTileEnt implements IFluidTankTile {

	protected static final float SOUND_VOLUME = 0.65f;

	public static final int BUTTON_ID_DUMP_INPUT = BUTTON_ID_REDSTONE + 1;
	public static final int BUTTON_ID_DUMP_OUTPUT = BUTTON_ID_REDSTONE + 2;
	public static final int BUTTON_ID_TOGGLE_DRAIN = BUTTON_ID_REDSTONE + 3;

	public static final int SLOT_INPUT1 = 0;
	public static final int SLOT_INPUT2 = 1;
	public static final int SLOT_BOTTLE = 2;
	public static final int SLOT_OUTPUT = 3;
	public static final int SLOT_UPGRADE = 4;

	public static final int CAPACITY_INPUT_TANK = 8 * 1000;
	public static final int CAPACITY_OUTPUT_TANK = 16 * 1000;

	public final MachineSlotItem input1 = new MachineSlotItem(this, SLOT_INPUT1);
	public final MachineSlotItem input2 = new MachineSlotItem(this, SLOT_INPUT2);
	public final MachineSlotItem inputBottle = new MachineSlotItem(this, SLOT_BOTTLE);

	public final FluidTank inputTank = new FluidTank(CAPACITY_INPUT_TANK) {
		@Override
		protected void onContentsChanged() {
			onTankChanged();
		}
	};
	public final FluidTank outputTank = new FluidTank(CAPACITY_OUTPUT_TANK) {
		@Override
		protected void onContentsChanged() {
			onTankChanged();
		}
	};
	public final IFluidHandler fluidHandler = new ChemLabFluidHandler();

	protected boolean drainInput = false;

	public ChemLabTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.CHEM_LAB.get(), pos, state, 5, true, 20000);
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
					case SLOT_INPUT1, SLOT_INPUT2, SLOT_BOTTLE -> isItemValidForSlot(slot, stack);
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

	protected void onTankChanged() {
		this.setContentsChanged(true);
		this.setChanged();
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new ChemLabMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.chemlab");
	}

	@Override
	public IFluidHandler getFluidHandler() {
		return fluidHandler;
	}

	public boolean isItemValidForSlot(int slot, ItemStack item) {
		if (slot == SLOT_BOTTLE) {
			return ChemLabRecipe.allowInBottleSlot(level, item);
		}
		if (ChemLabRecipe.allowInBottleSlot(level, item)) {
			return false;
		}
		ItemStack s1 = input1.get();
		ItemStack s2 = input2.get();
		if (slot == SLOT_INPUT1) {
			if (!s1.isEmpty()) {
				return ItemStack.isSameItemSameComponents(s1, item);
			}
			return s2.isEmpty() ? ChemLabRecipe.hasRecipeUsing(level, item) : ChemLabRecipe.allowAsInput2(level, s2, item);
		}
		if (slot == SLOT_INPUT2) {
			if (!s2.isEmpty()) {
				return ItemStack.isSameItemSameComponents(s2, item);
			}
			return !s1.isEmpty() && ChemLabRecipe.allowAsInput2(level, s1, item);
		}
		return false;
	}

	public int getValidSlotForItemInMachine(ItemStack item) {
		if (TGItems.isMachineUpgrade(item)) {
			return SLOT_UPGRADE;
		}
		for (int slot : new int[] { SLOT_BOTTLE, SLOT_INPUT1, SLOT_INPUT2 }) {
			if (isItemValidForSlot(slot, item)) {
				return slot;
			}
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

	protected boolean canOutput(MachineOperation op) {
		if (!this.canOutput(op.getItemOutput0(), SLOT_OUTPUT)) {
			return false;
		}
		FluidStack out = op.getFluidOutput0();
		return out.isEmpty() || outputTank.fill(out, IFluidHandler.FluidAction.SIMULATE) == out.getAmount();
	}

	protected boolean canConsume(MachineOperation op) {
		int multi = op.getStackMultiplier();
		return input1.canConsumeWithMultiplier(op.getInputs().get(0), multi) && input2.canConsumeWithMultiplier(op.getInputs().get(1), multi)
				&& inputBottle.canConsumeWithMultiplier(op.getInputs().get(2), multi)
				&& inputTank.getFluidAmount() >= op.getNeededAmountFluid(0);
	}

	protected void consume(MachineOperation op) {
		input1.consume(op.getNeededAmountItem(0));
		input2.consume(op.getNeededAmountItem(1));
		inputBottle.consume(op.getNeededAmountItem(2));
		int fluid = op.getNeededAmountFluid(0);
		if (fluid > 0) {
			inputTank.drain(fluid, IFluidHandler.FluidAction.EXECUTE);
		}
	}

	protected MachineOperation createOperation(ChemLabRecipe r, ChemLabRecipe.Match match) {
		boolean swapped = match == ChemLabRecipe.Match.VALID_SWAPPED;
		ItemStack s1 = swapped ? input2.get() : input1.get();
		ItemStack s2 = swapped ? input1.get() : input2.get();
		// inputs in slot order
		ItemStack in1 = s1.copyWithCount(ChemLabRecipe.count(r.input1()));
		ItemStack in2 = s2.copyWithCount(ChemLabRecipe.count(r.input2()));
		if (swapped) {
			ItemStack t = in1;
			in1 = in2;
			in2 = t;
		}
		ItemStack bottle = inputBottle.get().copyWithCount(ChemLabRecipe.count(r.bottle()));
		MachineOperation op = new MachineOperation(r.result().copy(), in1, in2, bottle);
		op.getFluidInputs().add(r.fluidInput().map(f -> inputTank.getFluid().copyWithAmount(f.amount())).orElse(FluidStack.EMPTY));
		op.getFluidOutputs().add(r.fluidResult().copy());
		return op.setPowerPerTick(r.powerPerTick());
	}

	@Override
	protected void checkAndStartOperation() {
		this.setContentsChanged(false);
		ChemLabRecipe.Input in = new ChemLabRecipe.Input(input1.get(), input2.get(), inputBottle.get(), inputTank.getFluid());
		var found = ChemLabRecipe.find(level, in);
		if (found.isEmpty()) {
			return;
		}
		ChemLabRecipe r = found.get().value();
		MachineOperation op = createOperation(r, r.match(in));
		if (!canOutput(op)) {
			return;
		}
		int maxStack = this.getMaxMachineUpgradeMultiplier(SLOT_UPGRADE);
		int multiplier = 1;
		for (int i = maxStack; i > 1; --i) {
			op.setStackMultiplier(i);
			if (canOutput(op) && canConsume(op)) {
				multiplier = i;
				break;
			}
		}
		op.setStackMultiplier(multiplier);
		this.consume(op);
		this.currentOperation = op;
		this.progress = 0;
		this.totaltime = ChemLabRecipe.TIME;
		this.needUpdate();
	}

	@Override
	protected void finishedOperation() {
		ItemStack out = currentOperation.getItemOutput0();
		if (!out.isEmpty()) {
			this.inventory.insertItemNoCheck(SLOT_OUTPUT, out, false);
		}
		FluidStack fluidOut = currentOperation.getFluidOutput0();
		if (!fluidOut.isEmpty()) {
			outputTank.fill(fluidOut, IFluidHandler.FluidAction.EXECUTE);
		}
	}

	@Override
	protected void playAmbientSound() {
		if (this.level == null) {
			return;
		}
		int half = Math.round(totaltime * 0.5f);
		double x = worldPosition.getX() + 0.5d, y = worldPosition.getY() + 0.5d, z = worldPosition.getZ() + 0.5d;
		if (this.progress == 1 || this.progress == 1 + half) {
			level.playLocalSound(x, y, z, TGSounds.CHEM_LAB_WORK.get(), SoundSource.BLOCKS, SOUND_VOLUME, 1.0f, true);
		}
		if (this.progress % 10 == 0) {
			RandomSource rng = level.random;
			level.addParticle(ParticleTypes.EFFECT, x + rng.nextFloat() - 0.5d, y + rng.nextFloat() * 0.5d, z + rng.nextFloat() - 0.5d, 0, 1, 0);
		}
	}

	public byte getDrainMode() {
		return (byte) (this.drainInput ? 1 : 0);
	}

	public void setDrainMode(boolean drainInput) {
		this.drainInput = drainInput;
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id < BUTTON_ID_DUMP_INPUT) {
			super.buttonClicked(id, ply);
			return;
		}
		if (!this.isUseableByPlayer(ply)) {
			return;
		}
		switch (id) {
			case BUTTON_ID_DUMP_INPUT -> inputTank.setFluid(FluidStack.EMPTY);
			case BUTTON_ID_DUMP_OUTPUT -> outputTank.setFluid(FluidStack.EMPTY);
			case BUTTON_ID_TOGGLE_DRAIN -> this.drainInput = !this.drainInput;
			default -> {
				return;
			}
		}
		this.setContentsChanged(true);
		this.needUpdate();
	}

	@Override
	public boolean onFluidContainerInteract(Player player, InteractionHand hand) {
		if (!this.isUseableByPlayer(player)) {
			return false;
		}
		return FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler);
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.drainInput = tags.getBoolean("drainInput");
		this.inputTank.readFromNBT(registries, tags.getCompound("inputTank"));
		this.outputTank.readFromNBT(registries, tags.getCompound("outputTank"));
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putBoolean("drainInput", this.drainInput);
		tags.put("inputTank", this.inputTank.writeToNBT(registries, new CompoundTag()));
		tags.put("outputTank", this.outputTank.writeToNBT(registries, new CompoundTag()));
	}

	/** pipes fill the input tank and drain the output tank, or the input tank in drain mode */
	private class ChemLabFluidHandler implements IFluidHandler {
		private FluidTank drainTank() {
			return drainInput ? inputTank : outputTank;
		}

		@Override
		public int getTanks() {
			return 2;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return tank == 0 ? inputTank.getFluid() : outputTank.getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank == 0 ? CAPACITY_INPUT_TANK : CAPACITY_OUTPUT_TANK;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return tank == 0;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return inputTank.fill(resource, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return drainTank().drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return drainTank().drain(maxDrain, action);
		}
	}
}
