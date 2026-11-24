package techguns.client.test;

import java.util.List;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.world.item.ItemStack;
import techguns.TGBlocks;

/** Ponder calls for the client test, in their own class so nothing loads them without Ponder. */
final class PonderHook {
	private PonderHook() {
	}

	/** an item for each Techguns Ponder scene (its controller) */
	static List<ItemStack> controllers() {
		return List.of(new ItemStack(TGBlocks.FABRICATOR_CONTROLLER.get()), new ItemStack(TGBlocks.REACTIONCHAMBER_CONTROLLER.get()),
				new ItemStack(TGBlocks.OREDRILL_CONTROLLER.get()));
	}

	static void open(ItemStack stack) {
		ScreenOpener.open(PonderUI.of(stack));
	}
}
