package techguns.client.render.tileentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.items.guns.GenericGun;
import techguns.tileentities.GrinderTileEnt;

/** The two spinning rolls and the item being ground (1.12 RenderGrinder). */
public class RenderGrinder implements BlockEntityRenderer<GrinderTileEnt> {

	private static final ResourceLocation TEXTURE = Techguns.rl("textures/blocks/grinder_roll.png");
	private static final float UNIT = 1f / 16f;
	private static final float TEX_X = 2 * UNIT;
	private static final float TEX_Z = 2 * UNIT;
	private static final float LEN = 12 * UNIT;

	@Override
	public void render(GrinderTileEnt te, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		float progress = te.isWorking() && te.totaltime > 0 ? Math.min(1f, (te.progress + partialTick) / te.totaltime) : 0f;
		float rot = te.isWorking() ? (45f + 1080f * progress) % 360f : 45f;

		ps.pushPose();
		ps.translate(0.5f, 0f, 0.5f);
		ps.mulPose(Axis.YP.rotationDegrees(te.getFacing().toYRot()));
		ps.translate(-0.5f, 0f, -0.5f);

		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		ps.pushPose();
		ps.translate(0, 7.5f * UNIT, 6.5f * UNIT);
		ps.mulPose(Axis.XP.rotationDegrees(rot));
		drawRoll(vc, ps.last(), 1.5f, 2f, light);
		ps.popPose();

		ps.pushPose();
		ps.translate(0, 7.5f * UNIT, 9.5f * UNIT);
		ps.mulPose(Axis.XP.rotationDegrees(-rot));
		drawRoll(vc, ps.last(), 1.5f, 2f, light);
		ps.popPose();

		if (te.isWorking() && te.currentOperation != null) {
			ItemStack item = te.currentOperation.getItemInputI(0);
			if (!item.isEmpty()) {
				boolean gun = item.getItem() instanceof GenericGun;
				ps.pushPose();
				ps.translate(0.5d, 0.6d - progress * 0.4d + (gun ? 0.15d : 0), 0.5d);
				if (gun) {
					ps.scale(0.5f, 0.5f, 0.5f);
					ps.mulPose(Axis.YP.rotationDegrees(90f));
				}
				Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.GROUND, light, overlay, ps, buffers, te.getLevel(), 0);
				ps.popPose();
			}
		}
		ps.popPose();
	}

	private static void drawRoll(VertexConsumer vc, PoseStack.Pose pose, float radius, float dx, int light) {
		float d = 2 * radius * UNIT;
		float r = UNIT * radius;
		float x0 = dx * UNIT, x1 = dx * UNIT + LEN;
		// top
		v(vc, pose, x0, r, -r, TEX_X, TEX_Z, 0, 1, 0, light);
		v(vc, pose, x0, r, r, TEX_X, TEX_Z + d, 0, 1, 0, light);
		v(vc, pose, x1, r, r, TEX_X + LEN, TEX_Z + d, 0, 1, 0, light);
		v(vc, pose, x1, r, -r, TEX_X + LEN, TEX_Z, 0, 1, 0, light);
		// +z side
		v(vc, pose, x0, r, r, TEX_X, TEX_Z, 0, 0, 1, light);
		v(vc, pose, x0, -r, r, TEX_X, TEX_Z + d, 0, 0, 1, light);
		v(vc, pose, x1, -r, r, TEX_X + LEN, TEX_Z + d, 0, 0, 1, light);
		v(vc, pose, x1, r, r, TEX_X + LEN, TEX_Z, 0, 0, 1, light);
		// bottom
		v(vc, pose, x0, -r, -r, TEX_X, TEX_Z + d, 0, -1, 0, light);
		v(vc, pose, x1, -r, -r, TEX_X + LEN, TEX_Z + d, 0, -1, 0, light);
		v(vc, pose, x1, -r, r, TEX_X + LEN, TEX_Z, 0, -1, 0, light);
		v(vc, pose, x0, -r, r, TEX_X, TEX_Z, 0, -1, 0, light);
		// -z side
		v(vc, pose, x0, r, -r, TEX_X, TEX_Z + d, 0, 0, -1, light);
		v(vc, pose, x1, r, -r, TEX_X + LEN, TEX_Z + d, 0, 0, -1, light);
		v(vc, pose, x1, -r, -r, TEX_X + LEN, TEX_Z, 0, 0, -1, light);
		v(vc, pose, x0, -r, -r, TEX_X, TEX_Z, 0, 0, -1, light);
	}

	private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float tv, float nx, float ny, float nz, int light) {
		vc.addVertex(pose, x, y, z).setColor(-1).setUv(u, tv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
	}
}
