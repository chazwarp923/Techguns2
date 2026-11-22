package techguns.entities.npcs;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import techguns.TGEntities;
import techguns.Techguns;
import techguns.api.npc.INPCTechgunsShooter;
import techguns.api.npc.factions.ITGNpcTeam;
import techguns.api.npc.factions.TGNpcFaction;
import techguns.entities.ai.RangedGunAttackGoal;
import techguns.factions.TGTeams;
import techguns.items.guns.GenericGun;
import techguns.tileentities.TurretTileEnt;

/**
 * Turret mounted on a turret base (1.12 NPCTurret). Never moves, takes gun, ammo, armor and power from the base block
 * entity. Weapon (main hand), armor tier and facing are synced to the client for the renderer.
 */
public class NPCTurret extends PathfinderMob implements RangedAttackMob, INPCTechgunsShooter, ITGNpcTeam {

	private static final ResourceLocation[] TEXTURES = new ResourceLocation[] { Techguns.rl("textures/blocks/turret_base.png"),
			Techguns.rl("textures/blocks/turret_base2.png"), Techguns.rl("textures/blocks/turret_base3.png"),
			Techguns.rl("textures/blocks/turret_base4.png"), Techguns.rl("textures/blocks/turret_base5.png") };

	private static final EntityDataAccessor<BlockPos> BASE_POS = SynchedEntityData.defineId(NPCTurret.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Boolean> FACING_DOWN = SynchedEntityData.defineId(NPCTurret.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(NPCTurret.class, EntityDataSerializers.INT);

	protected boolean active = true;
	@Nullable
	protected RangedGunAttackGoal<NPCTurret> aiRangedAttack;

	public NPCTurret(EntityType<? extends NPCTurret> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.setPersistenceRequired();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			this.setDropChance(slot, 0f);
		}
	}

	public NPCTurret(Level level, TurretTileEnt base) {
		this(TGEntities.TURRET.get(), level);
		BlockPos pos = base.getBlockPos();
		this.entityData.set(BASE_POS, pos);
		this.entityData.set(FACING_DOWN, base.getFacing() == Direction.DOWN);
		this.refreshDimensions();
		Vec pos2 = anchor();
		this.moveTo(pos2.x, pos2.y, pos2.z, 0f, 0f);
		this.updateFromBase(base);
	}

	private record Vec(double x, double y, double z) {
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
				.add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.FOLLOW_RANGE, 20.0D);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(BASE_POS, BlockPos.ZERO);
		builder.define(FACING_DOWN, false);
		builder.define(TIER, 0);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (FACING_DOWN.equals(key)) {
			this.refreshDimensions();
		}
	}

