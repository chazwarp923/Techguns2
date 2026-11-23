package techguns.client.render.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.resources.ResourceLocation;
import techguns.Techguns;
import techguns.client.render.TGRenderHelper;
import techguns.client.render.TGRenderHelper.RenderType;
import techguns.client.render.gl.GlStateManager;
import techguns.client.render.gl.TGRenderState;

/** Animated sprite-sheet quad (muzzle flashes, scopes) drawn into the current TGRenderState. */
public class ScreenEffect implements IScreenEffect {

	protected ResourceLocation fxTexture;
	protected int cols;
	protected int rows;
	protected int numSprites;
	protected RenderType type;
	protected float colorR = 1.0f;
	protected float colorG = 1.0f;
	protected float colorB = 1.0f;
	protected float alpha = 1.0f;
	protected FadeType fadeType = FadeType.NONE;
	protected boolean flipY = false;
	protected boolean flipX = false;
	/** Fabric port: the size follows this curve over the progress, from minScale to maxScale times */
	protected FadeType dynamicScaleType = null;
	protected float minScale = 0f;
	protected float maxScale = 0f;
	protected DrawMode3p drawMode3p = DrawMode3p.CROSS_XYZ;

	public ScreenEffect() {
	}

	public ScreenEffect(String fxTexture, int cols, int rows, int numSprites, RenderType type) {
		this.fxTexture = Techguns.rl(fxTexture.toLowerCase());
		this.cols = cols;
		this.rows = rows;
		this.numSprites = numSprites;
		this.type = type;
	}

	public ScreenEffect setColor(float R, float G, float B, float A) {
		this.colorR = R;
		this.colorG = G;
		this.colorB = B;
		this.alpha = A;
		return this;
	}

	public ScreenEffect setFade(FadeType type) {
		this.fadeType = type;
		return this;
	}

	public ScreenEffect setFlipAxis(boolean x, boolean y) {
		this.flipX = x;
		this.flipY = y;
		return this;
	}

	/** Fabric port: the size grows along the curve (min and max scale are stored, unused as in the Fabric port) */
	public ScreenEffect setDynamicScale(FadeType type, float minScale, float maxScale) {
		this.dynamicScaleType = type;
		this.minScale = minScale;
		this.maxScale = maxScale;
		return this;
	}

	/** which quads the effect draws in third person (default: all three planes) */
	public ScreenEffect setDrawMode3p(DrawMode3p mode) {
		this.drawMode3p = mode;
		return this;
	}

	@Override
	public void doRender(float progress, float offsetX, float offsetY, float offsetZ, float scale, float rot_x, float rot_y, float rot_z, boolean is3p) {
		if (!TGRenderState.isActive()) {
			return;
		}
		int currentFrame = (int) ((float) numSprites * progress);
		if (this.fadeType.isRising()) {
			// the rising fades go on past 1 (a charge held on), the sprites loop
			currentFrame = Math.floorMod(currentFrame, numSprites);
		} else if (currentFrame >= numSprites) {
			return;
		}
		int col = currentFrame % cols;
		int row = (currentFrame / cols);
		float u = 1.f / cols;
		float v = 1.f / rows;
		float U1 = col * u;
		float V1 = row * v;
		float U2 = (col + 1) * u;
		float V2 = (row + 1) * v;

		GlStateManager.pushMatrix();
		TGRenderHelper.enableBlendMode(type);
		TGRenderState.bindTexture(fxTexture);
		GlStateManager.rotate(rot_x, 1.0f, 0, 0);
		GlStateManager.rotate(rot_y, 0, 1.0f, 0);
		GlStateManager.rotate(rot_z, 0, 0, 1.0f);

		if (this.flipX) {
			float tmp = U1;
			U1 = U2;
			U2 = tmp;
		}
		if (this.flipY) {
			float tmp = V1;
			V1 = V2;
			V2 = tmp;
		}

		float f = scale / 2.0f;
		if (this.dynamicScaleType != null) {
			// as in the Fabric port: grows along the curve; min and max are kept but it never used them
			f *= this.dynamicScaleType.getValueAt(progress);
		}
		float a = this.fadeType.getValueAt(progress) * this.alpha;

		VertexConsumer vc = TGRenderState.buffer();
		PoseStack.Pose pose = TGRenderState.pose().last();
		if (!is3p || this.drawMode3p != DrawMode3p.CROSS_XY) {
			quad(vc, pose, offsetX - f, offsetY + f, offsetZ, U1, V2, offsetX - f, offsetY - f, offsetZ, U1, V1, offsetX + f, offsetY - f, offsetZ, U2, V1,
					offsetX + f, offsetY + f, offsetZ, U2, V2, a);
		}
		if (is3p && this.drawMode3p != DrawMode3p.PLANE_Z) {
			quad(vc, pose, offsetX - f, offsetY, offsetZ + f, U1, V2, offsetX - f, offsetY, offsetZ - f, U1, V1, offsetX + f, offsetY, offsetZ - f, U2, V1,
					offsetX + f, offsetY, offsetZ + f, U2, V2, a);
			quad(vc, pose, offsetX, offsetY - f, offsetZ + f, U1, V2, offsetX, offsetY - f, offsetZ - f, U1, V1, offsetX, offsetY + f, offsetZ - f, U2, V1,
					offsetX, offsetY + f, offsetZ + f, U2, V2, a);
		}
		TGRenderHelper.disableBlendMode(type);
		GlStateManager.popMatrix();
	}

