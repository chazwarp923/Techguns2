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
import techguns.tileentities.operation.AmmoPressBuildPlans;
import techguns.tileentities.operation.ItemStackHandlerPlus;
import techguns.tileentities.operation.MachineOperation;
import techguns.tileentities.operation.MachineSlotItem;

/** Ammo press: 1 bullet metal + 2 casing metal + 1 powder -> rounds of the selected plan (1.12 AmmoPressTileEnt). */
public class AmmoPressTileEnt extends BasicMachineTileEnt {

	protected static final float SOUND_VOLUME = 0.5f;

	public static final int SLOT_METAL1 = 0;
	public static final int SLOT_METAL2 = 1;
	public static final int SLOT_POWDER = 2;
	public static final int SLOT_OUTPUT = 3;
	public static final int SLOT_UPGRADE = 4;

	public static final int BUTTON_ID_NEXT = BUTTON_ID_REDSTONE + 1;
	public static final int BUTTON_ID_PREV = BUTTON_ID_REDSTONE + 2;

	public static final int POWER_PER_TICK = 5;

	public final MachineSlotItem inputMetal1 = new MachineSlotItem(this, SLOT_METAL1);
	public final MachineSlotItem inputMetal2 = new MachineSlotItem(this, SLOT_METAL2);
	public final MachineSlotItem inputPowder = new MachineSlotItem(this, SLOT_POWDER);

	public byte buildPlan = 0;

	public AmmoPressTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.AMMO_PRESS.get(), pos, state, 5, true, 20000);
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
					case SLOT_METAL1, SLOT_METAL2, SLOT_POWDER -> AmmoPressBuildPlans.isValidFor(stack, slot);
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
		return new techguns.gui.containers.AmmoPressMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.ammopress");
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.buildPlan = tags.getByte("BuildPlan");
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putByte("BuildPlan", this.buildPlan);
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id == BUTTON_ID_NEXT || id == BUTTON_ID_PREV) {
			if (this.isUseableByPlayer(ply)) {
				int p = this.buildPlan + (id == BUTTON_ID_NEXT ? 1 : -1);
				this.buildPlan = (byte) Math.floorMod(p, AmmoPressBuildPlans.PLAN_COUNT);
				this.setContentsChanged(true);
				this.needUpdate();
			}
		} else {
			super.buttonClicked(id, ply);
		}
	}

	@Override
	protected int getNeededPower() {
		return POWER_PER_TICK;
	}

	@Override
	protected void checkAndStartOperation() {
		this.setContentsChanged(false);
		ItemStack output = AmmoPressBuildPlans.getOutputFor(inputMetal1.get(), inputMetal2.get(), inputPowder.get(), this.buildPlan);
		if (output.isEmpty() || !canOutput(output, SLOT_OUTPUT)) {
			return;
		}
		int maxStack = this.getMaxMachineUpgradeMultiplier(SLOT_UPGRADE);
		int multiplier = 1;
		for (int i = maxStack; i > 1; --i) {
			if (canOutput(output.copyWithCount(output.getCount() * i), SLOT_OUTPUT) && inputMetal1.canConsume(i) && inputMetal2.canConsume(2 * i)
					&& inputPowder.canConsume(i)) {
				multiplier = i;
				break;
			}
		}
		ItemStack metal1 = inputMetal1.getTypeWithSize(1);
		ItemStack metal2 = inputMetal2.getTypeWithSize(2);
		ItemStack powder = inputPowder.getTypeWithSize(1);
		inputMetal1.consume(multiplier);
		inputMetal2.consume(multiplier * 2);
		inputPowder.consume(multiplier);
		this.currentOperation = new MachineOperation(output, metal1, metal2, powder).setStackMultiplier(multiplier);
		this.progress = 0;
		this.totaltime = 100;
		this.needUpdate();
	}

	@Override
	protected void finishedOperation() {
		this.inventory.insertItemNoCheck(SLOT_OUTPUT, currentOperation.getItemOutput0(), false);
	}

	@Override
	protected void playAmbientSound() {
		int soundTick1 = Math.round(this.totaltime * 0.05f);
		int soundTick2 = Math.round(this.totaltime * 0.30f);
		int halfTime = Math.round(totaltime * 0.5f);
		if (this.progress == soundTick1 || this.progress == soundTick1 + halfTime) {
			playLocalSound(TGSounds.AMMO_PRESS_WORK1.get(), SOUND_VOLUME);
		} else if (this.progress == soundTick2 || this.progress == soundTick2 + halfTime) {
			playLocalSound(TGSounds.AMMO_PRESS_WORK2.get(), SOUND_VOLUME);
		}
	}

	public static int getValidSlotForItemInMachine(ItemStack item) {
		for (int s : new int[] { SLOT_METAL1, SLOT_METAL2, SLOT_POWDER }) {
			if (AmmoPressBuildPlans.isValidFor(item, s)) {
				return s;
			}
		}
		return TGItems.isMachineUpgrade(item) ? SLOT_UPGRADE : -1;
	}
}
