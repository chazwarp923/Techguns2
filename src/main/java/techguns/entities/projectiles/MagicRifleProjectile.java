package techguns.entities.projectiles;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.api.damagesystem.DamageType;
import techguns.client.audio.TGSoundCategory;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGExplosion;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IChargedProjectileFactory;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;
import techguns.util.TGFXHooks;

/**
 * Magic rifle bolt (Fabric port MagicRifleProjectile), one kind per magic bullet:
 * <ul>
 * <li>arcane (plain magic bullets): energy damage, and an explosion that hurts what's around the hit but not the hit
 * entity itself;</li>
 * <li>fire: sets the hit entity on fire, an explosion that sets everything it hurts on fire, and with block damage on
 * it lights the hit block;</li>
 * <li>lightning: a lightning bolt where it hits; charged over a quarter, it stays a while after the hit and calls more
 * bolts around the spot.</li>
 * </ul>
 * Explosions, trails and lingering grow with the charge (0 to 1).
 */
public class MagicRifleProjectile extends GenericProjectile {
	public static final byte TYPE_ARCANE = 0;
	public static final byte TYPE_FIRE = 1;
	public static final byte TYPE_LIGHTNING = 2;

	public byte magicType = TYPE_ARCANE;
	public float chargeAmount = 0.0f;
	/** lightning: ticks it stays after the hit, calling bolts */
	public int postImpactTicks = 0;

	public MagicRifleProjectile(EntityType<? extends MagicRifleProjectile> type, Level level) {
		super(type, level);
		this.discardOnHit = false;
	}

