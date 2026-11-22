package techguns.entities.ai;

import java.util.EnumSet;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/**
 * Ranged attack with burst fire (1.12 EntityAIRangedAttack): the delay between bursts scales with the distance to the
 * target, inside a burst the shots come every shotDelay ticks.
 */
public class RangedGunAttackGoal<T extends Mob & RangedAttackMob> extends Goal {

	private final T mob;
	private LivingEntity target;
	private int rangedAttackTime = -1;
	private final double moveSpeed;
	private int ticksTargetSeen;
	private final int attackTimeVariance;
	private final int maxRangedAttackTime;
	private final float attackRange;
	private final float attackRangeSq;

	private final int maxBurstCount;
	private int burstCount;
	private final int shotDelay;

	public RangedGunAttackGoal(T mob, double moveSpeed, int attackTimeVariance, int attackTime, float attackRange, int maxBurstCount, int shotDelay) {
		this.mob = mob;
		this.moveSpeed = moveSpeed;
		this.attackTimeVariance = attackTimeVariance;
		this.maxRangedAttackTime = attackTime;
		this.attackRange = attackRange;
		this.attackRangeSq = attackRange * attackRange;
		this.maxBurstCount = maxBurstCount;
		this.burstCount = maxBurstCount;
		this.shotDelay = shotDelay;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = this.mob.getTarget();
		if (t == null || !t.isAlive()) {
			return false;
		}
		this.target = t;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse() || !this.mob.getNavigation().isDone();
	}

	@Override
	public void stop() {
		this.target = null;
		this.ticksTargetSeen = 0;
		this.rangedAttackTime = -1;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		double d0 = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
		boolean inSight = this.mob.getSensing().hasLineOfSight(this.target);

		if (inSight) {
			++this.ticksTargetSeen;
		} else {
			this.ticksTargetSeen = 0;
		}

		if (this.moveSpeed > 0) {
			if (d0 <= this.attackRangeSq && this.ticksTargetSeen >= 20) {
				this.mob.getNavigation().stop();
			} else {
				this.mob.getNavigation().moveTo(this.target, this.moveSpeed);
			}
		}

		this.mob.getLookControl().setLookAt(this.target, 30.0F, 55.0F);

		if (--this.rangedAttackTime == 0) {
			if (d0 > this.attackRangeSq || !inSight) {
				return;
			}
			float f = (float) Math.sqrt(d0) / this.attackRange;
			this.mob.performRangedAttack(this.target, Mth.clamp(f, 0.1F, 1.0F));

			if (maxBurstCount > 0) {
				burstCount--;
			}
			if (burstCount > 0) {
				this.rangedAttackTime = shotDelay;
			} else {
				burstCount = maxBurstCount;
				this.rangedAttackTime = Mth.floor(f * (this.maxRangedAttackTime - this.attackTimeVariance) + this.attackTimeVariance);
			}
		} else if (this.rangedAttackTime < 0) {
			float f = (float) Math.sqrt(d0) / this.attackRange;
			this.rangedAttackTime = Math.max(1, Mth.floor(f * (this.maxRangedAttackTime - this.attackTimeVariance) + this.attackTimeVariance));
		}
	}
}
