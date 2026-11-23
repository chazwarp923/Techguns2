package techguns.util;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import techguns.curios.TGCurios;
import techguns.items.ItemAmmoBox;

public final class InventoryUtil {
	private InventoryUtil() {}

	/** Add to the TG ammo slots first, then the ammo boxes, then the main inventory. Returns amount that did NOT fit. */
	public static int addAmmoToPlayerInventory(Player ply, ItemStack ammo) {
		int amount = addAmmoToAmmoInventory(ply, ammo);
		if (amount > 0) {
			amount = addAmmoToAmmoBoxes(ply, ammo.copyWithCount(amount));
		}
		if (amount > 0) {
			return addItemToInventory(ply.getInventory().items, ammo.copyWithCount(amount), 0, ply.getInventory().items.size());
		}
		return 0;
	}

	/** Add only to the TG ammo slots. Returns amount that did NOT fit. */
	public static int addAmmoToAmmoInventory(Player ply, ItemStack ammo) {
		List<ItemStack> slots = TGCurios.getStacks(ply, TGCurios.SLOT_AMMO);
		return addItemToInventory(slots, ammo, 0, slots.size());
	}

	/** the ammo boxes in the main inventory and the offhand (Fabric port: reloading uses them) */
	public static List<ItemStack> getAmmoBoxes(Player ply) {
		List<ItemStack> boxes = new java.util.ArrayList<>();
		for (ItemStack s : ply.getInventory().items) {
			if (s.getItem() instanceof ItemAmmoBox) {
				boxes.add(s);
			}
		}
		for (ItemStack s : ply.getInventory().offhand) {
			if (s.getItem() instanceof ItemAmmoBox) {
				boxes.add(s);
			}
		}
		return boxes;
	}

	/** Add to the ammo boxes. Returns amount that did NOT fit. */
	public static int addAmmoToAmmoBoxes(Player ply, ItemStack ammo) {
		ItemStack rest = ammo.copy();
		for (ItemStack box : getAmmoBoxes(ply)) {
			if (rest.isEmpty()) {
				break;
			}
			((ItemAmmoBox) box.getItem()).insert(box, rest);
		}
		return rest.getCount();
	}

