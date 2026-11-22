package techguns.client.render.entities.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import techguns.client.models.ModelMultipart;
import techguns.client.render.gl.GlStateManager;
import techguns.client.render.gl.TGRenderState;
import techguns.client.render.item.RenderItemBase;
import techguns.entities.projectiles.GrenadeProjectile;

/** Spinning thrown hand grenade (1.12 RenderGrenadeProjectile / RenderFragGrenadeProjectile). */
public class RenderGrenadeProjectile<T extends GrenadeProjectile> extends EntityRenderer<T> {

	private final ModelMultipart model;
	private final ResourceLocation texture;

	public RenderGrenadeProjectile(EntityRendererProvider.Context ctx, ModelMultipart model, ResourceLocation texture) {
		super(ctx);
		this.model = model;
		this.texture = texture;
	}

	@Override
	public void render(T entity, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		ps.pushPose();
		TGRenderState.begin(ps, buffers, packedLight, OverlayTexture.NO_OVERLAY);
		try {
			GlStateManager.rotate(Mth.lerp(ptt, entity.yRotO, entity.getYRot()) - 90.0f, 0f, 1f, 0f);
			GlStateManager.rotate(Mth.lerp(ptt, entity.xRotO, entity.getXRot()), 0f, 0f, 1f);
			float f = ((entity.tickCount % 20) + ptt) / 20.0f;
			GlStateManager.rotate(-360.0f * f, 0f, 0f, 1f);
			GlStateManager.scale(0.8f, 0.8f, 0.8f);
			GlStateManager.rotate(-180.0f, 1.0f, 0, 0);
			GlStateManager.rotate(180.0f, 0f, 1.0f, 0);
			GlStateManager.translate(-RenderItemBase.SCALE * 0.5f, 0.83f, -RenderItemBase.SCALE);
			TGRenderState.bindTexture(texture);
			model.render(entity, 0, 0, 0, 0, 0, RenderItemBase.SCALE, 0, 0, ItemDisplayContext.NONE, 0, 0, 0);
		} finally {
			TGRenderState.end();
		}
		ps.popPose();
		super.render(entity, entityYaw, ptt, ps, buffers, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return texture;
	}
}
