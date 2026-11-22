package techguns.client.models.armor;

import techguns.client.models.ModelBase;
import techguns.client.models.ModelRenderer;

/**
 * Re-implementation of the 1.12 ModelBiped part layout for the Techguns armor models. Part poses and visibility
 * are copied from the vanilla HumanoidModel each frame by {@link techguns.client.render.armor.TGArmorModel}.
 */
public class ModelBipedTG extends ModelBase {

	public ModelRenderer bipedHead;
	public ModelRenderer bipedHeadwear;
	public ModelRenderer bipedBody;
	public ModelRenderer bipedRightArm;
	public ModelRenderer bipedLeftArm;
	public ModelRenderer bipedRightLeg;
	public ModelRenderer bipedLeftLeg;

	public boolean isSneak;

	public ModelBipedTG() {
		this(0.0F);
	}

	public ModelBipedTG(float modelSize) {
		this(modelSize, 0.0F, 64, 32);
	}

	public ModelBipedTG(float modelSize, float yOffset, int textureWidthIn, int textureHeightIn) {
		this.textureWidth = textureWidthIn;
		this.textureHeight = textureHeightIn;
		this.bipedHead = new ModelRenderer(this, 0, 0);
		this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, modelSize);
		this.bipedHead.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
		this.bipedHeadwear = new ModelRenderer(this, 32, 0);
		this.bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, modelSize + 0.5F);
		this.bipedHeadwear.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
		this.bipedBody = new ModelRenderer(this, 16, 16);
		this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize);
		this.bipedBody.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
		this.bipedRightArm = new ModelRenderer(this, 40, 16);
		this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
		this.bipedRightArm.setRotationPoint(-5.0F, 2.0F + yOffset, 0.0F);
		this.bipedLeftArm = new ModelRenderer(this, 40, 16);
		this.bipedLeftArm.mirror = true;
		this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
		this.bipedLeftArm.setRotationPoint(5.0F, 2.0F + yOffset, 0.0F);
		this.bipedRightLeg = new ModelRenderer(this, 0, 16);
		this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
		this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F + yOffset, 0.0F);
		this.bipedLeftLeg = new ModelRenderer(this, 0, 16);
		this.bipedLeftLeg.mirror = true;
		this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
		this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F + yOffset, 0.0F);
	}

	/** whether the hat layer is drawn at all (the beret hides it) */
	public boolean showHeadwear() {
		return true;
	}
}
