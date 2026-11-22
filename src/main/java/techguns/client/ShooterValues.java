package techguns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import techguns.TGAttachments;
import techguns.api.capabilities.AttackTime;
import techguns.capabilities.TGExtendedPlayer;

/** Client-side recoil / reload / muzzle flash timing of shooters (players and NPCs). */
public class ShooterValues {

	protected static AttackTime getAttackTimes(LivingEntity ent, boolean offHand) {
		if (ent instanceof Player ply) {
			return TGExtendedPlayer.get(ply).getAttackTime(offHand);
		}
		return ent.getData(TGAttachments.SHOOTER_VALUES);
	}

	public static long getRecoiltime(LivingEntity ent, boolean offHand) {
		return getAttackTimes(ent, offHand).getRecoilTime();
	}

	public static boolean isStillRecoiling(LivingEntity ent, boolean offHand, byte attacktype) {
		Minecraft mc = Minecraft.getInstance();
		if (ent != mc.player) {
			return false;
		} else if (!mc.options.getCameraType().isFirstPerson()) {
			return false;
		}
		AttackTime attack = getAttackTimes(ent, offHand);
		if (attack.getAttackType() != attacktype) {
			return false;
		} else if (attack.getRecoilTime() <= 0) {
			return false;
		}
		return (System.currentTimeMillis() - attack.getRecoilTime()) < 0;
	}

	public static void setRecoiltime(LivingEntity ent, boolean offHand, long time, int total, byte attacktype) {
		setRecoiltime(ent, offHand, time, total, attacktype, 0f);
	}

	public static void setRecoiltime(LivingEntity ent, boolean offHand, long time, int total, byte attacktype, float chargeProgress) {
		AttackTime attack = getAttackTimes(ent, offHand);
		attack.setRecoilTime(time);
		attack.setRecoilTimeTotal(total);
		attack.setAttackType(attacktype);
		attack.setRecoilChargeProgress(chargeProgress);
	}

	public static long getRecoiltimeTotal(LivingEntity ent, boolean offHand) {
		return getAttackTimes(ent, offHand).getRecoilTimeTotal();
	}

	public static long getReloadtime(LivingEntity ent, boolean offHand) {
		return getAttackTimes(ent, offHand).getReloadTime();
	}

	public static byte getAttackType(LivingEntity ent, boolean offHand) {
		return getAttackTimes(ent, offHand).getAttackType();
	}

	public static void setReloadtime(LivingEntity ent, boolean offHand, long time, int total, byte attackType) {
		AttackTime attack = getAttackTimes(ent, offHand);
		attack.setReloadTime(time);
		attack.setReloadTimeTotal(total);
		attack.setAttackType(attackType);
	}

	public static int getReloadtimeTotal(LivingEntity ent, boolean offHand) {
		return getAttackTimes(ent, offHand).getReloadTimeTotal();
	}

	public static boolean getIsCurrentlyUsingGun(LivingEntity ent, boolean offHand) {
		AttackTime attack = getAttackTimes(ent, offHand);
		return attack.isReloading() || attack.isRecoiling();
	}

	public static boolean getPlayerIsReloading(LivingEntity ent, boolean offHand) {
		return getAttackTimes(ent, offHand).isReloading();
	}

	public static void setMuzzleFlashTime(LivingEntity ent, boolean offHand, long time, int total) {
		AttackTime attack = getAttackTimes(ent, offHand);
		attack.setMuzzleFlashTime(time);
		attack.setMuzzleFlashTimeTotal(total);
	}

	public static long getMuzzleFlashTime(LivingEntity ent, boolean offHand) {
		return getAttackTimes(ent, offHand).getMuzzleFlashTime();
	}

	public static int getMuzzleFlashTimeTotal(LivingEntity ent, boolean offHand) {
		return getAttackTimes(ent, offHand).getMuzzleFlashTimeTotal();
	}
}
