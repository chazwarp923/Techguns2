package techguns.client.particle;


import java.util.ArrayList;
import techguns.client.particle.TGParticleSystemType.AlphaEntry;
import techguns.client.particle.TGParticleSystemType.ColorEntry;
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
/**
 * An actual spawned particle
 */
public class TGParticle extends TGParticleBase implements ITGParticle {
	   
	
//	public double posX;
//	public double posY;
//	public double posZ;
	
	int lifetime;
	
	float angle;
	float angleRate;
	float angleRateDamping;
	
	float size;
	float sizePrev;
	float sizeRate;
	float sizeRateDamping;
	
	float animationSpeed;
	
	double velX;
	double velY;
	double velZ;
	float velocityDamping;
	float velocityDampingOnGround;
	
	float systemVelocityFactor;
	
	TGParticleSystem particleSystem;
	TGParticleSystemType type;
	
	int variationFrame;
	
	protected double depth;
	
	protected boolean itemAttached=false;
	
	//int angle;

	public TGParticle(Level worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn,
			double ySpeedIn, double zSpeedIn, TGParticleSystem particleSystem) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn);
		this.motionX = xSpeedIn;
		this.motionY = ySpeedIn;
		this.motionZ = zSpeedIn;

		this.particleSystem = particleSystem;
		this.type = particleSystem.type;
		this.init();
		}
		
		private void init() {
			this.lifetime = MathUtil.randomInt(rand, type.lifetimeMin, type.lifetimeMax);
			this.particleMaxAge = lifetime;
			this.size = MathUtil.randomFloat(rand, type.sizeMin, type.sizeMax) * this.particleSystem.scale;
			this.size+= (this.particleSystem.startSize);
			this.sizeRate = MathUtil.randomFloat(rand, type.sizeRateMin, type.sizeRateMax)  * this.particleSystem.scale;
			this.sizeRateDamping = MathUtil.randomFloat(rand, type.sizeRateDampingMin, type.sizeRateDampingMax);
			this.animationSpeed = MathUtil.randomFloat(rand, type.animationSpeedMin, type.animationSpeedMax);
			this.velocityDamping = MathUtil.randomFloat(rand, type.velocityDampingMin, type.velocityDampingMax);
			this.systemVelocityFactor = MathUtil.randomFloat(rand, type.systemVelocityFactorMin, type.systemVelocityFactorMax);
		    this.velocityDampingOnGround = MathUtil.randomFloat(rand, type.velocityDampingOnGroundMin, type.velocityDampingOnGroundMax);
			
		    this.angle = MathUtil.randomFloat(rand, type.angleMin, type.angleMax);
		    this.angleRate = MathUtil.randomFloat(rand, type.angleRateMin, type.angleRateMax);
		    this.angleRateDamping = MathUtil.randomFloat(rand, type.angleRateDampingMin, type.angleRateDampingMax);
		    
		    //System.out.printf("###INIT:Motion1=(%.2f / %.2f / %.2f)\n",this.motionX, this.motionY, this.motionZ);
		    
			this.motionX+=(systemVelocityFactor*particleSystem.motionX());
			this.motionY+=(systemVelocityFactor*particleSystem.motionY());
			this.motionZ+=(systemVelocityFactor*particleSystem.motionZ());
			
			//System.out.printf("###INIT:Motion=(%.2f / %.2f / %.2f)\n",this.motionX, this.motionY, this.motionZ);
			//System.out.println("###INIT:VelType="+this.type.velocityType.toString());
			//System.out.printf("###INIT:Type.VelocityData=[%.2f, %.2f, %.2f]\n",this.type.velocityDataMin[0], this.type.velocityDataMin[1], this.type.velocityDataMin[2]);
			
			this.velX = this.motionX;
			this.velY = this.motionY;
			this.velZ = this.motionZ;
			
			this.variationFrame = rand.nextInt(type.frames);
			
//			if (type.randomRotation) {
//				angle = rand.nextInt(4);
//			}
		}
	

    public void onUpdate()
    {

		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		this.sizePrev = this.size;

		lifetime--;
		if (this.particleAge++ >= this.particleMaxAge) {
			this.setExpired();
			return;
		}
		
		/*---
		 * Move with System
		 */
		if (this.type.particlesStickToSystem) {
			
			if (this.particleSystem.entity != null) {
				
				if (!this.particleSystem.entity.isAlive()) {
					this.setExpired();
					return;
				}
				
				if (this.type.particlesMoveWithSystem && this.particleSystem.attachToHead && this.particleSystem.entity instanceof LivingEntity) {
					LivingEntity ent = (LivingEntity)this.particleSystem.entity;
					
					double p = ent.getXRot()*MathUtil.D2R;
					double y = ent.getYHeadRot()*MathUtil.D2R;
					
					double prevP = ent.xRotO * MathUtil.D2R;
					double prevY =ent.yHeadRotO * MathUtil.D2R;
					
					Vec3 offsetBase = this.particleSystem.entityOffset.add(this.particleSystem.type.offset);
					
					//ViewBobbing
					/*if (this.particleSystem.entity == Minecraft.getMinecraft().player
							&& Minecraft.getMinecraft().gameSettings.thirdPersonView == 0
							&& Minecraft.getMinecraft().gameSettings.viewBobbing) {
						Vec3 vec = setupViewBobbing(1.0f).scale(2.0);
						offsetBase = offsetBase.add(vec);
					}
					*/
					
					Vec3 offset = offsetBase.xRot((float)-p);
					offset = offset.yRot((float)-y);
					
					Vec3 offsetP = offsetBase.xRot((float)-prevP);
					offsetP = offsetP.yRot((float)-prevY);
										
					this.prevPosX = this.particleSystem.entity.xo + offsetP.x;
					this.prevPosY = this.particleSystem.entity.yo + ent.getEyeHeight() + offsetP.y;
					this.prevPosZ = this.particleSystem.entity.zo + offsetP.z;
					this.posX = this.particleSystem.entity.getX() + offset.x;
					this.posY = this.particleSystem.entity.getY() + ent.getEyeHeight() + offset.y;
					this.posZ = this.particleSystem.entity.getZ() + offset.z;
				}else {
				
					this.prevPosX = this.particleSystem.entity.xo;
					this.prevPosY = this.particleSystem.entity.yo;
					this.prevPosZ = this.particleSystem.entity.zo;
					this.posX = this.particleSystem.entity.getX();
					this.posY = this.particleSystem.entity.getY();
					this.posZ = this.particleSystem.entity.getZ();
				}
			}else {
				
				if (!this.particleSystem.isAlive()) {
					this.setExpired();
					return;
				}
				
				this.posX = this.particleSystem.posX();
				this.posY = this.particleSystem.posY();
				this.posZ = this.particleSystem.posZ();
			}	
			
		}else if (this.type.particlesMoveWithSystem) {		
			double dP = (this.particleSystem.rotationPitch - this.particleSystem.prevRotationPitch)*MathUtil.D2R;
			double dY = (this.particleSystem.rotationYaw - this.particleSystem.prevRotationYaw)*MathUtil.D2R;
			
			Vec3 pos = new Vec3(this.posX,  this.posY, this.posZ);
			Vec3 sysPos = new Vec3(this.particleSystem.posX(), this.particleSystem.posY(), this.particleSystem.posZ());
			
			Vec3 offset = sysPos.subtract(pos);
			offset = offset.yRot((float)-dY);
			offset = offset.xRot((float)-dP);
			
			Vec3 motion = new Vec3 (this.motionX, this.motionY, this.motionZ);
			motion = motion.yRot((float)-dY);
			motion = motion.xRot((float)-dP);
			
			this.posX = sysPos.x+offset.x;
			this.posY = sysPos.y+offset.y;
			this.posZ = sysPos.z+offset.z;
			
			this.motionX = motion.x;
			this.motionY = motion.y;
			this.motionZ = motion.z;
		
		}
		
		
		/* -------------
		 * MOTION
		 */

		this.motionX = velX;
		this.motionY = velY;
		this.motionZ = velZ;
		this.motionY -= type.gravity; //(0.05d * (double) type.gravity * (double) this.ticksExisted);		
		//this.moveEntity(this.motionX, this.motionY, this.motionZ);
		//System.out.printf("Velocity=(%.2f / %.2f / %.2f)\n",this.velX, this.velY, this.velZ);
		//System.out.printf("Motion=(%.2f / %.2f / %.2f)\n",this.motionX, this.motionY, this.motionZ);
		this.setPosition(this.posX+this.motionX, this.posY+this.motionY, this.posZ+this.motionZ);
		
		
		this.velX *= velocityDamping;
		this.velY *= velocityDamping;
		this.velZ *= velocityDamping;

		if (this.onGround) {
			this.velX *= velocityDampingOnGround;
			this.velY *= velocityDampingOnGround; // ?
			this.velZ *= velocityDampingOnGround;
			if (type.removeOnGround)
				this.setExpired();
		}

		/* ------------
		 * SIZE
		 */
		size = Math.max(0.0f, size+sizeRate);
		sizeRate *= sizeRateDamping;
		
		/*
		 * ANGLE
		 */
		angle = (angle + angleRate) % 360.0f;
		angleRate *= angleRateDamping;
    }
    
    
	 /**
     * Renders the particle
     */
    public void renderParticle(TGParticleBuffers buffers, float partialTickTime, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ)
    {
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

//        float f6 = ((float)this.particleTextureIndexX + this.particleTextureJitterX / 4.0F) / 16.0F;
//        float f7 = f6 + 0.015609375F;
//        float f8 = ((float)this.particleTextureIndexY + this.particleTextureJitterY / 4.0F) / 16.0F;
//        float f9 = f8 + 0.015609375F;
        float fscale = 0.1F * this.particleScale;

        float fPosX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTickTime - (!this.itemAttached ? TGParticleManager.interpPosX :0));
        float fPosY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTickTime - (!this.itemAttached ? TGParticleManager.interpPosY :0));
        float fPosZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTickTime - (!this.itemAttached ? TGParticleManager.interpPosZ :0));
    
        /*
		if (this.particleSystem.type.particlesMoveWithSystem && this.particleSystem.attachToHead
				&& this.particleSystem.entity == Minecraft.getMinecraft().player
				&& Minecraft.getMinecraft().gameSettings.thirdPersonView == 0
				&& Minecraft.getMinecraft().gameSettings.viewBobbing) {
			Vec3 vec = setupViewBobbing(partialTickTime);
			
			Player entityplayer = (Player)Minecraft.getMinecraft().getRenderViewEntity();
			float pitch_ = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTickTime;
            float yaw_ = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTickTime;
			
            vec = vec.xRot(-pitch_ * (float)MathUtil.D2R);
            vec = vec.yRot(-yaw_ * (float)MathUtil.D2R);          
            
			fPosX += (float)vec.x;
			fPosY += (float)vec.y;
			fPosZ += (float)vec.z;
		}*/
        
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
		
