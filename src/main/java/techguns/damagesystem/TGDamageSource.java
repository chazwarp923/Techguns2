package techguns.damagesystem;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import techguns.TGDamageTypes;
import techguns.api.damagesystem.DamageType;
import techguns.deatheffects.DeathType;

/**
 * Techguns damage source. In 1.21 the vanilla damage "type" (death message, tags) is data driven
 * (see data/techguns/damage_type); this subclass keeps the extra Techguns properties.
 * <p>
 * The 1.12.2 "attackSuccessful" workaround is gone: use the boolean returned by Entity#hurt.
 */
public class TGDamageSource extends DamageSource {

	public DamageType damageType;
	public DeathType deathType;
	public float goreChance = 0.5f;

	public boolean knockbackOnShieldBlock = true;

	/** Anti-Toughness */
	public float armorPenetration = 0.0f;
	public boolean ignoreHurtresistTime = false;
	public float knockbackMultiplier = 1.0f;

	public TGDamageSource(Holder<net.minecraft.world.damagesource.DamageType> type, @Nullable Entity direct, @Nullable Entity attacker,
			DamageType damageType, DeathType deathType) {
		super(type, direct, attacker);
		this.damageType = damageType;
		this.deathType = deathType;
	}

	public TGDamageSource(Level level, ResourceKey<net.minecraft.world.damagesource.DamageType> key, @Nullable Entity direct,
			@Nullable Entity attacker, DamageType damageType, DeathType deathType) {
		this(holder(level, key), direct, attacker, damageType, deathType);
	}

	public static Holder<net.minecraft.world.damagesource.DamageType> holder(Level level,
			ResourceKey<net.minecraft.world.damagesource.DamageType> key) {
		return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
	}

	private static Level levelOf(Entity a, Entity b) {
		return a != null ? a.level() : b.level();
	}

	public static TGDamageSource causeBulletDamage(Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.BULLET, projectile, shooter, DamageType.PROJECTILE, deathType);
		src.knockbackOnShieldBlock = false;
		src.ignoreHurtresistTime = true;
		return src;
	}

	public static TGDamageSource causeExplosionDamage(Entity projectile, Entity shooter, DeathType deathType) {
		return new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.EXPLOSION, projectile, shooter, DamageType.EXPLOSION, deathType);
	}

	public static TGDamageSource causePoisonDamage(Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.POISON, projectile, shooter, DamageType.POISON, deathType);
		src.ignoreHurtresistTime = true;
		src.knockbackOnShieldBlock = false;
		return src;
	}

	public static TGDamageSource causeFireDamage(Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.FIRE, projectile, shooter, DamageType.FIRE, deathType);
		src.ignoreHurtresistTime = true;
		src.knockbackOnShieldBlock = false;
		return src;
	}

	public static TGDamageSource getKnockbackDummyDmgSrc(Entity projectile, Entity shooter) {
		return new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.KNOCKBACK, projectile, shooter, DamageType.PHYSICAL, DeathType.DEFAULT);
	}

	public static TGDamageSource causeEnergyDamage(Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.ENERGY, projectile, shooter, DamageType.ENERGY, deathType);
		src.ignoreHurtresistTime = true;
		src.knockbackOnShieldBlock = false;
		return src;
	}

	public static TGDamageSource causeRadiationDamage(Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.RADIATION, projectile, shooter, DamageType.RADIATION, deathType);
		src.ignoreHurtresistTime = true;
		src.knockbackOnShieldBlock = false;
		return src;
	}

	/** radiation damage without a source entity */
	public static TGDamageSource causeRadiationDamage(Level level, Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(level, TGDamageTypes.RADIATION, projectile, shooter, DamageType.RADIATION, deathType);
		src.ignoreHurtresistTime = true;
		src.knockbackOnShieldBlock = false;
		return src;
	}

	public static TGDamageSource causeLethalRadPoisoningDamage(Level level, Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(level, TGDamageTypes.RAD_POISONING, projectile, shooter, DamageType.UNRESISTABLE, deathType);
		src.ignoreHurtresistTime = true;
		src.knockbackOnShieldBlock = false;
		src.goreChance = 1.0f;
		return src;
	}

	public static TGDamageSource causeLightningDamage(Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.LIGHTNING, projectile, shooter, DamageType.LIGHTNING, deathType);
		src.ignoreHurtresistTime = true;
		src.knockbackOnShieldBlock = false;
		return src;
	}

	public static TGDamageSource causeDarkDamage(Entity projectile, Entity shooter, DeathType deathType) {
		TGDamageSource src = new TGDamageSource(levelOf(projectile, shooter), TGDamageTypes.DARK, projectile, shooter, DamageType.DARK, deathType);
		src.ignoreHurtresistTime = true;
		src.knockbackOnShieldBlock = false;
		return src;
	}

	/** Melee damage from a player wielding a Techguns melee weapon (vanilla "player" damage type). */
	public static TGDamageSource causePlayerMeleeDamage(Entity player, DamageType type, DeathType deathType) {
		return new TGDamageSource(player.level(), net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK, player, player, type, deathType);
	}

	public TGDamageSource setNoKnockback() {
		this.knockbackMultiplier = 0.0f;
		return this;
	}

	public TGDamageSource setKnockback(float mult) {
		this.knockbackMultiplier = mult;
		return this;
	}

	public boolean hasKnockback() {
		return this.knockbackMultiplier > 0.0f;
	}

	public boolean knockbackOnShieldBlock() {
		return knockbackOnShieldBlock;
	}

	public static TGDamageSource copyWithNewEnt(TGDamageSource other, Entity damagingEntity, Entity attacker) {
		TGDamageSource src = new TGDamageSource(other.typeHolder(), damagingEntity, attacker, other.damageType, other.deathType);
		src.goreChance = other.goreChance;
		src.knockbackOnShieldBlock = other.knockbackOnShieldBlock;
		src.armorPenetration = other.armorPenetration;
		src.ignoreHurtresistTime = other.ignoreHurtresistTime;
		src.knockbackMultiplier = other.knockbackMultiplier;
		return src;
	}
}
