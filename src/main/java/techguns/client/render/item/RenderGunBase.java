package techguns.client.render.item;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.TGAttachments;
import techguns.api.capabilities.AttackTime;
import techguns.api.npc.INPCTechgunsShooter;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.TGClientState;
import techguns.client.models.ModelMultipart;
import techguns.client.render.fx.IScreenEffect;
import techguns.client.render.gl.GlStateManager;
import techguns.client.render.gl.TGRenderState;
import techguns.items.guns.GenericGun;

public class RenderGunBase extends RenderItemBase {

	protected IScreenEffect muzzleFX = null;
	protected IScreenEffect chargeFX = null;
	protected float chargeFX_x_l = 0f;
	protected float chargeFX_x_r = 0f;
	protected float chargeFX_y = 0f;
	protected float chargeFX_z = 0f;
	protected float chargeFX_scale = 1.0f;
	protected float chargeFX_3p_y = 0f;
	protected float chargeFX_3p_z = 0f;
	protected float chargeFX_3p_scale = 0.5f;
	protected IScreenEffect scope = null;

	protected float muzzleFX_x_l = 0f;
	protected float muzzleFX_x_r = 0f;
	protected float muzzleFX_y = 0f;
	protected float muzzleFX_z = 0f;
	protected float muzzleFX_scale = 1.0f;

	protected float muzzleFX_3p_y = 0f;
	protected float muzzleFX_3p_z = 0f;
	protected float muzzleFX_3p_scale = 0.5f;

	protected float mf_jitterX = 0f;
	protected float mf_jitterY = 0f;
	protected float mf_jitterAngle = 0f;
	protected float mf_jitterScale = 0f;

	protected GunAnimation recoilAnim = GunAnimation.genericRecoil;
	protected float[] recoilParams = new float[] { 0.15f, 5.0f };

	protected GunAnimation reloadAnim = GunAnimation.genericReload;
	protected float[] reloadParams = new float[] { 1.0f, 40.0f };

	protected GunAnimation scopeRecoilAnim = null;
	protected float[] scopeRecoilParams = null;

	protected GunAnimation recoilAnim3p = GunAnimation.genericRecoil;
	protected float[] recoilParams3p = new float[] { 0f, 5.0f };

	protected GunAnimation reloadAnim3p = GunAnimation.genericReload;
	protected float[] reloadParams3p = new float[] { 0f, 40.0f };

	protected float scopescale = 3.0f;

	protected float chargeTranslation = 0.25f;

	public RenderGunBase(ModelMultipart model, int parts) {
		super(model, null);
		this.parts = parts;
		this.scale_ground = this.scale_thirdp;
	}

	@Override
	public RenderGunBase setBaseTranslation(float x, float y, float z) {
		return (RenderGunBase) super.setBaseTranslation(x, y, z);
	}

	@Override
	public RenderGunBase setGUIScale(float guiscale) {
		return (RenderGunBase) super.setGUIScale(guiscale);
	}

	@Override
	public RenderGunBase setFirstPersonScale(float scale) {
		return (RenderGunBase) super.setFirstPersonScale(scale);
	}

	@Override
	public RenderGunBase setGroundAndFrameScale(float scale) {
		return (RenderGunBase) super.setGroundAndFrameScale(scale);
	}

	public RenderGunBase setReloadAnim(GunAnimation anim, float... params) {
		this.reloadAnim = anim;
		this.reloadParams = params;
		return this;
	}

	public RenderGunBase setRecoilAnim(GunAnimation anim, float... params) {
		this.recoilAnim = anim;
		this.recoilParams = params;
		return this;
	}

	public RenderGunBase setScopeRecoilAnim(GunAnimation anim, float... params) {
		this.scopeRecoilAnim = anim;
		this.scopeRecoilParams = params;
		return this;
	}

	public RenderGunBase setReloadAnim3p(GunAnimation anim, float... params) {
		this.reloadAnim3p = anim;
		this.reloadParams3p = params;
		return this;
	}

	public RenderGunBase setRecoilAnim3p(GunAnimation anim, float... params) {
		this.recoilAnim3p = anim;
		this.recoilParams3p = params;
		return this;
	}

	public RenderGunBase setChargeTranslationAmount(float value) {
		this.chargeTranslation = value;
		return this;
	}

	@Override
	public RenderGunBase setTransformTranslations(float[][] translations) {
		return (RenderGunBase) super.setTransformTranslations(translations);
	}

