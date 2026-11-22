package techguns.client.render.entities.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import techguns.Techguns;
import techguns.entities.projectiles.LaserProjectile;

/** Laser beam: start segment + scrolling beam segment, two crossed planes. */
public class RenderLaserProjectile extends RenderBeamBase<LaserProjectile> {

	private static final ResourceLocation TEXTURE = Techguns.rl("textures/fx/laser3.png");
	private static final ResourceLocation TEXTURE_START = Techguns.rl("textures/fx/laser3_start.png");
	private static final double LASER_WIDTH = 3.0d;

	public RenderLaserProjectile(EntityRendererProvider.Context ctx) {
		super(ctx, TEXTURE);
	}

	@Override
	public void render(LaserProjectile laser, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		double distance = laser.distance;
		int maxTicks = Math.max(1, laser.maxTicks);
		float prog = ((float) laser.tickCount) / ((float) maxTicks);
		double width = LASER_WIDTH * (Math.sin(Math.sqrt(prog) * Math.PI)) * 2;
		double distanceStart = Math.min(1.0d, distance);
		double u = (distance / LASER_WIDTH) * 2.0D;

		ps.pushPose();
		ps.mulPose(Axis.YP.rotationDegrees(laser.laserYaw - 90F));
		ps.mulPose(Axis.ZP.rotationDegrees(laser.laserPitch));
		float f10 = 0.0125F;
		distance *= 80.0D;
		distanceStart *= 80.0D;
		float rotX = 45f + (prog * 180f);
		ps.mulPose(Axis.XP.rotationDegrees(rotX + 90f));
		ps.scale(f10, f10, f10);
		float brightness = (float) Math.sin(Math.sqrt(prog) * Math.PI);

		if (distance > distanceStart) {
			VertexConsumer vc = additive(buffers, TEXTURE);
			ps.pushPose();
			for (int i = 0; i < 2; ++i) {
				ps.mulPose(Axis.XP.rotationDegrees(90.0F));
				PoseStack.Pose pose = ps.last();
				v(vc, pose, distance, -width, 0, u + prog, 0, brightness);
				v(vc, pose, distanceStart, -width, 0, prog, 0, brightness);
				v(vc, pose, distanceStart, width, 0, prog, 1, brightness);
				v(vc, pose, distance, width, 0, u + prog, 1, brightness);
			}
			ps.popPose();
		}
		VertexConsumer vc = additive(buffers, TEXTURE_START);
		ps.pushPose();
		for (int i = 0; i < 2; ++i) {
			ps.mulPose(Axis.XP.rotationDegrees(90.0F));
			PoseStack.Pose pose = ps.last();
			v(vc, pose, distanceStart, -width, 0, 1, 0, brightness);
			v(vc, pose, 0, -width, 0, 0, 0, brightness);
			v(vc, pose, 0, width, 0, 0, 1, brightness);
			v(vc, pose, distanceStart, width, 0, 1, 1, brightness);
		}
		ps.popPose();
		ps.popPose();
	}
}
