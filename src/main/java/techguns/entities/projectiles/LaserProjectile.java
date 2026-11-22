package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;

/** Laser rifle / laser pistol beam. */
public class LaserProjectile extends AbstractBeamProjectile {

	public LaserProjectile(EntityType<? extends LaserProjectile> type, Level level) {
		super(type, level);
	}

	public LaserProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.LASER.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		trace();
	}

	@Override
	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		if (!this.level().isClientSide) {
			Vec3 hitVec = hit.getLocation();
			TGPackets.sendToAllAround(new PacketSpawnParticle("LaserGunImpact", hitVec.x, hitVec.y, hitVec.z), this.level(), hitVec.x, hitVec.y,
					hitVec.z, 35.0);
		}
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeEnergyDamage(this, this.shooter, DeathType.LASER);
		src.goreChance = 1.0f;
		src.armorPenetration = this.penetration;
		src.setNoKnockback();
		return src;
	}

	public static class Factory implements IProjectileFactory<LaserProjectile> {
		@Override
		public LaserProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new LaserProjectile(level, p, damage, speed, 7, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.ENERGY;
		}
	}
}
