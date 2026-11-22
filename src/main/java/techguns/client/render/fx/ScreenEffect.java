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

	public ScreenEffect() {
	}

	public ScreenEffect(String fxTexture, int cols, int rows, int numSprites, RenderType type) {
		this.fxTexture = Techguns.rl(fxTexture.toLowerCase());
		this.cols = cols;
		this.rows = rows;
		this.numSprites = numSprites;
		this.type = type;
	}

	public IScreenEffect setColor(float R, float G, float B, float A) {
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

	public IScreenEffect setFlipAxis(boolean x, boolean y) {
		this.flipX = x;
		this.flipY = y;
		return this;
	}

	@Override
	public void doRender(float progress, float offsetX, float offsetY, float offsetZ, float scale, float rot_x, float rot_y, float rot_z, boolean is3p) {
		if (!TGRenderState.isActive()) {
			return;
		}
		int currentFrame = (int) ((float) numSprites * progress);
		if (currentFrame >= numSprites) {
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
		float a;
		switch (fadeType) {
			case FAST:
				a = (float) ((1.0 - Math.cos(Math.sqrt(progress) * 2.0 * Math.PI)) * 0.5) * this.alpha;
				break;
			case SMOOTH:
				double d2 = Math.sin(Math.PI * progress);
				a = (float) (d2 * d2) * this.alpha;
				break;
			case NONE:
			default:
				a = this.alpha;
				break;
		}

		VertexConsumer vc = TGRenderState.buffer();
		PoseStack.Pose pose = TGRenderState.pose().last();
		quad(vc, pose, offsetX - f, offsetY + f, offsetZ, U1, V2, offsetX - f, offsetY - f, offsetZ, U1, V1, offsetX + f, offsetY - f, offsetZ, U2, V1,
				offsetX + f, offsetY + f, offsetZ, U2, V2, a);
		if (is3p) {
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
		SMOOTH, FAST, NONE;
	}
}
