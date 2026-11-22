package techguns.entities.npcs;

import java.util.EnumSet;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import techguns.api.npc.INPCTechgunsShooter;
import techguns.api.npc.INpcTGDamageSystem;
import techguns.damagesystem.TGDamageSource;
import techguns.entities.projectiles.AlienBlasterProjectile;
import techguns.entities.projectiles.EnumBulletFirePos;

/** 1.12 Ghastling: small walking ghast that fires alien blaster bursts like a blaze. */
public class Ghastling extends Monster implements INpcTGDamageSystem, INPCTechgunsShooter {

	private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(Ghastling.class, EntityDataSerializers.BOOLEAN);

	public Ghastling(EntityType<? extends Ghastling> type, Level level) {
		super(type, level);
		this.setPathfindingMalus(PathType.WATER, -1.0F);
		this.setPathfindingMalus(PathType.LAVA, 8.0F);
		this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
		this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20D).add(Attributes.FOLLOW_RANGE, 64D).add(Attributes.ARMOR, 10D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(4, new BlasterAttackGoal(this));
		this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0.0F));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
	protected SoundEvent getAmbientSound() {
		return SoundEvents.GHAST_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource src) {
		return SoundEvents.GHAST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GHAST_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 10.0F;
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
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		return 0;
	}

	/** turns head and body straight at the target, projectiles fly along head yaw / pitch */
	public static void aimAt(Mob mob, LivingEntity target) {
		double dx = target.getX() - mob.getX();
		double dz = target.getZ() - mob.getZ();
		double dy = target.getY(0.5) - mob.getEyeY();
		double h = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
		float pitch = (float) -(Mth.atan2(dy, h) * (180F / Math.PI));
		mob.setYRot(yaw);
		mob.setYHeadRot(yaw);
		mob.yBodyRot = yaw;
		mob.setXRot(pitch);
	}

	/** 1.12 AIFireballAttack: melee when close, otherwise bursts of 3 blaster shots */
	static class BlasterAttackGoal extends Goal {
		private final Ghastling mob;
		private int attackStep;
		private int attackTime;

		BlasterAttackGoal(Ghastling mob) {
			this.mob = mob;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = this.mob.getTarget();
			return t != null && t.isAlive() && this.mob.canAttack(t);
		}

		@Override
		public void start() {
			this.attackStep = 0;
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
			--this.attackTime;
			LivingEntity target = this.mob.getTarget();
			if (target == null) {
				return;
			}
			double d0 = this.mob.distanceToSqr(target);
			double follow = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
			if (d0 < 4.0D) {
				if (this.attackTime <= 0) {
					this.attackTime = 20;
					this.mob.doHurtTarget(target);
				}
				this.mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
			} else if (d0 < follow * follow) {
				if (this.attackTime <= 0) {
					++this.attackStep;
					if (this.attackStep == 1) {
						this.attackTime = 30;
						this.mob.setAttacking(true);
					} else if (this.attackStep <= 4) {
						this.attackTime = 6;
					} else {
						this.attackTime = 50;
						this.attackStep = 0;
						this.mob.setAttacking(false);
					}
					if (this.attackStep > 1) {
						this.mob.level().levelEvent(null, 1018, this.mob.blockPosition(), 0);
						aimAt(this.mob, target);
						this.mob.level().addFreshEntity(new AlienBlasterProjectile(this.mob.level(), this.mob, 6, 1.5f, 200, 0.05f, 200, 200, 6, 0, false,
								EnumBulletFirePos.CENTER));
					}
				}
				this.mob.getLookControl().setLookAt(target, 10.0F, 10.0F);
			} else {
				this.mob.getNavigation().stop();
				this.mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
			}
		}
	}
}
