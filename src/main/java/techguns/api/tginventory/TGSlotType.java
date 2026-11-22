package techguns.api.tginventory;

import net.minecraft.network.chat.Component;

public enum TGSlotType {
	NORMAL("normal_item"),
	FACESLOT("face"),
	BACKSLOT("back"),
	HANDSLOT("hands"),
	AMMOSLOT("ammo"),
	FOODSLOT("food"),
	HEALSLOT("healing"),
	TURRETARMOR("turret_armor"),
	REACTION_CHAMBER_FOCUS("reaction_focus"),
	DRILL_SMALL("small_drill"),
	DRILL_MEDIUM("medium_drill"),
	DRILL_LARGE("large_drill"),
	ARMOR_UPGRADE("armor_ugpgrade");

	private final String key;

	TGSlotType(String key) {
		this.key = key;
	}

	public Component getDisplayName() {
		return Component.translatable("techguns.tooltip.tgslottype." + key);
	}
}
