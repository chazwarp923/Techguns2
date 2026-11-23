package techguns.items.guns;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import techguns.capabilities.TGExtendedPlayer;
import techguns.entities.projectiles.GenericProjectile;

/**
 * Grappling hook (Fabric port GrapplingHook): holding use fires the hook, which pulls while use is held
 * ({@link techguns.entities.projectiles.GrapplingHookProjectile}). Letting go fires nothing and takes no more ammo;
 * the hook goes away.
 */
public class GrapplingHook extends GenericGunCharge {

	public GrapplingHook(ChargedProjectileSelector<? extends GenericProjectile> projectile_selector, boolean semiAuto, int minFiretime, int clipsize,
			int reloadtime, float damage, Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy,
			float fullChargeTime, int ammoConsumedOnFullCharge) {
		super(projectile_selector, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy, fullChargeTime,
				ammoConsumedOnFullCharge);
	}

	/** starting to hold use fires the hook; use() has just set the fire delay, which shootGunPrimary sets again */
	@Override
	protected void startCharge(ItemStack item, Level level, Player player) {
		TGExtendedPlayer.get(player).setFireDelay(player.getUsedItemHand(), 0);
		super.shootGunPrimary(item, level, player, false, player.getUsedItemHand(), null);
	}

	@Override
	public void releaseUsing(ItemStack item, Level level, LivingEntity entity, int timeLeft) {
		if (entity instanceof Player player) {
			TGExtendedPlayer.get(player).isChargingWeapon = false;
		}
	}

	@Override
	public boolean hasRightClickAction() {
		return true;
	}
}
