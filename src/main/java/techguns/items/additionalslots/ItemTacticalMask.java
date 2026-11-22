package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.TGItems;
import techguns.api.tginventory.TGSlotType;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.armors.TGArmorBonus;

/** Face slot: night vision (uses charge while enabled), oxygen gear, gas mask. Reloads with energy cells. */
public class ItemTacticalMask extends ItemTGSpecialSlot {

	public ItemTacticalMask(int camoCount, int dur) {
		super(TGSlotType.FACESLOT, camoCount, dur, TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY);
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
		ItemGasMask.cleanse(item, ply);
	}

	@Override
	public float getBonus(TGArmorBonus type, ItemStack stack, boolean consume, Player player) {
		if ((type == TGArmorBonus.NIGHTVISION || type == TGArmorBonus.OXYGEN_GEAR) && hasCharge(stack)) {
			return 1.0f;
		}
		return 0.0f;
	}
}
