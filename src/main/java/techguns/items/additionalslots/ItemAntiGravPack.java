package techguns.items.additionalslots;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import techguns.TGItems;
import techguns.api.tginventory.TGSlotType;
import techguns.items.armors.TGArmorBonus;
import techguns.util.TextUtil;

/** Back slot: creative flight with a fly speed bonus, uses charge while flying. Reloads with nuclear power cells. */
public class ItemAntiGravPack extends ItemTGSpecialSlot {

	private static final float FLYBONUS = 0.25f;

	public ItemAntiGravPack(int camoCount, int dur) {
		super(TGSlotType.BACKSLOT, camoCount, dur, TGItems.NUCLEAR_POWERCELL, TGItems.NUCLEAR_POWERCELL_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack item, Player ply) {
		if (item.getDamageValue() + 1 >= item.getMaxDamage()) {
			this.tryReloadAndRepair(item, ply);
		}
		if (ply.getAbilities().flying && !ply.isCreative() && !ply.isSpectator() && hasCharge(item)) {
			useCharge(item, 1);
		}
	}

	@Override
	public float getBonus(TGArmorBonus type, ItemStack stack, boolean consume, Player player) {
		if (type == TGArmorBonus.CREATIVE_FLIGHT && hasCharge(stack)) {
			return 1;
		} else if (type == TGArmorBonus.FLYSPEED && hasCharge(stack)) {
			return FLYBONUS;
		}
		return 0;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(TextUtil.transTG("armorTooltip.creativeFlight"));
		tooltip.add(TextUtil.transTG("armorTooltip.flyspeed").append(": +" + FLYBONUS * 100.0f + "%"));
	}
}
