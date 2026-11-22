package techguns.api.guns;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum GunHandType {
	TWO_HANDED(ChatFormatting.GOLD),
	ONE_HANDED(ChatFormatting.GREEN),
	ONE_POINT_FIVE_HANDED(ChatFormatting.YELLOW);

	private final ChatFormatting color;

	GunHandType(ChatFormatting color) {
		this.color = color;
	}

	public Component getDisplayName() {
		return Component.translatable("techguns.gunhandtype." + name().toLowerCase()).withStyle(color);
	}
}
