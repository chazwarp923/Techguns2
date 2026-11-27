package techguns.entities.projectiles;

import java.util.HashMap;
import java.util.Map;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.api.damagesystem.DamageType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GrapplingHook;
import techguns.items.guns.IChargedProjectileFactory;
import techguns.util.SoundUtil;
import techguns.client.audio.TGSoundCategory;

/**
 * Grappling hook (Fabric port GrapplingHookProjectile). Flies while the shooter holds use with the grappling hook, and
 * goes away when they let go. A block hit pulls the shooter to the spot; an entity hit pulls the shooter to the entity,
 * or with sneak, the entity to the shooter. Pulling ends at 1 block, after 5 seconds, or when the target goes. With
 * sneak, a block hit ends it.
 * <p>
 * The server decides; the state, the target entity and the block anchor are synced, so the client draws the hook and
 * its wire where the server has it.
 */
public class GrapplingHookProjectile extends GenericProjectile {
	public enum Status {
		LAUNCHING, GRAPPLING_BLOCK, GRAPPLING_ENTITY, PULL_ENTITY
	}

	private static final EntityDataAccessor<Byte> DATA_STATUS = SynchedEntityData.defineId(GrapplingHookProjectile.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(GrapplingHookProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Vector3f> DATA_ANCHOR = SynchedEntityData.defineId(GrapplingHookProjectile.class,
			EntityDataSerializers.VECTOR3);

	/** pulling speed in blocks per tick, slowed down within PULL_MAX_SPEED_DISTANCE of the target (Fabric values) */
	public static final float PULL_SPEED = 2.0f;
	public static final float PULL_MAX_SPEED_DISTANCE = 5.0f;
	/** pulling stops this close to the target */
	public static final float PULL_TARGET_DISTANCE = 1.0f;
	public static final int MAX_PULL_TICKS = 100;

	/** client: the hook each shooter has out, by shooter id, for the gun model and the wire */
	private static final Map<Integer, GrapplingHookProjectile> CLIENT_HOOKS = new HashMap<>();

	private BlockPos targetBlock;
	private int pullTicks = 0;
	/** client: the flying hook hit something; wait for the server's state */
	private boolean stuck = false;
	public EnumBulletFirePos firePos = EnumBulletFirePos.CENTER;

	public GrapplingHookProjectile(EntityType<? extends GrapplingHookProjectile> type, Level level) {
		super(type, level);
		this.discardOnHit = false;
	}

	public GrapplingHookProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.GRAPPLING_HOOK.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
		this.discardOnHit = false;
		this.firePos = firePos;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STATUS, (byte) Status.LAUNCHING.ordinal());
		builder.define(DATA_TARGET, -1);
		builder.define(DATA_ANCHOR, new Vector3f());
	}

	public Status getStatus() {
		return Status.values()[Math.floorMod(this.entityData.get(DATA_STATUS), Status.values().length)];
	}

	private void setStatus(Status s) {
		this.entityData.set(DATA_STATUS, (byte) s.ordinal());
	}

	public boolean isLaunching() {
		return getStatus() == Status.LAUNCHING;
	}

	/** the hooked entity, or null */
	public Entity getTarget() {
		int id = this.entityData.get(DATA_TARGET);
		return id >= 0 ? this.level().getEntity(id) : null;
	}

	/** the shooter keeps the hook out by holding use with a grappling hook */
	protected boolean isShooterGrappling() {
		return this.shooter instanceof Player p && p.isAlive() && p.isUsingItem() && p.getUseItem().getItem() instanceof GrapplingHook;
	}

	@Override
	public void tick() {
		if (this.level().isClientSide) {
			clientTick();
			return;
		}
		if (!isShooterGrappling()) {
			this.discard();
			return;
		}
		Status status = getStatus();
		if (status == Status.LAUNCHING) {
			super.tick();
			return;
		}
		if (++this.pullTicks > MAX_PULL_TICKS) {
			this.discard();
			return;
		}
		Entity target = getTarget();
		switch (status) {
			case GRAPPLING_BLOCK -> {
				if (this.targetBlock == null || this.level().getBlockState(this.targetBlock).isAir()) {
					this.discard();
				} else {
					pull(this.shooter, anchor());
				}
			}
			case GRAPPLING_ENTITY -> {
				if (target == null || !target.isAlive()) {
					this.discard();
				} else {
					Vec3 dst = target.getEyePosition();
					this.setPos(dst);
					pull(this.shooter, dst);
				}
			}
			case PULL_ENTITY -> {
				if (target == null || !target.isAlive()) {
					this.discard();
				} else {
					pull(target, this.shooter.position());
					this.setPos(target.getX(), target.getY() + target.getEyeHeight() * 0.5, target.getZ());
				}
			}
			default -> {
			}
		}
	}

