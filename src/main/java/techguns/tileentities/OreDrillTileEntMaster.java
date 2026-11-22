package techguns.tileentities;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.TGBlockEntities;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.TGOreClusters;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.blocks.BlockOreCluster;
import techguns.blocks.OreClusterType;
import techguns.gui.containers.OreDrillMenu;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;

/**
 * Ore drill controller (1.12 OreDrillTileEntMaster). Mines the ore cluster at the end of the rods. Runs on furnace
 * fuel, fuel fluids / lava in the input tank or energy; oil clusters fill the output tank.
 */
public class OreDrillTileEntMaster extends MultiBlockMachineTileEntMaster implements IFluidTankTile {

	protected static final float SOUND_VOLUME = 0.5f;

	public static final int CAPACITY_INPUT_TANK = 16 * 1000;
	public static final int CAPACITY_OUTPUT_TANK = 32 * 1000;

	public static final int SLOT_DRILL = 0;
	public static final int SLOT_FURNACE = 1;
	public static final int SLOT_OUTPUT1 = 2;
	public static final int SLOT_OUTPUT_COUNT = 9;

	public static final TagKey<Fluid> FUEL = TagKey.create(net.minecraft.core.registries.Registries.FLUID, Techguns.rl("fuel"));

	protected int engines;
	protected int rods;
	protected int radius;
	@Nullable
	protected Direction drillDirection;
	protected boolean hasPower = false;
	protected boolean hasDrill = false;
	/** 0 none, 1 steel, 2 obsidian steel, 3 carbon */
	protected int drillType = 0;
	protected int currentFuelBufferMax = 0;
	protected int fuelBuffer = 0;
	protected int soundLoopDelay = 0;
	protected ItemStack lastDrill = ItemStack.EMPTY;

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
	public final IFluidHandler fluidHandler = new OreDrillFluidHandler();

	public OreDrillTileEntMaster(BlockPos pos, BlockState state) {
		super(TGBlockEntities.ORE_DRILL.get(), pos, state, 2 + SLOT_OUTPUT_COUNT, 500000);
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
				return slot <= SLOT_FURNACE && isItemValidForSlot(slot, stack);
			}

			@Override
			protected boolean allowExtractFromSlot(int slot, int amount) {
				return slot >= SLOT_OUTPUT1 && slot < SLOT_OUTPUT1 + SLOT_OUTPUT_COUNT;
			}

