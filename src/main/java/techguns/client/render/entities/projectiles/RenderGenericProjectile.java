package techguns.client.render.entities.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import techguns.Techguns;
import techguns.entities.projectiles.GenericProjectile;

/** Bullet: four quads in a cross shape along the flight direction (1.12.2 RenderGenericProjectile). */
public class RenderGenericProjectile<T extends GenericProjectile> extends EntityRenderer<T> {

	public static final ResourceLocation BULLET = Techguns.rl("textures/entity/bullet1.png");
	public static final ResourceLocation BULLET_BLUE = Techguns.rl("textures/entity/bullet_blue.png");
	public static final ResourceLocation STONE_BULLET = Techguns.rl("textures/entity/handgunbullet.png");

	public static final ResourceLocation LASER = Techguns.rl("textures/fx/laser3.png");

	private final ResourceLocation texture;
	private boolean additive = false;

	public RenderGenericProjectile(EntityRendererProvider.Context ctx) {
		this(ctx, BULLET);
	}

	public RenderGenericProjectile(EntityRendererProvider.Context ctx, ResourceLocation texture) {
		super(ctx);
		this.texture = texture;
	}

	/** 1.12 RenderBlasterProjectile: additive blending, no lighting */
	public RenderGenericProjectile<T> setAdditive() {
		this.additive = true;
		return this;
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return texture;
	}

	@Override
	public void render(T entity, float entityYaw, float partialTicks, PoseStack ps, MultiBufferSource buffers, int packedLight) {
		if (entity.tickCount < 2 && !(entity.tickCount >= 1 && partialTicks > 0.25f)) {
			return;
		}
		ps.pushPose();
		ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
		ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

		float f10 = 0.0125F;
		ps.mulPose(Axis.XP.rotationDegrees(45.0F));
		ps.scale(f10, f10, f10);

		float length = 8.0f; // half of length/width
		float width = 2.0f;
		VertexConsumer vc = buffers.getBuffer(additive ? techguns.client.render.gl.TGRenderTypes.additive(getTextureLocation(entity)) : RenderType.entityCutoutNoCull(getTextureLocation(entity)));
		int light = 0xF000F0;
		for (int i = 0; i < 4; ++i) {
			ps.mulPose(Axis.XP.rotationDegrees(90.0F));
			PoseStack.Pose pose = ps.last();
			vertex(vc, pose, -length, -width, 0f, 1f, light);
			vertex(vc, pose, length, -width, 1f, 1f, light);
			vertex(vc, pose, length, width, 1f, 0f, light);
			vertex(vc, pose, -length, width, 0f, 0f, light);
		}
		ps.popPose();
		super.render(entity, entityYaw, partialTicks, ps, buffers, packedLight);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
		vc.addVertex(pose, x, y, 0f).setColor(-1).setUv(u, 1f - v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
	}
}
