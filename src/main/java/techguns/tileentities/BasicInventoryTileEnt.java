package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import techguns.tileentities.operation.ItemStackHandlerPlus;

/** Block entity with an item inventory (1.12 BasicInventoryTileEnt). */
public abstract class BasicInventoryTileEnt extends BasicTGTileEntity {

	protected ItemStackHandlerPlus inventory;
	protected boolean contentsChanged = true;

	public BasicInventoryTileEnt(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize, boolean hasRotation) {
		super(type, pos, state, hasRotation);
		this.inventory = this.createInventory(inventorySize);
	}

	/** override for slot rules; the returned handler must call {@link #onInventoryChanged(int)} */
	protected ItemStackHandlerPlus createInventory(int size) {
		return new ItemStackHandlerPlus(size) {
			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged(slot);
			}
		};
	}

	protected void onInventoryChanged(int slot) {
		this.setContentsChanged(true);
		this.setChanged();
	}

	public ItemStackHandlerPlus getInventory() {
		return inventory;
	}

	/** can output be put into slot */
	protected boolean canOutput(ItemStack output, int slot) {
		if (output.isEmpty()) {
			return true;
		}
		ItemStack inSlot = this.inventory.getStackInSlot(slot);
		if (inSlot.isEmpty()) {
			return output.getCount() <= this.inventory.getSlotLimit(slot);
		}
		return ItemStack.isSameItemSameComponents(inSlot, output) && inSlot.getCount() + output.getCount() <= inSlot.getMaxStackSize();
	}

	/** called when the block is removed */
	public void onBlockBreak() {
		if (this.level == null) {
			return;
		}
		for (int i = 0; i < inventory.getSlots(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty()) {
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
				inventory.setStackInSlot(i, ItemStack.EMPTY);
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("inventory")) {
			inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		}
	}

	/** a gui button was pressed (server side, via the menu) */
	public void buttonClicked(int id, Player ply) {
	}

	public void setContentsChanged(boolean contentsChanged) {
		this.contentsChanged = contentsChanged;
	}

	public boolean getContentsChanged() {
		return contentsChanged;
	}
}
