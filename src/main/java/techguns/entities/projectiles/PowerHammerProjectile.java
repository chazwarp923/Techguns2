package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.TGDamageTypes;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IChargedProjectileFactory;

/** Power hammer charged blow; damage scales with the ammo consumed by the charge. */
public class PowerHammerProjectile extends MeleeImpactProjectile {

	public PowerHammerProjectile(EntityType<? extends PowerHammerProjectile> type, Level level) {
		super(type, level);
	}

	public PowerHammerProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.POWERHAMMER.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = new TGDamageSource(this.level(), TGDamageTypes.POWERHAMMER, this, this.shooter, DamageType.PHYSICAL, DeathType.GORE);
		src.goreChance = 1.0f;
		src.armorPenetration = this.penetration;
		return src;
	}

	public static class Factory implements IChargedProjectileFactory<PowerHammerProjectile> {
		@Override
		public PowerHammerProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new PowerHammerProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.PHYSICAL;
		}

		@Override
		public PowerHammerProjectile createChargedProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity, float charge, int ammoConsumed) {
			return new PowerHammerProjectile(level, p, damage * ammoConsumed, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin * ammoConsumed,
					penetration, blockdamage, firePos);
		}
	}
}
