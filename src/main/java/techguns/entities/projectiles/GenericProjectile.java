package techguns.entities.projectiles;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.api.npc.INPCTechgunsShooter;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.packets.PacketGunImpactFX;

/**
 * Base bullet entity. Moves in a straight line (plus optional gravity), ray traces blocks and
 * entities each tick. The client simulates the flight from the initial motion sent in the spawn data.
 */
public class GenericProjectile extends Entity implements IEntityWithComplexSpawn {

	public static final Predicate<Entity> BULLET_TARGETS = EntitySelector.NO_SPECTATORS.and(Entity::isAlive).and(Entity::isPickable);

	protected float damage;
	public float speed = 1.0f; // speed in blocks per tick

	protected int ticksToLive = 100;
	protected int lifetime = 100; // TTL but not reduced per tick

	// DMG drop stats
	protected float damageDropStart;
	protected float damageDropEnd;
	protected float damageMin;
	@Nullable
	protected LivingEntity shooter;

	protected float penetration = 0.0f;

	protected boolean silenced = false;
	protected boolean blockdamage = false;

	protected boolean posInitialized = false;
	protected double startX;
	protected double startY;
	protected double startZ;

	protected float radius = 0.0f;
	/** beams keep existing after a hit (they are only visual afterwards) */
	protected boolean discardOnHit = true;
	protected double gravity = 0.0f;

	public GenericProjectile(EntityType<? extends GenericProjectile> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
	}

	public GenericProjectile(EntityType<? extends GenericProjectile> type, Level level, LivingEntity p, float damage, float speed, int TTL,
			float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		this(type, level);
		this.shooter = p;
		this.initProjectile(p.getX(), p.getEyeY(), p.getZ(), p.getYHeadRot(), p.getXRot(), damage, speed, TTL, spread, dmgDropStart, dmgDropEnd,
				dmgMin, penetration, blockdamage, firePos);
	}

	public GenericProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		this(TGEntities.GENERIC_PROJECTILE.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
	}

