package techguns.entities.projectiles;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.packets.PacketSpawnParticleOnEntity;
import techguns.packets.TGPackets;
import techguns.util.TGFXHooks;

public class FlamethrowerProjectile extends GenericProjectile {

	float chanceToIgnite = 0.5f;
	int entityIgniteTime = 3;

	public FlamethrowerProjectile(EntityType<? extends FlamethrowerProjectile> type, Level level) {
		super(type, level);
	}

	public FlamethrowerProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, double gravity) {
		super(TGEntities.FLAMETHROWER.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
		this.gravity = gravity;
	}

	@Override
	protected void onClientSpawn() {
		TGFXHooks.createFXOnEntity("FlamethrowerTrail", this);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeFireDamage(this, this.shooter, DeathType.DEFAULT);
		src.armorPenetration = this.penetration;
		src.setNoKnockback();
		return src;
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		ent.igniteForSeconds(this.entityIgniteTime);
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		if (this.blockdamage) {
			burnBlocks(this.level(), hit, chanceToIgnite);
		}
		Vec3 hitVec = hit.getLocation();
		TGFXHooks.createFX("FlamethrowerImpact", this.level(), hitVec.x, hitVec.y, hitVec.z, 0f, 0f, 0f);
	}

	@Override
	protected float inWaterUpdateBehaviour(float f1) {
		if (this.isInWater()) {
			Vec3 m = this.getDeltaMovement();
			for (int i = 0; i < 4; ++i) {
				this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - m.x * 0.25D, this.getY() - m.y * 0.25D, this.getZ() - m.z * 0.25D, m.x, m.y,
						m.z);
			}
			f1 = 0.85F;
		}
		if (this.isInWaterRainOrBubble()) {
			this.discard();
		}
		return f1;
	}

	public static class Factory implements IProjectileFactory<FlamethrowerProjectile> {
		@Override
		public FlamethrowerProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			float offsetX = 0.0f;
			if (firePos == EnumBulletFirePos.RIGHT) offsetX = -0.15f;
			else if (firePos == EnumBulletFirePos.LEFT) offsetX = 0.15f;
			TGPackets.sendToAllAround(new PacketSpawnParticleOnEntity("FlamethrowerFireFX", p, offsetX, -0.05f, 0.5f, true), p, 25.0);
			return new FlamethrowerProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos,
					gravity);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.FIRE;
		}
	}
}
