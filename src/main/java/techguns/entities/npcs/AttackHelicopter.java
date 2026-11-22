package techguns.entities.npcs;

import java.util.EnumSet;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import techguns.TGSounds;
import techguns.api.npc.INPCTechgunsShooter;
import techguns.api.npc.INpcTGDamageSystem;
import techguns.damagesystem.TGDamageSource;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.RocketProjectile;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;

/**
 * 1.12 AttackHelicopter: flies 24 blocks above ground, fires machine gun bursts and rockets at the nearest player,
 * crashes spinning when killed.
 */
public class AttackHelicopter extends FlyingMob implements Enemy, INpcTGDamageSystem, INPCTechgunsShooter {

	private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(AttackHelicopter.class, EntityDataSerializers.BOOLEAN);
	protected static final int TARGET_FLY_HEIGHT = 24;
	public static final int MAX_DEATH_TIME = 100;

	private int ambientSoundTick = 0;

	public AttackHelicopter(EntityType<? extends AttackHelicopter> type, Level level) {
		super(type, level);
		this.xpReward = 5;
		this.moveControl = new FlyingMoveControl(this);
		this.lookControl = new LookControl(this) {
			@Override
			protected boolean resetXRotOnTick() {
				return false;
			}
		};
	}

	public static AttributeSupplier.Builder createAttributes() {
		return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100D).add(Attributes.FOLLOW_RANGE, 128D).add(Attributes.ARMOR, 16D)
				.add(Attributes.ARMOR_TOUGHNESS, 5D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(5, new RandomFlyGoal(this));
		this.goalSelector.addGoal(7, new LookAroundGoal(this));
		this.goalSelector.addGoal(7, new HelicopterAttackGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ATTACKING, false);
	}

	public boolean isAttacking() {
		return this.entityData.get(ATTACKING);
	}

	public void setAttacking(boolean attacking) {
		this.entityData.set(ATTACKING, attacking);
	}

	@Override
	public SoundSource getSoundSource() {
		return SoundSource.HOSTILE;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource src) {
		return TGSounds.HELICOPTER_HIT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TGSounds.HELICOPTER_HIT.get();
	}