	public RenderGunBase setMuzzleFx(IScreenEffect muzzleFX, float x, float y, float z, float scale, float x_l) {
		this.muzzleFX = muzzleFX;
		this.muzzleFX_x_r = x;
		this.muzzleFX_x_l = x_l;
		this.muzzleFX_y = y;
		this.muzzleFX_z = z;
		this.muzzleFX_scale = scale;
		return this;
	}

	/** Fabric port: an effect drawn at the muzzle while charging, given the charge (going on past 1 while held) */
	public RenderGunBase setChargeFx(IScreenEffect chargeFX, float x, float y, float z, float scale, float x_l) {
		this.chargeFX = chargeFX;
		this.chargeFX_x_r = x;
		this.chargeFX_x_l = x_l;
		this.chargeFX_y = y;
		this.chargeFX_z = z;
		this.chargeFX_scale = scale;
		return this;
	}

	public RenderGunBase setChargeFXPos3P(float y, float z, float scale) {
		this.chargeFX_3p_y = y;
		this.chargeFX_3p_z = z;
		this.chargeFX_3p_scale = scale;
		return this;
	}

	public RenderGunBase setMuzzleFXPos3P(float y, float z) {
		this.muzzleFX_3p_y = y;
		this.muzzleFX_3p_z = z;
		return this;
	}

	public RenderGunBase setScope(IScreenEffect scope) {
		this.scope = scope;
		return this;
	}

	public RenderGunBase setScope(IScreenEffect scope, float scale) {
		this.scope = scope;
		this.scopescale = scale;
		return this;
	}

	public boolean hasScopeTexture() {
		return this.scope != null;
	}

	protected static AttackTime getAttackTime(LivingEntity ent, boolean offHand) {
		if (ent instanceof Player ply) {
			return TGExtendedPlayer.get(ply).getAttackTime(offHand);
		} else if (ent != null) {
			return ent.getData(TGAttachments.SHOOTER_VALUES);
		}
		return null;
	}

