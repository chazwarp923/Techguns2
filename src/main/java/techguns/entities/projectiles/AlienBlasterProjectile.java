package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.util.TGFXHooks;

public class AlienBlasterProjectile extends GenericProjectile {

	public static final int ENTITY_IGNITE_TIME = 3;

	public AlienBlasterProjectile(EntityType<? extends AlienBlasterProjectile> type, Level level) {
		super(type, level);
	}

	public AlienBlasterProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.ALIEN_BLASTER.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
	}

	@Override
	protected void onClientSpawn() {
		TGFXHooks.createFXOnEntity("AlienBlasterTrail", this);
	}

	@Override
	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		TGFXHooks.createFX("AlienExplosion", this.level(), hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 0, 0, 0);
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		ent.igniteForSeconds(ENTITY_IGNITE_TIME);
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		super.hitBlock(hit);
		if (this.blockdamage) {
			burnBlocks(this.level(), hit, 0.35);
		}
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeFireDamage(this, this.shooter, DeathType.LASER);
		src.armorPenetration = this.penetration;
		src.goreChance = 0.25f;
		return src;
	}

	public static class Factory implements IProjectileFactory<AlienBlasterProjectile> {
		@Override
		public AlienBlasterProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new AlienBlasterProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.FIRE;
		}
	}
}
