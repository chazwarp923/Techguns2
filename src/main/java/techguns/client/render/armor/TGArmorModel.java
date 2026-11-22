package techguns.client.render.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import techguns.client.models.ModelRenderer;
import techguns.client.models.armor.ModelBipedTG;
import techguns.client.render.gl.TGRenderState;

/**
 * Adapter between the vanilla armor layer and a 1.12 style {@link ModelBipedTG}: copies pose and part visibility
 * from the HumanoidModel the armor layer prepared, then draws the legacy boxes into the given buffer.
 */
public class TGArmorModel extends Model {

	private static final float SCALE = 0.0625f;

	private final ModelBipedTG model;
	private HumanoidModel<?> source;

	public TGArmorModel(ModelBipedTG model) {
		super(RenderType::armorCutoutNoCull);
		this.model = model;
	}

	public TGArmorModel prepare(HumanoidModel<?> source) {
		this.source = source;
		return this;
	}

	public static void copy(ModelPart from, ModelRenderer to) {
		to.rotationPointX = from.x;
		to.rotationPointY = from.y;
		to.rotationPointZ = from.z;
		to.rotateAngleX = from.xRot;
		to.rotateAngleY = from.yRot;
		to.rotateAngleZ = from.zRot;
		to.showModel = from.visible;
	}

	@Override
	public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
		if (source == null) {
			return;
		}
		copy(source.head, model.bipedHead);
		copy(source.hat, model.bipedHeadwear);
		copy(source.body, model.bipedBody);
		copy(source.rightArm, model.bipedRightArm);
		copy(source.leftArm, model.bipedLeftArm);
		copy(source.rightLeg, model.bipedRightLeg);
		copy(source.leftLeg, model.bipedLeftLeg);
		model.bipedHeadwear.showModel &= model.showHeadwear();
		model.isSneak = source.crouching;
		model.isChild = source.young;

		TGRenderState.beginFixed(ps, vc, light, overlay, color);
		try {
			if (source.young) {
				// HumanoidModel/AgeableListModel baby scaling
				ps.pushPose();
				float f = 1.5f / 2.0f;
				ps.scale(f, f, f);
				ps.translate(0.0f, 1.0f, 0.0f);
				model.bipedHead.render(SCALE);
				model.bipedHeadwear.render(SCALE);
				ps.popPose();
				ps.pushPose();
				float f1 = 1.0f / 2.0f;
				ps.scale(f1, f1, f1);
				ps.translate(0.0f, 1.5f, 0.0f);
				renderBody();
				ps.popPose();
			} else {
				model.bipedHead.render(SCALE);
				model.bipedHeadwear.render(SCALE);
				renderBody();
			}
		} finally {
			TGRenderState.end();
		}
	}

	/** first person: draw only the chest piece's arm, posed like the vanilla first person arm */
	public void renderFirstPersonArm(PoseStack ps, VertexConsumer vc, int light, ModelPart sourceArm, boolean right) {
		ModelRenderer arm = right ? model.bipedRightArm : model.bipedLeftArm;
		copy(sourceArm, arm);
		arm.showModel = true;
		TGRenderState.beginFixed(ps, vc, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		try {
			arm.render(SCALE);
		} finally {
			TGRenderState.end();
		}
	}

	private void renderBody() {
		model.bipedBody.render(SCALE);
		model.bipedRightArm.render(SCALE);
		model.bipedLeftArm.render(SCALE);
		model.bipedRightLeg.render(SCALE);
		model.bipedLeftLeg.render(SCALE);
	}
}
