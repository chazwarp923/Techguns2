package techguns.client.render.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.ModelMultipart;
import techguns.client.render.gl.GlStateManager;
import techguns.client.render.gl.TGRenderState;
import techguns.items.guns.GenericGrenade;

/** Hand grenade item; tilts back in first person while winding up a throw. */
public class RenderGrenade extends RenderItemBase {

	protected float baseRotY = 180.0f;

	public RenderGrenade(ModelMultipart model, ResourceLocation texture) {
		super(model, texture);
	}

	public RenderGrenade(ModelMultipart model, ResourceLocation texture, float baseRotationY) {
		super(model, texture);
		this.baseRotY = baseRotationY;
	}

	@Override
	protected void setBaseRotation(ItemDisplayContext transform) {
		GlStateManager.rotate(-180.0f, 1.0f, 0, 0);
		GlStateManager.rotate(baseRotY, 0f, 1.0f, 0);
	}

	@Override
	public void renderItem(ItemDisplayContext transform, ItemStack stack, LivingEntity elb, boolean leftHanded) {
		float chargeProgress = 0;
		GlStateManager.pushMatrix();
		TGRenderState.bindTexture(texture);
		this.applyTranslation(transform);
		if (isFirstPerson(transform)) {
			if (elb != null && elb.getUseItem() == stack && stack.getItem() instanceof GenericGrenade grenade) {
				chargeProgress = Math.max(0f, Math.min(1f, elb.getTicksUsingItem() / grenade.fullChargeTime));
				GlStateManager.rotate(25.0f * chargeProgress, 1f, 0f, 0f);
			}
		} else if (ItemDisplayContext.GUI == transform) {
			GlStateManager.rotate(40.0f, 0, 1f, 0);
			GlStateManager.rotate(20.0f, 1f, 0, 0);
		} else if (ItemDisplayContext.FIXED == transform) {
			GlStateManager.rotate(-90.0f, 0, 1.0f, 0);
		}
		this.setBaseScale(elb, transform);
		this.setBaseRotation(transform);
		this.applyBaseTranslation();
		for (int i = 0; i < parts; i++) {
			model.render(elb, 0, 0, 0, 0, 0, SCALE, 0, 0, transform, i, chargeProgress, chargeProgress);
		}
		GlStateManager.popMatrix();
	}
}
