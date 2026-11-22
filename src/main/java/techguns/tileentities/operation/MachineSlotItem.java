package techguns.tileentities.operation;

import net.minecraft.world.item.ItemStack;
import techguns.tileentities.BasicInventoryTileEnt;

/** One input slot of a machine (1.12 MachineSlotItem). */
public class MachineSlotItem {

	protected final BasicInventoryTileEnt tile;
	protected final int slotID;

	public MachineSlotItem(BasicInventoryTileEnt tile, int slotID) {
		this.tile = tile;
		this.slotID = slotID;
	}

	public void consume(int amount) {
		if (amount > 0) {
			this.tile.getInventory().extractWithoutCheck(slotID, amount, false);
		}
	}

	public ItemStack getTypeWithSize(int size) {
		return get().copyWithCount(size);
	}

	public boolean canConsume(int amount) {
		ItemStack stack = get();
		return !stack.isEmpty() && stack.getCount() >= amount;
	}

	public boolean canConsumeWithMultiplier(ItemStack stack, int multiplier) {
		if (stack.isEmpty()) {
			return true;
		}
		return stack.getCount() * multiplier <= this.get().getCount();
	}

	public ItemStack get() {
		return tile.getInventory().getStackInSlot(slotID);
	}

	public void setStackInSlot(ItemStack stack) {
		tile.getInventory().setStackInSlot(slotID, stack);
	}
}
