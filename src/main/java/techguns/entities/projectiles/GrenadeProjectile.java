package techguns.entities.projectiles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.damagesystem.TGExplosion;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;

/** Bouncing grenade base (40mm grenades, hand grenades). Bounces are new entities with reflected motion. */
public abstract class GrenadeProjectile extends GenericProjectile {

	protected int bounces = 3;
	protected boolean bounced = false;

	public GrenadeProjectile(EntityType<? extends GrenadeProjectile> type, Level level) {
		super(type, level);
	}

	public GrenadeProjectile(EntityType<? extends GrenadeProjectile> type, Level level, LivingEntity p, float damage, float speed, int TTL,
			float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos,
			double gravity, float radius, int bounces) {
		super(type, level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		this.bounces = bounces;
		this.gravity = gravity;
		this.radius = radius;
	}

	/** Copy on bounce constructor */
	public GrenadeProjectile(EntityType<? extends GrenadeProjectile> type, GrenadeProjectile other, double x, double y, double z) {
		super(type, other.level(), x, y, z, other.getYRot(), other.getXRot(), other.damage, other.speed, other.ticksToLive, 1.0f,
				other.damageDropStart, other.damageDropEnd, other.damageMin, other.penetration, other.blockdamage, EnumBulletFirePos.CENTER);
		this.shooter = other.shooter;
		this.bounces = other.bounces - 1;
		this.gravity = other.gravity;
		this.radius = other.radius;
		this.setPos(x, y, z);
		this.initStartPos();
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		if (this.bounces > 0 && !bounced) {
			Vec3 m = this.getDeltaMovement();
			float len = this.getBounceSpeedFactor();
			Vec3 refl = switch (hit.getDirection()) {
				case EAST, WEST -> new Vec3(-m.x, m.y, m.z);
				case NORTH, SOUTH -> new Vec3(m.x, m.y, -m.z);
				default -> new Vec3(m.x, -m.y, m.z);
			};
			this.playBounceSound();
			if (!this.level().isClientSide) {
				GrenadeProjectile gren = createBounceProjectile(hit.getLocation());
				Vec3 nm = refl.scale(len);
				gren.setDeltaMovement(nm);
				gren.setPos(gren.getX() + nm.x, gren.getY() + nm.y, gren.getZ() + nm.z);
				this.level().addFreshEntity(gren);
				createProjectileTrail(gren);
			}
			this.bounced = true;
		} else if (!bounced) {
			this.explode();
		}
	}

	protected abstract GrenadeProjectile createBounceProjectile(Vec3 hitvec);

	protected void playBounceSound() {
		if (this.level().isClientSide) {
			this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 1.0f, 1.0f, false);
		}
	}

	protected void createProjectileTrail(GrenadeProjectile gren) {
	}

	protected float getBounceSpeedFactor() {
		return 0.5f;
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		this.explode();
	}

	protected void explode() {
		if (!this.level().isClientSide) {
			TGPackets.sendToAllAround(new PacketSpawnParticle("RocketExplosion", this.getX(), this.getY(), this.getZ()), this, 50.0);
			TGExplosion exp = new TGExplosion(this.level(), shooter, this, getX(), getY(), getZ(), this.damage, this.damageMin, this.damageDropStart,
					this.damageDropEnd, 0);
			exp.doExplosion(true);
		}
		this.discard();
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tags) {
		super.readAdditionalSaveData(tags);
		this.bounces = tags.getByte("bounces");
		this.bounced = tags.getBoolean("bounced");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tags) {
		super.addAdditionalSaveData(tags);
		tags.putByte("bounces", (byte) this.bounces);
		tags.putBoolean("bounced", this.bounced);
	}
}
