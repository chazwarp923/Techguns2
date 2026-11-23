package techguns.client.models;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import techguns.client.render.gl.TGRenderState;

/**
 * Re-implementation of the 1.12.2 ModelRenderer API (addBox, setRotationPoint, rotateAngleX/Y/Z,
 * mirror, setTextureSize, render(scale), children) on top of vanilla {@link ModelPart.Cube}.
 * Rendering goes to the current {@link TGRenderState}.
 */
public class ModelRenderer {
	public float textureWidth;
	public float textureHeight;
	private int textureOffsetX;
	private int textureOffsetY;
	public float rotationPointX;
	public float rotationPointY;
	public float rotationPointZ;
	public float rotateAngleX;
	public float rotateAngleY;
	public float rotateAngleZ;
	public boolean mirror;
	public boolean showModel = true;
	public boolean isHidden;
	public List<ModelPart.Cube> cubeList = new ArrayList<>();
	public List<ModelRenderer> childModels;
	public final String boxName;
	private final ModelBase baseModel;
	public float offsetX;
	public float offsetY;
	public float offsetZ;

	private static final EnumSet<Direction> ALL_FACES = EnumSet.allOf(Direction.class);

	public ModelRenderer(ModelBase model, String boxNameIn) {
		this.textureWidth = 64.0F;
		this.textureHeight = 32.0F;
		this.baseModel = model;
		model.boxList.add(this);
		this.boxName = boxNameIn;
		this.setTextureSize(model.textureWidth, model.textureHeight);
	}

	public ModelRenderer(ModelBase model) {
		this(model, null);
	}

	public ModelRenderer(ModelBase model, int texOffX, int texOffY) {
		this(model);
		this.setTextureOffset(texOffX, texOffY);
	}

	public void addChild(ModelRenderer renderer) {
		if (this.childModels == null) {
			this.childModels = new ArrayList<>();
		}
		this.childModels.add(renderer);
	}

	public ModelRenderer setTextureOffset(int x, int y) {
		this.textureOffsetX = x;
		this.textureOffsetY = y;
		return this;
	}

	public ModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth) {
		return addBox(offX, offY, offZ, width, height, depth, 0.0F, this.mirror);
	}

	public ModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth, boolean mirrored) {
		return addBox(offX, offY, offZ, width, height, depth, 0.0F, mirrored);
	}

	public void addBox(float offX, float offY, float offZ, int width, int height, int depth, float scaleFactor) {
		addBox(offX, offY, offZ, width, height, depth, scaleFactor, this.mirror);
	}

	private ModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth, float delta, boolean mirrored) {
		this.cubeList.add(new ModelPart.Cube(this.textureOffsetX, this.textureOffsetY, offX, offY, offZ, width, height, depth, delta, delta, delta,
				mirrored, this.textureWidth, this.textureHeight, ALL_FACES));
		return this;
	}

	/**
	 * The Fabric port's ModelPart.addCuboid: float sizes, and an expansion that scales the box about the part's origin by
	 * (size + 2 * expansion) / size, keeping its texture mapping. Done here as a shifted box with a per-axis grow.
	 */
	public ModelRenderer addCuboid(float x, float y, float z, float w, float h, float d, float ex, float ey, float ez) {
		float[] gx = fabricExpansion(x, w, ex), gy = fabricExpansion(y, h, ey), gz = fabricExpansion(z, d, ez);
		this.cubeList.add(new ModelPart.Cube(this.textureOffsetX, this.textureOffsetY, gx[0], gy[0], gz[0], w, h, d, gx[1], gy[1], gz[1],
				this.mirror, this.textureWidth, this.textureHeight, ALL_FACES));
		return this;
	}

	/** origin and grow of a box spanning [o * s, (o + size) * s], s = (size + 2 e) / size */
	private static float[] fabricExpansion(float o, float size, float e) {
		if (e == 0.0F) {
			return new float[] { o, 0.0F };
		}
		float s = (size + 2.0F * e) / size;
		float origin = ((2.0F * o + size) * s - size) / 2.0F;
		return new float[] { origin, origin - o * s };
	}

	public void setRotationPoint(float x, float y, float z) {
		this.rotationPointX = x;
		this.rotationPointY = y;
		this.rotationPointZ = z;
	}

	public ModelRenderer setTextureSize(int w, int h) {
		this.textureWidth = (float) w;
		this.textureHeight = (float) h;
		return this;
	}

	private void drawCubesAndChildren(PoseStack ps, float scale) {
		if (!this.cubeList.isEmpty() && TGRenderState.isActive()) {
			VertexConsumer vc = TGRenderState.buffer();
			ps.pushPose();
			float s = scale * 16f;
			ps.scale(s, s, s);
			PoseStack.Pose pose = ps.last();
			int light = TGRenderState.light();
			int overlay = TGRenderState.overlay();
			int color = TGRenderState.packedColor();
			for (ModelPart.Cube cube : this.cubeList) {
				cube.compile(pose, vc, light, overlay, color);
			}
			ps.popPose();
		}
		if (this.childModels != null) {
			for (ModelRenderer child : this.childModels) {
				child.render(scale);
			}
		}
	}

	public void render(float scale) {
		if (this.isHidden || !this.showModel) {
			return;
		}
		PoseStack ps = TGRenderState.pose();
		ps.pushPose();
		ps.translate(this.offsetX, this.offsetY, this.offsetZ);
		ps.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
		applyRotation(ps);
		drawCubesAndChildren(ps, scale);
		ps.popPose();
	}

	public void renderWithRotation(float scale) {
		render(scale);
	}

	/** Applies the part's translation/rotation to the current matrix (used for attaching things to a part). */
	public void postRender(float scale) {
		if (this.isHidden || !this.showModel) {
			return;
		}
		PoseStack ps = TGRenderState.pose();
		ps.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
		applyRotation(ps);
	}

	private void applyRotation(PoseStack ps) {
		if (this.rotateAngleZ != 0.0F) {
			ps.mulPose(Axis.ZP.rotation(this.rotateAngleZ));
		}
		if (this.rotateAngleY != 0.0F) {
			ps.mulPose(Axis.YP.rotation(this.rotateAngleY));
		}
		if (this.rotateAngleX != 0.0F) {
			ps.mulPose(Axis.XP.rotation(this.rotateAngleX));
		}
	}
}
