package techguns.client.render.entities.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import techguns.client.render.TGRenderHelper;
import techguns.client.render.gl.TGRenderTypes;

/** Shared helpers for the additive FX style projectile renderers (POSITION_TEX_COLOR quads). */
public abstract class RenderBeamBase<T extends Entity> extends EntityRenderer<T> {

	protected final ResourceLocation texture;

	protected RenderBeamBase(EntityRendererProvider.Context ctx, ResourceLocation texture) {
		super(ctx);
		this.texture = texture;
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return texture;
	}

	protected static VertexConsumer additive(MultiBufferSource buffers, ResourceLocation tex) {
		return buffers.getBuffer(TGRenderTypes.fx(tex, TGRenderHelper.RenderType.ADDITIVE));
	}

	protected static void v(VertexConsumer vc, PoseStack.Pose pose, double x, double y, double z, double u, double v, float r, float g, float b,
			float a) {
		vc.addVertex(pose, (float) x, (float) y, (float) z).setUv((float) u, (float) v).setColor(r, g, b, a);
	}

	protected static void v(VertexConsumer vc, PoseStack.Pose pose, double x, double y, double z, double u, double v, float a) {
		v(vc, pose, x, y, z, u, v, 1f, 1f, 1f, a);
	}
}
