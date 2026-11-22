package techguns.entities.projectiles;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.util.MathUtil;

/** Tesla gun lightning beam, chains to up to CHAIN_TARGETS further targets. */
public class TeslaProjectile extends AbstractBeamProjectile {

	public static final int TTL = 10;
	public static final float CHAIN_RANGE = 8.0f;
	public static final int CHAIN_TARGETS = 4;
	public static final float CHAIN_DAMAGE_FACTOR = 0.75f;

	/** client render seed for the lightning shape */
	public long seed = 0;

	protected int chainTargets = CHAIN_TARGETS;
	protected Entity prevTarget = null;

	public TeslaProjectile(EntityType<? extends TeslaProjectile> type, Level level) {
		super(type, level);
		this.seed = level.random.nextLong();
	}

	public TeslaProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, int chainTargets) {
		super(TGEntities.TESLA.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		this.chainTargets = chainTargets;
		trace();
	}

	/** chain beam from source to target */
	public TeslaProjectile(Level level, LivingEntity shooter, Entity source, LivingEntity target, int chainTargets, float damage, float speed,
			int TTL, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage) {
		super(TGEntities.TESLA.get(), level, shooter, damage, speed, TTL, 0, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				EnumBulletFirePos.CENTER);
		this.maxTicks = (short) this.ticksToLive;
		this.chainTargets = chainTargets;
		this.prevTarget = target;

		Vec3 src = new Vec3(source.getX(), source.getY() + source.getEyeHeight() * 0.5f, source.getZ());
		this.setPos(src);
		this.initStartPos();
		Vec3 tgt = new Vec3(target.getX(), target.getY() + target.getEyeHeight() * 0.5f, target.getZ());
		Vec3 dir = tgt.subtract(src).normalize();
		this.distance = src.distanceTo(tgt);
		this.laserPitch = (float) (Math.atan2(dir.y, dir.horizontalDistance()) * MathUtil.R2D);
		this.laserYaw = (float) (Math.atan2(dir.x, dir.z) * MathUtil.R2D);
		this.setXRot(laserPitch);
		this.setYRot(laserYaw);
		this.setDeltaMovement(dir.scale(this.speed));
		if (distance <= 0) {
			distance = this.speed;
		}
		this.onHit(new EntityHitResult(target));
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!this.level().isClientSide && hit instanceof EntityHitResult ehr) {
			Entity lastTarget = ehr.getEntity();
			if (this.chainTargets > 0) {
				LivingEntity nextTarget = findNextTarget(lastTarget);
				if (nextTarget != null) {
					TeslaProjectile proj = new TeslaProjectile(this.level(), this.shooter, lastTarget, nextTarget, chainTargets - 1,
							this.damage * CHAIN_DAMAGE_FACTOR, this.speed, TTL, this.damageDropStart, this.damageDropEnd, this.damageMin, this.penetration,
							this.blockdamage);
					this.level().addFreshEntity(proj);
				}
			}
		}
	}

	private LivingEntity findNextTarget(Entity lastTarget) {
		Vec3 from = new Vec3(lastTarget.getX(), lastTarget.getY() + lastTarget.getEyeHeight() * 0.5f, lastTarget.getZ());
		List<Entity> list = this.level().getEntities(lastTarget, new AABB(from, from).inflate(CHAIN_RANGE), BULLET_TARGETS);
		for (Entity e : list) {
			if (e instanceof LivingEntity entity) {
				double dist = entity.position().distanceTo(from);
				if (dist < CHAIN_RANGE && entity.isAlive() && entity != lastTarget && !entity.equals(shooter) && !entity.equals(prevTarget)) {
					Vec3 to = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight() * 0.5f, entity.getZ());
					HitResult rtr = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
					if (rtr.getType() == HitResult.Type.MISS) {
						return entity;
					}
				}
			}
		}
		return null;
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeLightningDamage(this, this.shooter, DeathType.LASER);
		src.armorPenetration = this.penetration;
		src.setNoKnockback();
		src.goreChance = 0.5f;
		return src;
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		this.seed = this.random.nextLong();
	}

	public static class Factory implements IProjectileFactory<TeslaProjectile> {
		@Override
		public TeslaProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new TeslaProjectile(level, p, damage, speed, TeslaProjectile.TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
					firePos, CHAIN_TARGETS);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.LIGHTNING;
		}
	}
}
