package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGItems;
import techguns.blocks.machines.BlockMachineFacing;
import techguns.gui.containers.BlastFurnaceMenu;
import techguns.recipes.BlastFurnaceRecipe;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;
import techguns.tileentities.operation.MachineSlotItem;

/** Blast furnace: 2 inputs -> techguns:blast_furnace recipe result (1.12 BlastFurnaceTileEnt). */
public class BlastFurnaceTileEnt extends BasicMachineTileEnt {

	public static final int SLOT_INPUT1 = 0;
	public static final int SLOT_INPUT2 = 1;
	public static final int SLOT_OUTPUT = 2;
	public static final int SLOT_UPGRADE = 3;

	public final MachineSlotItem input1 = new MachineSlotItem(this, SLOT_INPUT1);
	public final MachineSlotItem input2 = new MachineSlotItem(this, SLOT_INPUT2);

	public BlastFurnaceTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.BLAST_FURNACE.get(), pos, state, 4, false, 40000);
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
					case SLOT_INPUT1 -> BlastFurnaceRecipe.hasRecipeWithInputForSlot(level, stack, 0);
					case SLOT_INPUT2 -> BlastFurnaceRecipe.hasRecipeWithInputForSlot(level, stack, 1);
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
		return new BlastFurnaceMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.blast_furnace");
	}

	public int getValidSlotForItemInMachine(ItemStack stack) {
		if (BlastFurnaceRecipe.hasRecipeWithInputForSlot(level, stack, 0)) {
			return SLOT_INPUT1;
		}
		if (BlastFurnaceRecipe.hasRecipeWithInputForSlot(level, stack, 1)) {
			return SLOT_INPUT2;
		}
		return TGItems.isMachineUpgrade(stack) ? SLOT_UPGRADE : -1;
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
		var found = BlastFurnaceRecipe.find(level, input1.get(), input2.get());
		if (found.isEmpty()) {
			return;
		}
		BlastFurnaceRecipe r = found.get().value();
		if (!canOutput(r.result(), SLOT_OUTPUT)) {
			return;
		}
		int n1 = r.input1().count();
		int n2 = r.input2().count();
		int maxStack = this.getMaxMachineUpgradeMultiplier(SLOT_UPGRADE);
		int multiplier = 1;
		for (int i = maxStack; i > 1; --i) {
			if (canOutput(r.result().copyWithCount(r.result().getCount() * i), SLOT_OUTPUT) && input1.canConsume(n1 * i) && input2.canConsume(n2 * i)) {
				multiplier = i;
				break;
			}
		}
		ItemStack in1 = input1.getTypeWithSize(n1);
		ItemStack in2 = input2.getTypeWithSize(n2);
		input1.consume(n1 * multiplier);
		input2.consume(n2 * multiplier);
		this.currentOperation = new MachineOperation(r.result().copy(), in1, in2).setPowerPerTick(r.powerPerTick()).setStackMultiplier(multiplier);
		this.progress = 0;
		this.totaltime = r.time();
		this.needUpdate();
	}

	@Override
	protected void finishedOperation() {
		this.inventory.insertItemNoCheck(SLOT_OUTPUT, currentOperation.getItemOutput0(), false);
	}

	public Direction getFacing() {
		BlockState state = this.getBlockState();
		return state.hasProperty(BlockMachineFacing.FACING) ? state.getValue(BlockMachineFacing.FACING) : Direction.NORTH;
	}

	@Override
	protected void playAmbientSound() {
		if (this.level == null) {
			return;
		}
		RandomSource rand = this.level.random;
		double x = worldPosition.getX() + 0.5d;
		double y = worldPosition.getY();
		double z = worldPosition.getZ() + 0.5d;
		if (this.progress % 35 == 0) {
			level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.75f, 1f, true);
		}
		if (this.progress % 20 == 0) {
			level.playLocalSound(x, y, z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.75f, 0.5f + rand.nextFloat() * 0.15f, true);
			Direction f = getFacing();
			double py = y + rand.nextDouble() * 6.0d / 16.0d + 3.0d / 16.0d;
			double side = rand.nextDouble() * 0.6d - 0.3d;
			double px = x + f.getStepX() * 0.52d + (f.getAxis() == Direction.Axis.Z ? side : 0);
			double pz = z + f.getStepZ() * 0.52d + (f.getAxis() == Direction.Axis.X ? side : 0);
			level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0, 0);
			level.addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0, 0);
		}
	}
}
