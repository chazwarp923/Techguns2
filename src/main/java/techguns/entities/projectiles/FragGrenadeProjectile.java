package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.items.guns.IGrenadeProjectileFactory;

/** Thrown frag grenade. */
public class FragGrenadeProjectile extends GrenadeProjectile {

	public FragGrenadeProjectile(EntityType<? extends FragGrenadeProjectile> type, Level level) {
		super(type, level);
	}

	public FragGrenadeProjectile(FragGrenadeProjectile other, double x, double y, double z) {
		super(TGEntities.FRAG_GRENADE.get(), other, x, y, z);
	}

	public FragGrenadeProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, double gravity, float radius,
			int bounces) {
		super(TGEntities.FRAG_GRENADE.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos,
				gravity, radius, bounces);
		this.bounces = 2; // 1.12 frag grenades always bounced twice
	}

	@Override
	protected GrenadeProjectile createBounceProjectile(Vec3 hitVec) {
		return new FragGrenadeProjectile(this, hitVec.x, hitVec.y, hitVec.z);
	}

	public static class Factory implements IGrenadeProjectileFactory<FragGrenadeProjectile> {
		@Override
		public FragGrenadeProjectile createProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
				float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius, double gravity,
				float charge, int bounces) {
			return new FragGrenadeProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
					firePos, gravity, radius, bounces);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.EXPLOSION;
		}
	}
}
