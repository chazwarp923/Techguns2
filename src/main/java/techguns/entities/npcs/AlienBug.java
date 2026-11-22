package techguns.entities.npcs;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.BlockPos;
import techguns.TGSounds;
import techguns.api.npc.INpcTGDamageSystem;
import techguns.api.npc.factions.ITGNpcTeam;
import techguns.api.npc.factions.TGNpcFaction;
import techguns.damagesystem.TGDamageSource;

/**
 * 1.12 AlienBug: wall climbing melee bug (was an EntitySpider; the climbing is copied so no spider jockeys / spider
 * spawn effects apply). Drops nothing.
 */
public class AlienBug extends Monster implements ITGNpcTeam, INpcTGDamageSystem {

	private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(AlienBug.class, EntityDataSerializers.BOOLEAN);
	private static final byte EVENT_BITE = 4;

	/** jaw animation, client */
	protected int attackTimer;

	public AlienBug(EntityType<? extends AlienBug> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE, 4D).add(Attributes.MAX_HEALTH, 20D).add(Attributes.MOVEMENT_SPEED, 1.0D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
		this.goalSelector.addGoal(4, new BugAttackGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new BugTargetGoal<>(this, Player.class));
		this.targetSelector.addGoal(3, new BugTargetGoal<>(this, IronGolem.class));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLIMBING, false);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WallClimberNavigation(this, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {
			this.entityData.set(CLIMBING, this.horizontalCollision);
		}
	}

	@Override
	public boolean onClimbable() {
		return this.entityData.get(CLIMBING);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.attackTimer > 0) {
			--this.attackTimer;
		}
	}

	public int getAttackTimer() {
		return this.attackTimer;
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		boolean b = super.doHurtTarget(target);
		this.attackTimer = 10;
		this.level().playSound(null, getX(), getY(), getZ(), TGSounds.ALIENBUG_BITE.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
		this.level().broadcastEntityEvent(this, EVENT_BITE);
		return b;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_BITE) {
			this.attackTimer = 10;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	protected ResourceKey<LootTable> getDefaultLootTable() {
		return BuiltInLootTables.EMPTY;
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.HOSTILE;
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		return switch (dmgsrc.damageType) {
			case ENERGY, EXPLOSION, ICE, LIGHTNING -> 5f;
			case PHYSICAL, PROJECTILE -> 10f;
			case POISON, RADIATION -> 20f;
			default -> 0f;
		};
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return TGSounds.ALIENBUG_IDLE.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource src) {
		return TGSounds.ALIENBUG_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TGSounds.ALIENBUG_DEATH.get();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(TGSounds.ALIENBUG_STEP.get(), 0.15F, 1.0F);
	}

	/** plays the aggro sound when it picks a target */
	static class BugTargetGoal<T extends net.minecraft.world.entity.LivingEntity> extends NearestAttackableTargetGoal<T> {
		BugTargetGoal(AlienBug bug, Class<T> target) {
			super(bug, target, true);
		}

		@Override
		public void start() {
			super.start();
			this.mob.level().playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), TGSounds.ALIENBUG_AGGRO.get(), SoundSource.HOSTILE, 1.0f,
					1.0f);
		}
	}

	/** spider attack: may give up in bright light, longer reach */
	static class BugAttackGoal extends MeleeAttackGoal {
		BugAttackGoal(AlienBug bug) {
			super(bug, 1.0D, true);
		}

		@Override
		public boolean canContinueToUse() {
			if (this.mob.getLightLevelDependentMagicValue() >= 0.5F && this.mob.getRandom().nextInt(100) == 0) {
				this.mob.setTarget(null);
				return false;
			}
			return super.canContinueToUse();
		}
	}
}