	@Override
	public void renderItem(ItemDisplayContext transform, ItemStack stack, LivingEntity entityIn, boolean leftHand) {
		GenericGun gun = ((GenericGun) stack.getItem());

		boolean sneaking = false;
		boolean isOffhand = false;
		if (entityIn != null) {
			sneaking = entityIn.isShiftKeyDown();
			isOffhand = ((!leftHand) && entityIn.getMainArm() == HumanoidArm.LEFT) || ((leftHand) && entityIn.getMainArm() == HumanoidArm.RIGHT);
		}

		GlStateManager.pushMatrix();

		float fireProgress = 0.0f;
		float reloadProgress = 0.0f;
		float muzzleFlashProgress = 0.0f;
		float chargeProgress = 0.0f;
		/** the charge, going on past 1 while held (for the charge effect) */
		float chargeProgressFull = 0.0f;
		byte attackType = 0;

		boolean renderScope = false;

		AttackTime attack = (isFirstPerson(transform) || isThirdPerson(transform)) ? getAttackTime(entityIn, isOffhand) : null;
		if (attack != null) {
			attackType = attack.getAttackType();

			if (gun.canCharge() && !isOffhand && entityIn.isUsingItem()) {
				chargeProgress = getChargeProgress(gun, stack, entityIn);
				if (gun instanceof techguns.items.guns.GenericGunCharge charge && entityIn.getUseItem() == stack) {
					chargeProgressFull = Math.max(0f, entityIn.getTicksUsingItem() / charge.fullChargeTime);
				}
			} else if (attack.isReloading()) {
				long diff = attack.getReloadTime() - System.currentTimeMillis();
				if (diff <= 0) {
					attack.setReloadTime(0);
					attack.setReloadTimeTotal(0);
					attack.setAttackType((byte) 0);
				} else {
					reloadProgress = 1.0f - ((float) diff / (float) attack.getReloadTimeTotal());
				}
			} else if (attack.isRecoiling()) {
				long diff = attack.getRecoilTime() - System.currentTimeMillis();
				if (diff <= 0) {
					attack.setRecoilTime(0);
					attack.setRecoilTimeTotal(0);
					attack.setAttackType((byte) 0);
					attack.setRecoilChargeProgress(0f);
				} else {
					fireProgress = 1.0f - ((float) diff / (float) attack.getRecoilTimeTotal());
					if (gun.canCharge()) {
						chargeProgress = (1.0f - fireProgress) * attack.getRecoilChargeProgress();
					}
				}
			}

			// muzzle flash progress
			if (attack.getMuzzleFlashTime() > 0) {
				long diff = attack.getMuzzleFlashTime() - System.currentTimeMillis();
				if (diff <= 0 || diff > attack.getMuzzleFlashTimeTotal()) {
					attack.setMuzzleFlashTime(0L);
					attack.setMuzzleFlashTimeTotal(0);
				} else {
					muzzleFlashProgress = 1.0f - ((float) diff / (float) attack.getMuzzleFlashTimeTotal());
				}
			}
		}

		this.applyTranslation(transform);

		if (isFirstPerson(transform)) {
			if (!isOffhand && gun.isZooming() && this.scope != null) {
				renderScope = true;
			} else {
				this.transformFirstPerson(fireProgress, reloadProgress, chargeProgress, ItemDisplayContext.FIRST_PERSON_LEFT_HAND == transform,
						sneaking && isOffhand);
			}
		} else if (isThirdPerson(transform)) {
			this.transformThirdPerson(entityIn, fireProgress, reloadProgress, ItemDisplayContext.THIRD_PERSON_LEFT_HAND == transform);
		} else if (ItemDisplayContext.GUI == transform) {
			this.transformGUI();
		} else if (ItemDisplayContext.GROUND == transform) {
			this.transformGround();
		} else if (ItemDisplayContext.FIXED == transform) {
			this.transformFixed();
		}

		if (!renderScope) {
			GlStateManager.pushMatrix();
			this.setBaseScale(entityIn, transform);
			this.setBaseRotation(transform);
			this.applyBaseTranslation();

			GlStateManager.color(1f, 1f, 1f, 1f);
			for (int i = 0; i < parts; i++) {
				this.bindTextureForPart(gun, i, stack);
				this.setGLColorForPart(gun, i, stack);
				model.render(entityIn, 0, 0, 0, 0, 0, SCALE, gun.getAmmoLeft(stack), reloadProgress, transform, i, fireProgress, chargeProgress);
				GlStateManager.color(1f, 1f, 1f, 1f);
			}
			GlStateManager.popMatrix();

			// item attached particles (Shishkebap flames), in hand space; 1.12 applied the reload animation once more for them
			GlStateManager.pushMatrix();
			if (reloadProgress != 0) {
				if (isFirstPerson(transform)) {
					this.transformFirstPerson(0f, reloadProgress, 0f, ItemDisplayContext.FIRST_PERSON_LEFT_HAND == transform, sneaking && isOffhand);
				} else if (isThirdPerson(transform)) {
					this.transformThirdPerson(entityIn, 0f, reloadProgress, ItemDisplayContext.THIRD_PERSON_LEFT_HAND == transform);
				}
			}
			this.renderItemParticles(entityIn, transform);
			GlStateManager.popMatrix();
			GlStateManager.popMatrix();

			// Draw muzzle FX (1.12: in hand space, without the per-transform translation / animation)
			if (muzzleFlashProgress > 0) {
				if (isFirstPerson(transform)) {
					this.drawMuzzleFx(muzzleFlashProgress, attackType, leftHand);
				} else {
					this.drawMuzzleFx3P(muzzleFlashProgress, attackType, leftHand);
				}
			} else {
				if (reloadProgress <= 0) {
					if (isFirstPerson(transform)) {
						this.drawIdleFx(leftHand);
					} else {
						this.drawIdleFx3P(leftHand);
					}
				}
				if (chargeProgressFull > 0 && this.chargeFX != null) {
					String variant = gun.getCurrentAmmoVariantKey(stack);
					if (isFirstPerson(transform)) {
						float x = leftHand ? this.chargeFX_x_l : this.chargeFX_x_r;
						this.chargeFX.doRender(chargeProgressFull, x, this.chargeFX_y, this.chargeFX_z, this.chargeFX_scale, false, variant);
					} else {
						this.chargeFX.doRender(chargeProgressFull, 0, this.chargeFX_3p_y, this.chargeFX_3p_z, this.chargeFX_scale * this.chargeFX_3p_scale,
								true, variant);
					}
				}
			}
		} else {
			GlStateManager.popMatrix();
			GlStateManager.pushMatrix();
			if (this.scopeRecoilAnim != null && fireProgress > 0f) {
				this.scopeRecoilAnim.play(fireProgress, ItemDisplayContext.FIRST_PERSON_LEFT_HAND == transform, this.scopeRecoilParams);
			}
			this.renderScope(fireProgress, leftHand);
			GlStateManager.popMatrix();
		}
	}

