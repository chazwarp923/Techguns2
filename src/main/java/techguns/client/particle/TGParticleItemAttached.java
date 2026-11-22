package techguns.client.particle;


import java.util.ArrayList;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import techguns.client.render.TGRenderHelper;
import techguns.client.render.TGRenderHelper.RenderType;
import techguns.util.EntityCondition;
import techguns.util.MathUtil;
public class TGParticleItemAttached extends TGParticle {

	protected Entity parentEntity;
	
	public TGParticleItemAttached(Level worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn,
			double ySpeedIn, double zSpeedIn, TGParticleSystem particleSystem, Entity parentEntity) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, particleSystem);
		this.parentEntity=parentEntity;
	}

	@Override
	public void doRender(TGParticleBuffers buffers, float partialTickTime, float rotX, float rotZ,
			float rotYZ, float rotXY, float rotXZ) {
		Entity renderViewEntity = Minecraft.getInstance().getCameraEntity();
		boolean thirdp = true;
		if(parentEntity!=null && parentEntity == renderViewEntity) {
			if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
				thirdp=false;
			}
		}
		this.renderParticle(buffers, partialTickTime, rotX, rotZ, rotYZ, rotXY, rotXZ, thirdp);
	}

	@Override
	public void renderParticle(TGParticleBuffers buffers, float partialTickTime, float rotX, float rotZ,
			float rotYZ, float rotXY, float rotXZ) {
		renderParticle(buffers, partialTickTime, rotX, rotZ, rotYZ, rotXY, rotXZ, true);
	}
	
	public void renderParticle(TGParticleBuffers buffers, float partialTickTime, float rotX, float rotZ,
			float rotYZ, float rotXY, float rotXZ, boolean thirdp) {
		float progress = ((float)this.particleAge+partialTickTime) / (float)this.particleMaxAge;
    	
    	preRenderStep(progress);   	
    	
		/*-------------------
		 * ANIMATION
		 */	
		int currentFrame = 0;
        if (type.hasVariations) {
        	currentFrame = variationFrame;
        }else {
        	currentFrame = ((int)((float)type.frames*(progress * this.animationSpeed))) % type.frames;
        }
    	
    	/* -------------
         * RENDER PARTICLE
         */
        this.particleScale = sizePrev + (size-sizePrev)*partialTickTime;

        float fscale = 0.1F * this.particleScale;

        float fPosX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTickTime - (!this.itemAttached ? TGParticleManager.interpPosX :0));
        float fPosY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTickTime - (!this.itemAttached ? TGParticleManager.interpPosY :0));
        float fPosZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTickTime - (!this.itemAttached ? TGParticleManager.interpPosZ :0));
    
        float r = fscale;
        
		int col = currentFrame % type.columns;
		int row = (currentFrame / type.columns);
		
		float u = 1.f/type.columns;
		float v = 1.f/type.columns; 
		float U1 = col*u;
		float V1 = row*v;
		float U2 = (col+1)*u;
		float V2 = (row+1)*v;
		
		float ua, va, ub, vb, uc, vc, ud, vd;
		ua=U2; va=V2; ub = U2; vb= V1; uc = U1; vc = V1; ud=U1; vd = V2;
		
		VertexConsumer buffer = buffers.get(type.texture, type.renderType);
        double a = (angle + (partialTickTime * angleRate)) * MathUtil.D2R;
		Vec3 p1, p2, p3, p4;
		
		if (this.type.groundAligned) {
			float s = fscale;
			p1 = new Vec3(-s,0,-s);
			p2 = new Vec3(s,0,-s);
			p3 = new Vec3(s,0,s);
			p4 = new Vec3(-s,0,s);
			if (a > 0.0001f) {
				p1 = p1.yRot((float) a);
				p2 = p2.yRot((float) a);
				p3 = p3.yRot((float) a);
				p4 = p4.yRot((float) a);
			}
		}else {
	        p1 = new Vec3((double)(- rotX * fscale - rotXY * fscale), (double)(- rotZ * fscale), (double)(- rotYZ * fscale - rotXZ * fscale));
	        p2 = new Vec3((double)(- rotX * fscale + rotXY * fscale), (double)( + rotZ * fscale), (double)( - rotYZ * fscale + rotXZ * fscale));
	        p3 = new Vec3((double)( rotX * fscale + rotXY * fscale), (double)( + rotZ * fscale), (double)( + rotYZ * fscale + rotXZ * fscale));
	        p4 = new Vec3((double)( rotX * fscale - rotXY * fscale), (double)( - rotZ * fscale), (double)( + rotYZ * fscale - rotXZ * fscale));        
	        
	        if (a > 0.0001f) {
		        Vec3 axis = p1.normalize().cross(p2.normalize());
				double cosa = Math.cos(a);
				double sina = Math.sin(a);
		        
		        p1 = rotAxis(p1, axis, sina, cosa);
		        p2 = rotAxis(p2, axis, sina, cosa);
		        p3 = rotAxis(p3, axis, sina, cosa);
		        p4 = rotAxis(p4, axis, sina, cosa);     
	        }	        		
		}
		
	//if(!thirdp) {
		buffers.vertex(buffer, p1.x + fPosX, p1.y + fPosY, p1.z + fPosZ, ua, va, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
		buffers.vertex(buffer, p2.x + fPosX, p2.y + fPosY, p2.z + fPosZ, ub, vb, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
		buffers.vertex(buffer, p3.x + fPosX, p3.y + fPosY, p3.z + fPosZ, uc, vc, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
		buffers.vertex(buffer, p4.x + fPosX, p4.y + fPosY, p4.z + fPosZ, ud, vd, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
	//}
		if(thirdp) {
			buffers.vertex(buffer, p1.z + fPosZ, p1.y + fPosY, p1.x + fPosX, ua, va, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
			buffers.vertex(buffer, p2.z + fPosZ, p2.y + fPosY, p2.x + fPosX, ub, vb, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
			buffers.vertex(buffer, p3.z + fPosZ, p3.y + fPosY, p3.x + fPosX, uc, vc, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
			buffers.vertex(buffer, p4.z + fPosZ, p4.y + fPosY, p4.x + fPosX, ud, vd, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
			
			double h = (p1.y+p3.y)*0.5d;
			
			buffers.vertex(buffer, p1.x + fPosX, h + fPosY, -p2.x + fPosX, ua, va, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
			buffers.vertex(buffer, p2.x + fPosX, h + fPosY, -p1.x + fPosX, ub, vb, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
			buffers.vertex(buffer, p3.x + fPosX, h + fPosY, -p4.x + fPosX, uc, vc, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
			buffers.vertex(buffer, p4.x + fPosX, h + fPosY, -p3.x + fPosX, ud, vd, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);

		}

	}
	
	
}
