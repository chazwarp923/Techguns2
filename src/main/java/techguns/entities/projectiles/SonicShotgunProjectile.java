package techguns.entities.projectiles;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import techguns.TGDamageTypes;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;

/** Sonic shotgun ring projectile; all projectiles of one shot share entitiesHit so a target is hit once. */
public class SonicShotgunProjectile extends GenericProjectile {

	public List<Entity> entitiesHit;
	public boolean mainProjectile = false;

	public SonicShotgunProjectile(EntityType<? extends SonicShotgunProjectile> type, Level level) {
		super(type, level);
	}

	public SonicShotgunProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.SONIC_SHOTGUN.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
				firePos);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = new TGDamageSource(this.level(), TGDamageTypes.SONIC, this, shooter, DamageType.PHYSICAL, DeathType.GORE);
		src.ignoreHurtresistTime = true;
		src.armorPenetration = this.penetration;
		src.setKnockback(2.0f);
		src.goreChance = 1.0f;
		return src;
	}

	@Override
	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		if (mainProjectile && !this.level().isClientSide) {
			TGPackets.sendToAllAround(new PacketSpawnParticle("SonicShotgunImpact", this.getX(), this.getY(), this.getZ()), this, 50.0);
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		if (hit instanceof EntityHitResult ehr) {
			Entity target = ehr.getEntity();
			if (!this.level().isClientSide && entitiesHit != null && !entitiesHit.contains(target)) {
				TGDamageSource src = getProjectileDamageSource();
				if (target instanceof LivingEntity ent) {
					float dmg = DamageSystem.getDamageFactor(this.shooter, ent);
					if (dmg > 0) {
						ent.hurt(TGDamageSource.getKnockbackDummyDmgSrc(this, this.shooter), 0.01f);
						if (ent.hurt(src, this.getDamage() * dmg)) {
							this.onHitEffect(ent, ehr);
						}
					}
				} else {
					target.hurt(src, this.getDamage());
				}
				entitiesHit.add(target);
			}
		} else if (hit instanceof BlockHitResult bhr) {
			this.hitBlock(bhr);
			this.discard();
		}
	}

	@Override
	protected void onClientSpawn() {
		if (mainProjectile) {
			techguns.util.TGFXHooks.createFXOnEntity("SonicShotgunTrail", this);
		}
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeBoolean(mainProjectile);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		this.mainProjectile = buffer.readBoolean();
	}

	public static class Factory implements IProjectileFactory<SonicShotgunProjectile> {
		@Override
		public SonicShotgunProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return new SonicShotgunProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.PHYSICAL;
		}
	}
}
