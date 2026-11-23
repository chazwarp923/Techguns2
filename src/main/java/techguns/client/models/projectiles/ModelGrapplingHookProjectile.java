package techguns.client.models.projectiles;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import techguns.client.models.ModelMultipart;
import techguns.client.models.ModelRenderer;

/** Flying grappling hook (Fabric port ModelGrapplingHookProjectile), converted to ModelRenderer. */
public class ModelGrapplingHookProjectile extends ModelMultipart {
	public ModelRenderer hook01;
	public ModelRenderer hook01_1;
	public ModelRenderer hook02;
	public ModelRenderer hook03;
	public ModelRenderer hook04;
	public ModelRenderer hook05;
	public ModelRenderer hook06;

	public ModelGrapplingHookProjectile() {
		this.textureWidth = 64;
		this.textureHeight = 32;
		this.hook05 = new ModelRenderer(this, 0, 0);
		this.hook05.setRotationPoint(1.4F, 0.0F, 0.0F);
		this.hook05.addCuboid(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F, -0.2F, -0.2F, -0.2F);
		setRotation(hook05, 0.0F, 0.0F, 0.7853981633974483F);
		this.hook06 = new ModelRenderer(this, 0, 0);
		this.hook06.setRotationPoint(1.4F, 0.0F, 0.0F);
		this.hook06.addCuboid(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F, -0.2F, -0.2F, -0.2F);
		setRotation(hook06, 0.0F, -0.7853981633974483F, 0.0F);
		this.hook01_1 = new ModelRenderer(this, 19, 20);
		this.hook01_1.setRotationPoint(3.0F, 0.0F, 0.0F);
		this.hook01_1.addCuboid(0.0F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F);
		setRotation(hook01_1, 0.7853981633974483F, 0.0F, 0.0F);
		this.hook04 = new ModelRenderer(this, 0, 0);
		this.hook04.setRotationPoint(1.4F, 0.0F, 0.0F);
		this.hook04.addCuboid(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F, -0.2F, -0.2F, -0.2F);
		setRotation(hook04, 0.0F, 0.7853981633974483F, 0.0F);
		this.hook03 = new ModelRenderer(this, 0, 0);
		this.hook03.setRotationPoint(1.4F, 0.0F, 0.0F);
		this.hook03.addCuboid(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F, -0.2F, -0.2F, -0.2F);
		setRotation(hook03, 0.0F, 0.0F, -0.7853981633974483F);
		this.hook02 = new ModelRenderer(this, 24, 19);
		this.hook02.setRotationPoint(1.0F, 0.0F, 0.0F);
		this.hook02.addCuboid(0.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0F);
		setRotation(hook02, 0.7853981633974483F, 0.0F, 0.0F);
		this.hook01 = new ModelRenderer(this, 31, 19);
		this.hook01.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.hook01.addCuboid(0.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F, 0.0F, -0.3F, -0.3F);
		setRotation(hook01, 0.7853981633974483F, 0.0F, 0.0F);
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale,
			int ammoLeft, float reloadProgress, ItemDisplayContext transformType, int part, float fireProgress, float chargeProgress) {
		this.hook05.render(scale);
		this.hook06.render(scale);
		this.hook01_1.render(scale);
		this.hook04.render(scale);
		this.hook03.render(scale);
		this.hook02.render(scale);
		this.hook01.render(scale);
	}
}
