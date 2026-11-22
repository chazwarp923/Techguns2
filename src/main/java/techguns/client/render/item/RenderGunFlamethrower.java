package techguns.client.render.item;

import net.minecraft.client.Minecraft;
import techguns.client.models.ModelMultipart;
import techguns.client.render.fx.IScreenEffect;

/** Flamethrower: small pilot flame at the muzzle while idle (not under water). */
public class RenderGunFlamethrower extends RenderGunBase90 {

	public RenderGunFlamethrower(ModelMultipart model, int parts) {
		super(model, parts);
	}

	private static float pilotFlameProgress() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.player.isUnderWater()) {
			return -1f;
		}
		return (float) (mc.level.getGameTime() % 30) / 30.0f;
	}

	@Override
	protected void drawIdleFx(boolean leftHand) {
		float p = pilotFlameProgress();
		if (p >= 0f) {
			float x = leftHand ? this.muzzleFX_x_l : this.muzzleFX_x_r;
			IScreenEffect.FlamethrowerMuzzleFlame.doRender(p, x, this.muzzleFX_y, this.muzzleFX_z, this.muzzleFX_scale * 0.5f, false);
		}
	}

	@Override
	protected void drawIdleFx3P(boolean leftHand) {
		float p = pilotFlameProgress();
		if (p >= 0f) {
			IScreenEffect.FlamethrowerMuzzleFlame.doRender(p, 0, this.muzzleFX_3p_y, this.muzzleFX_3p_z, this.muzzleFX_scale * 0.5f * this.muzzleFX_3p_scale,
					true);
		}
	}
}
