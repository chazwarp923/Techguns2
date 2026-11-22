package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.TGItems;
import techguns.api.tginventory.TGSlotType;
import techguns.items.armors.ArmorBonusUtil;
import techguns.items.armors.TGArmorBonus;

/** Back slot: refills air under water, needs oxygen gear (oxygen / tactical mask). Reloads with compressed air tanks. */
public class ItemScubaTanks extends ItemTGSpecialSlot {

	public ItemScubaTanks(int camoCount, int dur) {
		super(TGSlotType.BACKSLOT, camoCount, dur, TGItems.COMPRESSED_AIR_TANK, TGItems.COMPRESSED_AIR_TANK_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack item, Player ply) {
		if (!hasCharge(item)) {
			tryReloadAndRepair(item, ply);
		}
		if (hasCharge(item) && ply.getAirSupply() <= 280) {
			if (ArmorBonusUtil.getArmorBonusForPlayer(ply, TGArmorBonus.OXYGEN_GEAR, false) > 0) {
				ply.setAirSupply(Math.min(ply.getMaxAirSupply(), ply.getAirSupply() + 20));
				useCharge(item, 1);
			}
		}
	}
}
