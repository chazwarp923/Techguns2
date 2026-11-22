package techguns.gui.containers;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import techguns.tileentities.BasicInventoryTileEnt;
import techguns.tileentities.BasicMachineTileEnt;
import techguns.tileentities.BasicOwnedTileEnt;
import techguns.tileentities.BasicPoweredTileEnt;
import techguns.tileentities.BasicRedstoneTileEnt;

/**
 * Base menu of the Techguns block entities (1.12 TGBaseContainer / OwnedTileContainer / RedstoneTileContainer /
 * BasicMachineContainer). The synced values are data slots bound directly to the block entity fields, which exist
 * on both sides. Gui buttons use the vanilla container button packet ({@link #clickMenuButton}).
 */
public abstract class TGBaseMenu<T extends BasicInventoryTileEnt> extends AbstractContainerMenu {

	protected final T tile;
	/** number of block entity slots, they come first in the slot list */
	protected int tileSlots = 0;

	protected TGBaseMenu(MenuType<?> type, int id, Inventory inv, T tile) {
		super(type, id);
		this.tile = tile;
		if (tile instanceof BasicOwnedTileEnt owned) {
			addData(owned::getSecurity, v -> owned.setSecurity((byte) v));
		}
		if (tile instanceof BasicRedstoneTileEnt rs) {
			addData(rs::getRedstoneBehaviour, v -> rs.setRedstoneBehaviour((byte) v));
			addData(() -> rs.hasSignal() ? 1 : 0, v -> rs.setHasSignal(v != 0));
		}
		if (tile instanceof BasicPoweredTileEnt pw) {
			// data slots are sent as shorts: split the energy in two 16 bit halves
			addData(() -> pw.getEnergyStorage().getEnergyStored() & 0xFFFF,
					v -> pw.getEnergyStorage().setEnergyStored((pw.getEnergyStorage().getEnergyStored() & 0xFFFF0000) | (v & 0xFFFF)));
			addData(() -> pw.getEnergyStorage().getEnergyStored() >>> 16,
					v -> pw.getEnergyStorage().setEnergyStored((pw.getEnergyStorage().getEnergyStored() & 0xFFFF) | ((v & 0xFFFF) << 16)));
		}
		if (tile instanceof BasicMachineTileEnt m) {
			addData(() -> m.progress, v -> m.progress = v);
			addData(() -> m.totaltime, v -> m.totaltime = v);
		}
	}

	public T getTile() {
		return tile;
	}

	protected void addData(IntSupplier get, IntConsumer set) {
		this.addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return get.getAsInt();
			}

			@Override
			public void set(int value) {
				set.accept(value);
			}
		});
	}

	@Override
	protected Slot addSlot(Slot slot) {
		return super.addSlot(slot);
	}

	/** add the machine slots first, then call this */
	protected void addPlayerInventorySlots(Inventory inv) {
		this.tileSlots = this.slots.size();
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}
		for (int i = 0; i < 9; ++i) {
			this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return tile.isUseableByPlayer(player);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		tile.buttonClicked(id, player);
		return true;
	}

	/**
	 * Shift click: machine slots go to the player inventory, player items go to the machine slot validSlot returns
	 * (-1 = none).
	 */
	protected ItemStack quickMove(Player player, int index, ToIntFunction<ItemStack> validSlot) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack1 = slot.getItem();
		ItemStack stack = stack1.copy();
		int invStart = tileSlots;
		int invEnd = tileSlots + 36;
		if (index < tileSlots) {
			if (!this.moveItemStackTo(stack1, invStart, invEnd, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack1, stack);
		} else {
			int target = validSlot.applyAsInt(stack1);
			if (target < 0 || !this.moveItemStackTo(stack1, target, target + 1, false)) {
				return ItemStack.EMPTY;
			}
		}
		if (stack1.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack1.getCount() == stack.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack1);
		return stack;
	}
}