//		switch (angle) {
//			case 1:
//				 ua=U1; va = V2; ub=U2; vb=V2; uc = U2; vc= V1; ud = U1; vd = V1;
//				 break;
//			case 2:
//				 ua = U1; va = V1; ub=U1; vb = V2; uc=U2; vc=V2; ud = U2; vd= V1;
//				 break;
//			case 3:
//				 ua = U2; va= V1; ub = U1; vb = V1; uc=U1; vc = V2; ud=U2; vd=V2;
//				 break;
//			case 0:					
//			default:
//				ua=U2; va=V2; ub = U2; vb= V1; uc = U1; vc = V1; ud=U1; vd = V2;
//				break;
//		}
		
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
		
			//<TEST>
//			Vec3 angles = RenderItemBase.getTransformAngles((LivingEntity)entityIn, EnumHandSide.RIGHT, partialTickTime);
//			angles = angles.scale(-1.0);
//			
//			p1 = MathUtil.rotateVec3dAroundZ(p1, (float)angles.z);
//			p1 = p1.yRot((float)angles.y);
//			p1 = p1.xRot((float)angles.x);
//		
//			p2 = MathUtil.rotateVec3dAroundZ(p2, (float)angles.z);
//			p2 = p2.yRot((float)angles.y);
//			p2 = p2.xRot((float)angles.x);
//			
//			
//			p3 = MathUtil.rotateVec3dAroundZ(p3, (float)angles.z);
//			p3 = p3.yRot((float)angles.y);
//			p3 = p3.xRot((float)angles.x);
//
//			
//			p4 = MathUtil.rotateVec3dAroundZ(p4, (float)angles.z);
//			p4 = p4.yRot((float)angles.y);
//			p4 = p4.xRot((float)angles.x);
//
//			
			//</TEST>
			
	        
	        //AngleZ Rotation
	        
	        //double a = angle * MathUtil.D2R;
	        
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
		
		/*p1 = new Vec3(p1.x + fPosX, p1.y + fPosY, p1.z + fPosZ);
		p2 = new Vec3(p2.x + fPosX, p2.y + fPosY, p2.z + fPosZ);
		p3 = new Vec3(p3.x + fPosX, p3.y + fPosY, p3.z + fPosZ);
		p4 = new Vec3(p4.x + fPosX, p4.y + fPosY, p4.z + fPosZ);*/
        
		//System.out.println(String.format("p1 = %.2f, %.2f, %.2f, p2 = %.2f, %.2f, %.2f,  p3 = %.2f, %.2f, %.2f,  p4 = %.2f, %.2f, %.2f", p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, p4.x, p4.y, p4.z));
		//System.out.println(String.format("fpos = %.2f, %.2f, %.2f", fPosX, fPosY, fPosZ));

		buffers.vertex(buffer, p1.x + fPosX, p1.y + fPosY, p1.z + fPosZ, ua, va, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
		buffers.vertex(buffer, p2.x + fPosX, p2.y + fPosY, p2.z + fPosZ, ub, vb, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
		buffers.vertex(buffer, p3.x + fPosX, p3.y + fPosY, p3.z + fPosZ, uc, vc, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
		buffers.vertex(buffer, p4.x + fPosX, p4.y + fPosY, p4.z + fPosZ, ud, vd, this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);

        //new ModelRocket().render(null, 0, 0, 0, 0, 0, 0.625f, 0, 0f, TransformType.GROUND, 0, 0f, 0f);
        //System.out.println("DoRender");

    }
    
    /** java.awt.Color.RGBtoHSB equivalent on 0..1 floats (quantized to 8 bit like 1.12) */
    protected static float[] rgbToHsb(float rf, float gf, float bf) {
    	int r = (int) (rf * 255), g = (int) (gf * 255), b = (int) (bf * 255);
    	int cmax = Math.max(r, Math.max(g, b));
    	int cmin = Math.min(r, Math.min(g, b));
    	float brightness = cmax / 255.0f;
    	float saturation = cmax != 0 ? (float) (cmax - cmin) / (float) cmax : 0;
    	float hue;
    	if (saturation == 0) {
    		hue = 0;
    	} else {
    		float redc = (float) (cmax - r) / (float) (cmax - cmin);
    		float greenc = (float) (cmax - g) / (float) (cmax - cmin);
    		float bluec = (float) (cmax - b) / (float) (cmax - cmin);
    		if (r == cmax) hue = bluec - greenc;
    		else if (g == cmax) hue = 2.0f + redc - bluec;
    		else hue = 4.0f + greenc - redc;
    		hue = hue / 6.0f;
    		if (hue < 0) hue = hue + 1.0f;
    	}
    	return new float[] { hue, saturation, brightness };
    }

    /**
     * interpolate colors and alpha values
     */
    protected void preRenderStep(float progress) {
    	
		/* ------------------------
		 * INTERPOLATE COLOR VALUES
		 */
    	
		ColorEntry c1 = null;
		ColorEntry c2 = null;
    	if (type.colorEntries.size()==0) {
    		c1 =new ColorEntry(1.0f,1.0f,1.0f,0);
    		c2 = c1;
    	}else if (type.colorEntries.size() == 1) {
    		c1 = type.colorEntries.get(0);
    		c2 = c1;
    	}else {
    		c1 = type.colorEntries.get(0);
    		for (int i = 1; i < type.colorEntries.size(); i++) {
    			c2 = type.colorEntries.get(i);
				if (progress < c2.time) {
					break;
				}else {
					c1 = c2;
				}
			}
    	}
		float p = (progress-c1.time) / (c2.time-c1.time);		
		if (c1 != c2) {
			
			//RGB to HSB
			float[] hsb1 = rgbToHsb(c1.r, c1.g, c1.b);
			float[] hsb2 = rgbToHsb(c2.r, c2.g, c2.b);	
			//HSB to RGB;
			int color = Mth.hsvToRgb(Mth.positiveModulo(hsb1[0]*(1f-p) + hsb2[0]*p, 1f), hsb1[1]*(1f-p) + hsb2[1]*p, hsb1[2]*(1f-p) + hsb2[2]*p);
			this.particleRed = (float)((color >> 16) & 0xFF) / 255.0f;
			this.particleGreen = (float)((color >> 8) & 0xFF) / 255.0f;
			this.particleBlue = (float)(color & 0xFF) / 255.0f;
		}else {
			this.particleRed = (float)c1.r;
			this.particleGreen = (float)c1.g;
			this.particleBlue = (float)c1.b;
		}
		
//		if (p > 0.99f)
//			System.out.println(String.format("R=%.3f, G=%.3f, B=%.3f", this.particleRed, this.particleGreen, this.particleBlue));
		
		/*-------------------------
		 * INTERPOLATE ALPHA VALUES
		 */
		AlphaEntry a1 = null;
		AlphaEntry a2 = null;
		if (type.alphaEntries.size() == 0) {
			this.particleAlpha = 1.0f;
		}else if (type.alphaEntries.size() == 1) {
			a1 = type.alphaEntries.get(0);
			this.particleAlpha = a1.alpha;
		}else {
			a1 = type.alphaEntries.get(0);
    		for (int i = 1; i < type.alphaEntries.size(); i++) {
    			a2 = type.alphaEntries.get(i);
				if (progress < a2.time) {
					break;
				}else {
					a1 = a2;
				}
			}
    		if (a1.time != a2.time) {
    			p = (progress-a1.time) / (a2.time-a1.time);		
    			//interpolate
    			this.particleAlpha = a1.alpha*(1f-p) + a2.alpha * p;
    		}else {
    			this.particleAlpha = a1.alpha;
    		}
		}
		
	
		
//		if (p > 0.99f)
//			System.out.println(String.format("A=%.3f", this.particleAlpha));
	
        
    }
    
    public int getBrightnessForRender(float p_189214_1_)
    {
        return 61680;
    }
	
    /**
	 * Retrieve what effect layer (what texture) the particle should be rendered
	 * with. 0 for the particle sprite sheet, 1 for the main Texture atlas, and 3
	 * for a custom texture
	 */
	public int getFXLayer() {
		return 3;
	}

// DON'T NEED THIS
//    //    public static class Factory implements IParticleFactory
//    {
//        public Particle createParticle(int particleID, Level worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, int... parameters)
//        {
//        	if (parameters.length <= 0) return null;
//        	TGParticleSystemType type = TGParticleList.getType(parameters[0]);
//            return new TGParticle(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, type);
//        }
//    }
	
	public double posX() {
		return posX;
	}
	
	public double posY() {
		return posY;
	}
	
	public double posZ() {
		return posZ;
	}

	
	protected Vec3 rotAxis(Vec3 p1, Vec3 axis, double sina, double cosa) {	
		  Vec3 v1 = axis.cross(p1);
		  double d1 = axis.dot(p1);
		  return p1.scale(cosa).add(v1.scale(sina)).add(axis.scale(d1*(1.0 - cosa)));			
		//  return p1.scale(cosa).add(axis.cross(p1).scale(Math.sin(a))).add(axis.scale(axis.dot(p1)*(1.0 - Math.cos(a))));			
	}

	@Override
	public Vec3 getPos() {
		return new Vec3(this.posX, this.posY, this.posZ);
	}

	@Override
	public boolean shouldRemove() {
		return !this.isAlive();
	}

	@Override
	public void updateTick() {
		this.onUpdate();
	}

	@Override
	public void doRender(TGParticleBuffers buffers, float partialTickTime, float rotX, float rotZ,
			float rotYZ, float rotXY, float rotXZ) {
		this.renderParticle(buffers, partialTickTime, rotX, rotZ, rotYZ, rotXY, rotXZ);
	}

		public AABB getRenderBoundingBox(float partialTickTime, Entity viewEnt) {
	    //float fPosX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTickTime - interpPosX);
        //float fPosY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTickTime - interpPosY);
        //float fPosZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTickTime - interpPosZ);
		double fPosX = (this.posX-viewEnt.getX());
		double fPosY = (this.posY-viewEnt.getY());
		double fPosZ = (this.posZ-viewEnt.getZ());
	    
		double s = size*0.5;
		return new AABB(fPosX-s, fPosY-s, fPosZ-s, fPosX+s, fPosY+s, fPosZ+s);
	}

	@Override
	public double getDepth() {
		return this.depth;
	}

	@Override
	public void setDepth(double depth) {
		this.depth=depth;
	}
	
	@Override
	public void setItemAttached() {
		this.itemAttached=true;
	}

	@Override
	public void setExpired() {
		super.setExpired();
		this.particleSystem=null;
	}
	
}
