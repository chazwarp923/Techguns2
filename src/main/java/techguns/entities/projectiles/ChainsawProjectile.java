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

/** Chainsaw / mining drill / shishkebap right click attack. */
public class ChainsawProjectile extends MeleeImpactProjectile {

	public ChainsawProjectile(EntityType<? extends ChainsawProjectile> type, Level level) {
		super(type, level);
	}

	public ChainsawProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.CHAINSAW.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = new TGDamageSource(this.level(), TGDamageTypes.CHAINSAW, this, this.shooter, DamageType.PHYSICAL, DeathType.GORE);
		src.goreChance = 1.0f;
		src.armorPenetration = this.penetration;
		return src;
	}

	@Override
	protected float inWaterUpdateBehaviour(float f1) {
		return f1;
	}

	public static class Factory implements IChargedProjectileFactory<ChainsawProjectile> {
		@Override
		public ChainsawProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new ChainsawProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.PHYSICAL;
		}

		@Override
		public ChainsawProjectile createChargedProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity, float charge, int ammoConsumed) {
			return createProjectile(null, level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos,
					radius, gravity);
		}
	}
}
