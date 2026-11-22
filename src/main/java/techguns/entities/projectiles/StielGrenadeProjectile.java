package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.items.guns.IGrenadeProjectileFactory;

/** Thrown stick grenade (1.12: the concrete GrenadeProjectile class). */
public class StielGrenadeProjectile extends GrenadeProjectile {

	public StielGrenadeProjectile(EntityType<? extends StielGrenadeProjectile> type, Level level) {
		super(type, level);
	}

	public StielGrenadeProjectile(StielGrenadeProjectile other, double x, double y, double z) {
		super(TGEntities.GRENADE.get(), other, x, y, z);
	}

	public StielGrenadeProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, double gravity, float radius,
			int bounces) {
		super(TGEntities.GRENADE.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos,
				gravity, radius, bounces);
	}

	@Override
	protected GrenadeProjectile createBounceProjectile(Vec3 hitVec) {
		return new StielGrenadeProjectile(this, hitVec.x, hitVec.y, hitVec.z);
	}

	public static class Factory implements IGrenadeProjectileFactory<StielGrenadeProjectile> {
		@Override
		public StielGrenadeProjectile createProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
				float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius, double gravity,
				float charge, int bounces) {
			return new StielGrenadeProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
					firePos, gravity, radius, bounces);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.EXPLOSION;
		}
	}
}
