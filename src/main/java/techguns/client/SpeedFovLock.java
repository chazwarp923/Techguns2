package techguns.client;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import techguns.TGConfig;

/**
 * The speed FOV lock (1.12 TGEventHandler.handleFovEvent, config client.LockSpeedDependantFov), used by
 * TGClientEvents.onComputeFov. No client-only classes, so game tests can check it.
 */
public final class SpeedFovLock {
	private SpeedFovLock() {
	}

	/**
	 * The factor that undoes vanilla's speed term ((speed / walkSpeed + 1) / 2) and applies the fixed sprint
	 * multiplier instead, or 1 when the lock is off or vanilla skipped the speed term itself.
	 */
	public static float factor(Player ply) {
		if (!TGConfig.cl_lockSpeedFov) {
			return 1.0f;
		}
		float walk = ply.getAbilities().getWalkingSpeed();
		float speedTerm = ((float) ply.getAttributeValue(Attributes.MOVEMENT_SPEED) / walk + 1.0f) / 2.0f;
		if (walk == 0.0f || !Float.isFinite(speedTerm) || speedTerm <= 0.0f) {
			return 1.0f;
		}
		float f = 1.0f / speedTerm;
		if (ply.isSprinting()) {
			f *= TGConfig.cl_fixedSprintFov;
		}
		return f;
	}
}
