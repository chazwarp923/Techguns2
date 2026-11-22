package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;

public class StoneBulletProjectile extends GenericProjectile {

	public StoneBulletProjectile(EntityType<? extends StoneBulletProjectile> type, Level level) {
		super(type, level);
	}

	public StoneBulletProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, double gravity) {
		super(TGEntities.STONE_BULLET.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
		this.gravity = gravity;
	}

	public static class Factory implements IProjectileFactory<StoneBulletProjectile> {
		@Override
		public StoneBulletProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new StoneBulletProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos,
					gravity);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.PROJECTILE;
		}
	}
}