			@Override
			public int getSlotLimit(int slot) {
				return slot == SLOT_DRILL ? 1 : super.getSlotLimit(slot);
			}
		};
	}

	protected void onTankChanged() {
		this.setContentsChanged(true);
		this.setChanged();
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new OreDrillMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.oredrill");
	}

	@Override
	protected int getUseRangeSquared() {
		return 144;
	}

	/* ---- drill heads ---- */

	/** drill head size class: 0 small, 1 medium, 2 large */
	public static int getDrillSize(int drillRadius) {
		return drillRadius == 0 ? 0 : drillRadius <= 2 ? 1 : 2;
	}

	/** [size class, mining level] of a drill head item, null if none */
	@Nullable
	public static int[] getDrillHead(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		Item i = stack.getItem();
		if (i == TGItems.OREDRILLHEAD_STEEL.get()) return new int[] { 0, 1 };
		if (i == TGItems.OREDRILLHEAD_OBSIDIANSTEEL.get()) return new int[] { 0, 2 };
		if (i == TGItems.OREDRILLHEAD_CARBON.get()) return new int[] { 0, 3 };
		if (i == TGItems.OREDRILLHEAD_MEDIUM_STEEL.get()) return new int[] { 1, 1 };
		if (i == TGItems.OREDRILLHEAD_MEDIUM_OBSIDIANSTEEL.get()) return new int[] { 1, 2 };
		if (i == TGItems.OREDRILLHEAD_MEDIUM_CARBON.get()) return new int[] { 1, 3 };
		if (i == TGItems.OREDRILLHEAD_LARGE_STEEL.get()) return new int[] { 2, 1 };
		if (i == TGItems.OREDRILLHEAD_LARGE_OBSIDIANSTEEL.get()) return new int[] { 2, 2 };
		if (i == TGItems.OREDRILLHEAD_LARGE_CARBON.get()) return new int[] { 2, 3 };
		return null;
	}

	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (slot == SLOT_DRILL) {
			// 1.12 SlotDrill: only the drill size that fits the formed drill
			int[] head = getDrillHead(stack);
			return head != null && isFormed() && head[0] == getDrillSize(getDrillRadius());
		} else if (slot == SLOT_FURNACE) {
			return stack.getBurnTime(RecipeType.SMELTING) > 0;
		}
		return false;
	}

	public int getValidSlotForItemInMachine(ItemStack stack) {
		if (getDrillHead(stack) != null) {
			return SLOT_DRILL;
		}
		if (isItemValidForSlot(SLOT_FURNACE, stack)) {
			return SLOT_FURNACE;
		}
		return -1;
	}

	/** mining level of the drill head, -1 if none or the wrong size */
	public int getDrillItemMiningLevel() {
		int[] head = getDrillHead(this.inventory.getStackInSlot(SLOT_DRILL));
		if (head != null && isFormed() && head[0] == getDrillSize(getDrillRadius())) {
			return head[1];
		}
		return -1;
	}

	/* ---- multiblock ---- */

	public void formOreDrill(int engines, int rods, int radius, Direction drillDirection) {
		this.engines = engines;
		this.rods = rods;
		this.radius = radius;
		this.drillDirection = drillDirection;
		this.setFormed(true, getMultiblockDirection());
	}

	public void unformOreDrill() {
		this.engines = 0;
		this.rods = 0;
		this.radius = 0;
		this.drillDirection = null;
		this.currentOperation = null;
		this.progress = 0;
		this.totaltime = 0;
		this.hasDrill = false;
		this.hasPower = false;
		this.setFormed(false, getMultiblockDirection());
	}

	public int getEngines() {
		return engines;
	}

	public int getRods() {
		return rods;
	}

	public int getRadius() {
		return radius;
	}

	@Nullable
	public Direction getDrillDirection() {
		return drillDirection;
	}

	/** radius including the middle block: 0 for the controller + 1 rod drill, else engine radius + 1 */
	public int getDrillRadius() {
		return engines == 0 ? 0 : radius + 1;
	}

	public int getTotalDrillLength() {
		return engines + rods;
	}

	public boolean hasDrill() {
		return hasDrill;
	}

	public boolean hasPower() {
		return hasPower;
	}

	public int getDrillType() {
		return drillType;
	}

	/* ---- fuel / power ---- */

	public int getFuelBuffer() {
		return fuelBuffer;
	}

	public void setFuelBuffer(int fuelBuffer) {
		this.fuelBuffer = fuelBuffer;
	}

	public int getCurrentFuelBufferMax() {
		return currentFuelBufferMax;
	}

	public void setCurrentFuelBufferMax(int max) {
		this.currentFuelBufferMax = max;
	}

	public int getFuelBufferScaled(int limit) {
		return currentFuelBufferMax > 0 ? fuelBuffer * limit / currentFuelBufferMax : 0;
	}

	public int getPowerPerTick() {
		return currentOperation != null ? currentOperation.getPowerPerTick() : 0;
	}

	@Override
	protected int getNeededPower() {
		return getPowerPerTick();
	}

	/** fuel value per mB of the input tank fluid */
	protected float getFuelValueFromInputTank() {
		FluidStack f = inputTank.getFluid();
		if (f.isEmpty()) {
			return 0f;
		}
		if (f.is(FluidTags.LAVA)) {
			// same as a lava bucket / 1000
			return 20f;
		}
		if (f.is(FUEL)) {
			return TGConfig.oreDrillFuelValueFuel;
		}
		return 0f;
	}

	private boolean useBuffer(int amount, float factor) {
		if (this.fuelBuffer * factor >= amount) {
			this.fuelBuffer -= Math.max((int) (amount / factor), 1);
			return true;
		}
		return false;
	}

	/** fuel buffer first, refilled from the fuel slot or the input tank, then energy (1.12 consumePower) */
	@Override
	protected boolean consumePower(int amount) {
		if (TGConfig.machinesNeedNoPower || amount <= 0) {
			return true;
		}
		float factor = TGConfig.oreDrillMultiplierFuel;
		if (useBuffer(amount, factor)) {
			return true;
		}
		amount -= (int) (this.fuelBuffer * factor);
		this.fuelBuffer = 0;
		this.currentFuelBufferMax = 0;
		ItemStack fuel = this.inventory.getStackInSlot(SLOT_FURNACE);
		int fuelValue = fuel.isEmpty() ? 0 : fuel.getBurnTime(RecipeType.SMELTING);
		if (fuelValue > 0) {
			ItemStack container = fuel.getCraftingRemainingItem();
			this.inventory.extractWithoutCheck(SLOT_FURNACE, 1, false);
			if (!container.isEmpty()) {
				ItemStack rest = container.copy();
				if (this.inventory.getStackInSlot(SLOT_FURNACE).isEmpty()) {
					// bucket back into the fuel slot
					this.inventory.setStackInSlot(SLOT_FURNACE, rest);
					rest = ItemStack.EMPTY;
				}
				for (int s = SLOT_OUTPUT1; s < SLOT_OUTPUT1 + SLOT_OUTPUT_COUNT && !rest.isEmpty(); s++) {
					rest = this.inventory.insertItemNoCheck(s, rest, false);
				}
				if (!rest.isEmpty()) {
					Containers.dropItemStack(level, worldPosition.getX() + 0.5d, worldPosition.getY() + 0.5d, worldPosition.getZ() + 0.5d, rest);
				}
			}
			this.fuelBuffer = fuelValue;
			this.currentFuelBufferMax = fuelValue;
		} else {
			float fluidValue = getFuelValueFromInputTank();
			if (fluidValue > 0) {
				FluidStack drained = this.inputTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
				this.fuelBuffer = (int) (drained.getAmount() * fluidValue);
				this.currentFuelBufferMax = this.fuelBuffer;
			}
		}
		if (useBuffer(amount, factor)) {
			return true;
		}
		if (this.energy.getEnergyStored() >= amount) {
			this.energy.extractEnergy(amount, false);
			return true;
		}
		return false;
	}

	/* ---- operation ---- */

	/** every second: a changed drill head stops the operation */
	protected boolean checkDrill() {
		if (level.getGameTime() % 20 == 0) {
			ItemStack drill = this.inventory.getStackInSlot(SLOT_DRILL);
			if (this.lastDrill.isEmpty() && drill.isEmpty()) {
				this.hasDrill = false;
				this.needUpdate();
				return false;
			} else if (this.lastDrill.isEmpty()) {
				this.lastDrill = drill.copy();
			} else if (drill.isEmpty() || !ItemStack.isSameItem(this.lastDrill, drill)) {
				this.lastDrill = drill.copy();
				this.currentOperation = null;
				this.progress = 0;
				this.totaltime = 0;
				this.setContentsChanged(true);
				this.needUpdate();
				return false;
			}
		}
		return true;
	}

	@Override
	protected void tick() {
		if (this.level == null || !this.isRedstoneEnabled() || !this.isFormed()) {
			return;
		}
		if (this.level.isClientSide) {
			// animation and sound only, the server decides
			if (this.currentOperation != null && this.hasPower) {
				this.progress = Math.min(this.progress + 1, this.totaltime);
				this.playAmbientSound();
			}
			return;
		}
		if (this.currentOperation != null) {
			if (!this.checkDrill()) {
				return;
			}
			if (this.consumePower(this.getNeededPower() * this.currentOperation.getStackMultiplier())) {
				this.progress++;
				if (!this.hasPower) {
					this.hasPower = true;
					this.needUpdate();
				}
				if (this.progress >= this.totaltime) {
					this.finishedOperation();
					this.progress = 0;
					this.totaltime = 0;
					this.currentOperation = null;
					this.checkAndStartOperation();
					this.needUpdate();
				}
			} else if (this.hasPower) {
				this.hasPower = false;
				this.needUpdate();
			}
		} else if (this.contentsChanged) {
			this.checkAndStartOperation();
		}
	}

	@Override
	protected void checkAndStartOperation() {
		if (!this.isFormed() || this.drillDirection == null) {
			return;
		}
		this.setContentsChanged(false);
		BlockPos targetPos = worldPosition.relative(drillDirection, rods + engines + 1);
		BlockState bs = level.getBlockState(targetPos);
		if (!(bs.getBlock() instanceof BlockOreCluster cluster)) {
			// cluster gone
			this.onMultiBlockBreak();
			this.needUpdate();
			return;
		}
		OreClusterType type = cluster.getType();
		int drillLevel = this.getDrillItemMiningLevel();
		this.lastDrill = this.inventory.getStackInSlot(SLOT_DRILL).copy();
		if (drillLevel > 0) {
			this.hasDrill = true;
			this.drillType = drillLevel;
			int effectiveLevel = drillLevel + this.getDrillRadius() - type.getMiningLevel();
			if (effectiveLevel >= 0) {
				int clusterSize = getClusterSize(targetPos, bs, this.getTotalDrillLength());
				int effectiveSize = Math.min(clusterSize, this.getTotalDrillLength());
				double orePerHour = ((effectiveSize * 3) + (effectiveSize * effectiveLevel * 0.5)) * TGConfig.oreDrillMultiplierOres * type.getAmountMultiplier();
				this.currentOperation = TGOreClusters.getNewOperation(type, level.random, orePerHour, this.getDrillRadius());
				this.totaltime = Math.max(1, (int) (60.0 / orePerHour * 1200.0));
			} else {
				this.currentOperation = TGOreClusters.getCobbleStoneOperation();
				this.totaltime = 20 * 20;
			}
			this.progress = 0;
		} else {
			this.hasDrill = false;
			this.drillType = 0;
		}
		this.needUpdate();
	}

	/** connected blocks of the same cluster, counted up to max */
	protected int getClusterSize(BlockPos start, BlockState cluster, int max) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		visited.add(start);
		queue.add(start);
		while (!queue.isEmpty() && visited.size() < max) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (!visited.contains(n) && level.isLoaded(n) && level.getBlockState(n) == cluster) {
					visited.add(n);
					queue.add(n);
				}
			}
		}
		return visited.size();
	}

	@Override
	protected void finishedOperation() {
		ItemStack rest = this.currentOperation.getItemOutput0();
		for (int s = SLOT_OUTPUT1; s < SLOT_OUTPUT1 + SLOT_OUTPUT_COUNT && !rest.isEmpty(); s++) {
			rest = this.inventory.insertItemNoCheck(s, rest, false);
		}
		if (!rest.isEmpty()) {
			Containers.dropItemStack(level, worldPosition.getX() + 0.5d, worldPosition.getY() + 0.5d, worldPosition.getZ() + 0.5d, rest);
		}
		FluidStack fluidOut = this.currentOperation.getFluidOutput0();
		if (!fluidOut.isEmpty()) {
			this.outputTank.fill(fluidOut, IFluidHandler.FluidAction.EXECUTE);
		}
	}

	/** client: work sound loop in the middle of the rods */
	@Override
	protected void playAmbientSound() {
		if (drillDirection == null || soundLoopDelay-- > 0) {
			return;
		}
		BlockPos p = worldPosition.relative(drillDirection, engines + rods / 2);
		SoundEvent sound = radius == 0 ? TGSounds.ORE_DRILL_WORK_SMALL.get()
				: radius <= 2 ? TGSounds.ORE_DRILL_WORK_MEDIUM.get() : TGSounds.ORE_DRILL_WORK_LARGE.get();
		level.playLocalSound(p.getX() + 0.5d, p.getY() + 0.5d, p.getZ() + 0.5d, sound, SoundSource.BLOCKS, SOUND_VOLUME, 1.0f, false);
		// 62 ticks = 3.1 sec sound
		soundLoopDelay = 61;
	}

	/* ---- tanks ---- */

	@Override
	public IFluidHandler getFluidHandler() {
		return fluidHandler;
	}

	@Override
	public boolean onFluidContainerInteract(Player player, InteractionHand hand) {
		if (!this.isUseableByPlayer(player)) {
			return false;
		}
		return FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler);
	}

	/** pipes fill the input tank and drain the output tank */
	private class OreDrillFluidHandler implements IFluidHandler {
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
			return outputTank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return outputTank.drain(maxDrain, action);
		}
	}

	/* ---- save / sync ---- */

	@Override
	protected MachineOperation createEmptyOperation() {
		return new MachineOperation();
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		if (tags.contains("drillDirection")) {
			this.drillDirection = Direction.from3DDataValue(tags.getByte("drillDirection"));
			this.engines = tags.getByte("engines");
			this.rods = tags.getByte("rods");
			this.radius = tags.getByte("radius");
		} else {
			this.drillDirection = null;
			this.engines = 0;
			this.rods = 0;
			this.radius = 0;
		}
		this.hasPower = tags.getBoolean("hasPower");
		this.hasDrill = tags.getBoolean("hasDrill");
		this.drillType = tags.getByte("drillType");
		this.fuelBuffer = tags.getInt("fuelBuffer");
		this.currentFuelBufferMax = tags.getInt("fuelBufferMax");
		this.inputTank.setFluid(FluidStack.EMPTY);
		this.outputTank.setFluid(FluidStack.EMPTY);
		this.inputTank.readFromNBT(registries, tags.getCompound("inputTank"));
		this.outputTank.readFromNBT(registries, tags.getCompound("outputTank"));
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		if (this.drillDirection != null) {
			tags.putByte("drillDirection", (byte) this.drillDirection.get3DDataValue());
			tags.putByte("engines", (byte) engines);
			tags.putByte("rods", (byte) rods);
			tags.putByte("radius", (byte) radius);
		}
		tags.putBoolean("hasPower", hasPower);
		tags.putBoolean("hasDrill", hasDrill);
		tags.putByte("drillType", (byte) drillType);
		tags.putInt("fuelBuffer", fuelBuffer);
		tags.putInt("fuelBufferMax", currentFuelBufferMax);
		tags.put("inputTank", this.inputTank.writeToNBT(registries, new CompoundTag()));
		tags.put("outputTank", this.outputTank.writeToNBT(registries, new CompoundTag()));
	}
}
