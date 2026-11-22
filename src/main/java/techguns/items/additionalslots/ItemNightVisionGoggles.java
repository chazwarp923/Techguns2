package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.TGItems;
import techguns.api.tginventory.TGSlotType;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.armors.TGArmorBonus;

/** Face slot: night vision, uses charge while enabled. Reloads with redstone batteries. */
public class ItemNightVisionGoggles extends ItemTGSpecialSlot {

	public ItemNightVisionGoggles(int camoCount, int dur) {
		super(TGSlotType.FACESLOT, camoCount, dur, TGItems.REDSTONE_BATTERY, TGItems.REDSTONE_BATTERY_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack item, Player ply) {
		if (hasCharge(item)) {
			if (TGExtendedPlayer.get(ply).enableNightVision) {
				useCharge(item, 1);
			}
		} else {
			this.tryReloadAndRepair(item, ply);
		}
	}

	@Override
	public float getBonus(TGArmorBonus type, ItemStack stack, boolean consume, Player player) {
		return type == TGArmorBonus.NIGHTVISION && hasCharge(stack) ? 1.0f : 0.0f;
	}
}
