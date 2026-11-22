package techguns.entities.projectiles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import techguns.TGEntities;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.IProjectileFactory;
import techguns.items.guns.ammo.DamageModifier;

public class GenericProjectileIncendiary extends GenericProjectile {

	protected boolean showFireTrail = false;

	public GenericProjectileIncendiary(EntityType<? extends GenericProjectileIncendiary> type, Level level) {
		super(type, level);
	}

	public GenericProjectileIncendiary(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart,
			float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, boolean fireTrail) {
		super(TGEntities.GENERIC_PROJECTILE_INCENDIARY.get(), level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration,
				blockdamage, firePos);
		this.showFireTrail = fireTrail;
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
		super.onHitEffect(ent, hit);
		if (!ent.fireImmune()) {
			ent.igniteForSeconds(3);
		}
	}

	@Override
	protected void hitBlock(BlockHitResult hit) {
		if (this.blockdamage) {
			burnBlocks(this.level(), hit, damage / 40f);
		}
		super.hitBlock(hit);
	}

	@Override
	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		var loc = hit.getLocation();
		float pitch = 0.0f;
		float yaw = 0.0f;
		if (hit instanceof BlockHitResult bhr) {
			switch (bhr.getDirection()) {
				case UP -> pitch = -90.0f;
				case DOWN -> pitch = 90.0f;
				default -> yaw = bhr.getDirection().toYRot();
			}
		} else {
			pitch = -this.getXRot();
			yaw = -this.getYRot();
		}
		this.sendImpactFX(loc.x, loc.y, loc.z, pitch, yaw, this.showFireTrail ? 5 : getImpactType(sound), true);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeBoolean(showFireTrail);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		this.showFireTrail = buffer.readBoolean();

	}

	@Override
	protected void onClientSpawn() {
		if (showFireTrail) {
			techguns.util.TGFXHooks.createFXOnEntity("IncendiaryShotgunTrail", this);
		}
	}

	public boolean showsFireTrail() {
		return showFireTrail;
	}

	public static class Factory implements IProjectileFactory<GenericProjectile> {
		protected DamageModifier mod = new DamageModifier().setDmg(1.1f, 0f);
		protected boolean fireTrail;

		public Factory(boolean fireTrail) {
			this.fireTrail = fireTrail;
		}

		@Override
		public DamageModifier getDamageModifier() {
			return mod;
		}

		@Override
		public GenericProjectileIncendiary createProjectile(GenericGun gun, Level level, LivingEntity p, float damage, float speed, int TTL,
				float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos,
				float radius, double gravity) {
			return new GenericProjectileIncendiary(level, p, mod.getDamage(damage), speed, TTL, spread, dmgDropStart, dmgDropEnd,
					mod.getDamage(dmgMin), penetration, blockdamage, firePos, this.fireTrail);
		}

		@Override
		public DamageType getDamageType() {
			return DamageType.FIRE;
		}
	}
}
