package techguns.client.render.entities.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import techguns.Techguns;
import techguns.client.models.projectiles.ModelRocket;
import techguns.client.render.gl.TGRenderState;
import techguns.entities.projectiles.RocketProjectile;
import techguns.entities.projectiles.RocketProjectileHV;
import techguns.entities.projectiles.RocketProjectileNuke;

public class RenderRocketProjectile extends EntityRenderer<RocketProjectile> {

	private static final ResourceLocation TEX = Techguns.rl("textures/guns/rocket.png");
	private static final ResourceLocation TEX_NUKE = Techguns.rl("textures/guns/rocket_nuke.png");
	private static final ResourceLocation TEX_HV = Techguns.rl("textures/guns/rocket_hv.png");

	private final ModelRocket model = new ModelRocket();

	public RenderRocketProjectile(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(RocketProjectile entity, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		ps.pushPose();
		ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(ptt, entity.yRotO, entity.getYRot()) - 90.0F));
		ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(ptt, entity.xRotO, entity.getXRot())));
		ps.scale(0.9f, 0.9f, 0.9f);
		TGRenderState.begin(ps, buffers, packedLight, OverlayTexture.NO_OVERLAY);
		try {
			TGRenderState.bindTexture(getTextureLocation(entity));
			model.render(entity, 0, 0, 0, 0, 0, 0.0625F);
		} finally {
			TGRenderState.end();
		}
		ps.popPose();
		super.render(entity, entityYaw, ptt, ps, buffers, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(RocketProjectile entity) {
		if (entity instanceof RocketProjectileNuke) return TEX_NUKE;
		if (entity instanceof RocketProjectileHV) return TEX_HV;
		return TEX;
	}
}
