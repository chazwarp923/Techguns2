package techguns.api.tginventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.items.armors.TGArmorBonus;

/** Items that fit into one of the Techguns special inventory slots. */
public interface ITGSpecialSlot {
	TGSlotType getSlot(ItemStack item);

	default float getBonus(TGArmorBonus type, ItemStack stack, boolean consume, Player player) {
		return 0f;
	}

	/** ticked while worn in its Curios slot (both sides) */
	default void onPlayerTick(ItemStack stack, Player player) {
	}
}
