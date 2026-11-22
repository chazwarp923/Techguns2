package techguns.entities.projectiles;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.util.MathUtil;
import techguns.util.TGFXHooks;

/** Nuclear death ray: a beam that follows the shooter's aim and damages every tick. */
public class NDRProjectile extends AbstractBeamProjectile {

	public static final int BEAM_LIFETIME = 10;

	public NDRProjectile(EntityType<? extends NDRProjectile> type, Level level) {
		super(type, level);
	}

	public NDRProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.NDR.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
	}

	@Override
	protected void onClientSpawn() {
		TGFXHooks.createFXOnEntity("BeamGunMuzzleFX", this);
	}

	@Override
	public void tick() {
		LivingEntity shooter = this.shooter;
		if (shooter != null) {
			float f = 100.0F;
			double dx = -Mth.sin(shooter.getYHeadRot() / 180.0F * (float) Math.PI) * Mth.cos(shooter.getXRot() / 180.0F * (float) Math.PI) * f;
			double dz = Mth.cos(shooter.getYHeadRot() / 180.0F * (float) Math.PI) * Mth.cos(shooter.getXRot() / 180.0F * (float) Math.PI) * f;
			double dy = -Mth.sin((shooter.getXRot()) / 180.0F * (float) Math.PI) * f;
			double x = shooter.getX();
			double y = shooter.getY();
			double z = shooter.getZ();
			Vec3 motion = new Vec3(dx, dy, dz).normalize();
			this.setDeltaMovement(motion.scale(speed));
			if (TGFXHooks.isClientPlayerAndIn1stPerson(shooter)) {
				Vec3 offset = this.getFPOffset();
				offset = offset.xRot((float) (shooter.getXRot() * MathUtil.D2R)).yRot((float) ((-90.0 - shooter.getYHeadRot()) * MathUtil.D2R));
				x += offset.x;
				y += offset.y + shooter.getEyeHeight();
				z += offset.z;
			} else {
				x -= Mth.cos(shooter.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
				y += shooter.getEyeHeight() - 0.10000000149011612D + this.get3PYOffset();
				z -= Mth.sin(shooter.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
			}
			this.setPos(x, y, z);
			this.setXRot(shooter.getXRot());
			this.setYRot(shooter.getYHeadRot());
		}
		this.distance = -1;
		trace();

		--this.ticksToLive;
		if (this.ticksToLive <= 0) {
			this.discard();
		}
	}

	@Override
	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		Vec3 hitVec = hit.getLocation();
		TGFXHooks.createFX("BeamGunImpactFX", this.level(), hitVec.x, hitVec.y, hitVec.z, 0f, 0f, 0f);
	}

	public Vec3 getFPOffset() {
		return new Vec3(0, -0.08, 0.12);
	}

	public float get3PYOffset() {
		return 0.0f;
	}

	public float getBeam3PYOffset() {
		return 0.0f;
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		super.onHitEffect(ent, hit);
		if (techguns.radiation.TGRadiationSystem.isEnabled()) {
			techguns.radiation.TGRadiationSystem.addRadiation(ent, 40, 4);
		}
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeRadiationDamage(this, this.shooter, DeathType.LASER);
		src.armorPenetration = this.penetration;
		src.setNoKnockback();
		return src;
	}

	public static class Factory implements IProjectileFactory<NDRProjectile> {
		@Override
		public NDRProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new NDRProjectile(level, p, damage, speed, BEAM_LIFETIME, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.RADIATION;
		}
	}
}
