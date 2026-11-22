package techguns.entities.npcs;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGArmors;
import techguns.TGuns;
import techguns.api.npc.INPCTechgunsShooter;
import techguns.api.npc.INpcTGDamageSystem;
import techguns.api.npc.factions.ITGNpcTeam;
import techguns.api.npc.factions.TGNpcFaction;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.TGDamageSource;
import techguns.entities.ai.RangedGunAttackGoal;
import techguns.entities.ai.TGHurtByTargetGoal;
import techguns.items.guns.GenericGun;

/**
 * Base of the Techguns humanoid NPCs (1.12 GenericNPC): shoots the gun in its main hand (or fights in melee without
 * one), gets its gear on spawn depending on difficulty, uses the Techguns armor formula with a fixed armor value.
 */
public abstract class GenericNPC extends Monster implements RangedAttackMob, INPCTechgunsShooter, INpcTGDamageSystem, ITGNpcTeam {

	@Nullable
	private RangedGunAttackGoal<GenericNPC> aiRangedAttack;
	private final MeleeAttackGoal aiMelee = new MeleeAttackGoal(this, 1.2D, false);

	protected float armor = 0f;
	protected float penetrationResistance = 0f;

	protected GenericNPC(EntityType<? extends GenericNPC> type, Level level) {
		super(type, level);
		this.setCombatTask();
	}

	public static AttributeSupplier.Builder createAttributes(double speed, double health, double attack, double followRange) {
		return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.MAX_HEALTH, health)
				.add(Attributes.ATTACK_DAMAGE, attack).add(Attributes.FOLLOW_RANGE, followRange);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new TGHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	protected void setTGArmorStats(float armor, float penetrationResistance) {
		this.armor = armor;
		this.penetrationResistance = penetrationResistance;
	}

	/* ------------------------------------------------------------------ gear */

	@Override
	@Nullable
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
		data = super.finalizeSpawn(level, difficulty, spawnType, data);
		this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
		this.setCanPickUpLoot(false);
		this.setCombatTask();
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		this.addRandomArmor(Math.round(difficulty.getSpecialMultiplier() * 3f));
	}

	/** gear when spawned by the Techguns spawn manager with a danger level */
	public void onSpawnByManager(int difficulty) {
		this.setCanPickUpLoot(false);
		this.addRandomArmor(difficulty);
		this.setCombatTask();
	}

	protected void addRandomArmor(int difficulty) {
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(pick(TGuns.REVOLVER.get(), TGuns.THOMPSON.get(), TGuns.AS50.get(), TGuns.GRIMREAPER.get())));
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TGArmors.T1_COMBAT_HELMET.get()));
		this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TGArmors.T1_COMBAT_CHESTPLATE.get()));
		this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(TGArmors.T1_COMBAT_LEGGINGS.get()));
		this.setItemSlot(EquipmentSlot.FEET, new ItemStack(TGArmors.T1_COMBAT_BOOTS.get()));
	}

	/** puts the item in the slot with the given chance */
	protected void equipChance(EquipmentSlot slot, ItemLike item, double chance) {
		equipChance(slot, new ItemStack(item), chance);
	}

	protected void equipChance(EquipmentSlot slot, ItemStack stack, double chance) {
		if (this.random.nextDouble() <= chance) {
			this.setItemSlot(slot, stack);
		}
	}

	protected void equip(EquipmentSlot slot, ItemLike item) {
		this.setItemSlot(slot, new ItemStack(item));
	}

	protected Item pick(Item... items) {
		return items[this.random.nextInt(items.length)];
	}

	/* ------------------------------------------------------------------ combat */

	/** ranged attack with the gun in the main hand, else melee */
	public void setCombatTask() {
		if (this.level() == null || this.level().isClientSide) {
			return;
		}
		this.goalSelector.removeGoal(this.aiMelee);
		if (this.aiRangedAttack != null) {
			this.goalSelector.removeGoal(this.aiRangedAttack);
		}
		ItemStack stack = this.getMainHandItem();
		if (stack.getItem() instanceof GenericGun gun) {
			this.aiRangedAttack = new RangedGunAttackGoal<>(this, 1.0D, gun.getAI_attackTime() / 3, gun.getAI_attackTime(), gun.getAI_attackRange(),
					gun.getAI_burstCount(), gun.getAI_burstAttackTime());
			this.goalSelector.addGoal(4, this.aiRangedAttack);
		} else {
			this.goalSelector.addGoal(4, this.aiMelee);
		}
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		super.setItemSlot(slot, stack);
		if (slot == EquipmentSlot.MAINHAND) {
			this.setCombatTask();
		}
	}

	@Override
	public void performRangedAttack(LivingEntity target, float distanceFactor) {
		if (this.getMainHandItem().getItem() instanceof GenericGun gun) {
			float acc = 1.0f;
			float dmg = 1.0f;
			switch (this.level().getDifficulty()) {
				case EASY -> {
					acc = 1.3f;
					dmg = 0.6f;
				}
				case NORMAL -> {
					acc = 1.15f;
					dmg = 0.8f;
				}
				default -> {
				}
			}
			gun.fireWeaponFromNPC(this, dmg, acc);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.setCombatTask();
	}

	/* ------------------------------------------------------------------ Techguns interfaces */

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
		return DamageSystem.getArmorAgainstDamageTypeDefault(this, this.armor, dmgsrc.damageType);
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.HOSTILE;
	}

	/* ------------------------------------------------------------------ sounds */

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource src) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	/** quiet step sound (1.12 playStepSound at volume 0.15), null = vanilla */
	@Nullable
	protected SoundEvent getStepSound() {
		return null;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		SoundEvent s = getStepSound();
		if (s != null) {
			this.playSound(s, 0.15F, 1.0F);
		} else {
			super.playStepSound(pos, state);
		}
	}
}
