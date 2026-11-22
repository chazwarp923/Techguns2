package techguns.entities.special;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import techguns.TGEntities;
import techguns.radiation.TGRadiationSystem;

/**
 * Invisible radiation area (1.12 EntityRadiation, left by nukes): every second gives radiation to living entities
 * nearby, from half its lifetime on the strength drops to 0.
 */
public class EntityRadiation extends Entity {

	public static final int RAD_INTERVAL = 20;

	protected int duration;
	protected float radius;
	protected float innerRadius;
	protected int strength;
	protected int strengthOuter;
	protected int strengthOriginal;
	protected int strengthOuterOriginal;

	public EntityRadiation(EntityType<? extends EntityRadiation> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public EntityRadiation(Level level, double x, double y, double z, int duration, float innerRadius, int strength, float radius, int strengthOuter) {
		this(TGEntities.RADIATION.get(), level);
		this.setPos(x, y, z);
		this.duration = duration;
		this.innerRadius = innerRadius;
		this.strength = strength;
		this.radius = radius;
		this.strengthOuter = strengthOuter;
		this.strengthOriginal = strength;
		this.strengthOuterOriginal = strengthOuter;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			return;
		}
		if (this.tickCount > this.duration) {
			this.discard();
			return;
		}
		if (this.tickCount % RAD_INTERVAL == 0) {
			if (this.tickCount * 2 >= this.duration) {
				double factor = 1d - ((this.tickCount - this.duration * 0.5) / (this.duration * 0.5));
				this.strength = Math.max((int) Math.round(this.strengthOriginal * factor), 0);
				this.strengthOuter = Math.max((int) Math.round(this.strengthOuterOriginal * factor), 0);
			}
			TGRadiationSystem.applyRadToEntities(level(), getX(), getY(), getZ(), this.radius, RAD_INTERVAL + 2, this.strength, this.innerRadius,
					this.strengthOuter);
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.tickCount = tag.getInt("Age");
		this.duration = tag.getInt("Duration");
		this.radius = tag.getFloat("radius");
		this.innerRadius = tag.getFloat("inner_radius");
		this.strength = tag.getShort("strength");
		this.strengthOuter = tag.getShort("strength_outer");
		this.strengthOriginal = tag.getShort("strength_original");
		this.strengthOuterOriginal = tag.getShort("strength_outer_original");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Age", this.tickCount);
		tag.putInt("Duration", this.duration);
		tag.putFloat("radius", this.radius);
		tag.putFloat("inner_radius", this.innerRadius);
		tag.putShort("strength", (short) this.strength);
		tag.putShort("strength_outer", (short) this.strengthOuter);
		tag.putShort("strength_original", (short) this.strengthOriginal);
		tag.putShort("strength_outer_original", (short) this.strengthOuterOriginal);
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}
}
