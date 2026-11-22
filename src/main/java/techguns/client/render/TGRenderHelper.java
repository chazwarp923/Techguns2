package techguns.client.render;

import techguns.client.render.gl.GlStateManager;
import techguns.client.render.gl.TGRenderState;

/** Same API as 1.12.2; maps to {@link TGRenderState} flags (fullbright, blend, additive). */
public class TGRenderHelper {

	public static void enableFXLighting() {
		TGRenderState.setFullbright(true);
	}

	public static void disableFXLighting() {
		TGRenderState.setFullbright(false);
	}

	public static void enableFluidGlow(int luminosity) {
		if (luminosity > 0) {
			TGRenderState.setFullbright(true);
		}
	}

	public static void disableFluidGlow() {
		TGRenderState.setFullbright(false);
	}

	public enum RenderType {
		ALPHA, ADDITIVE, SOLID, ALPHA_SHADED, NO_Z_TEST;
	}

	/** This includes FXLighting! */
	public static void enableBlendMode(RenderType type) {
		if (type != RenderType.SOLID) {
			GlStateManager.enableBlend();
		}
		if (type == RenderType.ADDITIVE || type == RenderType.NO_Z_TEST) {
			TGRenderState.setAdditive(true);
		}
		if (type != RenderType.ALPHA_SHADED) {
			enableFXLighting();
		}
	}

	/** This includes FXLighting! */
	public static void disableBlendMode(RenderType type) {
		if (type != RenderType.ALPHA_SHADED) {
			disableFXLighting();
		}
		if (type != RenderType.SOLID) {
			GlStateManager.disableBlend();
		}
		TGRenderState.setAdditive(false);
	}
}
