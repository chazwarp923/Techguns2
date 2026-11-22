package techguns.entities.projectiles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.items.guns.ammo.DamageModifier;

public class GenericProjectileExplosive extends GenericProjectile {

	public GenericProjectileExplosive(EntityType<? extends GenericProjectileExplosive> type, Level level) {
		super(type, level);
	}

	public GenericProjectileExplosive(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.GENERIC_PROJECTILE_EXPLOSIVE.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration,
				blockdamage, firePos);
	}

	@Override
	protected TGDamageSource getProjectileDamageSource() {
		TGDamageSource src = TGDamageSource.causeExplosionDamage(this, this.shooter, DeathType.GORE);
		src.armorPenetration = this.penetration;
		src.goreChance = 1.0f;
		src.knockbackMultiplier = 3.0f;
		return src;
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		this.explode(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		this.explode(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
	}

	protected void explode(double x, double y, double z) {
		if (this.level() instanceof ServerLevel sl) {
			techguns.packets.TGPackets.sendToAllAround(new techguns.packets.PacketSpawnParticle("MiningChargeBlockExplosion", x, y, z), sl, x, y, z, 100.0);
			sl.explode(this, x, y, z, 1.5f, false, this.blockdamage ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
		}
		this.discard();
	}

	public static class Factory implements IProjectileFactory<GenericProjectile> {
		protected DamageModifier mod = new DamageModifier().setDmg(1.15f, 0f);

		@Override
		public DamageModifier getDamageModifier() {
			return mod;
		}

		@Override
		public GenericProjectileExplosive createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL,
				float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos,
				float radius, double gravity) {
			return new GenericProjectileExplosive(level, p, mod.getDamage(damage), speed, TTL, spread, dmgDropStart, dmgDropEnd, mod.getDamage(dmgMin),
					penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.EXPLOSION;
		}
	}
}
