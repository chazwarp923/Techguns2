package techguns.tileentities;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.gui.containers.RepairBenchMenu;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.GenericShield;
import techguns.tileentities.operation.ItemStackHandlerPlus;

/**
 * Repair bench (1.12 RepairBenchTileEnt): 9 material slots and one repair slot. The gui buttons repair the player's
 * armor, offhand shield or the item in the repair slot with the materials from the bench.
 */
public class RepairBenchTileEnt extends BasicOwnedTileEnt {

	public static final int MAT_SLOTS = 9;
	public static final int SLOT_REPAIR = 9;

	/** helmet, chest, legs, boots */
	public static final int BUTTON_ID_ARMOR0 = 10;
	public static final int BUTTON_ID_OFFHAND = BUTTON_ID_ARMOR0 + 4;
	public static final int BUTTON_ID_SLOT = BUTTON_ID_OFFHAND + 1;

	public static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

	public RepairBenchTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.REPAIR_BENCH.get(), pos, state, 10, false);
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
				return slot != SLOT_REPAIR || isRepairable(stack);
			}
		};
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new RepairBenchMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.repairbench");
	}

	public static boolean isRepairable(ItemStack stack) {
		return stack.getItem() instanceof GenericArmor || stack.getItem() instanceof GenericShield;
	}

	/** null: the item can not be repaired here, empty: nothing to repair */
	public static List<ItemStack> getRepairMats(ItemStack item) {
		if (item.getItem() instanceof GenericArmor a && a.canRepairOnRepairBench(item)) {
			return a.getRepairMats(item);
		}
		if (item.getItem() instanceof GenericShield s && s.canRepairOnRepairBench(item)) {
			return s.getRepairMats(item);
		}
		return null;
	}

	/** how many of mat are missing in the material slots */
	public int countMissing(ItemStack mat) {
		int need = mat.getCount();
		for (int i = 0; i < MAT_SLOTS && need > 0; i++) {
			ItemStack s = inventory.getStackInSlot(i);
			if (ItemStack.isSameItemSameComponents(s, mat)) {
				need -= Math.min(need, s.getCount());
			}
		}
		return need;
	}

	protected void consume(ItemStack mat) {
		int need = mat.getCount();
		for (int i = 0; i < MAT_SLOTS && need > 0; i++) {
			if (ItemStack.isSameItemSameComponents(inventory.getStackInSlot(i), mat)) {
				need -= inventory.extractWithoutCheck(i, need, false).getCount();
			}
		}
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id >= BUTTON_ID_ARMOR0 && id <= BUTTON_ID_SLOT) {
			if (!this.isUseableByPlayer(ply)) {
				return;
			}
			if (id < BUTTON_ID_OFFHAND) {
				doRepair(ply.getItemBySlot(ARMOR_SLOTS[id - BUTTON_ID_ARMOR0]));
			} else if (id == BUTTON_ID_OFFHAND) {
				doRepair(ply.getOffhandItem());
			} else {
				doRepair(inventory.getStackInSlot(SLOT_REPAIR));
				this.setChanged();
			}
		} else {
			super.buttonClicked(id, ply);
		}
	}

	protected void doRepair(ItemStack item) {
		if (item.isEmpty()) {
			return;
		}
		List<ItemStack> mats = getRepairMats(item);
		if (mats == null || mats.isEmpty()) {
			return;
		}
		for (ItemStack m : mats) {
			if (countMissing(m) > 0) {
				return;
			}
		}
		mats.forEach(this::consume);
		item.setDamageValue(0);
	}
}
