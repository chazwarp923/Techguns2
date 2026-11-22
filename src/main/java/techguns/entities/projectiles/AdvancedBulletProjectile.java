package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;

/** PDW / pulse rifle bullets, blue impact FX. */
public class AdvancedBulletProjectile extends GenericProjectile {

	public AdvancedBulletProjectile(EntityType<? extends AdvancedBulletProjectile> type, Level level) {
		super(type, level);
	}

	protected AdvancedBulletProjectile(EntityType<? extends AdvancedBulletProjectile> type, Level level, LivingEntity p, float damage, float speed,
			int TTL, float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage,
			EnumBulletFirePos firePos) {
		super(type, level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
	}

	public AdvancedBulletProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.ADVANCED_BULLET.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
	}

	@Override
	protected boolean hasBlueImpactFX() {
		return true;
	}

	public static class Factory implements IProjectileFactory<AdvancedBulletProjectile> {
		@Override
		public AdvancedBulletProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL,
				float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos,
				float radius, double gravity) {
			return new AdvancedBulletProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
					firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.PROJECTILE;
		}
	}
}
