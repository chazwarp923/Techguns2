package techguns.gametest;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.inventory.IMekanismInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fills a Mekanism machine's slots and energy buffer directly, the way its GUI and energy upgrades see them. A new
 * machine's unsided item and Forge Energy capabilities take nothing from outside, and piping isn't what the compat
 * tests check. Only called when Mekanism is loaded: this class
 * uses Mekanism's API.
 */
final class MekanismMachineChecks {

	private MekanismMachineChecks() {
	}

	/** inserts into the first slot that takes the stack; returns what's left */
	static ItemStack insert(BlockEntity be, ItemStack stack) {
		ItemStack rest = stack.copy();
		if (be instanceof IMekanismInventory inv) {
			for (IInventorySlot slot : inv.getInventorySlots(null)) {
				rest = slot.insertItem(rest, Action.EXECUTE, AutomationType.MANUAL);
				if (rest.isEmpty()) {
					break;
				}
			}
		}
		return rest;
	}

	/** fills the machine's energy buffers; returns the energy stored afterwards */
	static long fillEnergy(BlockEntity be) {
		long stored = 0;
		if (be instanceof IMekanismStrictEnergyHandler h) {
			for (IEnergyContainer c : h.getEnergyContainers(null)) {
				c.setEnergy(c.getMaxEnergy());
				stored += c.getEnergy();
			}
		}
		return stored;
	}
}
