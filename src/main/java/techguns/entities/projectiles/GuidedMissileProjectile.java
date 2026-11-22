package techguns.entities.projectiles;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGExplosion;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IChargedProjectileFactory;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;
import techguns.util.MathUtil;
import techguns.util.TGFXHooks;

/** Rocket that steers towards its lock-on target (max 9 degrees per tick). */
public class GuidedMissileProjectile extends RocketProjectile {

	public static final double MAX_TURN_ANGLE = 9.0 * Math.PI / 180.0;

	@Nullable
	public Entity target;

	public GuidedMissileProjectile(EntityType<? extends GuidedMissileProjectile> type, Level level) {
		super(type, level);
	}

	protected GuidedMissileProjectile(EntityType<? extends GuidedMissileProjectile> type, Level level, LivingEntity p, float damage, float speed,
			int TTL, float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage,
			EnumBulletFirePos firePos, float radius, double gravity) {
		super(type, level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos, radius, gravity);
	}

	public GuidedMissileProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius, double gravity) {
		this(TGEntities.GUIDED_MISSILE.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos, radius, gravity);
	}

	@Override
	protected void createTrailFX() {
		TGFXHooks.createFXOnEntity("GuidedMissileExhaust", this);
	}

	@Override
	protected void explodeRocket() {
		if (!this.level().isClientSide) {
			TGPackets.sendToAllAround(new PacketSpawnParticle("GuidedMissileExplosion", this.getX(), this.getY(), this.getZ()), this, 50.0);
			TGExplosion explosion = new TGExplosion(this.level(), this.shooter, this, getX(), getY(), getZ(), this.damage, this.damageMin,
					this.damageDropStart, this.damageDropEnd, this.blockdamage ? 0.25 : 0.0);
			explosion.doExplosion(true);
		}
		this.discard();
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeInt(target != null ? target.getId() : -1);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		int id = buffer.readInt();
		this.target = id != -1 ? this.level().getEntity(id) : null;
	}

	@Override
	public void tick() {
		if (this.target != null) {
			if (this.target.isRemoved()) {
				this.target = null;
			} else {
				Vec3 motion = this.getDeltaMovement();
				double speed = motion.length();
				if (speed > 1.0E-6) {
					Vec3 v2 = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5f, target.getZ()).subtract(this.position()).normalize();
					Vec3 v1 = motion.normalize();
					double angle = Math.acos(Math.max(-1.0, Math.min(1.0, v1.dot(v2))));
					if (angle < MAX_TURN_ANGLE) {
						motion = v2.scale(speed);
					} else {
						Vec3 axis = v1.cross(v2).normalize();
						motion = MathUtil.rotateVector(v1, axis, MAX_TURN_ANGLE).scale(speed);
					}
					this.setDeltaMovement(motion);
				}
			}
		}
		super.tick();
	}

	public static class Factory implements IChargedProjectileFactory<GuidedMissileProjectile> {
		@Override
		public GuidedMissileProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			// target is set by GuidedMissileLauncher#spawnProjectile; unguided missiles get a little gravity
			return new GuidedMissileProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos,
					radius, 0.01f);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.EXPLOSION;
		}

		@Override
		public GuidedMissileProjectile createChargedProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity, float charge, int ammoConsumed) {
			return null;
		}
	}
}
