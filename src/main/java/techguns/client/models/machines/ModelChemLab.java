package techguns.client.models.machines;

import techguns.client.models.ModelRenderer;
import techguns.client.render.gl.GlStateManager;
import net.minecraft.world.entity.Entity;

public class ModelChemLab extends ModelMachine{

	//fields
    ModelRenderer L8;
    ModelRenderer L5;
    ModelRenderer L4;
    ModelRenderer L3;
    ModelRenderer L1;
    ModelRenderer L2;
    ModelRenderer L7;
  
  public ModelChemLab()
  {
    textureWidth = 128;
    textureHeight = 64;
    
      L8 = new ModelRenderer(this, 114, 27);
      L8.addBox(0F, 0F, 0F, 3, 1, 3);
      L8.setRotationPoint(-6.5F, 19.5F, -6.5F);
      L8.setTextureSize(128, 64);
      L8.mirror = true;
      setRotation(L8, 0F, 0F, 0F);
      L5 = new ModelRenderer(this, 107, 43);
      L5.addBox(-1F, 0F, -1F, 2, 1, 2);
      L5.setRotationPoint(5F, 16F, 5.5F);
      L5.setTextureSize(128, 64);
      L5.mirror = true;
      setRotation(L5, 0F, 0F, 0F);
      L4 = new ModelRenderer(this, 103, 46);
      L4.addBox(0F, 0F, 0F, 3, 2, 3);
      L4.setRotationPoint(3.5F, 17F, 4F);
      L4.setTextureSize(128, 64);
      L4.mirror = true;
      setRotation(L4, 0F, 0F, 0F);
      L3 = new ModelRenderer(this, 123, 47);
      L3.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
      L3.setRotationPoint(-6F, 19.5F, 5.5F);
      L3.setTextureSize(128, 64);
      L3.mirror = true;
      setRotation(L3, 0F, 0.7853982F, 0F);
      L1 = new ModelRenderer(this, 123, 41);
      L1.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
      L1.setRotationPoint(0F, 19.5F, 5.5F);
      L1.setTextureSize(128, 64);
      L1.mirror = true;
      setRotation(L1, 0F, 0.7853982F, 0F);
      L2 = new ModelRenderer(this, 123, 44);
      L2.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
      L2.setRotationPoint(-3F, 19.5F, 5.5F);
      L2.setTextureSize(128, 64);
      L2.mirror = true;
      setRotation(L2, 0F, 0.7853982F, 0F);
      L7 = new ModelRenderer(this, 114, 22);
      L7.addBox(0F, 0F, 0F, 3, 1, 3);
      L7.setRotationPoint(3.5F, 19.5F, -6.5F);
      L7.setTextureSize(128, 64);
      L7.mirror = true;
      setRotation(L7, 0F, 0F, 0F);
  }
	
	@Override
	public void render(Entity entity, float f0, float f1, float f2, float f3,
			float f4, float f5, float progress) {	
	    
		
	    
	    if (progress > 0) {
	    	//Liquid in bottle
	    	L4.render(f5);
			L5.render(f5);
	    	
			//3 glasses
			if (progress > 0.20f) { // < 0.4f;
				float prog;
				if (progress > 0.8f) {
					prog = 1.0f - ((progress-0.8f)*5f);
				}else {
					prog = Math.min(1.0f, (progress-0.2f)*5f );
				    prog = (float) Math.sqrt(prog);
				}
			    
			    int count = Math.round(prog*16f); //16f = height of glass
			    
			   GlStateManager.pushMatrix();
			    for (int i = 0; i < count; i++) {
			    	L1.render(f5);
					GlStateManager.translate(0, -0.03125f, 0);
			    }
				GlStateManager.popMatrix();	
			}
			if (progress > 0.40f) { // < 0.6f;
				float prog;
				if (progress > 0.8f) {
					prog = 1.0f - ((progress-0.8f)*5f);
				}else {
					prog = Math.min(1.0f, (progress-0.4f)*5f );
				    prog = (float) Math.sqrt(prog);
				}
			    
			    int count = Math.round(prog*16f); //16f = height of glass
			    
			    GlStateManager.pushMatrix();
			    for (int i = 0; i < count; i++) {
			    	L2.render(f5);
					GlStateManager.translate(0, -0.03125f, 0);
			    }
			    GlStateManager.popMatrix();
			}
			if (progress > 0.60f) { // < 0.8f;
				float prog;
				if (progress > 0.8f) {
					prog = 1.0f - ((progress-0.8f)*5f);
				}else {
					prog = Math.min(1.0f, (progress-0.6f)*5f );
					prog = (float) Math.sqrt(prog);
				}
			    
			    int count = Math.round(prog*16f); //16f = height of glass
			    
			    GlStateManager.pushMatrix();
			    for (int i = 0; i < count; i++) {
			    	L3.render(f5);
					//GL11.glTranslatef(0, -0.03125f, 0);
					GlStateManager.translate(0, -0.03125f, 0);
			    }
			    GlStateManager.popMatrix();
			}
		   
		    
		    //tanks
			float prog1 = 1f;
			float prog2 = 0f;
			
			if (progress < (0.333f)) {
				prog1 = 1.0f - Math.min(1.0f, (progress)*3f);	
			}else if (progress < 0.666f) {
				prog1 = 0f;
			}else {
				prog1 = 0f;
				prog2 = Math.min(1.0f, (progress-0.666f)*3f);	
			}
			
			int count1 = Math.round(prog1*16f); //16f = height of glass
			int count2 = Math.round(prog2*16f);
			
		    GlStateManager.pushMatrix();
		    for (int i = 0; i < 16; i++) {
		    	if (count1 > i) L7.render(f5);
		    	if (count2 > i) L8.render(f5);
				 GlStateManager.translate(0, -0.03125f, 0);
		    }	
			 GlStateManager.popMatrix();
	    }

	}

}