	protected float getChargeProgress(GenericGun gun, ItemStack stack, LivingEntity entityIn) {
		if (gun instanceof techguns.items.guns.GenericGunCharge charge && entityIn.getUseItem() == stack) {
			return Math.max(0f, Math.min(1f, entityIn.getTicksUsingItem() / charge.fullChargeTime));
		}
		return 0f;
	}

	protected void drawIdleFx(boolean leftHand) {
	}

	protected void drawIdleFx3P(boolean leftHand) {
	}

	protected void setGLColorForPart(GenericGun gun, int part, ItemStack stack) {
	}

	protected void transformFirstPerson(float fireProgress, float reloadProgress, float chargeProgress, boolean left, boolean shoudLowerWeapon) {
		if (chargeProgress > 0) {
			GlStateManager.translate(0, 0, this.chargeTranslation * chargeProgress);
		}
		if (fireProgress > 0) {
			this.recoilAnim.play(fireProgress, left, this.recoilParams);
		} else if (reloadProgress > 0) {
			this.reloadAnim.play(reloadProgress, left, this.reloadParams);
		} else if (shoudLowerWeapon) {
			GlStateManager.rotate(-35f, 1, 0, 0);
		}
	}

	protected void transformThirdPerson(LivingEntity ent, float fireProgress, float reloadProgress, boolean left) {
		if (ent instanceof INPCTechgunsShooter shooter && !shooter.hasWeaponArmPose()) {
			GlStateManager.rotate(90.0f, 1, 0, 0);
		}
		if (fireProgress > 0) {
			this.recoilAnim3p.play(fireProgress, left, this.recoilParams3p);
		} else if (reloadProgress > 0) {
			this.reloadAnim3p.play(reloadProgress, left, this.reloadParams3p);
		}
	}

	protected void transformGUI() {
		GlStateManager.rotate(40.0f, 0, 1f, 0);
		GlStateManager.rotate(20.0f, 1f, 0, 0);
	}

	protected void transformGround() {
	}

	protected void transformFixed() {
		GlStateManager.rotate(-90.0f, 0, 1.0f, 0);
	}

	protected void drawMuzzleFx(float progress, byte attackType, boolean leftHand) {
		if (this.muzzleFX != null) {
			float x = leftHand ? this.muzzleFX_x_l : this.muzzleFX_x_r;
			float scale = this.muzzleFX_scale;
			float offsetX = x;
			float offsetY = this.muzzleFX_y;
			if (this.mf_jitterScale > 0.0f) scale += mf_jitterScale * TGClientState.muzzleFlashJitterScale;
			if (this.mf_jitterX > 0.0f) offsetX += mf_jitterX * TGClientState.muzzleFlashJitterX;
			if (this.mf_jitterY > 0.0f) offsetY += mf_jitterY * TGClientState.muzzleFlashJitterY;
			this.muzzleFX.doRender(progress, offsetX, offsetY, this.muzzleFX_z, scale, false);
		}
	}

	protected void drawMuzzleFx3P(float progress, byte attackType, boolean leftHand) {
		if (this.muzzleFX != null) {
			this.muzzleFX.doRender(progress, 0, this.muzzleFX_3p_y, this.muzzleFX_3p_z, this.muzzleFX_scale * this.muzzleFX_3p_scale, true);
		}
	}

	protected void renderScope(float fireProgress, boolean leftHand) {
		if (this.scope != null) {
			float x = leftHand ? 0.56f : -0.56f;
			this.scope.doRender(fireProgress, x, 0.52f, 0f, scopescale, false);
		}
	}

	@Override
	public RenderGunBase setBaseScale(float baseScale) {
		return (RenderGunBase) super.setBaseScale(baseScale);
	}

	public RenderGunBase setMuzzleFlashJitter(float jX, float jY, float jAngle, float jScale) {
		this.mf_jitterX = jX;
		this.mf_jitterY = jY;
		this.mf_jitterAngle = jAngle;
		this.mf_jitterScale = jScale;
		return this;
	}

	public void bindTextureForPart(GenericGun gun, int part, ItemStack stack) {
		if (part == 0) {
			if (gun.hasCustomTexture) {
				TGRenderState.bindTexture(gun.getCurrentTexture(stack));
			} else {
				TGRenderState.bindTexture(InventoryMenu.BLOCK_ATLAS);
			}
		}
	}

	@Override
	public RenderGunBase setAmbientParticleFX(String ambientParticleFX) {
		this.ambientParticleFX = ambientParticleFX;
		return this;
	}
}
