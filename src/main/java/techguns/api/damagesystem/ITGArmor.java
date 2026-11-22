package techguns.api.damagesystem;

import net.minecraft.world.item.ItemStack;

/** Armor items with per-damage-type armor values (Techguns GenericArmor). */
public interface ITGArmor {
	float getArmorValue(ItemStack stack, DamageType type);
}