	public GenericProjectile(EntityType<? extends GenericProjectile> type, Level level, double posX, double posY, double posZ, float yaw, float pitch,
			float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration,
			boolean blockdamage, EnumBulletFirePos firePos) {
		this(type, level);
		this.shooter = null;
		this.initProjectile(posX, posY, posZ, yaw, pitch, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
	}

	protected void initProjectile(double posX, double posY, double posZ, float yaw, float pitch, float damage, float speed, int TTL, float spread,
			float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {

		float offsetSide = 0.16F;
		float offsetHeight = 0f;
		if (this.shooter instanceof INPCTechgunsShooter tgshooter) {
			offsetSide += tgshooter.getBulletOffsetSide();
			offsetHeight += tgshooter.getBulletOffsetHeight();
		}

		float yRot = yaw + (float) (spread - (2 * Math.random() * spread)) * 40.0f;
		float xRot = pitch + (float) (spread - (2 * Math.random() * spread)) * 40.0f;
		this.moveTo(posX, posY, posZ, yRot, xRot);

		double x = posX, y = posY, z = posZ;
		if (firePos == EnumBulletFirePos.RIGHT) {
			x -= Mth.cos(yRot / 180.0F * (float) Math.PI) * offsetSide;
			z -= Mth.sin(yRot / 180.0F * (float) Math.PI) * offsetSide;
		} else if (firePos == EnumBulletFirePos.LEFT) {
			x += Mth.cos(yRot / 180.0F * (float) Math.PI) * offsetSide;
			z += Mth.sin(yRot / 180.0F * (float) Math.PI) * offsetSide;
		}
		y += (-0.10000000149011612D + offsetHeight);
		this.setPos(x, y, z);

		float f = 0.4F;
		double mx = -Mth.sin(yRot / 180.0F * (float) Math.PI) * Mth.cos(xRot / 180.0F * (float) Math.PI) * f;
		double mz = Mth.cos(yRot / 180.0F * (float) Math.PI) * Mth.cos(xRot / 180.0F * (float) Math.PI) * f;
		double my = -Mth.sin(xRot / 180.0F * (float) Math.PI) * f;
		this.shoot(mx, my, mz, 1.5f, 1.0F);

		// legacy quirk: the normalized vector was discarded, motion stays at velocity 1.5 * speed
		this.setDeltaMovement(this.getDeltaMovement().scale(speed));
		this.damage = damage;
		this.speed = speed;
		this.ticksToLive = TTL;
		this.lifetime = TTL;
		this.penetration = penetration;

		this.damageMin = dmgMin;
		this.damageDropStart = dmgDropStart;
		this.damageDropEnd = dmgDropEnd;
		this.blockdamage = blockdamage;
	}

	/** Points the projectile into the x, y, z direction. */
	public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
		Vec3 v = new Vec3(x, y, z).normalize().add(this.random.nextGaussian() * 0.007499999832361937D * inaccuracy,
				this.random.nextGaussian() * 0.007499999832361937D * inaccuracy, this.random.nextGaussian() * 0.007499999832361937D * inaccuracy)
				.scale(velocity);
		this.setDeltaMovement(v);
		double f1 = v.horizontalDistance();
		this.setYRot((float) (Mth.atan2(v.x, v.z) * (180D / Math.PI)));
		this.setXRot((float) (Mth.atan2(v.y, f1) * (180D / Math.PI)));
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		if (!this.posInitialized && !this.level().isClientSide) {
			this.initStartPos();
		}
		super.tick();

		Vec3 motion = this.getDeltaMovement();
		if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
			double f = motion.horizontalDistance();
			this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180D / Math.PI)));
			this.setXRot((float) (Mth.atan2(motion.y, f) * (180D / Math.PI)));
			this.yRotO = this.getYRot();
			this.xRotO = this.getXRot();
		}

		--this.ticksToLive;
		Vec3 start = this.position();
		Vec3 end = start.add(motion);
		HitResult hit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if (hit.getType() != HitResult.Type.MISS) {
			end = hit.getLocation();
		} else {
			hit = null;
		}

		EntityHitResult entityHit = this.findEntityOnPath(start, end);
		if (entityHit != null) {
			hit = entityHit;
		}

		if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player target) {
			if (this.shooter instanceof Player sp && !sp.canHarmPlayer(target)) {
				hit = null;
			}
		}

		if (hit != null && !this.isRemoved()) {
			this.onHit(hit);
		}

		if (this.isRemoved()) {
			return;
		}

		double nx = this.getX() + motion.x;
		double ny = this.getY() + motion.y;
		double nz = this.getZ() + motion.z;
		this.updateRotationFromMotion(motion);

		float f1 = this.inWaterUpdateBehaviour(0.99F);
		motion = motion.scale(f1);
		if (this.gravity != 0) {
			motion = motion.subtract(0, this.gravity, 0);
		}
		this.setDeltaMovement(motion);
		this.setPos(nx, ny, nz);
		this.checkInsideBlocks();

		if (this.ticksToLive <= 0) {
			this.discard();
		}
	}

	protected void updateRotationFromMotion(Vec3 motion) {
		double f4 = motion.horizontalDistance();
		this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180D / Math.PI)));
		this.setXRot((float) (Mth.atan2(motion.y, f4) * (180D / Math.PI)));
		while (this.getXRot() - this.xRotO < -180.0F) this.xRotO -= 360.0F;
		while (this.getXRot() - this.xRotO >= 180.0F) this.xRotO += 360.0F;
		while (this.getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
		while (this.getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
		this.setXRot(Mth.lerp(0.2F, this.xRotO, this.getXRot()));
		this.setYRot(Mth.lerp(0.2F, this.yRotO, this.getYRot()));
	}

	protected float inWaterUpdateBehaviour(float f1) {
		if (this.isInWater()) {
			Vec3 m = this.getDeltaMovement();
			for (int i = 0; i < 4; ++i) {
				this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - m.x * 0.25D, this.getY() - m.y * 0.25D, this.getZ() - m.z * 0.25D, m.x,
						m.y, m.z);
			}
			f1 = 0.85F;
		}
		if (this.isInWaterRainOrBubble()) {
			this.clearFire();
		}
		return f1;
	}

	protected void initStartPos() {
		this.startX = this.getX();
		this.startY = this.getY();
		this.startZ = this.getZ();
		this.posInitialized = true;
	}

	@Nullable
	protected EntityHitResult findEntityOnPath(Vec3 start, Vec3 end) {
		Entity entity = null;
		Vec3 hitPos = null;
		double d0 = 0.0D;
		List<Entity> list = this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), BULLET_TARGETS);
		for (Entity e : list) {
			if (e == this.shooter) {
				continue;
			}
			AABB box = e.getBoundingBox().inflate(0.30000001192092896D);
			Optional<Vec3> clip = box.clip(start, end);
			if (clip.isPresent()) {
				double d1 = start.distanceToSqr(clip.get());
				if (d1 < d0 || d0 == 0.0D) {
					entity = e;
					d0 = d1;
					hitPos = clip.get();
				}
			}
		}
		return entity != null ? new EntityHitResult(entity, hitPos) : null;
	}

	/** Called when the bullet hits a block or an entity. */
	protected void onHit(HitResult hit) {
		if (hit instanceof EntityHitResult ehr) {
			if (!this.level().isClientSide) {
				Entity target = ehr.getEntity();
				TGDamageSource src = getProjectileDamageSource();
				if (target instanceof LivingEntity ent) {
					float dmg = DamageSystem.getDamageFactor(this.shooter, ent) * this.getDamage();
					if (dmg > 0.0f) {
						if (src.knockbackMultiplier == 0.0f) {
							TGDamageSource knockback = TGDamageSource.getKnockbackDummyDmgSrc(this, this.shooter);
							ent.hurt(knockback, 0.01f);
						}
						if (ent.hurt(src, dmg)) {
							this.onHitEffect(ent, ehr);
						}
					}
				} else {
					target.hurt(src, this.getDamage());
				}
			}
			if (this.discardOnHit) this.discard();
		} else if (hit instanceof BlockHitResult bhr) {
			if (!this.level().isClientSide) {
				// like an arrow landing; the shooter is the source, so a Warden goes for them (vanilla uses the arrow's owner)
				Entity source = this.shooter != null ? this.shooter : this;
				this.level().gameEvent(net.minecraft.world.level.gameevent.GameEvent.PROJECTILE_LAND, bhr.getBlockPos(),
						net.minecraft.world.level.gameevent.GameEvent.Context.of(source, this.level().getBlockState(bhr.getBlockPos())));
			}
			this.hitBlock(bhr);
			if (this.discardOnHit) this.discard();
		}
	}

	public void shiftForward(float factor) {
		Vec3 m = this.getDeltaMovement();
		this.setPos(this.getX() + m.x * factor, this.getY() + m.y * factor, this.getZ() + m.z * factor);
	}

	/** Override in subclass for entity hit effect (potion effects etc). */
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
	}

	/** Override for block hit effect. */
	protected void hitBlock(BlockHitResult hit) {
		BlockPos targetPos = hit.getBlockPos();
		BlockState target = this.level().getBlockState(targetPos);
		SoundType sound = target.getSoundType(this.level(), targetPos, this);
		this.doImpactEffects(target, hit, sound);
	}

	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		Vec3 loc = hit.getLocation();
		float pitch = 0.0f;
		float yaw = 0.0f;
		if (hit instanceof BlockHitResult bhr) {
			if (bhr.getDirection() == Direction.UP) {
				pitch = -90.0f;
			} else if (bhr.getDirection() == Direction.DOWN) {
				pitch = 90.0f;
			} else {
				yaw = bhr.getDirection().toYRot();
			}
		} else {
			pitch = -this.getXRot();
			yaw = -this.getYRot();
		}
		this.sendImpactFX(loc.x, loc.y, loc.z, pitch, yaw, getImpactType(sound));
	}

	public static int getImpactType(SoundType sound) {
		if (sound == SoundType.STONE || sound == SoundType.DEEPSLATE || sound == SoundType.TUFF || sound == SoundType.NETHERRACK) {
			return 0;
		} else if (sound == SoundType.WOOD || sound == SoundType.LADDER || sound == SoundType.CHERRY_WOOD || sound == SoundType.BAMBOO_WOOD
				|| sound == SoundType.NETHER_WOOD) {
			return 1;
		} else if (sound == SoundType.GLASS) {
			return 2;
		} else if (sound == SoundType.METAL || sound == SoundType.ANVIL || sound == SoundType.COPPER || sound == SoundType.CHAIN) {
			return 3;
		} else if (sound == SoundType.GRAVEL || sound == SoundType.SAND || sound == SoundType.GRASS || sound == SoundType.ROOTED_DIRT
				|| sound == SoundType.MUD) {
			return 4;
		}
		return -1;
	}

	/** use the "Impact_Bullet*_Blue" FX variants */
	protected boolean hasBlueImpactFX() {
		return false;
	}

	protected void sendImpactFX(double x, double y, double z, float pitch, float yaw, int type) {
		sendImpactFX(x, y, z, pitch, yaw, type, false);
	}

	protected void sendImpactFX(double x, double y, double z, float pitch, float yaw, int type, boolean incendiary) {
		if (this.level() instanceof ServerLevel sl) {
			PacketDistributor.sendToPlayersNear(sl, null, x, y, z, TGEntities.BULLET_TRACK_RANGE,
					new PacketGunImpactFX((short) type, x, y, z, pitch, yaw, incendiary, this.hasBlueImpactFX()));
		}
	}

	/** Override to change damage type etc. */
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeBulletDamage(this, this.shooter, DeathType.GORE);
		src.armorPenetration = this.penetration;
		src.setNoKnockback();
		return src;
	}

	protected double getDistanceTravelled() {
		return new Vec3(this.startX, this.startY, this.startZ).distanceTo(this.position());
	}

	public GenericProjectile setSilenced() {
		this.silenced = true;
		return this;
	}

	/** Damage for the travelled distance. */
	protected float getDamage() {
		if (this.damageDropEnd == 0f) {
			return this.damage;
		}
		double distance = this.getDistanceTravelled();
		if (distance <= this.damageDropStart) {
			return this.damage;
		} else if (distance > this.damageDropEnd) {
			return this.damageMin;
		} else {
			float factor = 1.0f - (float) ((distance - this.damageDropStart) / (this.damageDropEnd - this.damageDropStart));
			return (this.damageMin + (this.damage - this.damageMin) * factor);
		}
	}

	@Nullable
	public LivingEntity getShooter() {
		return shooter;
	}

	public int getLifetime() {
		return lifetime;
	}

	public int getTicksToLive() {
		return ticksToLive;
	}

	public void setGravity(double gravity) {
		this.gravity = gravity;
	}

	public void setRadiusAndGravity(float radius, double gravity) {
		this.radius = radius;
		this.gravity = gravity;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double d0 = this.getBoundingBox().getSize() * 10.0D;
		if (Double.isNaN(d0)) {
			d0 = 1.0D;
		}
		d0 *= 64.0D * getViewScale();
		return distance < d0 * d0;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tags) {
		this.damage = tags.getFloat("damage");
		this.speed = tags.getFloat("speed");
		this.ticksToLive = tags.getShort("ticksToLive");
		this.lifetime = tags.getShort("lifetime");
		this.damageDropStart = tags.getShort("damageDropStart");
		this.damageDropEnd = tags.getShort("damageDropEnd");
		this.damageMin = tags.getFloat("damageMin");
		this.gravity = tags.getFloat("gravity");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tags) {
		tags.putFloat("damage", this.damage);
		tags.putFloat("speed", this.speed);
		tags.putShort("ticksToLive", (short) this.ticksToLive);
		tags.putShort("lifetime", (short) this.lifetime);
		tags.putShort("damageDropStart", (short) this.damageDropStart);
		tags.putShort("damageDropEnd", (short) this.damageDropEnd);
		tags.putFloat("damageMin", this.damageMin);
		if (this.gravity != 0d) {
			tags.putFloat("gravity", (float) this.gravity);
		}
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		Vec3 m = this.getDeltaMovement();
		buffer.writeFloat(this.speed);
		buffer.writeDouble(m.x);
		buffer.writeDouble(m.y);
		buffer.writeDouble(m.z);
		buffer.writeShort(this.ticksToLive);
		buffer.writeFloat((float) this.gravity);
		buffer.writeInt(this.shooter != null ? this.shooter.getId() : -1);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		this.speed = buffer.readFloat();
		this.setDeltaMovement(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		this.ticksToLive = buffer.readShort();
		this.lifetime = this.ticksToLive;
		this.gravity = buffer.readFloat();
		int id = buffer.readInt();
		if (id >= 0 && this.level().getEntity(id) instanceof LivingEntity le) {
			this.shooter = le;
		}
		this.updateRotationFromMotion(this.getDeltaMovement());
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
	}

	@Override
	public void onAddedToLevel() {
		super.onAddedToLevel();
		if (this.level().isClientSide) {
			this.onClientSpawn();
		}
	}

	/** Client: called when added to the client level (spawn data was read). Used for trail FX. */
	protected void onClientSpawn() {
	}

	/** Client: the server only sends position corrections; keep simulating locally. */
	@Override
	public void lerpMotion(double x, double y, double z) {
	}

	public static void burnBlocks(Level level, BlockHitResult hit, double chanceToIgnite) {
		if (level.isClientSide) {
			return;
		}
		if (Math.random() <= chanceToIgnite) {
			BlockPos pos = hit.getBlockPos();
			BlockPos p = pos.relative(hit.getDirection());
			if (hit.getDirection() == Direction.UP && level.getBlockState(pos).is(Blocks.FARMLAND)) {
				level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
			}
			if (level.isEmptyBlock(p)) {
				level.setBlockAndUpdate(p, Blocks.FIRE.defaultBlockState());
			}
		}
	}

	public static class Factory implements IProjectileFactory<GenericProjectile> {
		@Override
		public GenericProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			GenericProjectile proj = new GenericProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration,
					blockdamage, firePos);
			proj.setRadiusAndGravity(radius, gravity);
			return proj;
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.PROJECTILE;
		}
	}
}
