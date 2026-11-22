package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGExplosion;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;
import techguns.util.TGFXHooks;

public class RocketProjectile extends GenericProjectile {

	public RocketProjectile(EntityType<? extends RocketProjectile> type, Level level) {
		super(type, level);
	}

	protected RocketProjectile(EntityType<? extends RocketProjectile> type, Level level, LivingEntity p, float damage, float speed, int TTL,
			float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos,
			float radius, double gravity) {
		super(type, level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		this.radius = radius;
		this.gravity = gravity;
	}

	public RocketProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius, double gravity) {
		this(TGEntities.ROCKET.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos, radius,
				gravity);
	}

	@Override
	protected void onClientSpawn() {
		this.createTrailFX();
	}

	protected void createTrailFX() {
		TGFXHooks.createFXOnEntity("RocketLauncherExhaust", this);
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		this.explodeRocket();
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		this.explodeRocket();
	}

	protected void explodeRocket() {
		if (!this.level().isClientSide) {
			TGPackets.sendToAllAround(new PacketSpawnParticle("RocketExplosion", this.getX(), this.getY(), this.getZ()), this, 50.0);
			TGExplosion explosion = new TGExplosion(this.level(), this.shooter, this, getX(), getY(), getZ(), this.damage, this.damageMin,
					this.damageDropStart, this.damageDropEnd, this.blockdamage ? 0.5 : 0.0);
			explosion.doExplosion(true);
		}
		this.discard();
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource dmgsrc = TGDamageSource.causeExplosionDamage(this, this.shooter, DeathType.GORE);
		dmgsrc.goreChance = 1.0f;
		dmgsrc.knockbackMultiplier = 3.0f;
		return dmgsrc;
	}

	public static class Factory implements IProjectileFactory<RocketProjectile> {
		@Override
		public RocketProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new RocketProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos, radius,
					gravity);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.EXPLOSION;
		}
	}
}
