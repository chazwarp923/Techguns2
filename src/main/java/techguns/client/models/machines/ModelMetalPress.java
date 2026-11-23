package techguns.client.models.machines;

import techguns.client.models.ModelRenderer;
import techguns.client.render.gl.GlStateManager;
import net.minecraft.world.entity.Entity;

/**
 * Moving parts of the metal press. The static parts are the JSON block model models/block/metal_press.json (the resting machine is
 * models/item/metal_press.json), drawn with the terrain; RenderMachine draws these on top.
 */
public class ModelMetalPress extends ModelMachine {

	  //fields
  ModelRenderer MetalPiece;
  ModelRenderer p1;
  ModelRenderer p2;
  ModelRenderer p3;
  ModelRenderer p4;
  ModelRenderer p5;
  ModelRenderer p6;

	public ModelMetalPress() {
		textureWidth = 128;
	    textureHeight = 64;
	    
	      MetalPiece = new ModelRenderer(this, 104, 46);
	      MetalPiece.addBox(0F, 0F, 0F, 6, 1, 6);
	      MetalPiece.setRotationPoint(-3F, 18F, -3F);
	      MetalPiece.setTextureSize(128, 64);
	      MetalPiece.mirror = true;
	      setRotation(MetalPiece, 0F, 0F, 0F);
	      p1 = new ModelRenderer(this, 89, 10);
	      p1.addBox(0F, 0F, 0F, 8, 5, 8);
	      p1.setRotationPoint(-4F, 9F, -4F);
	      p1.setTextureSize(128, 64);
	      p1.mirror = true;
	      setRotation(p1, 0F, 0F, 0F);
	      p2 = new ModelRenderer(this, 86, 0);
	      p2.addBox(0F, 0F, 0F, 1, 5, 1);
	      p2.setRotationPoint(3.5F, 9F, -0.5F);
	      p2.setTextureSize(128, 64);
	      p2.mirror = true;
	      setRotation(p2, 0F, 0F, 0F);
	      p3 = new ModelRenderer(this, 86, 0);
	      p3.addBox(0F, 0F, 0F, 9, 1, 9);
	      p3.setRotationPoint(-4.5F, 14F, -4.5F);
	      p3.setTextureSize(128, 64);
	      p3.mirror = true;
	      setRotation(p3, 0F, 0F, 0F);
	      p4 = new ModelRenderer(this, 86, 0);
	      p4.addBox(0F, 0F, 0F, 1, 5, 1);
	      p4.setRotationPoint(-0.5F, 9F, -4.5F);
	      p4.setTextureSize(128, 64);
	      p4.mirror = true;
	      setRotation(p4, 0F, 0F, 0F);
	      p5 = new ModelRenderer(this, 86, 0);
	      p5.addBox(0F, 0F, 0F, 1, 5, 1);
	      p5.setRotationPoint(-0.5F, 9F, 3.5F);
	      p5.setTextureSize(128, 64);
	      p5.mirror = true;
	      setRotation(p5, 0F, 0F, 0F);
	      p6 = new ModelRenderer(this, 86, 0);
	      p6.addBox(0F, 0F, 0F, 1, 5, 1);
	      p6.setRotationPoint(-4.5F, 9F, -0.5F);
	      p6.setTextureSize(128, 64);
	      p6.mirror = true;
	      setRotation(p6, 0F, 0F, 0F);
	}

	
	
	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, float progress) {
	    //MetalPiece.render(f5);
	    
		if (progress > 0.0f) {
			MetalPiece.render(scale);
			if (progress > 0.5f) {
				progress-= 0.5f;
			}
		    float y =0.5f + (float) Math.sin((Math.sqrt(progress*2.0f)+0.75f)*Math.PI*2.0f)/2.0f;
		    GlStateManager.translate(0f, 0.25f*y, 0f);
		}
		p1.render(scale);
		p2.render(scale);
		p3.render(scale);
		p4.render(scale);
		p5.render(scale);
		p6.render(scale);

	}
}

