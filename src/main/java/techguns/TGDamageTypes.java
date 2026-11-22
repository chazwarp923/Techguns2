package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/** Keys of the data-driven damage types in data/techguns/damage_type. */
public class TGDamageTypes {
	public static final ResourceKey<DamageType> BULLET = key("tg_bullet");
	public static final ResourceKey<DamageType> EXPLOSION = key("tg_explosion");
	public static final ResourceKey<DamageType> POISON = key("tg_poison");
	public static final ResourceKey<DamageType> FIRE = key("tg_fire");
	public static final ResourceKey<DamageType> KNOCKBACK = key("tg_knockback");
	public static final ResourceKey<DamageType> ENERGY = key("tg_energy");
	public static final ResourceKey<DamageType> RADIATION = key("tg_rad");
	public static final ResourceKey<DamageType> RAD_POISONING = key("tg_rad_poisoning");
	public static final ResourceKey<DamageType> LIGHTNING = key("tg_lightning");
	public static final ResourceKey<DamageType> DARK = key("tg_dark");
	public static final ResourceKey<DamageType> SONIC = key("tg_sonic");
	public static final ResourceKey<DamageType> CHAINSAW = key("chainsaw");
	public static final ResourceKey<DamageType> POWERHAMMER = key("powerhammer");

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, Techguns.rl(name));
	}
}
