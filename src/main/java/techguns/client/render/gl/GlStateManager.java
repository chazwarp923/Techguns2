package techguns.client.render.gl;

import com.mojang.math.Axis;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Drop-in replacement for the 1.12.2 GlStateManager calls used by Techguns render code.
 * Matrix operations go to the current {@link TGRenderState#pose()} PoseStack, colour/texture/blend
 * calls change the state used to pick RenderTypes. Pure GL state calls without a 1.21 equivalent
 * in buffered rendering are no-ops.
 */
public final class GlStateManager {

	private GlStateManager() {
	}

	public static void pushMatrix() {
		TGRenderState.pose().pushPose();
	}

	public static void popMatrix() {
		TGRenderState.pose().popPose();
	}

	public static void translate(float x, float y, float z) {
		TGRenderState.pose().translate(x, y, z);
	}

	public static void translate(double x, double y, double z) {
		TGRenderState.pose().translate(x, y, z);
	}

	public static void scale(float x, float y, float z) {
		TGRenderState.pose().scale(x, y, z);
	}

	public static void scale(double x, double y, double z) {
		TGRenderState.pose().scale((float) x, (float) y, (float) z);
	}

	/** angle in degrees around (x,y,z), like glRotatef */
	public static void rotate(float angle, float x, float y, float z) {
		if (angle == 0f) {
			return;
		}
		if (x == 1f && y == 0f && z == 0f) {
			TGRenderState.pose().mulPose(Axis.XP.rotationDegrees(angle));
		} else if (x == 0f && y == 1f && z == 0f) {
			TGRenderState.pose().mulPose(Axis.YP.rotationDegrees(angle));
		} else if (x == 0f && y == 0f && z == 1f) {
			TGRenderState.pose().mulPose(Axis.ZP.rotationDegrees(angle));
		} else {
			Vector3f axis = new Vector3f(x, y, z);
			if (axis.lengthSquared() == 0f) {
				return;
			}
			axis.normalize();
			TGRenderState.pose().mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(angle), axis));
		}
	}

	public static void color(float r, float g, float b, float a) {
		TGRenderState.color(r, g, b, a);
	}

	public static void color(float r, float g, float b) {
		TGRenderState.color(r, g, b, 1f);
	}

	public static void enableBlend() {
		TGRenderState.setBlend(true);
	}

	public static void disableBlend() {
		TGRenderState.setBlend(false);
		TGRenderState.setAdditive(false);
	}

	/** Only distinguishes additive (dst = ONE) from normal alpha blending. */
	public static void blendFunc(int src, int dst) {
		TGRenderState.setAdditive(dst == 1 /* GL_ONE */);
	}

	public static void blendFunc(SourceFactor src, DestFactor dst) {
		TGRenderState.setAdditive(dst == DestFactor.ONE);
	}

	public static void tryBlendFuncSeparate(SourceFactor src, DestFactor dst, SourceFactor srcA, DestFactor dstA) {
		blendFunc(src, dst);
	}

	public static void enableLighting() {
		TGRenderState.setFullbright(false);
	}

	public static void disableLighting() {
		TGRenderState.setFullbright(true);
	}

	public static void enableCull() {
	}

	public static void disableCull() {
	}

	public static void enableRescaleNormal() {
	}

	public static void disableRescaleNormal() {
	}

	public static void depthMask(boolean flag) {
	}

	public static void enableAlpha() {
	}

	public static void disableAlpha() {
	}

	public static void alphaFunc(int func, float ref) {
	}

	public static void enableTexture2D() {
	}

	public static void disableTexture2D() {
	}

	public static void enableDepth() {
	}

	public static void disableDepth() {
	}

	public static void shadeModel(int mode) {
	}

	public enum SourceFactor {
		CONSTANT_ALPHA, CONSTANT_COLOR, DST_ALPHA, DST_COLOR, ONE, ONE_MINUS_CONSTANT_ALPHA, ONE_MINUS_CONSTANT_COLOR, ONE_MINUS_DST_ALPHA,
		ONE_MINUS_DST_COLOR, ONE_MINUS_SRC_ALPHA, ONE_MINUS_SRC_COLOR, SRC_ALPHA, SRC_ALPHA_SATURATE, SRC_COLOR, ZERO
	}

	public enum DestFactor {
		CONSTANT_ALPHA, CONSTANT_COLOR, DST_ALPHA, DST_COLOR, ONE, ONE_MINUS_CONSTANT_ALPHA, ONE_MINUS_CONSTANT_COLOR, ONE_MINUS_DST_ALPHA,
		ONE_MINUS_DST_COLOR, ONE_MINUS_SRC_ALPHA, ONE_MINUS_SRC_COLOR, SRC_ALPHA, SRC_COLOR, ZERO
	}
}
