package techguns.tileentities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.TGBlockEntities;
import techguns.TGSounds;
import techguns.blocks.machines.multiblocks.ReactionChamberSchematic;
import techguns.gui.containers.ReactionChamberMenu;
import techguns.recipes.ReactionChamberRecipe;
import techguns.recipes.ReactionChamberRecipe.RiskType;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;
import techguns.tileentities.operation.ReactionBeamFocus;
import techguns.tileentities.operation.ReactionChamberOperation;

/**
 * Reaction chamber controller (1.12 ReactionChamberTileEntMaster). The tank is filled up to the liquid level setting
 * (buckets), a reaction starts when input, focus, fluid level and intensity match a recipe. Failed reactions may
 * break the item or blow up the chamber.
 */
public class ReactionChamberTileEntMaster extends MultiBlockMachineTileEntMaster implements IFluidTankTile {

	public static final int CAPACITY_INPUT_TANK = ReactionChamberRecipe.MAX_LEVEL * 1000;

	public static final int SLOT_INPUT = 0;
	public static final int SLOT_FOCUS = 1;
	public static final int SLOT_OUTPUT = 2;
	public static final int OUTPUT_SLOTS_COUNT = 4;

	public static final int BUTTON_ID_LIQUIDLEVEL_INC = BUTTON_ID_REDSTONE + 1;
	public static final int BUTTON_ID_LIQUIDLEVEL_DEC = BUTTON_ID_REDSTONE + 2;
	public static final int BUTTON_ID_INTENSITY_INC = BUTTON_ID_REDSTONE + 3;
	public static final int BUTTON_ID_INTENSITY_DEC = BUTTON_ID_REDSTONE + 4;
	public static final int BUTTON_ID_DUMPTANK = BUTTON_ID_REDSTONE + 5;

	protected int intensity = 0;
	protected int liquidLevel = 0;
	protected boolean fluidsChanged = false;

	public final ReactionChamberTank inputTank = new ReactionChamberTank();

	public ReactionChamberTileEntMaster(BlockPos pos, BlockState state) {
		super(TGBlockEntities.REACTION_CHAMBER.get(), pos, state, 2 + OUTPUT_SLOTS_COUNT, 1000000);
	}