	private void quad(VertexConsumer vc, PoseStack.Pose pose, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2,
			float v2, float x3, float y3, float z3, float u3, float v3, float x4, float y4, float z4, float u4, float v4, float a) {
		vert(vc, pose, x1, y1, z1, u1, v1, a);
		vert(vc, pose, x2, y2, z2, u2, v2, a);
		vert(vc, pose, x3, y3, z3, u3, v3, a);
		vert(vc, pose, x4, y4, z4, u4, v4, a);
	}

	private void vert(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, float a) {
		vc.addVertex(pose, x, y, z).setColor(colorR, colorG, colorB, a).setUv(u, v).setOverlay(TGRenderState.overlay()).setLight(0xF000F0)
				.setNormal(pose, 0, 0, 1);
	}

	public enum FadeType {
		SMOOTH, FAST, NONE,
		/** Fabric port: rises smoothly to 1 over the progress, then stays */
		RISE_SMOOTH,
		/** Fabric port: rises smoothly to 1, then pulses down to 0.5 and back each further unit of progress */
		RISE_SMOOTH_AND_PULSE_FAST;

		public boolean isRising() {
			return this == RISE_SMOOTH || this == RISE_SMOOTH_AND_PULSE_FAST;
		}

		public float getValueAt(float progress) {
			switch (this) {
				case FAST: {
					progress = Math.max(0.0f, Math.min(1.0f, progress));
					return (float) ((1.0 - Math.cos(Math.sqrt(progress) * 2.0 * Math.PI)) * 0.5);
				}
				case SMOOTH: {
					progress = Math.max(0.0f, Math.min(1.0f, progress));
					double d2 = Math.sin(Math.PI * progress);
					return (float) (d2 * d2);
				}
				case RISE_SMOOTH: {
					if (progress >= 1.0f) {
						return 1.0f;
					}
					double d2 = Math.sin(Math.PI * 0.5 * progress);
					return (float) (d2 * d2);
				}
				case RISE_SMOOTH_AND_PULSE_FAST: {
					if (progress <= 1.0f) {
						double d2 = Math.sin(Math.PI * 0.5 * progress);
						return (float) (d2 * d2);
					}
					progress = progress - (int) progress;
					return (float) (1.0f - (0.5f * Math.pow(Math.sin(progress * Math.PI * 2), 2)));
				}
				case NONE:
				default:
					return 1.0f;
			}
		}
	}

	public enum DrawMode3p {
		/** the three planes */
		CROSS_XYZ,
		/** the two planes along the barrel */
		CROSS_XY,
		/** only the plane facing along the barrel */
		PLANE_Z
	}
}