	public static ResourceLocation getTexture(int tier) {
		return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, tier))];
	}

	public ResourceLocation getTexture() {
		return getTexture(this.entityData.get(TIER));
	}

	public Direction getTurretFacing() {
		return this.entityData.get(FACING_DOWN) ? Direction.DOWN : Direction.UP;
	}

	public BlockPos getBasePos() {
		return this.entityData.get(BASE_POS);
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		boolean down = this.entityData != null && this.entityData.get(FACING_DOWN);
		return EntityDimensions.fixed(0.8f, 0.9f).withEyeHeight(down ? 0.15f : 0.75f);
	}

	/** entity position for the base pos and facing */
	protected Vec anchor() {
		BlockPos p = getBasePos();
		double y = getTurretFacing() == Direction.DOWN ? p.getY() - this.getBbHeight() : p.getY() + 1.0d;
		return new Vec(p.getX() + 0.5d, y, p.getZ() + 0.5d);
	}

	@Nullable
	public TurretTileEnt getBase() {
		if (level().getBlockEntity(getBasePos()) instanceof TurretTileEnt te) {
			return te;
		}
		return null;
	}

	/** weapon, armor tier changed on the base */
	public void updateFromBase(TurretTileEnt base) {
		this.entityData.set(TIER, base.getTurretTier());
		if (!level().isClientSide) {
			this.setCombatTask(base.getWeapon());
		}
	}

	/* ------------------------------------------------------------------ AI */

	@Override
	protected void registerGoals() {
		this.setAITasks();
	}

	public void setAITasks() {
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, LivingEntity.class, 20.0F, 0.02F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, NPCTurret.class) {
			@Override
			public boolean canUse() {
				LivingEntity attacker = NPCTurret.this.getLastHurtByMob();
				if (attacker instanceof Player ply) {
					TurretTileEnt base = getBase();
					if (base != null && base.getOwner() != null && base.getOwner().equals(ply.getUUID())) {
						return false;
					}
				}
				return super.canUse();
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, false, this::isValidTarget) {
			@Override
			protected AABB getTargetSearchArea(double distance) {
				return this.mob.getBoundingBox().inflate(distance, 20.0D, distance);
			}
		});
		if (aiRangedAttack != null) {
			this.goalSelector.addGoal(1, aiRangedAttack);
		}
	}

	/** 1.12 turret target selector: monsters, pvp setting, untamed animals */
	protected boolean isValidTarget(LivingEntity entity) {
		if (entity instanceof Enemy) {
			return true;
		}
		TurretTileEnt base = getBase();
		if (base == null) {
			return false;
		}
		byte pvp = base.getPvpSetting();
		UUID owner = base.getOwner();
		if (pvp != 0 && owner != null) {
			if (entity instanceof NPCTurret other) {
				TurretTileEnt otherBase = other.getBase();
				if (otherBase != null && otherBase.getOwner() != null) {
					return TGTeams.shouldAttack(owner, otherBase.getOwner(), pvp);
				}
			} else if (entity instanceof Player ply) {
				return TGTeams.shouldAttack(owner, ply.getUUID(), pvp);
			}
		}
		if (base.attackAnimals && (entity instanceof Animal || entity instanceof WaterAnimal)) {
			if (entity instanceof OwnableEntity o && o.getOwnerUUID() != null) {
				return false;
			}
			if (entity instanceof AbstractHorse h && h.isTamed()) {
				return false;
			}
			return true;
		}
		return false;
	}

	public void setActive(boolean active) {
		if (this.active == active) {
			return;
		}
		this.active = active;
		this.goalSelector.removeAllGoals(g -> true);
		this.targetSelector.removeAllGoals(g -> true);
		if (active) {
			this.setAITasks();
		} else {
			this.setTarget(null);
		}
	}

	public boolean isActive() {
		return active;
	}

	/** new attack ai for the gun in the base */
	protected void setCombatTask(ItemStack weapon) {
		if (this.aiRangedAttack != null) {
			this.goalSelector.removeGoal(this.aiRangedAttack);
			this.aiRangedAttack = null;
		}
		if (weapon.getItem() instanceof GenericGun gun) {
			this.aiRangedAttack = new RangedGunAttackGoal<>(this, 0.0D, gun.getAI_attackTime() / 3, gun.getAI_attackTime(), gun.getAI_attackRange(),
					gun.getAI_burstCount(), gun.getAI_burstAttackTime());
			if (active) {
				this.goalSelector.addGoal(1, this.aiRangedAttack);
			}
			var range = this.getAttribute(Attributes.FOLLOW_RANGE);
			if (range != null) {
				range.setBaseValue(gun.getAI_attackRange());
			}
		}
	}

	@Override
	public void performRangedAttack(LivingEntity target, float distanceFactor) {
		TurretTileEnt base = getBase();
		if (base == null) {
			return;
		}
		ItemStack weapon = base.getWeapon();
		if (weapon.getItem() instanceof GenericGun gun && base.consumeAmmo()) {
			gun.fireWeaponFromNPC(this, 1.0f, 1.0f);
			base.setChanged();
		}
	}

	/* ------------------------------------------------------------------ base link */

	@Override
	public void tick() {
		super.tick();
		Vec a = anchor();
		this.setPos(a.x, a.y, a.z);
		this.setDeltaMovement(0, 0, 0);
		if (!level().isClientSide) {
			TurretTileEnt base = getBase();
			if (base == null) {
				this.discard();
				return;
			}
			if (!base.isMountedTurret(this)) {
				// an orphan, the base built a new turret
				this.discard();
				return;
			}
			if (base.getTurret() != this) {
				base.setMountedTurret(this);
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide) {
			TurretTileEnt base = getBase();
			if (base != null && base.isMountedTurret(this)) {
				base.onTurretDeath();
			}
		}
	}

	/** server side the main hand is the gun in the base, this also syncs it to clients */
	@Override
	public ItemStack getItemBySlot(EquipmentSlot slot) {
		if (slot == EquipmentSlot.MAINHAND && !level().isClientSide) {
			TurretTileEnt base = getBase();
			return base != null ? base.getWeapon() : ItemStack.EMPTY;
		}
		return super.getItemBySlot(slot);
	}

	@Override
	public int getArmorValue() {
		TurretTileEnt base = level().isClientSide ? null : getBase();
		return base != null ? base.getTurretArmorValue() : super.getArmorValue();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
	}

	@Override
	protected void dropEquipment() {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(Entity entity) {
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("basePos", NbtUtils.writeBlockPos(getBasePos()));
		tag.putBoolean("facingDown", this.entityData.get(FACING_DOWN));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		NbtUtils.readBlockPos(tag, "basePos").ifPresent(p -> this.entityData.set(BASE_POS, p));
		this.entityData.set(FACING_DOWN, tag.getBoolean("facingDown"));
		this.refreshDimensions();
	}

	/* ------------------------------------------------------------------ techguns shooter */

	@Override
	public float getWeaponPosX() {
		return 0.05f;
	}

	@Override
	public float getWeaponPosY() {
		return 0.7f;
	}

	@Override
	public float getWeaponPosZ() {
		return 0.8f;
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.TURRET;
	}
}
