package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.util.TGFXHooks;

/** Nether blaster projectile. */
public class CyberdemonBlasterProjectile extends GenericProjectile {

	public CyberdemonBlasterProjectile(EntityType<? extends CyberdemonBlasterProjectile> type, Level level) {
		super(type, level);
	}

	public CyberdemonBlasterProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.CYBERDEMON_BLASTER.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
	}

	@Override
	protected void onClientSpawn() {
		TGFXHooks.createFXOnEntity("CyberDemonBlasterTrail", this);
	}

	@Override
	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		TGFXHooks.createFX("CyberdemonBlasterImpact", this.level(), hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 0, 0, 0);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeFireDamage(this, this.shooter, DeathType.DEFAULT);
		src.armorPenetration = this.penetration;
		src.knockbackMultiplier = 1.0f;
		return src;
	}

	public static class Factory implements IProjectileFactory<CyberdemonBlasterProjectile> {
		@Override
		public CyberdemonBlasterProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL,
				float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos,
				float radius, double gravity) {
			return new CyberdemonBlasterProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
					firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.FIRE;
		}
	}
}
