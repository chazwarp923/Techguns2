package techguns.api.damagesystem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Techguns damage categories (independent of vanilla's data-driven damage types). */
public enum DamageType {
	PHYSICAL(ChatFormatting.DARK_GRAY), // Melee hits
	PROJECTILE(ChatFormatting.GRAY), // Arrows, bullets,..
	FIRE(ChatFormatting.RED), // fire, flamethrower
	EXPLOSION(ChatFormatting.DARK_RED), // Explosions like TNT, Rockets
	ENERGY(ChatFormatting.DARK_AQUA), // Energy Weapons, Lasers, Magic
	POISON(ChatFormatting.DARK_GREEN), // gas, slimy stuff
	UNRESISTABLE(ChatFormatting.BLACK), // never reduced by armor: starve, falldmg, suffocate...
	ICE(ChatFormatting.AQUA), // anything cold related
	LIGHTNING(ChatFormatting.YELLOW), // electricity, shock, lightning magic
	RADIATION(ChatFormatting.GREEN),
	DARK(ChatFormatting.BLACK); // dark energy, black holes, dark magic

	private final ChatFormatting color;

	DamageType(ChatFormatting color) {
		this.color = color;
	}

	public Component getDisplayName() {
		return Component.translatable("techguns.TGDamageType." + name()).withStyle(color);
	}
}
