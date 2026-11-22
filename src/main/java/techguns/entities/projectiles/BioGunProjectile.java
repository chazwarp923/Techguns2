package techguns.entities.projectiles;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IChargedProjectileFactory;
import techguns.util.TGFXHooks;

/** Bio gun goo; charge level = ammo consumed. */
public class BioGunProjectile extends GenericProjectile {

	public int chargeLevel;

	public BioGunProjectile(EntityType<? extends BioGunProjectile> type, Level level) {
		super(type, level);
	}

	public BioGunProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, double gravity, int chargeLevel) {
		super(TGEntities.BIOGUN.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		this.chargeLevel = chargeLevel;
		this.gravity = gravity;
	}

	@Override
	protected void onClientSpawn() {
		TGFXHooks.createFXOnEntity("BioGunTrail", this);
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		ent.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 3, false, true));
		doImpactEffects(null, hit, null);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causePoisonDamage(this, this.shooter, DeathType.BIO);
		src.goreChance = 1.0f;
		src.armorPenetration = this.penetration;
		src.setNoKnockback();
		return src;
	}

	@Override
	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		Vec3 v = hit.getLocation();
		float pitch = 0.0f;
		float yaw = 0.0f;
		if (hit instanceof BlockHitResult bhr) {
			Direction side = bhr.getDirection();
			if (side == Direction.UP) {
				pitch = -90.0f;
			} else if (side == Direction.DOWN) {
				pitch = 90.0f;
			} else {
				yaw = side.toYRot();
			}
		} else {
			pitch = -this.getXRot();
			yaw = -this.getYRot();
		}
		if (this.level().isClientSide) {
			this.level().playLocalSound(v.x, v.y, v.z, TGSounds.BIOGUN_IMPACT.get(), SoundSource.AMBIENT, 1.0f, 1.0f, true);
		}
		TGFXHooks.createFX("biogunImpact", this.level(), v.x, v.y, v.z, 0.0D, 0.0D, 0.0D, pitch, yaw);
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		techguns.blocks.BlockBioblob.onBioShotHit(this.level(), hit.getBlockPos(), hit.getDirection(), this.chargeLevel, this.blockdamage, this.shooter);
		super.hitBlock(hit);
	}

	@Override
	protected float inWaterUpdateBehaviour(float f1) {
		if (this.isInWater()) {
			this.discard();
		}
		return f1;
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeByte((byte) this.chargeLevel);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		this.chargeLevel = buffer.readByte();
	}

	public static class Factory implements IChargedProjectileFactory<BioGunProjectile> {
		@Override
		public BioGunProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return this.createChargedProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
					firePos, radius, gravity, 0f, 1);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.POISON;
		}

		@Override
		public BioGunProjectile createChargedProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity, float charge, int ammoConsumed) {
			return new BioGunProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos, gravity,
					ammoConsumed);
		}
	}
}
