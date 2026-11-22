package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.util.TGFXHooks;

/** Blaster rifle / scatter beam rifle bolt. */
public class BlasterProjectile extends GenericProjectile {

	public BlasterProjectile(EntityType<? extends BlasterProjectile> type, Level level) {
		super(type, level);
	}

	public BlasterProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.BLASTER.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeEnergyDamage(this, this.shooter, DeathType.LASER);
		src.armorPenetration = this.penetration;
		src.setNoKnockback();
		return src;
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		TGFXHooks.createFX("LaserGunImpact", this.level(), hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 0, 0, 0);
	}

	@Override
	protected float inWaterUpdateBehaviour(float f1) {
		if (this.isInWaterRainOrBubble()) {
			this.clearFire();
		}
		return f1;
	}

	public static class Factory implements IProjectileFactory<BlasterProjectile> {
		@Override
		public BlasterProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new BlasterProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.ENERGY;
		}
	}
}
