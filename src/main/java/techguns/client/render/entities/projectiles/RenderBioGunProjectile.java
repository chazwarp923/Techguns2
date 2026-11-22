package techguns.client.render.entities.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import techguns.Techguns;
import techguns.entities.projectiles.BioGunProjectile;

/** Camera facing additive goo sprite, bigger with higher charge level (1.12 RenderBioGunProjectile). */
public class RenderBioGunProjectile extends RenderBeamBase<BioGunProjectile> {

	private static final float BASE_SIZE = 0.15f;

	public RenderBioGunProjectile(EntityRendererProvider.Context ctx) {
		super(ctx, Techguns.rl("textures/entity/bioblob.png"));
	}

	@Override
	public void render(BioGunProjectile entity, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		float scale = switch (entity.chargeLevel) {
			case 1 -> 1.25f;
			case 2 -> 2.5f;
			case 3 -> 4.0f;
			default -> 1.0f;
		};
		ps.pushPose();
		float s = BASE_SIZE * scale;
		ps.scale(s, s, s);
		ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
		PoseStack.Pose pose = ps.last();
		VertexConsumer vc = additive(buffers, texture);
		v(vc, pose, -0.5, -0.25, 0, 0, 1, 1f);
		v(vc, pose, 0.5, -0.25, 0, 1, 1, 1f);
		v(vc, pose, 0.5, 0.75, 0, 1, 0, 1f);
		v(vc, pose, -0.5, 0.75, 0, 0, 0, 1f);
		ps.popPose();
	}
}
