package techguns.client.render.item;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.api.npc.INPCTechgunsShooter;
import techguns.api.render.IItemRenderer;
import techguns.client.models.ModelMultipart;
import techguns.client.particle.ITGParticle;
import techguns.client.particle.TGEntityParticles;
import techguns.client.particle.TGParticleBuffers;
import techguns.client.render.gl.GlStateManager;
import techguns.client.render.gl.TGRenderState;

public class RenderItemBase implements IItemRenderer {

	public static final float SCALE = 0.0625f;

	protected ModelMultipart model;
	protected ResourceLocation texture;

	protected float baseScale = 1.0f;

	protected float scale_thirdp = 0.35f;
	protected float scale_ground = 0.5f;
	protected float scale_ego = 0.5f;
	protected float scale_gui = 0.4f;
	protected float scale_itemframe = 0.5f;

	protected float[] translateBase = { 0f, 0f, 0f };

	protected float[][] translateType = { { 0f, 0f, 0f }, // TRANSLATE FIRST
			{ 0f, 0f, 0f }, // TRANSLATE THIRD PERSON
			{ 0f, 0f, 0f }, // TRANSLATE GUI
			{ 0f, 0f, 0f }, // TRANSLATE GROUND
			{ 0f, 0f, -0.05f } // TRANLATE FIXED (frame)
	};

	protected int parts = 1;

	protected String ambientParticleFX = null;

	public RenderItemBase(ModelMultipart model, ResourceLocation texture) {
		this.model = model;
		this.texture = texture;
	}

	public RenderItemBase setGUIScale(float guiscale) {
		this.scale_gui = guiscale;
		return this;
	}

	public RenderItemBase setFirstPersonScale(float scale) {
		this.scale_ego = scale;
		return this;
	}

	public RenderItemBase setGroundAndFrameScale(float scale) {
		this.scale_ground = scale;
		this.scale_itemframe = scale;
		return this;
	}

	/** Set the basic translation applied to ALL types */
	public RenderItemBase setBaseTranslation(float x, float y, float z) {
		this.translateBase[0] = x;
		this.translateBase[1] = y;
		this.translateBase[2] = z;
		return this;
	}

	/**
	 * TRANSLATE FIRST PERSON, THIRD PERSON, GUI, GROUND, FIXED (frame) x,y,z. Left hand gets
	 * automatically mirrored.
	 *
	 * @param translations - must be a float[5][3]
	 */
	public RenderItemBase setTransformTranslations(float[][] translations) {
		this.translateType = translations;
		return this;
	}

	public RenderItemBase setBaseScale(float baseScale) {
		this.baseScale = baseScale;
		return this;
	}

	protected static boolean isFirstPerson(ItemDisplayContext t) {
		return t == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || t == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
	}

	protected static boolean isThirdPerson(ItemDisplayContext t) {
		return t == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || t == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	protected float getScaleFactorFromTransform(ItemDisplayContext transform) {
		switch (transform) {
			case FIRST_PERSON_LEFT_HAND:
			case FIRST_PERSON_RIGHT_HAND:
				return baseScale * scale_ego;
			case THIRD_PERSON_LEFT_HAND:
			case THIRD_PERSON_RIGHT_HAND:
				return baseScale * scale_thirdp;
			case GUI:
				return baseScale * scale_gui;
			case GROUND:
				return baseScale * scale_ground;
			case FIXED:
				return baseScale * scale_itemframe;
			default:
				return baseScale;
		}
	}

	protected void applyTranslation(ItemDisplayContext transform) {
		int index = -1;
		boolean flip = false;
		switch (transform) {
			case FIRST_PERSON_LEFT_HAND:
				flip = true; // fallthrough
			case FIRST_PERSON_RIGHT_HAND:
				index = 0;
				break;
			case THIRD_PERSON_LEFT_HAND:
				flip = true; // fallthrough
			case THIRD_PERSON_RIGHT_HAND:
				index = 1;
				break;
			case GUI:
				index = 2;
				break;
			case GROUND:
				index = 3;
				break;
			case FIXED:
				index = 4;
				break;
			default:
				break;
		}
		if (index >= 0) {
			float mirror = flip ? -1.0f : 1.0f;
			GlStateManager.translate((translateType[index][0]) * mirror, translateType[index][1], translateType[index][2]);
		}
	}

	/** center the model */
	protected void applyBaseTranslation() {
		GlStateManager.translate(this.translateBase[0], this.translateBase[1], this.translateBase[2]);
	}

	@Override
	public void renderItem(ItemDisplayContext transform, ItemStack stack, LivingEntity elb, boolean leftHanded) {
		GlStateManager.pushMatrix();
		TGRenderState.bindTexture(texture);
		this.applyTranslation(transform);

		if (ItemDisplayContext.GUI == transform) {
			GlStateManager.rotate(40.0f, 0, 1f, 0);
			GlStateManager.rotate(20.0f, 1f, 0, 0);
		} else if (ItemDisplayContext.FIXED == transform) {
			GlStateManager.rotate(-90.0f, 0, 1.0f, 0);
		}

		this.setBaseScale(elb, transform);
		this.setBaseRotation(transform);
		this.applyBaseTranslation();

		for (int i = 0; i < parts; i++) {
			model.render(elb, 0, 0, 0, 0, 0, SCALE, 0, 0, transform, i, 0, 0f);
		}
		GlStateManager.popMatrix();
	}

	protected void setBaseScale(LivingEntity entity, ItemDisplayContext transform) {
		float scale = getScaleFactorFromTransform(transform);
		if (entity instanceof INPCTechgunsShooter shooter) {
			scale *= shooter.getGunScale();
		}
		GlStateManager.scale(scale, scale, scale);
	}

	protected void setBaseRotation(ItemDisplayContext transform) {
		GlStateManager.rotate(-180.0f, 1.0f, 0, 0);
		GlStateManager.rotate(180.0f, 0f, 1.0f, 0);
	}

	/** 1.12 renderItemParticles: particles of item attached systems, drawn in the current hand space */
	protected void renderItemParticles(LivingEntity ent, ItemDisplayContext transform) {
		if (ent == null) {
			return;
		}
		HumanoidArm side;
		if (transform == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || transform == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
			side = HumanoidArm.LEFT;
		} else if (transform == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || transform == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
			side = HumanoidArm.RIGHT;
		} else {
			return;
		}
		InteractionHand hand = ent.getMainArm() == side ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		TGEntityParticles attachments = TGEntityParticles.getIfPresent(ent);
		if (attachments == null) {
			return;
		}
		List<ITGParticle> particles = hand == InteractionHand.MAIN_HAND ? attachments.getEntityParticlesMH() : attachments.getEntityParticlesOH();
		if (particles == null || particles.isEmpty() || TGRenderState.buffers() == null) {
			return;
		}
		float ptt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
		TGParticleBuffers buffers = TGParticleBuffers.posed(TGRenderState.buffers(), TGRenderState.pose().last());
		for (ITGParticle p : particles) {
			p.doRender(buffers, ptt, 1.0f, 1.0f, 0, 0, 0);
		}
	}

	public String getAmbientParticleFX() {
		return ambientParticleFX;
	}

	public RenderItemBase setAmbientParticleFX(String ambientParticleFX) {
		this.ambientParticleFX = ambientParticleFX;
		return this;
	}
}