	@Override
	protected ItemStackHandlerPlus createInventory(int size) {
		return new ItemStackHandlerPlus(size) {
			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged(slot);
				if (slot <= SLOT_FOCUS) {
					// the renderer shows input and focus
					fluidsChanged = true;
				}
			}

			@Override
			protected boolean allowItemInSlot(int slot, ItemStack stack) {
				return slot == SLOT_INPUT || (slot == SLOT_FOCUS && ReactionBeamFocus.get(stack) != null);
			}

			@Override
			protected boolean allowExtractFromSlot(int slot, int amount) {
				return slot >= SLOT_OUTPUT && slot < SLOT_OUTPUT + OUTPUT_SLOTS_COUNT;
			}
		};
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new ReactionChamberMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.reactionchamber");
	}

	@Override
	protected MachineOperation createEmptyOperation() {
		return new ReactionChamberOperation();
	}

	@Nullable
	public ReactionChamberOperation getCurrentReaction() {
		return this.currentOperation instanceof ReactionChamberOperation op ? op : null;
	}

	public ItemStack getFocus() {
		return this.inventory.getStackInSlot(SLOT_FOCUS);
	}

	public ItemStack getInput() {
		return this.inventory.getStackInSlot(SLOT_INPUT);
	}

	public int getIntensity() {
		return intensity;
	}

	public int getLiquidLevel() {
		return liquidLevel;
	}

	public void setIntensity(int intensity) {
		this.intensity = intensity;
	}

	public void setLiquidLevel(int liquidLevel) {
		this.liquidLevel = liquidLevel;
	}

	public int getValidSlotForItemInMachine(ItemStack stack) {
		return ReactionBeamFocus.get(stack) != null ? SLOT_FOCUS : SLOT_INPUT;
	}

	/* ---- multiblock ---- */

	/** side connectors take items and fluids, the top connector energy (1.12 ReactionChamberTileEntSlave) */
	@Override
	public boolean forwardsCapability(int slaveType, BlockCapability<?, ?> cap) {
		if (slaveType == ReactionChamberSchematic.TYPE_SIDE_CONNECTOR) {
			return cap == Capabilities.ItemHandler.BLOCK || cap == Capabilities.FluidHandler.BLOCK;
		}
		return slaveType == ReactionChamberSchematic.TYPE_ENERGY_CONNECTOR && cap == Capabilities.EnergyStorage.BLOCK;
	}

	public static final double BOTTOM_DIFF = 0.25d;
	public static final double MIDDLE_DIFF = 0.45d;
	public static final double HEIGHT_DIFF = 0.35d;

	/** formed part shapes by offset from the bottom center block (1.12 ReactionChamberDefinition.boundingBoxes) */
	private static final Map<Long, VoxelShape> SHAPES = new HashMap<>();

	private static void shape(int x, int y, int z, double x1, double y1, double z1, double x2, double y2, double z2) {
		SHAPES.put(BlockPos.asLong(x, y, z), Shapes.box(x1, y1, z1, x2, y2, z2));
	}

	static {
		double b = BOTTOM_DIFF, m = MIDDLE_DIFF, h = 1 - HEIGHT_DIFF;
		shape(-1, 0, -1, b, 0, b, 1, 1, 1);
		shape(1, 0, -1, 0, 0, b, 1 - b, 1, 1);
		shape(-1, 0, 1, b, 0, 0, 1, 1, 1 - b);
		shape(1, 0, 1, 0, 0, 0, 1 - b, 1, 1 - b);
		for (int y = 1; y <= 3; y++) {
			double top = y == 3 ? h : 1;
			shape(-1, y, -1, m, 0, m, 1, top, 1);
			shape(1, y, -1, 0, 0, m, 1 - m, top, 1);
			shape(-1, y, 1, m, 0, 0, 1, top, 1 - m);
			shape(1, y, 1, 0, 0, 0, 1 - m, top, 1 - m);
			shape(-1, y, 0, m, 0, 0, 1, top, 1);
			shape(1, y, 0, 0, 0, 0, 1 - m, top, 1);
			shape(0, y, -1, 0, 0, m, 1, top, 1);
			shape(0, y, 1, 0, 0, 0, 1, top, 1 - m);
		}
	}

	@Override
	public VoxelShape getSlaveShape(int slaveType, BlockPos slavePos) {
		BlockPos center = worldPosition.relative(getMultiblockDirection());
		VoxelShape s = SHAPES.get(BlockPos.asLong(slavePos.getX() - center.getX(), slavePos.getY() - center.getY(), slavePos.getZ() - center.getZ()));
		return s != null ? s : Shapes.block();
	}

	/** bottom block in the middle of the chamber, the reaction happens above it */
	public BlockPos getCenterPos() {
		return worldPosition.relative(getMultiblockDirection());
	}

	/* ---- tank ---- */

	@Override
	public IFluidHandler getFluidHandler() {
		return inputTank;
	}

	@Override
	public boolean onFluidContainerInteract(Player player, InteractionHand hand) {
		if (!this.isUseableByPlayer(player)) {
			return false;
		}
		return FluidUtil.interactWithFluidHandler(player, hand, this.inputTank);
	}

	/** the tank only fills up to the liquid level setting and only gives away what is above it */
	public class ReactionChamberTank extends FluidTank {
		public ReactionChamberTank() {
			super(CAPACITY_INPUT_TANK);
		}

		private int fillCapacity() {
			return liquidLevel * 1000;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			int room = fillCapacity() - getFluidAmount();
			if (resource.isEmpty() || room <= 0) {
				return 0;
			}
			return super.fill(resource.copyWithAmount(Math.min(room, resource.getAmount())), action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			int above = Math.max(0, getFluidAmount() - fillCapacity());
			return super.drain(Math.min(maxDrain, above), action);
		}

		/** remove fluid ignoring the level setting */
		public void removeInternal(int amount) {
			if (!fluid.isEmpty()) {
				fluid.shrink(amount);
				if (fluid.isEmpty()) {
					fluid = FluidStack.EMPTY;
				}
				onContentsChanged();
			}
		}

		@Override
		protected void onContentsChanged() {
			fluidsChanged = true;
			setContentsChanged(true);
			setChanged();
		}
	}

	/* ---- operation ---- */

	@Override
	protected int getNeededPower() {
		return 0;
	}

	/** power is drawn once per reaction tick */
	public boolean consumeReactionPower(int amount) {
		return this.consumePower(amount);
	}

	@Override
	protected void tick() {
		if (this.level == null) {
			return;
		}
		boolean client = this.level.isClientSide;
		if (!client && this.fluidsChanged) {
			this.fluidsChanged = false;
			this.needUpdate();
		}
		if (!this.isRedstoneEnabled() || !this.enabled()) {
			return;
		}
		ReactionChamberOperation op = getCurrentReaction();
		if (op != null) {
			ReactionChamberRecipe rec = op.getRecipe(level);
			if (rec == null) {
				// recipe removed by a datapack reload
				if (!client) {
					clearOperation();
					this.needUpdate();
				}
				return;
			}
			this.progress++;
			boolean changed = op.tick(this, rec, level.random);
			if (!client && changed) {
				if (op.isSuccess(rec)) {
					this.finishedOperation();
					clearOperation();
					this.checkAndStartOperation();
				} else if (this.progress >= this.totaltime) {
					clearOperation();
					if (rec.risk() == RiskType.EXPLOSION_LOW) {
						this.explode(0);
					} else if (rec.risk() == RiskType.EXPLOSION_MEDIUM) {
						this.explode(1);
					}
				}
				this.needUpdate();
			}
			if (client && op.nextTick == ReactionChamberOperation.RECIPE_TICKRATE - 1) {
				this.playReactionTickSound(op.requiredIntensity == this.intensity);
			}
		} else if (!client && this.contentsChanged) {
			this.checkAndStartOperation();
			if (this.currentOperation != null) {
				this.needUpdate();
			}
		}
	}

	private void clearOperation() {
		this.progress = 0;
		this.totaltime = 0;
		this.currentOperation = null;
	}

	public ReactionChamberRecipe.Input recipeInput() {
		return new ReactionChamberRecipe.Input(getInput(), getFocus(), inputTank.getFluid(), liquidLevel, intensity);
	}

	@Override
	protected void checkAndStartOperation() {
		this.setContentsChanged(false);
		var found = ReactionChamberRecipe.find(level, recipeInput());
		if (found.isEmpty()) {
			return;
		}
		ReactionChamberRecipe rec = found.get().value();
		if (rec.power() > this.energy.getEnergyStored() && !techguns.TGConfig.machinesNeedNoPower) {
			// try again next tick
			this.setContentsChanged(true);
			return;
		}
		this.currentOperation = new ReactionChamberOperation(found.get(), getInput());
		this.inventory.extractWithoutCheck(SLOT_INPUT, 1, false);
		this.totaltime = rec.ticks() * ReactionChamberOperation.RECIPE_TICKRATE;
		this.progress = 0;
		// consuming the input marked the contents changed again
		this.setContentsChanged(false);
	}

	@Override
	protected void finishedOperation() {
		ReactionChamberOperation op = getCurrentReaction();
		ReactionChamberRecipe rec = op != null ? op.getRecipe(level) : null;
		if (rec == null) {
			return;
		}
		for (ItemStack result : rec.results()) {
			ItemStack rest = result.copy();
			for (int s = SLOT_OUTPUT; s < SLOT_OUTPUT + OUTPUT_SLOTS_COUNT && !rest.isEmpty(); s++) {
				rest = this.inventory.insertItemNoCheck(s, rest, false);
			}
			if (!rest.isEmpty()) {
				Containers.dropItemStack(level, worldPosition.getX() + 0.5d, worldPosition.getY() + 0.5d, worldPosition.getZ() + 0.5d, rest);
			}
		}
		if (rec.liquidConsumption() > 0) {
			this.inputTank.removeInternal(rec.liquidConsumption());
		}
	}

	@Override
	protected void playAmbientSound() {
	}

	protected void playReactionTickSound(boolean goodTick) {
		BlockPos pos = getCenterPos().above();
		ReactionBeamFocus focus = ReactionBeamFocus.get(getFocus());
		double x = pos.getX() + 0.5d, y = pos.getY() + 0.5d, z = pos.getZ() + 0.5d;
		if (focus != null) {
			level.playLocalSound(x, y, z, focus.sound().get(), SoundSource.BLOCKS, 1.0f, 1.0f, true);
		}
		for (int i = 0; i < 4; i++) {
			level.addParticle(ParticleTypes.EFFECT, pos.getX() + level.random.nextFloat(), pos.getY() + level.random.nextFloat(),
					pos.getZ() + level.random.nextFloat(), 0, 1, 0);
		}
		level.playLocalSound(x, y, z, goodTick ? TGSounds.REACTION_CHAMBER_BEEP.get() : TGSounds.REACTION_CHAMBER_WARNING.get(), SoundSource.BLOCKS, 1.0f,
				1.0f, true);
	}

	/** failed reaction: 0 - the chamber center bursts, 1 - explosion that removes the whole glass */
	protected void explode(int type) {
		if (level == null || level.isClientSide) {
			return;
		}
		Direction dir = getMultiblockDirection();
		FluidStack fluid = inputTank.getFluid();
		BlockState fluidBlock = fluid.isEmpty() ? Blocks.AIR.defaultBlockState() : fluid.getFluid().defaultFluidState().createLegacyBlock();
		BlockPos masterPos = worldPosition;
		BlockPos centerPos = masterPos.relative(dir).above();
		double cx = centerPos.getX() + 0.5d, cy = centerPos.getY() + 0.5d, cz = centerPos.getZ() + 0.5d;

		this.onMultiBlockBreak();
		List<BlockPos> remove = new ArrayList<>();
		remove.add(centerPos);
		if (type == 0) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				remove.add(centerPos.relative(d));
			}
		} else {
			Direction left = dir.getClockWise();
			BlockPos.betweenClosed(masterPos.relative(left).above(), masterPos.relative(left.getOpposite()).relative(dir, 2).above(2))
					.forEach(p -> remove.add(p.immutable()));
		}
		remove.forEach(p -> level.setBlock(p, Blocks.AIR.defaultBlockState(), 3));

		if (type == 0) {
			level.setBlock(centerPos, fluidBlock, 3);
			if (level instanceof net.minecraft.server.level.ServerLevel sl) {
				sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 1, 0, 0, 0, 0);
			}
			level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f,
					(1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
		} else {
			level.explode(null, cx, cy, cz, 4f, Level.ExplosionInteraction.BLOCK);
			if (!fluid.isEmpty()) {
				level.setBlock(centerPos, fluidBlock, 3);
				level.setBlock(centerPos.above(), Blocks.AIR.defaultBlockState(), 3);
				level.setBlock(centerPos.below(), Blocks.GRAVEL.defaultBlockState(), 3);
			}
		}
		this.energy.setEnergyStored(0);
		this.inputTank.setFluid(FluidStack.EMPTY);
		ItemStack content = getInput();
		if (!content.isEmpty()) {
			this.inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
			Containers.dropItemStack(level, cx, cy, cz, content);
		}
		this.needUpdate();
	}

	/* ---- gui ---- */

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id <= BUTTON_ID_REDSTONE) {
			super.buttonClicked(id, ply);
			return;
		}
		if (!this.isUseableByPlayer(ply)) {
			return;
		}
		int max = ReactionChamberRecipe.MAX_LEVEL;
		switch (id) {
			case BUTTON_ID_LIQUIDLEVEL_INC -> this.liquidLevel = Math.min(max, this.liquidLevel + 1);
			case BUTTON_ID_LIQUIDLEVEL_DEC -> this.liquidLevel = Math.max(0, this.liquidLevel - 1);
			case BUTTON_ID_INTENSITY_INC -> this.intensity = Math.min(max, this.intensity + 1);
			case BUTTON_ID_INTENSITY_DEC -> this.intensity = Math.max(0, this.intensity - 1);
			case BUTTON_ID_DUMPTANK -> this.inputTank.setFluid(FluidStack.EMPTY);
			default -> {
				return;
			}
		}
		this.setContentsChanged(true);
		this.setChanged();
		this.needUpdate();
	}

	/* ---- save / sync ---- */

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.inputTank.setFluid(FluidStack.EMPTY);
		this.inputTank.readFromNBT(registries, tags.getCompound("inputTank"));
		this.intensity = tags.getByte("intensity");
		this.liquidLevel = tags.getByte("liquidLevel");
		if (level != null && level.isClientSide) {
			// the renderer shows input and focus
			this.inventory.setStackInSlot(SLOT_INPUT, ItemStack.parseOptional(registries, tags.getCompound("inputSlot")));
			this.inventory.setStackInSlot(SLOT_FOCUS, ItemStack.parseOptional(registries, tags.getCompound("focusSlot")));
		}
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.put("inputTank", this.inputTank.writeToNBT(registries, new CompoundTag()));
		tags.putByte("intensity", (byte) this.intensity);
		tags.putByte("liquidLevel", (byte) this.liquidLevel);
		if (!getInput().isEmpty()) {
			tags.put("inputSlot", getInput().save(registries));
		}
		if (!getFocus().isEmpty()) {
			tags.put("focusSlot", getFocus().save(registries));
		}
	}
}
