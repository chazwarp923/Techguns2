package techguns.client.render.entities.projectiles;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import techguns.Techguns;
import techguns.entities.projectiles.NDRProjectile;
import techguns.util.MathUtil;

/** Nuclear death ray: animated beam from the shooter's gun plus a spiral. */
public class RenderNDRProjectile extends RenderBeamBase<NDRProjectile> {

	private static final double LASER_WIDTH = 3.0D;

	public RenderNDRProjectile(EntityRendererProvider.Context ctx) {
		super(ctx, Techguns.rl("textures/fx/nukebeam.png"));
	}

	@Override
	public boolean shouldRender(NDRProjectile entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
		return entity.getShooter() != null;
	}

	@Override
	public void render(NDRProjectile entity, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		LivingEntity shooter = entity.getShooter();
		if (shooter == null) return;
		Minecraft mc = Minecraft.getInstance();
		Random rand = new Random(entity.getId());
		double distance = entity.distance;
		float prog = ((float) entity.tickCount + ptt) / ((float) Math.max(1, entity.maxTicks));
		double width = LASER_WIDTH * (Math.sin(Math.sqrt(prog) * Math.PI)) * 2;

		// pose origin is the (interpolated) entity position; move to the shooter's eyes
		double x = Mth.lerp(ptt, shooter.xo, shooter.getX()) - Mth.lerp(ptt, entity.xo, entity.getX());
		double y = Mth.lerp(ptt, shooter.yo, shooter.getY()) + shooter.getEyeHeight() - Mth.lerp(ptt, entity.yo, entity.getY());
		double z = Mth.lerp(ptt, shooter.zo, shooter.getZ()) - Mth.lerp(ptt, entity.zo, entity.getZ());
		float pitch = Mth.lerp(ptt, shooter.xRotO, shooter.getXRot());
		float yaw = Mth.rotLerp(ptt, shooter.yHeadRotO, shooter.getYHeadRot());

		boolean firstPerson = shooter == mc.player && mc.options.getCameraType().isFirstPerson();
		if (!firstPerson) {
			x -= Mth.cos(shooter.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
			y -= -0.10000000149011612D - entity.get3PYOffset();
			z -= Mth.sin(shooter.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
		}

		ps.pushPose();
		ps.translate(x, y, z);
		ps.mulPose(Axis.YP.rotationDegrees(-(yaw - 90F)));
		ps.mulPose(Axis.ZP.rotationDegrees(pitch));
		ps.translate(0, -0.08, -0.12);
		ps.translate(-0.25, 0, 0);
		distance += 0.25;

		float f10 = 0.0125F;
		distance *= 80.0D;
		float rotX = 45f + (prog * 360f);
		ps.mulPose(Axis.XP.rotationDegrees(rotX + 90f));
		ps.scale(f10, f10, f10);
		float brightness = (float) Math.sin(Math.sqrt(prog) * Math.PI);
		double uvScale = 2.0D;
		int numFrames = 17;
		float frametime = 0.5f;
		double u = distance / (LASER_WIDTH * 8.0 * uvScale);
		int frame = (int) ((((float) entity.tickCount + ptt) * frametime) % numFrames);
		double v1 = (1.0D / (double) numFrames) * (double) frame;
		double v2 = (1.0D / (double) numFrames) * (double) (frame + 1);

		VertexConsumer vc = additive(buffers, texture);
		for (int i = 0; i < 2; ++i) {
			ps.mulPose(Axis.XP.rotationDegrees(90.0F));
			PoseStack.Pose pose = ps.last();
			v(vc, pose, -distance, -width, 0, 0 + i, v1, brightness);
			v(vc, pose, 0, -width, 0, u + i, v1, brightness);
			v(vc, pose, 0, width, 0, u + i, v2, brightness);
			v(vc, pose, -distance, width, 0, 0 + i, v2, brightness);
		}

		// spiral
		PoseStack.Pose pose = ps.last();
		distance = Math.min(distance, 400.0);
		int segments = Math.max(1, (int) (distance / 8.0));
		double w = 4.0;
		double angle = Math.PI / 8.0;
		double prevX = 0, prevY = 0, prevZ = 0, prevu = 0;
		double cos2 = Math.cos((Math.PI - angle) * 0.5) * 2.0;
		double angleOffset = rand.nextDouble() * Math.PI * 2.0;
		uvScale = 8.0;
		double d = distance / segments;
		for (int i = 0; i < segments; i++) {
			double sx = -d * i;
			float iprog = (float) i / (float) segments;
			double radius = 0.5 * (1 - Math.cos(2.0 * Math.sqrt(iprog) * Math.PI)) * 8.0;
			MathUtil.Vec2 yz = MathUtil.polarOffsetXZ(0, 0, (float) radius, (float) (angleOffset - angle * i));
			double sy = yz.x;
			double sz = yz.y;
			u = prevu + (cos2 * radius / uvScale);
			v(vc, pose, prevX - w, prevY, prevZ, prevu, v2, brightness);
			v(vc, pose, sx - w, sy, sz, u, v2, brightness);
			v(vc, pose, sx + w, sy, sz, u, v1, brightness);
			v(vc, pose, prevX + w, prevY, prevZ, prevu, v1, brightness);
			prevX = sx;
			prevY = sy;
			prevZ = sz;
			prevu = u;
		}
		ps.popPose();
	}
}
