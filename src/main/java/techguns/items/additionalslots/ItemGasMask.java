package techguns.items.additionalslots;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import techguns.api.tginventory.TGSlotType;
import techguns.items.armors.TGArmorBonus;

/** Face slot: removes poison and wither (one durability each). */
public class ItemGasMask extends ItemTGSpecialSlot {

	protected float radresist = 0f;

	public ItemGasMask(int camoCount, int dur) {
		super(TGSlotType.FACESLOT, camoCount, dur);
	}

	public ItemGasMask setRadresist(float radresist) {
		this.radresist = radresist;
		return this;
	}

	static void cleanse(ItemStack item, Player ply) {
		if (ply.level().isClientSide) {
			return;
		}
		if (ply.hasEffect(MobEffects.POISON) && hasCharge(item)) {
			ply.removeEffect(MobEffects.POISON);
			useCharge(item, 1);
		}
		if (ply.hasEffect(MobEffects.WITHER) && hasCharge(item)) {
			ply.removeEffect(MobEffects.WITHER);
			useCharge(item, 1);
		}
	}

	@Override
	public void onPlayerTick(ItemStack item, Player ply) {
		cleanse(item, ply);
	}

	public float getRadresist() {
		return radresist;
	}

	@Override
	public float getBonus(TGArmorBonus type, ItemStack stack, boolean consume, Player player) {
		return type == TGArmorBonus.RAD_RESISTANCE ? radresist : 0f;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		if (this.radresist > 0) {
			tooltip.add(Component.literal("RAD Resistance: " + this.radresist).withStyle(ChatFormatting.BLUE));
		}
	}
}