	private void clientTick() {
		Status status = getStatus();
		if (status == Status.LAUNCHING) {
			if (!this.stuck) {
				super.tick();
			}
		} else if (status == Status.GRAPPLING_BLOCK) {
			this.setPos(anchor());
		} else {
			Entity target = getTarget();
			if (target != null) {
				this.setPos(status == Status.GRAPPLING_ENTITY ? target.getEyePosition()
						: new Vec3(target.getX(), target.getY() + target.getEyeHeight() * 0.5, target.getZ()));
			}
		}
	}

	private Vec3 anchor() {
		Vector3f a = this.entityData.get(DATA_ANCHOR);
		return new Vec3(a.x(), a.y(), a.z());
	}

	/** moves the entity towards dst (Fabric pullEntity); at the target distance it stops it and the hook goes */
	protected void pull(Entity entity, Vec3 dst) {
		Vec3 diff = dst.subtract(entity.position());
		double dist = diff.length();
		entity.resetFallDistance();
		if (dist <= PULL_TARGET_DISTANCE) {
			entity.setDeltaMovement(Vec3.ZERO);
			entity.hurtMarked = true;
			this.discard();
			return;
		}
		float speed = dist >= PULL_MAX_SPEED_DISTANCE ? PULL_SPEED : (float) (PULL_SPEED * dist / PULL_MAX_SPEED_DISTANCE);
		entity.setDeltaMovement(diff.normalize().scale(speed));
		// players move themselves: hurtMarked sends them the new motion
		entity.hurtMarked = true;
	}

	@Override
	protected void onHit(HitResult hit) {
		if (this.level().isClientSide) {
			this.stuck = true;
			return;
		}
		if (getStatus() != Status.LAUNCHING || this.shooter == null) {
			return;
		}
		// damage and impact effects as a bullet; discardOnHit is off
		super.onHit(hit);
		if (hit instanceof EntityHitResult ehr) {
			Entity target = ehr.getEntity();
			if (!target.isAlive()) {
				this.discard();
				return;
			}
			this.entityData.set(DATA_TARGET, target.getId());
			this.setDeltaMovement(Vec3.ZERO);
			setStatus(this.shooter.isShiftKeyDown() ? Status.PULL_ENTITY : Status.GRAPPLING_ENTITY);
			playPullSound();
		} else if (hit instanceof BlockHitResult bhr) {
			if (this.shooter.isShiftKeyDown() || this.level().getBlockState(bhr.getBlockPos()).isAir()) {
				this.discard();
				return;
			}
			this.targetBlock = bhr.getBlockPos();
			Vec3 a = bhr.getLocation();
			if (bhr.getDirection() == Direction.UP) {
				a = a.add(0, 0.15, 0);
			}
			this.entityData.set(DATA_ANCHOR, new Vector3f((float) a.x, (float) a.y, (float) a.z));
			this.setPos(a);
			// stop here: GenericProjectile.tick moves on by the motion after onHit, which put the hook a step behind
			// the wall, sometimes in a chunk where entities don't tick, so it never pulled
			this.setDeltaMovement(Vec3.ZERO);
			setStatus(Status.GRAPPLING_BLOCK);
			playPullSound();
		}
	}

	private void playPullSound() {
		SoundUtil.playSoundOnEntityGunPosition(this.level(), this.shooter, TGSounds.GRAPPLING_HOOK_PULL.get(), GenericGun.SOUND_DISTANCE, 1.0F, false,
				false, TGSoundCategory.GUN_FIRE);
	}

	/* ---- client hook lookup ---- */

	@Override
	protected void onClientSpawn() {
		super.onClientSpawn();
		if (this.shooter != null) {
			CLIENT_HOOKS.put(this.shooter.getId(), this);
		}
	}

	@Override
	public void onRemovedFromLevel() {
		super.onRemovedFromLevel();
		if (this.level().isClientSide && this.shooter != null) {
			CLIENT_HOOKS.remove(this.shooter.getId(), this);
		}
	}

	/** client: the hook the entity has out, or null */
	public static GrapplingHookProjectile clientHookOf(Entity shooter) {
		GrapplingHookProjectile h = CLIENT_HOOKS.get(shooter.getId());
		if (h != null && (h.isRemoved() || h.level() != shooter.level())) {
			CLIENT_HOOKS.remove(shooter.getId());
			return null;
		}
		return h;
	}

	public LivingEntity getShooterEntity() {
		return this.shooter;
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeByte(this.firePos.ordinal());
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		int p = buffer.readByte();
		this.firePos = p >= 0 && p < EnumBulletFirePos.values().length ? EnumBulletFirePos.values()[p] : EnumBulletFirePos.CENTER;
	}

	public static class Factory implements IChargedProjectileFactory<GrapplingHookProjectile> {
		@Override
		public GrapplingHookProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			GrapplingHookProjectile proj = new GrapplingHookProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin,
					penetration, blockdamage, firePos);
			proj.setRadiusAndGravity(radius, gravity);
			return proj;
		}

		/** letting go fires nothing */
		@Override
		public GrapplingHookProjectile createChargedProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity, float charge, int ammoConsumed) {
			return null;
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.PROJECTILE;
		}
	}
}
