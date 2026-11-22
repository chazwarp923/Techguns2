package techguns.gui.widgets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import techguns.TGItems;
import techguns.Techguns;
import techguns.tileentities.operation.ItemStackHandlerPlus;

/** Machine gui slots (1.12 SlotMachineInput, SlotItemHandlerOutput, SlotMachineUpgrade, ...). */
public final class TGSlots {

	/* empty slot icons, textures/gui/emptyslots (added to the block atlas by assets/minecraft/atlases/blocks.json) */
	public static final ResourceLocation INGOTSLOT_TEX = Techguns.rl("gui/emptyslots/emptyslot_ingot");
	public static final ResourceLocation INGOTDARKSLOT_TEX = Techguns.rl("gui/emptyslots/emptyslot_ingot_dark");
	public static final ResourceLocation POWDERSLOT_TEX = Techguns.rl("gui/emptyslots/emptyslot_powder");

	private TGSlots() {
	}

	/** input slot: the player may take items out, automation may not */
	public static class MachineInput extends SlotItemHandler {
		protected final ItemStackHandlerPlus inv;

		public MachineInput(ItemStackHandlerPlus handler, int index, int x, int y) {
			super(handler, index, x, y);
			this.inv = handler;
		}

		public MachineInput withBackground(ResourceLocation icon) {
			this.setBackground(InventoryMenu.BLOCK_ATLAS, icon);
			return this;
		}

		@Override
		public boolean mayPickup(Player player) {
			return !inv.extractWithoutCheck(getSlotIndex(), 1, true).isEmpty();
		}

		@Override
		public ItemStack remove(int amount) {
			return inv.extractWithoutCheck(getSlotIndex(), amount, false);
		}
	}

	/** output slot, nothing can be put in */
	public static class Output extends MachineInput {
		public Output(ItemStackHandlerPlus handler, int index, int x, int y) {
			super(handler, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

	/** machine stack upgrade slot, max 7 */
	public static class Upgrade extends MachineInput {
		public Upgrade(ItemStackHandlerPlus handler, int index, int x, int y) {
			super(handler, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return TGItems.isMachineUpgrade(stack);
		}

		@Override
		public int getMaxStackSize() {
			return 7;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return 7;
		}
	}
}
