package techguns.client.render.entities.projectiles;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import techguns.Techguns;
import techguns.entities.projectiles.SonicShotgunProjectile;

/** Only the main projectile draws: expanding shock wave rings. */
public class RenderSonicShotgunProjectile extends RenderBeamBase<SonicShotgunProjectile> {

	public RenderSonicShotgunProjectile(EntityRendererProvider.Context ctx) {
		super(ctx, Techguns.rl("textures/fx/shockwave.png"));
	}

	@Override
	public void render(SonicShotgunProjectile proj, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		if (proj.tickCount < 3 || !proj.mainProjectile) {
			return;
		}
		Random rand = new Random(proj.getId());
		ps.pushPose();
		ps.mulPose(Axis.YP.rotationDegrees(proj.getYRot() - 90));
		ps.mulPose(Axis.ZP.rotationDegrees(proj.getXRot()));
		ps.mulPose(Axis.XP.rotationDegrees(90.0F));
		double d = ((float) proj.tickCount + ptt) * 0.3;
		float prog = (float) Math.min(1.0, ((float) proj.tickCount + ptt) / 20.0f);
		float opacity = 1.0f - (float) (Math.pow(prog, 2.0));

		VertexConsumer vc = additive(buffers, texture);
		PoseStack.Pose pose = ps.last();
		for (int i = 0; i <= 5; i++) {
			float r = opacity * rand.nextFloat();
			float g = opacity * rand.nextFloat();
			float b = opacity * (0.5f + (rand.nextFloat() * 0.5f));
			double offset = 2.5 - (double) i;
			double size = d * (0.5 + (rand.nextDouble()));
			v(vc, pose, offset, -size, -size, 0, 0, r, g, b, opacity);
			v(vc, pose, offset, -size, size, 0, 1, r, g, b, opacity);
			v(vc, pose, offset, size, size, 1, 1, r, g, b, opacity);
			v(vc, pose, offset, size, -size, 1, 0, r, g, b, opacity);
		}
		ps.popPose();
	}
}
