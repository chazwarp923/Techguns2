package techguns.tileentities;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.blocks.machines.BlockMachineFacing;
import techguns.gui.containers.GrinderMenu;
import techguns.items.guns.GenericGun;
import techguns.recipes.GrinderRecipe;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;
import techguns.tileentities.operation.MachineSlotItem;

/** Grinder: grinds one item into its materials (1.12 GrinderTileEnt). */
public class GrinderTileEnt extends BasicMachineTileEnt {

	public static final int SLOT_INPUT = 0;
	public static final int SLOT_UPGRADE = 1;
	public static final int SLOT_OUTPUT0 = 2;
	/** exclusive */
	public static final int SLOT_OUTPUT_END = SLOT_OUTPUT0 + 9;

	public static final int POWER_PER_TICK = 5;
	public static final int TIME = 100;

	public final MachineSlotItem input = new MachineSlotItem(this, SLOT_INPUT);

	public GrinderTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.GRINDER.get(), pos, state, 11, false, 20000);
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
					case SLOT_INPUT -> level == null || GrinderRecipe.canGrind(level, stack);
					case SLOT_UPGRADE -> TGItems.isMachineUpgrade(stack);
					default -> false;
				};
			}

			@Override
			protected boolean allowExtractFromSlot(int slot, int amount) {
				return slot >= SLOT_OUTPUT0 && slot < SLOT_OUTPUT_END;
			}
		};
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new GrinderMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.grinder");
	}

	@Override
	protected int getNeededPower() {
		return POWER_PER_TICK;
	}

	public int getValidSlotForItemInMachine(ItemStack item) {
		if (TGItems.isMachineUpgrade(item)) {
			return SLOT_UPGRADE;
		}
		return GrinderRecipe.canGrind(level, item) ? SLOT_INPUT : -1;
	}

	/** splits outputs into stacks of max size so they fit the slot checks */
	private static List<ItemStack> split(List<ItemStack> list) {
		List<ItemStack> ret = new ArrayList<>();
		for (ItemStack s : list) {
			int count = s.getCount();
			int max = s.getMaxStackSize();
			while (count > 0) {
				int n = Math.min(count, max);
				ret.add(s.copyWithCount(n));
				count -= n;
			}
		}
		return ret;
	}

	@Override
	protected void checkAndStartOperation() {
		this.setContentsChanged(false);
		ItemStack in = input.get();
		if (in.isEmpty()) {
			return;
		}
		var recipe = GrinderRecipe.find(level, in);
		List<ItemStack> armorOutputs = recipe.isPresent() ? List.of() : GrinderRecipe.getArmorOutputs(in);
		if (recipe.isEmpty() && armorOutputs.isEmpty()) {
			return;
		}
		// armor is ground one piece at a time, the outputs depend on its durability
		int maxStack = recipe.isPresent() ? this.getMaxMachineUpgradeMultiplier(SLOT_UPGRADE) : 1;
		List<ItemStack> outputs = null;
		int multiplier = 1;
		for (int i = maxStack; i >= 1; --i) {
			if (!input.canConsume(i)) {
				continue;
			}
			List<ItemStack> out = split(recipe.isPresent() ? recipe.get().value().roll(i, level.random) : armorOutputs);
			if (this.inventory.canInsertAll(SLOT_OUTPUT0, SLOT_OUTPUT_END, out)) {
				outputs = out;
				multiplier = i;
				break;
			}
		}
		if (outputs == null) {
			return;
		}
		ItemStack consumed = input.getTypeWithSize(1);
		input.consume(multiplier);
		// outputs are stored already rolled and multiplied, see finishedOperation
		this.currentOperation = new MachineOperation(outputs, List.of(consumed)).setStackMultiplier(multiplier);
		this.progress = 0;
		this.totaltime = TIME;
		this.needUpdate();
	}

	@Override
	protected void finishedOperation() {
		List<ItemStack> leftover = this.inventory.mergeAll(SLOT_OUTPUT0, SLOT_OUTPUT_END, this.currentOperation.getOutputs());
		for (ItemStack s : leftover) {
			if (!s.isEmpty()) {
				Containers.dropItemStack(level, worldPosition.getX() + 0.5d, worldPosition.getY() + 0.5d, worldPosition.getZ() + 0.5d, s);
			}
		}
	}

	public Direction getFacing() {
		BlockState state = this.getBlockState();
		return state.hasProperty(BlockMachineFacing.FACING) ? state.getValue(BlockMachineFacing.FACING) : Direction.NORTH;
	}

	@Override
	protected void playAmbientSound() {
		if (this.level == null || this.currentOperation == null) {
			return;
		}
		int sWork1 = Math.round(this.totaltime * 0.2f) + 1;
		int sWork2 = Math.round(this.totaltime * 0.6f) - 1;
		if (this.progress % 2 == 0) {
			ItemStack it = this.currentOperation.getItemInputI(0);
			if (it.getItem() instanceof GenericGun) {
				it = new ItemStack(TGItems.PLATE_STEEL.get());
			}
			if (!it.isEmpty()) {
				double f = 0.1d;
				double f1 = 0.05d;
				var rand = this.level.random;
				level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, it), worldPosition.getX() + 0.5d, worldPosition.getY() + 0.6d,
						worldPosition.getZ() + 0.5d, rand.nextDouble() * f - f1, rand.nextDouble() * f + f, rand.nextDouble() * f - f1);
			}
		}
		if (this.progress == 1) {
			playLocalSound(TGSounds.GRINDER_START.get(), 1.0f);
		} else if (this.progress == sWork1 || this.progress == sWork2) {
			playLocalSound(TGSounds.GRINDER_WORK.get(), 1.0f);
		}
	}
}
