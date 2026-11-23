package techguns.client.render.entities.projectiles;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import techguns.Techguns;
import techguns.client.models.projectiles.ModelGrapplingHookProjectile;
import techguns.client.render.gl.TGRenderState;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GrapplingHookProjectile;

/**
 * The grappling hook and its wire (Fabric port RenderGrapplingHookProjectile). The wire runs from the gun, at the
 * Fabric port's offset from the shooter's eyes, to the hook, as two crossed quads.
 */
public class RenderGrapplingHookProjectile extends EntityRenderer<GrapplingHookProjectile> {
	private static final ResourceLocation TEX = Techguns.rl("textures/guns/grappling_hook.png");
	private static final ResourceLocation TEX_WIRE = Techguns.rl("textures/entity/steel_wire.png");
	private static final float WIRE_WIDTH = 0.02f;

	private final ModelGrapplingHookProjectile model = new ModelGrapplingHookProjectile();

	public RenderGrapplingHookProjectile(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(GrapplingHookProjectile entity, Frustum frustum, double x, double y, double z) {
		// the wire reaches back to the shooter
		return true;
	}

	@Override
	public void render(GrapplingHookProjectile entity, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		ps.pushPose();
		ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(ptt, entity.yRotO, entity.getYRot()) - 90.0F));
		ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(ptt, entity.xRotO, entity.getXRot())));
		ps.scale(0.35f, 0.35f, 0.35f);
		TGRenderState.begin(ps, buffers, packedLight, OverlayTexture.NO_OVERLAY);
		try {
			TGRenderState.bindTexture(TEX);
			model.render(entity, 0, 0, 0, 0, 0, 0.0625F);
		} finally {
			TGRenderState.end();
		}
		ps.popPose();

		LivingEntity shooter = entity.getShooterEntity();
		if (shooter != null) {
			renderWire(entity, shooter, ptt, ps, buffers, packedLight);
		}
		super.render(entity, entityYaw, ptt, ps, buffers, packedLight);
	}

	private void renderWire(GrapplingHookProjectile entity, LivingEntity shooter, float ptt, PoseStack ps, MultiBufferSource buffers, int light) {
		float pitch = Mth.lerp(ptt, shooter.xRotO, shooter.getXRot());
		float yaw = Mth.lerp(ptt, shooter.yHeadRotO, shooter.yHeadRot);
		float side = entity.firePos == EnumBulletFirePos.RIGHT ? 0.15f : entity.firePos == EnumBulletFirePos.LEFT ? -0.15f : 0f;
		// forward 0.25, down 0.12, to the side of the gun; turned with the shooter's view
		Vec3 offset = new Vec3(0.25, -0.12, side).zRot((float) Math.toRadians(pitch)).yRot((float) Math.toRadians(-(yaw + 90f)));
		Vec3 src = shooter.getEyePosition(ptt).add(offset);
		Vec3 hook = new Vec3(Mth.lerp(ptt, entity.xo, entity.getX()), Mth.lerp(ptt, entity.yo, entity.getY()), Mth.lerp(ptt, entity.zo, entity.getZ()));
		Vec3 a = src.subtract(hook);
		float len = (float) a.length();
		if (len < 1.0e-3f) {
			return;
		}
		Vec3 d = a.scale(1.0 / len);
		Vec3 p1 = Math.abs(d.y) > 0.99 ? new Vec3(1, 0, 0) : d.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 p2 = d.cross(p1).normalize();
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_WIRE));
		Matrix4f m = ps.last().pose();
		float u2 = len * 4.0f;
		for (Vec3 p : new Vec3[] { p1, p2 }) {
			Vec3 w = p.scale(WIRE_WIDTH);
			Vector3f n = new Vector3f((float) p.x, (float) p.y, (float) p.z).cross((float) d.x, (float) d.y, (float) d.z);
			vertex(vc, m, ps, w.scale(-1), 0f, 0f, light, n);
			vertex(vc, m, ps, a.subtract(w), u2, 0f, light, n);
			vertex(vc, m, ps, a.add(w), u2, 1f, light, n);
			vertex(vc, m, ps, w, 0f, 1f, light, n);
		}
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack ps, Vec3 v, float u, float tv, int light, Vector3f n) {
		vc.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(1f, 1f, 1f, 1f).setUv(u, tv).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light).setNormal(ps.last(), n.x(), n.y(), n.z());
	}

	@Override
	public ResourceLocation getTextureLocation(GrapplingHookProjectile entity) {
		return TEX;
	}
}
