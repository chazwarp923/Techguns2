package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.PacketSpawnParticleOnEntity;
import techguns.packets.TGPackets;
import techguns.util.TGFXHooks;

/** Gauss rifle slug: advanced bullet with blue trail and impact FX. */
public class GaussProjectile extends AdvancedBulletProjectile {

	public GaussProjectile(EntityType<? extends GaussProjectile> type, Level level) {
		super(type, level);
	}

	public GaussProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(TGEntities.GAUSS.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
	}

	@Override
	protected void onClientSpawn() {
		TGFXHooks.createFXOnEntity("GaussProjectileTrail", this);
	}

	private void sendGaussImpact(Vec3 v) {
		if (!this.level().isClientSide) {
			TGPackets.sendToAllAround(new PacketSpawnParticle("GaussRifleImpact_Block", v.x, v.y, v.z), this.level(), v.x, v.y, v.z, 50.0);
		}
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		super.onHitEffect(ent, hit);
		sendGaussImpact(hit.getLocation());
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		super.hitBlock(hit);
		sendGaussImpact(hit.getLocation());
	}

	public static class Factory implements IProjectileFactory<GaussProjectile> {
		@Override
		public GaussProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			float offsetX = 0.0f;
			if (firePos == EnumBulletFirePos.RIGHT) offsetX = -0.14f;
			else if (firePos == EnumBulletFirePos.LEFT) offsetX = 0.14f;
			TGPackets.sendToAllAround(new PacketSpawnParticleOnEntity("GaussFireFX", p, offsetX, -0.09f, 0.5f, true), p, 25.0);
			return new GaussProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.PROJECTILE;
		}
	}
}