	/** Try to merge a stack into a list of slots; returns the amount that could NOT be merged. */
	public static int addItemToInventory(List<ItemStack> inv, ItemStack item2, int startIndex, int endIndex) {
		ItemStack item = item2.copy();
		for (int i = startIndex; i < endIndex && !item.isEmpty(); i++) {
			ItemStack slot = inv.get(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, item) && slot.getCount() < item.getMaxStackSize()) {
				int move = Math.min(item.getCount(), item.getMaxStackSize() - slot.getCount());
				slot.grow(move);
				item.shrink(move);
			}
		}
		for (int i = startIndex; i < endIndex && !item.isEmpty(); i++) {
			if (inv.get(i).isEmpty()) {
				inv.set(i, item.copy());
				item.setCount(0);
			}
		}
		return item.getCount();
	}

	public static int addItemToInventory(IItemHandler inv, ItemStack item, int startIndex, int endIndex) {
		ItemStack rest = item.copy();
		for (int i = startIndex; i < endIndex && !rest.isEmpty(); i++) {
			rest = inv.insertItem(i, rest, false);
		}
		return rest.getCount();
	}

	/** Consume all given ammo stacks (all-or-nothing) from TG ammo slots + main inventory. */
	public static boolean consumeAmmoPlayer(Player ply, ItemStack[] ammo) {
		if (ply.isCreative()) {
			return true;
		}
		for (ItemStack a : ammo) {
			if (!a.isEmpty() && !canConsumeAmmoPlayer(ply, a)) {
				return false;
			}
		}
		for (ItemStack a : ammo) {
			if (!a.isEmpty()) {
				consumeAmmoPlayer(ply, a);
			}
		}
		return true;
	}

	public static boolean consumeAmmoPlayer(Player ply, ItemStack ammo) {
		if (ply.isCreative()) {
			return true;
		}
		if (!canConsumeAmmoPlayer(ply, ammo)) {
			return false;
		}
		List<ItemStack> slots = TGCurios.getStacks(ply, TGCurios.SLOT_AMMO);
		int missing = consumeItem(slots, ammo, 0, slots.size());
		for (ItemStack box : getAmmoBoxes(ply)) {
			if (missing <= 0) {
				break;
			}
			missing -= ItemAmmoBox.remove(box, ammo, missing);
		}
		if (missing > 0) {
			consumeItem(ply.getInventory().items, ammo.copyWithCount(missing), 0, ply.getInventory().items.size());
		}
		return true;
	}

	public static boolean canConsumeAmmoPlayer(Player ply, ItemStack ammo) {
		if (ply.isCreative()) {
			return true;
		}
		List<ItemStack> slots = TGCurios.getStacks(ply, TGCurios.SLOT_AMMO);
		int have = countItemInInv(slots, ammo, 0, slots.size())
				+ countItemInInv(ply.getInventory().items, ammo, 0, ply.getInventory().items.size());
		for (ItemStack box : getAmmoBoxes(ply)) {
			have += ItemAmmoBox.count(box, ammo);
		}
		return have >= ammo.getCount();
	}

	/** Removes up to item.getCount() matching items; returns how many could NOT be removed. */
	public static int consumeItem(List<ItemStack> inv, ItemStack item, int startIndex, int endIndex) {
		int toRemove = item.getCount();
		for (int i = startIndex; i < endIndex && toRemove > 0; i++) {
			ItemStack s = inv.get(i);
			if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, item)) {
				int n = Math.min(toRemove, s.getCount());
				s.shrink(n);
				toRemove -= n;
				if (s.isEmpty()) {
					inv.set(i, ItemStack.EMPTY);
				}
			}
		}
		return toRemove;
	}

	public static int consumeItem(IItemHandler inv, ItemStack item, int startIndex, int endIndex) {
		int toRemove = item.getCount();
		for (int i = startIndex; i < endIndex && toRemove > 0; i++) {
			ItemStack s = inv.getStackInSlot(i);
			if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, item)) {
				toRemove -= inv.extractItem(i, toRemove, false).getCount();
			}
		}
		return toRemove;
	}

	/** Returns how many of item.getCount() could NOT be consumed (0 = all available). */
	public static int canConsumeItem(IItemHandler inv, ItemStack item, int startIndex, int endIndex) {
		int toRemove = item.getCount();
		for (int i = startIndex; i < endIndex && toRemove > 0; i++) {
			ItemStack s = inv.getStackInSlot(i);
			if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, item)) {
				toRemove -= Math.min(toRemove, s.getCount());
			}
		}
		return toRemove;
	}

	public static int countItemInInv(List<ItemStack> inv, ItemStack search, int startIndex, int endIndex) {
		int count = 0;
		for (int i = startIndex; i < endIndex; i++) {
			ItemStack s = inv.get(i);
			if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, search)) {
				count += s.getCount();
			}
		}
		return count;
	}

	/** Consumes one food item from the given slots and returns a single-item copy (or EMPTY). */
	public static ItemStack consumeFood(List<ItemStack> inv, int startIndex, int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			ItemStack s = inv.get(i);
			if (!s.isEmpty() && s.has(DataComponents.FOOD)) {
				ItemStack ret = s.copyWithCount(1);
				s.shrink(1);
				if (s.isEmpty()) {
					inv.set(i, ItemStack.EMPTY);
				}
				return ret;
			}
		}
		return ItemStack.EMPTY;
	}

	public static void dropInventoryItems(Level level, BlockPos pos, IItemHandler inventory) {
		for (int i = 0; i < inventory.getSlots(); i++) {
			ItemStack s = inventory.getStackInSlot(i);
			if (!s.isEmpty()) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s.copy());
				if (inventory instanceof IItemHandlerModifiable m) {
					m.setStackInSlot(i, ItemStack.EMPTY);
				}
			}
		}
	}

	/** Gives the stack to the player, dropping whatever does not fit (server side only). */
	public static void addAmmoToPlayerOrDropInWorld(Player player, ItemStack stack) {
		int amount = addAmmoToPlayerInventory(player, stack);
		if (amount > 0 && !player.level().isClientSide) {
			player.drop(stack.copyWithCount(amount), false);
		}
	}
}
