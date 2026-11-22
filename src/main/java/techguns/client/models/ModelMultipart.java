package techguns.client.models;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;

public abstract class ModelMultipart extends ModelBase {

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, 100, -1.0f, ItemDisplayContext.GROUND, 0, 0.0f, 0f);
	}

	public abstract void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
			float scale, int ammoLeft, float reloadProgress, ItemDisplayContext transformType, int part, float fireProgress, float chargeProgress);

	protected void setRotation(ModelRenderer model, float x, float y, float z) {
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}
}
