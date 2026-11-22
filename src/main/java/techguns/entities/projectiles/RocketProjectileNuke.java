package techguns.entities.projectiles;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGExplosion;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.items.guns.ammo.DamageModifier;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;

public class RocketProjectileNuke extends RocketProjectile {

	public RocketProjectileNuke(EntityType<? extends RocketProjectileNuke> type, Level level) {
		super(type, level);
	}

	public RocketProjectileNuke(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius, double gravity) {
		super(TGEntities.ROCKET_NUKE.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos,
				radius, gravity);
	}

	@Override
	protected void explodeRocket() {
		if (!this.level().isClientSide) {
			TGPackets.sendToAllAround(new PacketSpawnParticle("NukeExplosion", this.getX(), this.getY(), this.getZ()), this, 150.0);
			TGExplosion explosion = new TGExplosion(this.level(), this.shooter, this, getX(), getY(), getZ(), this.damage, this.damageMin,
					this.damageDropStart, this.damageDropEnd, this.blockdamage ? 0.5 : 0.0);
			explosion.blockDropChance = 0.05f;
			explosion.setDmgSrc(getProjectileDamageSource());
			explosion.doExplosion(false);
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(), TGSounds.NUKE_EXPLOSION.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
			if (techguns.radiation.TGRadiationSystem.isEnabled()) {
				this.level().addFreshEntity(new techguns.entities.special.EntityRadiation(this.level(), getX(), getY(), getZ(), 20 * 60, this.damageDropStart, 9,
						this.damageDropEnd, 2));
			}
		}
		this.discard();
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource dmgsrc = TGDamageSource.causeExplosionDamage(this, this.shooter, DeathType.LASER);
		dmgsrc.goreChance = 1.0f;
		dmgsrc.knockbackMultiplier = 3.0f;
		return dmgsrc;
	}

	public static class Factory implements IProjectileFactory<RocketProjectileNuke> {
		protected DamageModifier mod = new DamageModifier().setDmg(5f, 0f).setRadius(5f, 0f).setRange(5f, 0f);

		@Override
		public DamageModifier getDamageModifier() {
			return mod;
		}

		@Override
		public RocketProjectileNuke createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new RocketProjectileNuke(level, p, mod.getDamage(damage), speed, TTL, spread, mod.getRange(dmgDropStart), mod.getRange(dmgDropEnd),
					mod.getDamage(dmgMin), penetration, blockdamage, firePos, mod.getRadius(radius), gravity);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.EXPLOSION;
		}
	}
}
