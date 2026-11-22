package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IChargedProjectileFactory;
import techguns.items.guns.ammo.DamageModifier;

public class GuidedMissileProjectileHV extends GuidedMissileProjectile {

	public GuidedMissileProjectileHV(EntityType<? extends GuidedMissileProjectileHV> type, Level level) {
		super(type, level);
	}

	public GuidedMissileProjectileHV(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius, double gravity) {
		super(TGEntities.GUIDED_MISSILE_HV.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos, radius, gravity);
	}

	public static class Factory implements IChargedProjectileFactory<GuidedMissileProjectileHV> {
		protected DamageModifier mod = new DamageModifier().setRadius(0.75f, 0f).setRange(0.75f, 0f).setVelocity(2.0f, 0f);

		@Override
		public DamageModifier getDamageModifier() {
			return mod;
		}

		@Override
		public GuidedMissileProjectileHV createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL,
				float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos,
				float radius, double gravity) {
			return new GuidedMissileProjectileHV(level, p, mod.getDamage(damage), mod.getVelocity(speed), mod.getTTL(TTL), spread,
					mod.getRange(dmgDropStart), mod.getRange(dmgDropEnd), mod.getDamage(dmgMin), penetration, blockdamage, firePos, mod.getRadius(radius),
					0.01f);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.EXPLOSION;
		}

		@Override
		public GuidedMissileProjectileHV createChargedProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity, float charge, int ammoConsumed) {
			return null;
		}
	}
}
