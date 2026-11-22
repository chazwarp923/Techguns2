package techguns.items.guns;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.SonicShotgunProjectile;
import techguns.util.MathUtil;

/** Fires a center projectile plus two rings; all share one hit list so each target is damaged once. */
public class SonicShotgun extends GenericGun {

	public SonicShotgun(ProjectileSelector<SonicShotgunProjectile> projectileSelector, boolean semiAuto, int minFiretime, int clipsize,
			int reloadtime, float damage, Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy) {
		super(projectileSelector, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy);
	}

	private SonicShotgunProjectile create(Level level, LivingEntity player, float damagebonus, EnumBulletFirePos firePos, List<Entity> entitiesHit) {
		SonicShotgunProjectile proj = new SonicShotgunProjectile(level, player, damage * damagebonus, speed, this.ticksToLive, 0f,
				this.damageDropStart, this.damageDropEnd, this.damageMin * damagebonus, this.penetration, getDoBlockDamage(player), firePos);
		proj.entitiesHit = entitiesHit;
		return proj;
	}

	@Override
	protected void spawnProjectile(Level level, LivingEntity player, ItemStack itemstack, float spread, float offset, float damagebonus,
			EnumBulletFirePos firePos, @Nullable Entity target) {
		int count = 5; // base projectile count per ring
		int rings = 2;
		spread = 0.075f;

		List<Entity> entitiesHit = new ArrayList<>();
		SonicShotgunProjectile proj = create(level, player, damagebonus, firePos, entitiesHit);
		proj.mainProjectile = true;
		if (offset > 0.0f) {
			proj.shiftForward(offset);
		}
		level.addFreshEntity(proj);

		for (int j = 1; j <= rings; j++) {
			float angle = (float) ((Math.PI * 2.0) / (double) (count * j));
			for (int i = 0; i < (count * j); i++) {
				proj = create(level, player, damagebonus, firePos, entitiesHit);
				Vec3 dir = new Vec3(1, 0, 0);
				dir = dir.yRot(spread * j);
				dir = dir.xRot(angle * i);
				dir = MathUtil.rotateVec3dAroundZ(dir, (float) (player.getXRot() * MathUtil.D2R));
				dir = dir.yRot((float) ((-player.getYHeadRot() - 90.0) * MathUtil.D2R));
				proj.setDeltaMovement(dir.scale(speed));
				if (offset > 0.0f) {
					proj.shiftForward(offset);
				}
				level.addFreshEntity(proj);
			}
		}
	}
}
