package techguns.items.guns;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGSounds;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.audio.TGSoundCategory;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.GuidedMissileProjectile;
import techguns.util.SoundUtil;

/**
 * Hold right mouse to lock on (client side trace into TGExtendedPlayer.lockOnEntity / lockOnTicks),
 * left click fires; the locked target is sent to the server with PacketShootGun.
 */
public class GuidedMissileLauncher extends GenericGunCharge {

	public static final float LOCK_RANGE = 150.0f;
	public static final float LOCK_ERROR_THRESHOLD = 0.5f;

	public GuidedMissileLauncher(ChargedProjectileSelector<? extends GenericProjectile> sel, boolean semiAuto, int minFiretime, int clipsize,
			int reloadtime, float damage, Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy,
			float fullChargeTime, int ammoConsumedOnFullCharge) {
		super(sel, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy, fullChargeTime,
				ammoConsumedOnFullCharge);
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
		super.onUseTick(level, entity, stack, count);
		if (level.isClientSide && entity instanceof Player player) {
			TGExtendedPlayer epc = TGExtendedPlayer.get(player);
			traceTarget(player, epc);
			if (epc.lockOnTicks >= this.lockOnTicks) {
				if (count % 4 == 0) beep(level, player, TGSounds.LOCKED_BEEP.get(), 1.0f);
			} else if (epc.lockOnTicks > 0) {
				if (count % 4 == 0) beep(level, player, TGSounds.LOCKON_BEEP.get(), 1.0f);
			} else if (count % 12 == 0) {
				beep(level, player, TGSounds.LOCKON_BEEP.get(), 0.5f);
			}
		}
	}

	private static void beep(Level level, Player player, SoundEvent s, float pitch) {
		SoundUtil.playSoundOnEntityGunPosition(level, player, s, SOUND_DISTANCE, pitch, false, false, true, TGSoundCategory.PLAYER_EFFECT);
	}

	protected void traceTarget(Player shooter, TGExtendedPlayer epc) {
		Vec3 start = shooter.getEyePosition();
		Vec3 end = start.add(shooter.getLookAngle().scale(LOCK_RANGE));
		HitResult blockHit = shooter.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
		}
		Entity hit = this.findEntityOnPath(shooter, start, end, epc.lockOnEntity);
		if (hit instanceof Player p && !shooter.canHarmPlayer(p)) {
			hit = null;
		}

		if (hit != null) {
			if (epc.lockOnEntity != null && epc.lockOnEntity.isAlive()) {
				if (epc.lockOnEntity == hit) {
					epc.lockOnTicks++;
					if (epc.lockOnTicks >= this.lockOnTicks) {
						epc.lockOnTicks = this.lockOnTicks + this.lockOnPersistTicks;
					}
				} else if (epc.lockOnTicks > 0) {
					epc.lockOnTicks--;
				} else {
					epc.lockOnEntity = null;
					epc.lockOnTicks = 0;
				}
			} else {
				epc.lockOnEntity = hit;
				epc.lockOnTicks = 1;
			}
		} else if (epc.lockOnEntity != null && !epc.lockOnEntity.isAlive()) {
			epc.lockOnEntity = null;
			epc.lockOnTicks = 0;
		} else if (epc.lockOnTicks > 0) {
			epc.lockOnTicks--;
		} else {
			epc.lockOnEntity = null;
		}
	}

	/** closest entity on the ray (boxes grown by LOCK_ERROR_THRESHOLD); the previous target wins if it is on the ray */
	@Nullable
	protected Entity findEntityOnPath(LivingEntity shooter, Vec3 start, Vec3 end, @Nullable Entity prevTarget) {
		Entity best = null;
		double bestDist = Double.MAX_VALUE;
		AABB area = new AABB(start, end).inflate(1.0D);
		for (Entity e : shooter.level().getEntities(shooter, area, GenericProjectile.BULLET_TARGETS)) {
			var clip = e.getBoundingBox().inflate(LOCK_ERROR_THRESHOLD).clip(start, end);
			if (clip.isPresent()) {
				if (e == prevTarget) {
					return e;
				}
				double d = start.distanceToSqr(clip.get());
				if (d < bestDist) {
					best = e;
					bestDist = d;
				}
			}
		}
		return best;
	}

	@Override
	protected void spawnProjectile(Level level, LivingEntity player, ItemStack itemstack, float spread, float offset, float damagebonus,
			EnumBulletFirePos firePos, @Nullable Entity target) {
		IProjectileFactory<? extends GenericProjectile> projectile = this.projectile_selector.getFactoryForType(this.getCurrentAmmoVariantKey(itemstack));
		GenericProjectile proj = projectile.createProjectile(this, level, player, damage * damagebonus, speed, this.getScaledTTL(), spread,
				this.damageDropStart, this.damageDropEnd, this.damageMin * damagebonus, this.penetration, getDoBlockDamage(player), firePos, radius,
				gravity);
		if (silenced) {
			proj.setSilenced();
		}
		if (offset > 0.0f) {
			proj.shiftForward(offset / speed);
		}
		if (proj instanceof GuidedMissileProjectile gm && target != null) {
			gm.target = target;
			gm.setGravity(0.0);
		}
		level.addFreshEntity(proj);
	}

	@Override
	public int consumeAmmoCharge(ItemStack item, float f, boolean creative) {
		return 0;
	}
}
