package techguns.client.render.gl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * Render state used by the 1.12-style render code (GlStateManager shim, ModelRenderer shim).
 * A renderer calls {@link #begin} with the PoseStack/buffers handed to it by vanilla, then the
 * legacy code draws through {@link GlStateManager} and {@code ModelRenderer.render(scale)}.
 */
public final class TGRenderState {

	private static PoseStack poseStack = new PoseStack();
	private static MultiBufferSource buffers;
	private static VertexConsumer fixedConsumer;
	private static int light = 0xF000F0;
	private static int overlay = OverlayTexture.NO_OVERLAY;

	private static ResourceLocation texture;
	private static float r = 1f, g = 1f, b = 1f, a = 1f;
	private static boolean blend = false;
	private static boolean additive = false;
	private static boolean fullbright = false;

	private TGRenderState() {
	}

	public static void begin(PoseStack ps, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		poseStack = ps;
		buffers = bufferSource;
		fixedConsumer = null;
		light = packedLight;
		overlay = packedOverlay;
		texture = null;
		r = g = b = a = 1f;
		blend = false;
		additive = false;
		fullbright = false;
	}

	/** Draw into one given consumer (e.g. the vanilla armor layer's buffer); bound textures/blend are ignored. */
	public static void beginFixed(PoseStack ps, VertexConsumer consumer, int packedLight, int packedOverlay, int argb) {
		begin(ps, null, packedLight, packedOverlay);
		fixedConsumer = consumer;
		a = FastColor.ARGB32.alpha(argb) / 255f;
		r = FastColor.ARGB32.red(argb) / 255f;
		g = FastColor.ARGB32.green(argb) / 255f;
		b = FastColor.ARGB32.blue(argb) / 255f;
	}

	public static void end() {
		buffers = null;
		fixedConsumer = null;
		poseStack = new PoseStack();
	}

	public static PoseStack pose() {
		return poseStack;
	}

	public static MultiBufferSource buffers() {
		return buffers;
	}

	public static boolean isActive() {
		return buffers != null || fixedConsumer != null;
	}

	public static void bindTexture(ResourceLocation tex) {
		texture = tex;
	}

	public static ResourceLocation texture() {
		return texture;
	}

	public static void color(float red, float green, float blue, float alpha) {
		r = red;
		g = green;
		b = blue;
		a = alpha;
	}

	public static int packedColor() {
		return FastColor.ARGB32.colorFromFloat(a, r, g, b);
	}

	public static void setBlend(boolean enabled) {
		blend = enabled;
	}

	/** GL_SRC_ALPHA, GL_ONE style blending */
	public static void setAdditive(boolean enabled) {
		additive = enabled;
	}

	public static void setFullbright(boolean enabled) {
		fullbright = enabled;
	}

	public static int light() {
		return fullbright ? 0xF000F0 : light;
	}

	public static int overlay() {
		return overlay;
	}

	/** RenderType for the currently bound texture and blend state. */
	public static RenderType renderType() {
		ResourceLocation tex = texture != null ? texture : net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
		if (additive) {
			return TGRenderTypes.additive(tex);
		}
		if (blend || a < 1f) {
			return RenderType.entityTranslucent(tex);
		}
		return RenderType.entityCutoutNoCull(tex);
	}

	public static VertexConsumer buffer() {
		if (fixedConsumer != null) {
			return fixedConsumer;
		}
		return buffers.getBuffer(renderType());
	}
}
