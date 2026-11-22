package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.recipes.MetalPressRecipe;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;
import techguns.tileentities.operation.MachineSlotItem;

/** Metal press: 2 inputs -> techguns:metal_press recipe result (1.12 MetalPressTileEnt). */
public class MetalPressTileEnt extends BasicMachineTileEnt {

	protected static final float SOUND_VOLUME = 0.5f;

	public static final int SLOT_INPUT1 = 0;
	public static final int SLOT_INPUT2 = 1;
	public static final int SLOT_OUTPUT = 2;
	public static final int SLOT_UPGRADE = 3;

	public static final int BUTTON_ID_AUTOSPLIT = BUTTON_ID_REDSTONE + 1;

	public static final int POWER_PER_TICK = 20;

	protected byte autoSplitMode = 0;

	public final MachineSlotItem input1 = new MachineSlotItem(this, SLOT_INPUT1);
	public final MachineSlotItem input2 = new MachineSlotItem(this, SLOT_INPUT2);

	public MetalPressTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.METAL_PRESS.get(), pos, state, 4, true, 20000);
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
					case SLOT_INPUT1, SLOT_INPUT2 -> isItemValidForSlot(slot, stack);
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
	public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
		return new techguns.gui.containers.MetalPressMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.metalpress");
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.autoSplitMode = tags.getByte("autoSplitMode");
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putByte("autoSplitMode", this.autoSplitMode);
	}

	@Override
	protected int getNeededPower() {
		return POWER_PER_TICK;
	}

	protected void splitSlot(MachineSlotItem src, MachineSlotItem target) {
		int amount = src.get().getCount();
		int amount2 = amount / 2;
		ItemStack targetStack = src.get().copyWithCount(amount2);
		src.setStackInSlot(src.get().copyWithCount(amount - amount2));
		target.setStackInSlot(targetStack);
	}

	@Override
	protected void checkAndStartOperation() {
		if (this.autoSplitMode > 0) {
			if (!input1.get().isEmpty() && input2.get().isEmpty() && input1.get().getCount() > 1) {
				if (!MetalPressRecipe.getOutputFor(level, input1.get(), input1.get()).isEmpty()) {
					this.splitSlot(input1, input2);
				}
			} else if (input1.get().isEmpty() && !input2.get().isEmpty() && input2.get().getCount() > 1) {
				if (!MetalPressRecipe.getOutputFor(level, input2.get(), input2.get()).isEmpty()) {
					this.splitSlot(input2, input1);
				}
			}
		}
		ItemStack output = MetalPressRecipe.getOutputFor(level, input1.get(), input2.get());
		this.setContentsChanged(false);
		if (output.isEmpty() || !canOutput(output, SLOT_OUTPUT)) {
			return;
		}
		int maxStack = this.getMaxMachineUpgradeMultiplier(SLOT_UPGRADE);
		int multiplier = 1;
		for (int i = maxStack; i > 1; --i) {
			if (canOutput(output.copyWithCount(output.getCount() * i), SLOT_OUTPUT) && input1.get().getCount() >= i && input2.get().getCount() >= i) {
				multiplier = i;
				break;
			}
		}
		ItemStack in1 = input1.getTypeWithSize(1);
		ItemStack in2 = input2.getTypeWithSize(1);
		input1.consume(multiplier);
		input2.consume(multiplier);
		this.progress = 0;
		this.totaltime = 100;
		this.currentOperation = new MachineOperation(output.copy(), in1, in2).setStackMultiplier(multiplier);
		this.needUpdate();
	}

	@Override
	protected void finishedOperation() {
		this.inventory.insertItemNoCheck(SLOT_OUTPUT, currentOperation.getItemOutput0(), false);
	}

	@Override
	protected void playAmbientSound() {
		int soundTick1 = Math.round(this.totaltime * 0.075f);
		int halfTime = Math.round(totaltime * 0.5f);
		if (this.progress == soundTick1 || this.progress == soundTick1 + halfTime) {
			playLocalSound(TGSounds.METAL_PRESS_WORK.get(), SOUND_VOLUME);
		}
	}

	public byte getAutoSplitMode() {
		return this.autoSplitMode;
	}

	public void setAutoSplitMode(byte mode) {
		this.autoSplitMode = mode;
	}

	public boolean isItemValidForSlot(int slot, ItemStack item) {
		if (level == null) {
			return false;
		}
		boolean empty1 = input1.get().isEmpty();
		boolean empty2 = input2.get().isEmpty();
		if (empty1 && empty2) {
			return MetalPressRecipe.hasRecipeUsing(level, item);
		} else if (empty1 && slot == SLOT_INPUT1) {
			return !MetalPressRecipe.getOutputFor(level, item, input2.get()).isEmpty();
		} else if (empty2 && slot == SLOT_INPUT2) {
			return !MetalPressRecipe.getOutputFor(level, input1.get(), item).isEmpty();
		}
		return ItemStack.isSameItemSameComponents(this.inventory.getStackInSlot(slot), item);
	}

	public int getValidSlotForItemInMachine(ItemStack item) {
		if (!input1.get().isEmpty() && ItemStack.isSameItemSameComponents(input1.get(), item)) {
			return SLOT_INPUT1;
		} else if (!input2.get().isEmpty() && ItemStack.isSameItemSameComponents(input2.get(), item)) {
			return SLOT_INPUT2;
		} else if (input1.get().isEmpty() && MetalPressRecipe.hasRecipeUsing(level, item)) {
			return SLOT_INPUT1;
		} else if (!input1.get().isEmpty() && input2.get().isEmpty() && !MetalPressRecipe.getOutputFor(level, input1.get(), item).isEmpty()) {
			return SLOT_INPUT2;
		} else if (TGItems.isMachineUpgrade(item)) {
			return SLOT_UPGRADE;
		}
		return -1;
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id == BUTTON_ID_AUTOSPLIT) {
			if (this.isUseableByPlayer(ply)) {
				this.autoSplitMode = (byte) (this.autoSplitMode == 0 ? 1 : 0);
				this.setContentsChanged(true);
				this.needUpdate();
			}
		} else {
			super.buttonClicked(id, ply);
		}
	}
}