	public MagicRifleProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, byte magicType, float charge) {
		super(TGEntities.MAGIC_RIFLE.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
		this.discardOnHit = false;
		this.magicType = magicType;
		this.chargeAmount = charge;
	}

	@Override
	public void tick() {
		if (this.postImpactTicks > 0) {
			this.postImpactTicks--;
			if (!this.level().isClientSide) {
				lightningPostImpactTick();
				if (this.postImpactTicks <= 0) {
					this.discard();
				}
			}
			return;
		}
		super.tick();
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (this.isRemoved()) {
			return;
		}
		if (this.magicType == TYPE_LIGHTNING && this.ticksToLive > 0 && this.chargeAmount > 0.25f) {
			// stays at the hit spot for a while: half the Fabric port's 10 + charge * 30 ticks, so half the strikes
			this.postImpactTicks = 5 + (int) (this.chargeAmount * 15);
			this.setPos(hit.getLocation());
			this.setDeltaMovement(Vec3.ZERO);
		} else {
			this.discard();
		}
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		switch (this.magicType) {
			case TYPE_FIRE -> {
				// the Fabric port's (int) chargeAmount * 7: 10 seconds at full charge, else 3
				int fireTime = 3 + (int) this.chargeAmount * 7;
				if (!ent.fireImmune()) {
					ent.igniteForSeconds(fireTime);
				}
				explodeFire();
			}
			case TYPE_LIGHTNING -> {
				BlockPos pos = getFreeBlockPos(this.level(), ent.getX(), ent.getY(), ent.getZ());
				if (pos != null) {
					createLightning(pos);
				}
			}
			default -> explodeArcane(ent);
		}
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		super.hitBlock(hit);
		if (this.level().isClientSide) {
			return;
		}
		switch (this.magicType) {
			case TYPE_FIRE -> {
				if (this.blockdamage) {
					burnBlocks(this.level(), hit, 1.0);
				}
				explodeFire();
			}
			case TYPE_LIGHTNING -> createLightning(hit.getBlockPos());
			default -> explodeArcane(null);
		}
	}

	@Override
	protected void sendImpactFX(double x, double y, double z, float pitch, float yaw, int type) {
		super.sendImpactFX(x, y, z, pitch, yaw, type, this.magicType == TYPE_FIRE);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = switch (this.magicType) {
			case TYPE_FIRE -> TGDamageSource.causeFireDamage(this, this.shooter, DeathType.LASER);
			case TYPE_LIGHTNING -> TGDamageSource.causeLightningDamage(this, this.shooter, DeathType.GORE);
			default -> TGDamageSource.causeEnergyDamage(this, this.shooter, DeathType.GORE);
		};
		src.armorPenetration = this.penetration;
		return src;
	}

	/* ---- arcane and fire explosions ---- */

	private void explodeArcane(LivingEntity hitEntity) {
		if (this.level().isClientSide) {
			return;
		}
		float size = 0.5f + this.chargeAmount * 2.5f;
		TGPackets.sendToAllAround(new PacketSpawnParticle("MagicRifleExplosion_Arcane", getX(), getY(), getZ(), size), this, 100.0);
		TGExplosion explosion = new TGExplosion(this.level(), this.shooter, this, getX(), getY(), getZ(), this.damage * 0.66f * size,
				this.damage * 0.33f * size, size, size * 2.0f, 0.0);
		explosion.setDmgSrc(getProjectileDamageSource());
		if (hitEntity != null) {
			explosion.excludedEntities = Set.of(hitEntity);
		}
		explosion.doExplosion(false);
		playExplosionSound();
	}

	private void explodeFire() {
		if (this.level().isClientSide) {
			return;
		}
		float size = 0.5f + this.chargeAmount * 1.5f;
		TGPackets.sendToAllAround(new PacketSpawnParticle(this.chargeAmount > 0.8f ? "MagicRifleExplosion_Fire_Charged" : "MagicRifleExplosion_Fire",
				getX(), getY(), getZ(), size), this, 100.0);
		TGExplosion explosion = new TGExplosion(this.level(), this.shooter, this, getX(), getY(), getZ(), this.damage * 0.5f * size,
				this.damage * 0.25f * size, size * 1.75f, size * 3.5f, 0.0);
		explosion.setDmgSrc(getProjectileDamageSource());
		explosion.entityFireTime = 3 + (int) this.chargeAmount * 7;
		explosion.doExplosion(false);
		playExplosionSound();
	}

	private void playExplosionSound() {
		this.level().playSound(null, getX(), getY(), getZ(), TGSounds.TFG_EXPLOSION.get(), TGSoundCategory.EXPLOISON.getVanillaCategory(), 5.0f, 1.0f);
	}

	/* ---- lightning ---- */

	private void lightningPostImpactTick() {
		// a bolt every 8 ticks on the spot, and now and then one around it
		if (this.postImpactTicks % 8 == 0) {
			BlockPos pos = getFreeBlockPos(this.level(), getX(), getY(), getZ());
			if (pos != null) {
				createLightning(pos);
			}
		}
		if (this.postImpactTicks % 4 == 0 && this.chargeAmount * 0.5f > this.random.nextFloat()) {
			float r = this.random.nextFloat() * this.chargeAmount * 5.0f;
			float a = this.random.nextFloat() * Mth.TWO_PI;
			BlockPos pos = getFreeBlockPos(this.level(), getX() + r * Mth.cos(a), getY(), getZ() + r * Mth.sin(a));
			if (pos != null) {
				createLightning(pos);
			}
		}
	}

	private void createLightning(BlockPos pos) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(this.level());
		if (bolt == null) {
			return;
		}
		bolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
		bolt.setCause(this.shooter instanceof ServerPlayer sp ? sp : null);
		this.level().addFreshEntity(bolt);
		this.level().playSound(null, pos, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.WEATHER, 5.0f, 1.0f);
	}

	/** the nearest solid block with air (or fire) above it, up to 3 blocks down or up, or null */
	public static BlockPos getFreeBlockPos(Level level, double x, double y, double z) {
		BlockPos start = BlockPos.containing(x, y, z);
		if (isFree(level, start)) {
			return start;
		}
		for (int i = 1; i <= 3; i++) {
			if (isFree(level, start.below(i))) {
				return start.below(i);
			}
		}
		for (int i = 1; i <= 3; i++) {
			if (isFree(level, start.above(i))) {
				return start.above(i);
			}
		}
		return null;
	}

	private static boolean isFree(Level level, BlockPos pos) {
		if (level.getBlockState(pos).isAir()) {
			return false;
		}
		var above = level.getBlockState(pos.above());
		return above.isAir() || above.is(Blocks.FIRE);
	}

	/* ---- client trail, sync and saving ---- */

	@Override
	protected void onClientSpawn() {
		super.onClientSpawn();
		switch (this.magicType) {
			case TYPE_FIRE -> TGFXHooks.createFXOnEntity("MagicRifleProjectileTrail_Fire", this, 0.5f + this.chargeAmount * 1.5f);
			case TYPE_LIGHTNING -> TGFXHooks.createFXOnEntity("MagicRifleProjectileTrail_Lightning", this, 0.5f + this.chargeAmount * 2.5f);
			default -> TGFXHooks.createFXOnEntity("MagicRifleProjectileTrail_Arcane", this, 0.5f + this.chargeAmount * 2.5f);
		}
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeByte(this.magicType);
		buffer.writeFloat(this.chargeAmount);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		this.magicType = buffer.readByte();
		this.chargeAmount = buffer.readFloat();
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tags) {
		super.readAdditionalSaveData(tags);
		this.magicType = tags.getByte("magicType");
		this.chargeAmount = tags.getFloat("chargeAmount");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tags) {
		super.addAdditionalSaveData(tags);
		tags.putByte("magicType", this.magicType);
		tags.putFloat("chargeAmount", this.chargeAmount);
	}

	public static class Factory implements IChargedProjectileFactory<MagicRifleProjectile> {
		protected final byte magicType;

		public Factory(byte magicType) {
			this.magicType = magicType;
		}

		@Override
		public MagicRifleProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return createChargedProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos,
					radius, gravity, 0f, 1);
		}

		@Override
		public MagicRifleProjectile createChargedProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity, float charge, int ammoConsumed) {
			MagicRifleProjectile proj = new MagicRifleProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration,
					blockdamage, firePos, this.magicType, charge);
			proj.setRadiusAndGravity(radius, gravity);
			return proj;
		}

		/** the damage the bolt does (the Fabric port said projectile for all three) */
		@Override
		public DamageType getDamageType() {
			return switch (this.magicType) {
				case TYPE_FIRE -> DamageType.FIRE;
				case TYPE_LIGHTNING -> DamageType.LIGHTNING;
				default -> DamageType.ENERGY;
			};
		}
	}
}
