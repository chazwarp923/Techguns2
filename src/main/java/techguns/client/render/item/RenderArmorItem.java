package techguns.client.render.item;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.ModelRenderer;
import techguns.client.models.armor.ModelBipedTG;
import techguns.client.render.gl.GlStateManager;
import techguns.client.render.gl.TGRenderState;

/** Inventory / hand rendering of an armor piece with its 3d armor model (1.12 RenderArmorItem). */
public class RenderArmorItem extends RenderItemBase {

	protected final ModelBipedTG modelBiped;
	/** legs/feet: fit the measured geometry into the GUI slot instead of the 1.12 offsets */
	protected final boolean fitInGui;
	/** px bounds of the visible parts: minX,minY,minZ,maxX,maxY,maxZ */
	private float[] bounds;

	/** fills this fraction of the slot, about the size of the helmet / chestplate icons */
	private static final float GUI_FIT = 0.75f;

	public RenderArmorItem(ModelBipedTG model, ResourceLocation texture, EquipmentSlot armorSlot) {
		super(null, texture);
		this.modelBiped = model;
		this.fitInGui = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;
		modelBiped.bipedHead.showModel = armorSlot == EquipmentSlot.HEAD;
		modelBiped.bipedHeadwear.showModel = armorSlot == EquipmentSlot.HEAD && model.showHeadwear();
		modelBiped.bipedBody.showModel = armorSlot == EquipmentSlot.CHEST || armorSlot == EquipmentSlot.LEGS;
		modelBiped.bipedRightArm.showModel = armorSlot == EquipmentSlot.CHEST;
		modelBiped.bipedLeftArm.showModel = armorSlot == EquipmentSlot.CHEST;
		modelBiped.bipedRightLeg.showModel = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;
		modelBiped.bipedLeftLeg.showModel = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;
		this.scale_thirdp = 1.0f;
		this.scale_ego = 1.0f;
		this.scale_gui = 1.5f;
		this.scale_itemframe = 1.5f;
		this.scale_ground = armorSlot == EquipmentSlot.HEAD ? 1.25f : 1.5f;
		switch (armorSlot) {
			case HEAD -> this.translateBase[1] = -0.54f;
			case CHEST -> this.translateBase[1] = -0.9f;
			case LEGS -> this.translateBase[1] = -1.29f;
			case FEET -> this.translateBase[1] = -1.42f;
			default -> {
			}
		}
		this.translateType[4][2] = 0.07f;
		this.translateType[0][1] = 0.1f;
		this.translateType[1][1] = 0.04f;
	}

	@Override
	public void renderItem(ItemDisplayContext transform, ItemStack stack, LivingEntity elb, boolean leftHanded) {
		if (fitInGui && transform == ItemDisplayContext.GUI) {
			renderFitted();
			return;
		}
		GlStateManager.pushMatrix();
		TGRenderState.bindTexture(texture);
		this.applyTranslation(transform);
		if (ItemDisplayContext.GUI == transform) {
			GlStateManager.rotate(180.0f, 0, 1f, 0);
		}
		this.setBaseScale(elb, transform);
		this.setBaseRotation(transform);
		this.applyBaseTranslation();
		// 1.12 ModelBase.isChild defaulted to true and was never reset for item-only models, so ModelBiped.render
		// drew these with the child layout; the translations above are tuned for that.
		GlStateManager.pushMatrix();
		GlStateManager.scale(0.75f, 0.75f, 0.75f);
		GlStateManager.translate(0.0f, 16.0f * SCALE, 0.0f);
		modelBiped.bipedHead.render(SCALE);
		GlStateManager.popMatrix();
		GlStateManager.pushMatrix();
		GlStateManager.scale(0.5f, 0.5f, 0.5f);
		GlStateManager.translate(0.0f, 24.0f * SCALE, 0.0f);
		modelBiped.bipedBody.render(SCALE);
		modelBiped.bipedRightArm.render(SCALE);
		modelBiped.bipedLeftArm.render(SCALE);
		modelBiped.bipedRightLeg.render(SCALE);
		modelBiped.bipedLeftLeg.render(SCALE);
		modelBiped.bipedHeadwear.render(SCALE);
		GlStateManager.popMatrix();
		GlStateManager.popMatrix();
	}

	private void renderFitted() {
		if (bounds == null) {
			bounds = measure();
		}
		float w = bounds[3] - bounds[0], h = bounds[4] - bounds[1];
		float size = Math.max(Math.max(w, h), 1f) * SCALE;
		float s = GUI_FIT / size;
		GlStateManager.pushMatrix();
		TGRenderState.bindTexture(texture);
		GlStateManager.rotate(180.0f, 0, 1f, 0);
		this.setBaseRotation(ItemDisplayContext.GUI);
		GlStateManager.scale(s, s, s);
		GlStateManager.translate(-(bounds[0] + bounds[3]) * 0.5f * SCALE, -(bounds[1] + bounds[4]) * 0.5f * SCALE,
				-(bounds[2] + bounds[5]) * 0.5f * SCALE);
		modelBiped.bipedBody.render(SCALE);
		modelBiped.bipedRightLeg.render(SCALE);
		modelBiped.bipedLeftLeg.render(SCALE);
		GlStateManager.popMatrix();
	}

	private float[] measure() {
		float[] b = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
		for (ModelRenderer part : new ModelRenderer[] { modelBiped.bipedBody, modelBiped.bipedRightLeg, modelBiped.bipedLeftLeg }) {
			measure(part, new Matrix4f(), b);
		}
		if (b[0] > b[3]) {
			return new float[] { -4, 0, -2, 4, 24, 2 };
		}
		return b;
	}

	private static void measure(ModelRenderer part, Matrix4f parent, float[] b) {
		if (part.isHidden || !part.showModel) {
			return;
		}
		Matrix4f m = new Matrix4f(parent).translate(part.rotationPointX, part.rotationPointY, part.rotationPointZ).rotateZ(part.rotateAngleZ)
				.rotateY(part.rotateAngleY).rotateX(part.rotateAngleX);
		Vector3f v = new Vector3f();
		for (ModelPart.Cube c : part.cubeList) {
			for (int i = 0; i < 8; i++) {
				m.transformPosition((i & 1) == 0 ? c.minX : c.maxX, (i & 2) == 0 ? c.minY : c.maxY, (i & 4) == 0 ? c.minZ : c.maxZ, v);
				b[0] = Math.min(b[0], v.x);
				b[1] = Math.min(b[1], v.y);
				b[2] = Math.min(b[2], v.z);
				b[3] = Math.max(b[3], v.x);
				b[4] = Math.max(b[4], v.y);
				b[5] = Math.max(b[5], v.z);
			}
		}
		if (part.childModels != null) {
			for (ModelRenderer child : part.childModels) {
				measure(child, m, b);
			}
		}
	}
}
