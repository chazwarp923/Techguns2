package techguns.client.render.entities.projectiles;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import techguns.Techguns;
import techguns.entities.projectiles.TeslaProjectile;

/** Lightning bolt made of overlapping sine curves with a travelling pulse. */
public class RenderTeslaProjectile extends RenderBeamBase<TeslaProjectile> {

	static final int SIN_COUNT = 5;
	static final double WIDTH = 0.5;
	static final double SIN_DISTANCE = 10.0;
	static final double OFFSET = 0.20;

	public RenderTeslaProjectile(EntityRendererProvider.Context ctx) {
		super(ctx, Techguns.rl("textures/fx/laser_blue.png"));
	}

	@Override
	public void render(TeslaProjectile laser, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		double distance = laser.distance;
		if (distance <= 0) return;
		int maxTicks = Math.max(1, laser.maxTicks);
		float progress = ((float) laser.tickCount) / ((float) maxTicks);
		Random rand = new Random(laser.seed);
		double[] dY = new double[SIN_COUNT];
		double[] dZ = new double[SIN_COUNT];
		for (int i = 0; i < SIN_COUNT; i++) {
			dY[i] = 0.5 - rand.nextDouble();
			dZ[i] = 0.5 - rand.nextDouble();
		}
		int count = Math.max(1, (int) Math.round(distance / OFFSET));
		double offset = distance / (double) count;
		int xreps = Math.max(1, (int) Math.round(distance / SIN_DISTANCE));
		double xprev = 0, yprev = 0, zprev = 0, widthprev = 1.0;

		VertexConsumer vc = additive(buffers, texture);
		ps.pushPose();
		ps.mulPose(Axis.YP.rotationDegrees(laser.laserYaw - 90F));
		ps.mulPose(Axis.ZP.rotationDegrees(laser.laserPitch));
		ps.mulPose(Axis.XP.rotationDegrees(45.0f));
		PoseStack.Pose pose = ps.last();
		for (int i = 0; i <= count; i++) {
			double d = (double) i / (double) count;
			double x = (double) i * offset;
			double y = 0;
			double z = 0;
			if (i > 1) {
				for (int j = 1; j <= SIN_COUNT; j++) {
					double yfactor = ((rand.nextDouble() - 0.5) + (progress * dY[j - 1])) * (2.0 / (double) j);
					double zfactor = ((rand.nextDouble() - 0.5) + (progress * dZ[j - 1])) * (2.0 / (double) j);
					y += Math.sin((d * Math.PI) * (double) (j * xreps)) * yfactor;
					z += Math.sin((d * Math.PI) * (double) (j * xreps)) * zfactor;
				}
			}
			double pulse = 1.0 - Math.sqrt(Math.abs(progress - d) * 2.0);
			double width = Math.max(0.0, WIDTH * pulse);
			if (i >= 1) {
				drawSegment(vc, pose, xprev, yprev, zprev, x, y, z, widthprev, width, (float) Math.max(0, pulse));
			}
			widthprev = width;
			xprev = x;
			yprev = y;
			zprev = z;
		}
		ps.popPose();
	}

	private static void drawSegment(VertexConsumer vc, PoseStack.Pose pose, double x1, double y1, double z1, double x2, double y2, double z2,
			double width1, double width2, float a) {
		double scale = 0.5;
		y1 *= scale;
		y2 *= scale;
		z1 *= scale;
		z2 *= scale;
		width1 *= scale;
		width2 *= scale;
		v(vc, pose, x1, y1 - width1, z1, 0, 0, a);
		v(vc, pose, x1, y1 + width1, z1, 0, 1, a);
		v(vc, pose, x2, y2 + width2, z2, 1, 1, a);
		v(vc, pose, x2, y2 - width2, z2, 1, 0, a);

		v(vc, pose, x1, y1, z1 - width1, 0, 0, a);
		v(vc, pose, x1, y1, z1 + width1, 0, 1, a);
		v(vc, pose, x2, y2, z2 + width2, 1, 1, a);
		v(vc, pose, x2, y2, z2 - width2, 1, 0, a);
	}
}
