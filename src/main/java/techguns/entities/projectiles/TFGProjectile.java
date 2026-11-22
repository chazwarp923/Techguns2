package techguns.entities.projectiles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGExplosion;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IChargedProjectileFactory;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;
import techguns.util.TGFXHooks;
import techguns.client.audio.TGSoundCategory;

/** TFG plasma ball; size grows with charge, explodes on impact. */
public class TFGProjectile extends GenericProjectile {

	public float size = 1.0f;

	public TFGProjectile(EntityType<? extends TFGProjectile> type, Level level) {
		super(type, level);
	}

	public TFGProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, double gravity, float size) {
		super(TGEntities.TFG.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
		this.size = size;
		this.gravity = gravity;
	}

	@Override
	protected void onClientSpawn() {
		TGFXHooks.createFXOnEntity("TFGTrail", this, this.size * 0.75f);
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		this.explode();
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		this.explode();
	}

	protected void explode() {
		if (!this.level().isClientSide) {
			float expDmgMax = this.damage * 0.66f * size;
			float expDmgMin = this.damage * 0.33f * size;
			float expR1 = size * 1.0f;
			float expR2 = size * 2.0f;
			TGPackets.sendToAllAround(new PacketSpawnParticle("TFGExplosion", this.getX(), this.getY(), this.getZ(), size * 0.75f), this, 100.0);
			TGExplosion explosion = new TGExplosion(this.level(), this.shooter, this, getX(), getY(), getZ(), expDmgMax, expDmgMin, expR1, expR2,
					this.blockdamage ? 0.5 : 0.0);
			explosion.blockDropChance = 0.1f;
			explosion.doExplosion(false);
			this.level().playSound(null, getX(), getY(), getZ(), TGSounds.TFG_EXPLOSION.get(), TGSoundCategory.EXPLOISON.getVanillaCategory(), 5.0f,
					1.0f);
			if (this.size > 3.0f) {
				this.level().playSound(null, getX(), getY(), getZ(), TGSounds.TFG_EXPLOSION_ECHO.get(), TGSoundCategory.EXPLOISON.getVanillaCategory(),
						10.0f, 1.0f);
			}
		}
		// 1.12 also created an Albedo light pulse on the client; not ported.
		this.discard();
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeFloat(this.size);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		this.size = buffer.readFloat();
	}

	public static class Factory implements IChargedProjectileFactory<TFGProjectile> {
		@Override
		public TFGProjectile createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL, float spread,
				float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius,
				double gravity) {
			return this.createChargedProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
					firePos, radius, gravity, 0f, 1);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.ENERGY;
		}

		@Override
		public TFGProjectile createChargedProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
				float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius, double gravity,
				float charge, int ammoConsumed) {
			TFGProjectile proj = new TFGProjectile(level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage,
					firePos, gravity, ammoConsumed);
			proj.size = 1.0f + (charge * 3.0f);
			if (charge >= 1.0f) {
				proj.size += 1.0f;
			}
			return proj;
		}
	}
}
