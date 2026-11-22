package techguns.client.models;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;

/** Minimal re-implementation of the 1.12.2 net.minecraft.client.model.ModelBase used by Techguns models. */
public abstract class ModelBase {
	public int textureWidth = 64;
	public int textureHeight = 32;
	public List<ModelRenderer> boxList = new ArrayList<>();
	public float swingProgress;
	public boolean isRiding;
	public boolean isChild = true;

	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
	}

	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor,
			Entity entityIn) {
	}

	public static void copyModelAngles(ModelRenderer source, ModelRenderer dest) {
		dest.rotateAngleX = source.rotateAngleX;
		dest.rotateAngleY = source.rotateAngleY;
		dest.rotateAngleZ = source.rotateAngleZ;
		dest.rotationPointX = source.rotationPointX;
		dest.rotationPointY = source.rotationPointY;
		dest.rotationPointZ = source.rotationPointZ;
	}
}
