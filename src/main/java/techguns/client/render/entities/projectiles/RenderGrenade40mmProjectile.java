package techguns.client.render.entities.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import techguns.Techguns;
import techguns.client.models.ModelObj;
import techguns.client.render.gl.TGRenderState;
import techguns.entities.projectiles.Grenade40mmProjectile;

public class RenderGrenade40mmProjectile extends EntityRenderer<Grenade40mmProjectile> {

	private static final ResourceLocation TEXTURE = Techguns.rl("textures/entity/launchergrenade.png");
	private static final ModelObj.Mesh MESH = new ModelObj.Mesh("models/item/grenade40mm.obj", false);

	public RenderGrenade40mmProjectile(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(Grenade40mmProjectile entity, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		ps.pushPose();
		ps.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90));
		ps.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
		ps.mulPose(Axis.XP.rotationDegrees(180));
		ps.scale(0.5f, 0.5f, 0.5f);
		TGRenderState.begin(ps, buffers, packedLight, OverlayTexture.NO_OVERLAY);
		try {
			TGRenderState.bindTexture(TEXTURE);
			MESH.render();
		} finally {
			TGRenderState.end();
		}
		ps.popPose();
		super.render(entity, entityYaw, ptt, ps, buffers, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(Grenade40mmProjectile entity) {
		return TEXTURE;
	}
}
