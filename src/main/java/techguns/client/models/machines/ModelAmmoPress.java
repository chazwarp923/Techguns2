package techguns.client.models.machines;

import techguns.client.models.ModelRenderer;
import techguns.client.render.gl.GlStateManager;
import net.minecraft.world.entity.Entity;

/**
 * Moving parts of the ammo press. The static parts are the JSON block model models/block/ammo_press.json (the resting machine is
 * models/item/ammo_press.json), drawn with the terrain; RenderMachine draws these on top.
 */
public class ModelAmmoPress extends ModelMachine {
	// fields
	ModelRenderer MetalPiece;
	ModelRenderer pb2;
	ModelRenderer pb1;
	ModelRenderer pa1;
	ModelRenderer pa2;
	ModelRenderer pc1;
	ModelRenderer bullet1;
	ModelRenderer bullet3;
	ModelRenderer bullet2;

	public ModelAmmoPress() {
		textureWidth = 128;
		textureHeight = 64;

		MetalPiece = new ModelRenderer(this, 110, 46);
		MetalPiece.addBox(-0.5F, 0F, 0F, 1, 5, 5);
		MetalPiece.setRotationPoint(0F, 15.5F, -2.5F);
		MetalPiece.setTextureSize(128, 64);
		MetalPiece.mirror = true;
		setRotation(MetalPiece, 0F, 0F, 0F);
		pb2 = new ModelRenderer(this, 94, 0);
		pb2.addBox(0F, 0F, 0F, 1, 5, 7);
		pb2.setRotationPoint(-3F, 15F, -3.5F);
		pb2.setTextureSize(128, 64);
		pb2.mirror = true;
		setRotation(pb2, 0F, 0F, 0F);
		pb1 = new ModelRenderer(this, 110, 13);
		pb1.addBox(0F, 0F, 0F, 3, 5, 6);
		pb1.setRotationPoint(-6F, 15F, -3F);
		pb1.setTextureSize(128, 64);
		pb1.mirror = true;
		setRotation(pb1, 0F, 0F, 0F);
		pa1 = new ModelRenderer(this, 110, 13);
		pa1.addBox(0F, 0F, 0F, 3, 5, 6);
		pa1.setRotationPoint(3F, 15F, -3F);
		pa1.setTextureSize(128, 64);
		pa1.mirror = true;
		setRotation(pa1, 0F, 0F, 0F);
		pa2 = new ModelRenderer(this, 94, 0);
		pa2.addBox(0F, 0F, 0F, 1, 5, 7);
		pa2.setRotationPoint(2F, 15F, -3.5F);
		pa2.setTextureSize(128, 64);
		pa2.mirror = true;
		setRotation(pa2, 0F, 0F, 0F);
		pc1 = new ModelRenderer(this, 108, 33);
		pc1.addBox(0F, 0F, 0F, 4, 3, 6);
		pc1.setRotationPoint(-2F, 11F, -3F);
		pc1.setTextureSize(128, 64);
		pc1.mirror = true;
		setRotation(pc1, 0F, 0F, 0F);
		bullet1 = new ModelRenderer(this, 110, 47);
		bullet1.addBox(-0.5F, 0F, -0.5F, 1, 5, 1);
		bullet1.setRotationPoint(0F, 15.5F, -2F);
		bullet1.setTextureSize(128, 64);
		bullet1.mirror = true;
		setRotation(bullet1, 0F, 0.7853982F, 0F);
		bullet3 = new ModelRenderer(this, 118, 47);
		bullet3.addBox(-0.5F, 0F, -0.5F, 1, 5, 1);
		bullet3.setRotationPoint(0F, 15.5F, 2F);
		bullet3.setTextureSize(128, 64);
		bullet3.mirror = true;
		setRotation(bullet3, 0F, 0.7853982F, 0F);
		bullet2 = new ModelRenderer(this, 114, 47);
		bullet2.addBox(-0.5F, 0F, -0.5F, 1, 5, 1);
		bullet2.setRotationPoint(0F, 15.5F, 0F);
		bullet2.setTextureSize(128, 64);
		bullet2.mirror = true;
		setRotation(bullet2, 0F, 0.7853982F, 0F);
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, float progress) {
		if (progress > 0.0f) {
			// Repeat once
			if (progress > 0.5f) {
				progress -= 0.5f;
			}
			progress = progress * 2.0f;

			float ab = 0;
			float c = 0;

			if (progress < 0.25f) {
				MetalPiece.render(scale);
				float p = progress * 4f;
				ab = (float) Math.sqrt(p);
				// 0,5+(SIN(((B2/2)+0,75)*2*PI())/2)
			} else if (progress < 0.66f) {
				float p = ((progress - 0.25f) * 2.439f);
				ab = (float) (0.5f + (Math.sin((((1f - p) / 2.0f) + 0.75f) * 2.0f * Math.PI) / 2.0f));

				bullet1.render(scale);
				bullet3.render(scale);
				bullet2.render(scale);
			} else {

				float p = Math.min(1.0f, (progress - 0.66f) * 3f);

				c = 0.5f + (float) Math.sin((Math.sqrt(p) + 0.75f) * Math.PI * 2.0f) / 2.0f;

				if (progress < 0.9f) {
					bullet1.render(scale);
					bullet3.render(scale);
					bullet2.render(scale);
				}

			}

			// left
			GlStateManager.pushMatrix();
			GlStateManager.translate((0.125f * -ab), 0f, 0f);
			pa1.render(scale);
			pa2.render(scale);
			GlStateManager.popMatrix();
			// right
			GlStateManager.pushMatrix();
			GlStateManager.translate((0.125f * ab), 0f, 0f);
			pb1.render(scale);
			pb2.render(scale);
			GlStateManager.popMatrix();
			// top
			GlStateManager.pushMatrix();
			GlStateManager.translate(0f, (0.09375f * c), 0f);
			pc1.render(scale);
			GlStateManager.popMatrix();
		} else {
			pa1.render(scale);
			pa2.render(scale);
			pb1.render(scale);
			pb2.render(scale);
			pc1.render(scale);
		}

	}

}