	@Override
	protected float getSoundVolume() {
		return 10.0F;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide && this.level().getDifficulty() == Difficulty.PEACEFUL) {
			this.discard();
			return;
		}
		if (--ambientSoundTick <= 0 && this.deathTime == 0) {
			this.playSound(TGSounds.HELICOPTER_ROTOR.get(), 6.0f, 1.0f);
			ambientSoundTick = 61;
		}
	}

	/** 5 second crash, then explodes */
	@Override
	protected void tickDeath() {
		if (this.deathTime == 0) {
			this.playSound(TGSounds.HELICOPTER_DEATH.get(), 6.0f, 1.0f);
		}
		++this.deathTime;
		if (this.deathTime >= MAX_DEATH_TIME && !this.level().isClientSide() && !this.isRemoved()) {
			this.playSound(TGSounds.HELICOPTER_EXPLODE.get(), 6.0f, 1.0f);
			TGPackets.sendToAllAround(new PacketSpawnParticle("HelicopterDeathExplosion", getX(), getY() - 8D, getZ()), this, 150.0);
			this.level().broadcastEntityEvent(this, (byte) 60);
			this.remove(Entity.RemovalReason.KILLED);
		}
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		return switch (dmgsrc.damageType) {
			case LIGHTNING, ENERGY -> 10f;
			case FIRE, ICE, PHYSICAL, PROJECTILE, POISON, RADIATION -> 20f;
			default -> 0f;
		};
	}

	@Override
	public float getWeaponPosX() {
		return 0;
	}

	@Override
	public float getWeaponPosY() {
		return 0;
	}

	@Override
	public float getWeaponPosZ() {
		return 0;
	}

	@Override
	public float getBulletOffsetSide() {
		return 2.0f;
	}

	/* ------------------------------------------------------------------ AI (1.12 GenericFlyingMob) */

	/** faces its flight direction, or the target with the gun pitched at it */
	static class LookAroundGoal extends Goal {
		private final AttackHelicopter mob;

		LookAroundGoal(AttackHelicopter mob) {
			this.mob = mob;
			this.setFlags(EnumSet.of(Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return true;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = this.mob.getTarget();
			if (target == null) {
				Vec3 m = this.mob.getDeltaMovement();
				if (m.horizontalDistanceSqr() > 1.0E-6) {
					float yaw = -((float) Mth.atan2(m.x, m.z)) * (180F / (float) Math.PI);
					this.mob.setYRot(yaw);
					this.mob.yBodyRot = yaw;
					this.mob.setYHeadRot(yaw);
				}
				this.mob.setXRot(0f);
			} else if (target.distanceToSqr(this.mob) < 96.0 * 96.0) {
				Ghastling.aimAt(this.mob, target);
				if (this.mob.getXRot() < 0f) {
					this.mob.setXRot(0f);
				}
			}
		}
	}

	/** picks a random point 16 blocks around at fly height above ground */
	static class RandomFlyGoal extends Goal {
		private final AttackHelicopter mob;

		RandomFlyGoal(AttackHelicopter mob) {
			this.mob = mob;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			MoveControl mc = this.mob.getMoveControl();
			if (!mc.hasWanted()) {
				return true;
			}
			double d0 = mc.getWantedX() - this.mob.getX();
			double d1 = mc.getWantedY() - this.mob.getY();
			double d2 = mc.getWantedZ() - this.mob.getZ();
			double d3 = d0 * d0 + d1 * d1 + d2 * d2;
			return d3 < 1.0D || d3 > 3600.0D;
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			RandomSource random = this.mob.getRandom();
			double x = this.mob.getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
			double z = this.mob.getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
			int ground = this.mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
			this.mob.getMoveControl().setWantedPosition(x, ground + TARGET_FLY_HEIGHT, z, 1.0D);
		}
	}

	/** ghast style movement */
	static class FlyingMoveControl extends MoveControl {
		private final AttackHelicopter mob;
		private int courseChangeCooldown;

		FlyingMoveControl(AttackHelicopter mob) {
			super(mob);
			this.mob = mob;
		}

		@Override
		public void tick() {
			if (this.operation == MoveControl.Operation.MOVE_TO) {
				if (this.courseChangeCooldown-- <= 0) {
					this.courseChangeCooldown += this.mob.getRandom().nextInt(5) + 2;
					Vec3 v = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
					double d = v.length();
					v = v.normalize();
					if (this.canReach(v, Mth.ceil(d))) {
						this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(v.scale(0.1D)));
					} else {
						this.operation = MoveControl.Operation.WAIT;
					}
				}
			}
		}

		private boolean canReach(Vec3 dir, int steps) {
			AABB bb = this.mob.getBoundingBox();
			for (int i = 1; i < steps; ++i) {
				bb = bb.move(dir);
				if (!this.mob.level().noCollision(this.mob, bb)) {
					return false;
				}
			}
			return true;
		}
	}

	/** machine gun burst, then a rocket */
	static class HelicopterAttackGoal extends Goal {
		private final AttackHelicopter mob;
		private int attackTimer;

		HelicopterAttackGoal(AttackHelicopter mob) {
			this.mob = mob;
		}

		@Override
		public boolean canUse() {
			return this.mob.getTarget() != null;
		}

		@Override
		public void start() {
			this.attackTimer = 0;
		}

		@Override
		public void stop() {
			this.mob.setAttacking(false);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = this.mob.getTarget();
			if (target == null) {
				return;
			}
			Level level = this.mob.level();
			if (target.distanceToSqr(this.mob) < 4096.0D && this.mob.hasLineOfSight(target)) {
				++this.attackTimer;
				if (this.attackTimer >= 14 && this.attackTimer < 24 && this.attackTimer % 2 == 0) {
					if (this.attackTimer == 14) {
						level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), TGSounds.HELICOPTER_BURST.get(), SoundSource.HOSTILE, 8.0f, 1.0f);
					}
					Ghastling.aimAt(this.mob, target);
					level.addFreshEntity(new GenericProjectile(level, this.mob, 12.0f, 1.0f, 100, 0.05f, 30, 40, 8.0f, 0.25f, false, EnumBulletFirePos.CENTER));
				} else if (this.attackTimer == 35) {
					level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), TGSounds.ROCKET_FIRE.get(), SoundSource.HOSTILE, 8.0f, 1.0f);
					Ghastling.aimAt(this.mob, target);
					level.addFreshEntity(new RocketProjectile(level, this.mob, 12.0f, 1.0f, 100, 0.05f, 30, 40, 8.0f, 0.25f, false,
							this.mob.getRandom().nextBoolean() ? EnumBulletFirePos.LEFT : EnumBulletFirePos.RIGHT, 4.0f, 0.0));
				} else if (this.attackTimer > 35) {
					this.attackTimer = -30;
				}
			} else if (this.attackTimer > 0) {
				--this.attackTimer;
			}
			this.mob.setAttacking(this.attackTimer > 10);
		}
	}
}
