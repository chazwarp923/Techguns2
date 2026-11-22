package techguns.entities.projectiles;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGSounds;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;

/** Shared base of the invisible short range "melee" projectiles (power hammer, chainsaw / drill). */
public abstract class MeleeImpactProjectile extends GenericProjectile {

	protected MeleeImpactProjectile(EntityType<? extends MeleeImpactProjectile> type, Level level) {
		super(type, level);
	}

	protected MeleeImpactProjectile(EntityType<? extends MeleeImpactProjectile> type, Level level, LivingEntity p, float damage, float speed, int TTL,
			float spread, float dmgDropStart, float dmgDropEnd, float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos) {
		super(type, level, p, damage, speed, TTL, spread, dmgDropStart, dmgDropEnd, dmgMin, penetration, blockdamage, firePos);
	}

	protected void impactFX(HitResult hit) {
		if (!this.level().isClientSide) {
			Vec3 v = hit.getLocation();
			this.level().playSound(null, v.x, v.y, v.z, TGSounds.POWERHAMMER_IMPACT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
			TGPackets.sendToAllAround(new PacketSpawnParticle("PowerhammerImpact", v.x, v.y, v.z), this, 32.0);
		}
	}

	@Override
	protected void onHitEffect(LivingEntity ent, EntityHitResult hit) {
		impactFX(hit);
	}

	@Override
	protected void doImpactEffects(BlockState state, HitResult hit, SoundType sound) {
		impactFX(hit);
	}
}
