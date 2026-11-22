package techguns.tileentities.operation;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Item handler of the machines (1.12 ItemStackHandlerPlus): automation may only insert what
 * {@link #allowItemInSlot} accepts and only extract where {@link #allowExtractFromSlot} allows. The machine itself
 * uses the NoCheck / WithoutCheck variants.
 */
public class ItemStackHandlerPlus extends ItemStackHandler {

	private boolean bypass = false;

	public ItemStackHandlerPlus(int size) {
		super(size);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return bypass || allowItemInSlot(slot, stack);
	}

	public ItemStack insertItemNoCheck(int slot, ItemStack stack, boolean simulate) {
		boolean old = bypass;
		bypass = true;
		try {
			return super.insertItem(slot, stack, simulate);
		} finally {
			bypass = old;
		}
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (!allowExtractFromSlot(slot, amount)) {
			return ItemStack.EMPTY;
		}
		return super.extractItem(slot, amount, simulate);
	}

	public ItemStack extractWithoutCheck(int slot, int amount, boolean simulate) {
		return super.extractItem(slot, amount, simulate);
	}

	protected boolean allowItemInSlot(int slot, ItemStack stack) {
		return true;
	}

	protected boolean allowExtractFromSlot(int slot, int amount) {
		return true;
	}

	/** can all stacks be merged into the slots [startSlot, endSlot) */
	public boolean canInsertAll(int startSlot, int endSlot, List<ItemStack> stacks) {
		if (endSlot < startSlot) {
			return false;
		}
		ItemStackHandlerPlus temp = new ItemStackHandlerPlus(endSlot - startSlot);
		for (int i = 0; i < endSlot - startSlot; i++) {
			temp.setStackInSlot(i, this.getStackInSlot(i + startSlot).copy());
		}
		List<ItemStack> leftover = temp.mergeAll(0, endSlot - startSlot, stacks);
		return leftover.stream().allMatch(ItemStack::isEmpty);
	}

	/** merges into the slots [startSlot, endSlot), filling existing stacks first, returns what did not fit */
	public List<ItemStack> mergeAll(int startSlot, int endSlot, List<ItemStack> stacks) {
		List<ItemStack> leftover = new ArrayList<>(stacks.size());
		stacks.forEach(s -> leftover.add(s.copy()));
		if (endSlot < startSlot) {
			return leftover;
		}
		for (int j = 0; j < leftover.size(); j++) {
			for (int i = startSlot; i < endSlot; i++) {
				if (!this.getStackInSlot(i).isEmpty()) {
					leftover.set(j, this.insertItemNoCheck(i, leftover.get(j), false));
				}
			}
			for (int i = startSlot; i < endSlot; i++) {
				if (this.getStackInSlot(i).isEmpty()) {
					leftover.set(j, this.insertItemNoCheck(i, leftover.get(j), false));
				}
			}
		}
		return leftover;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		// keep the machine's slot count, even when the saved inventory was smaller / bigger
		int size = this.getSlots();
		super.deserializeNBT(provider, nbt);
		if (this.getSlots() != size) {
			List<ItemStack> old = new ArrayList<>(this.stacks);
			this.setSize(size);
			for (int i = 0; i < Math.min(size, old.size()); i++) {
				this.stacks.set(i, old.get(i));
			}
		}
	}
}
