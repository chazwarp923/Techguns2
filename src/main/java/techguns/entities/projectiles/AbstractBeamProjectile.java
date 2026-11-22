package techguns.entities.projectiles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Instant hit beam: traces the full range when created (damage applied immediately), then only
 * lives TTL ticks for rendering. distance / laserPitch / laserYaw are synced in the spawn data.
 */
public abstract class AbstractBeamProjectile extends GenericProjectile {

	public double distance = -1d;
	public float laserPitch = 0.0f;
	public float laserYaw = 0.0f;
	public short maxTicks = 0;

	public AbstractBeamProjectile(EntityType<? extends AbstractBeamProjectile> type, Level level) {
		super(type, level);
		this.discardOnHit = false;
	}

	public AbstractBeamProjectile(EntityType<? extends AbstractBeamProjectile> type, Level level, LivingEntity p, float damage, float speed, int TTL,
			float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(type, level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		this.discardOnHit = false;
		this.maxTicks = (short) this.ticksToLive;
	}

	protected void trace() {
		if (!this.posInitialized) {
			this.initStartPos();
		}
		Vec3 start = this.position();
		Vec3 end = start.add(this.getDeltaMovement());
		HitResult hit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if (hit.getType() != HitResult.Type.MISS) {
			end = hit.getLocation();
		} else {
			hit = null;
		}

		EntityHitResult entityHit = this.findEntityOnPath(start, end);
		if (entityHit != null) {
			hit = entityHit;
		}

		if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player target) {
			if (this.shooter instanceof Player sp && !sp.canHarmPlayer(target)) {
				hit = null;
			}
		}

		if (hit != null) {
			this.onHit(hit);
			distance = start.distanceTo(hit.getLocation());
		}

		laserPitch = this.getXRot();
		laserYaw = this.getYRot();
		if (distance <= 0) {
			distance = this.speed;
		}
	}

	@Override
	public void tick() {
		--this.ticksToLive;
		if (this.ticksToLive <= 0) {
			this.discard();
		}
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeDouble(this.distance);
		buffer.writeFloat(this.laserPitch);
		buffer.writeFloat(this.laserYaw);
		buffer.writeShort(this.maxTicks);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		this.distance = buffer.readDouble();
		this.laserPitch = buffer.readFloat();
		this.laserYaw = buffer.readFloat();
		this.maxTicks = buffer.readShort();
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		// projectile rotations use the arrow convention (yaw = atan2(x, z), pitch up = positive)
		Vec3 dir = Vec3.directionFromRotation(-laserPitch, -laserYaw).scale(Math.max(distance, 1.0));
		return new AABB(this.position(), this.position().add(dir)).inflate(1.0);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double dist) {
		return dist < 256 * 256;
	}
}
