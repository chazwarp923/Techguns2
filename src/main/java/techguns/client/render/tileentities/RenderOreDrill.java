package techguns.client.render.tileentities;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import techguns.Techguns;
import techguns.tileentities.OreDrillTileEntMaster;

/**
 * Formed ore drill (1.12 RenderOreDrill): the rod line, or with a drill head a tapering stack of turning cubes, from
 * the engines to the ore cluster.
 */
public class RenderOreDrill implements BlockEntityRenderer<OreDrillTileEntMaster> {

	private static final int CUBES_PER_BLOCK = 4;
	private static final ResourceLocation[] TEXTURES = { Techguns.rl("textures/blocks/drillhead.png"), Techguns.rl("textures/blocks/drillhead.png"),
			Techguns.rl("textures/blocks/drillhead_obsidian.png"), Techguns.rl("textures/blocks/drillhead_carbon.png") };

	@Override
	public void render(OreDrillTileEntMaster te, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		Direction dir = te.getDrillDirection();
		if (!te.isFormed() || dir == null || te.getLevel() == null) {
			return;
		}
		int engines = te.getEngines();
		int rods = te.getRods();
		int rodLight = LevelRenderer.getLightColor(te.getLevel(), te.getBlockPos().relative(dir, engines + 1 + rods / 2));

		ps.pushPose();
		ps.translate(0.5d, 0.5d, 0.5d);
		// local +y along the drill
		ps.mulPose(new Quaternionf().rotationTo(0f, 1f, 0f, dir.getStepX(), dir.getStepY(), dir.getStepZ()));
		// local origin at the start of the first rod
		ps.translate(0d, engines + 0.5d, 0d);

		if (te.hasDrill() && te.isRedstoneEnabled() && te.hasPower()) {
			float rot = ((te.getLevel().getGameTime() % 36) + partialTicks) * 10f;
			ps.mulPose(Axis.YP.rotationDegrees(rot));
		}
		if (te.hasDrill() && te.getDrillType() > 0) {
			drawDriller(te, ps, buffers.getBuffer(RenderType.entityCutout(TEXTURES[Math.min(3, te.getDrillType())])), rods, rodLight);
		} else {
			drawDrillRod(ps, buffers.getBuffer(RenderType.entityCutout(TEXTURES[0])), rods, rodLight);
		}
		ps.popPose();
	}

	/** tapering stack, every second cube turned by 45 degrees */
	private void drawDriller(OreDrillTileEntMaster te, PoseStack ps, VertexConsumer vc, int rods, int light) {
		int n = rods * CUBES_PER_BLOCK;
		float step = 1.0f / CUBES_PER_BLOCK;
		float max = (float) Math.log(n + 1.0f);
		int drillRadius = te.getDrillRadius();
		float radiusFactor = switch (drillRadius) {
			case 0, 1 -> 0.35f;
			case 2 -> 0.5f;
			case 3 -> 0.55f;
			default -> 0.6f;
		};
		for (int i = 0; i < n; i++) {
			float factor = (float) Math.log(n - i + 1.0f);
			float size = (drillRadius == 0 ? 1.0f : drillRadius) * factor / max * radiusFactor;
			ps.pushPose();
			ps.translate(0f, step * (i + 1), 0f);
			if (i % 2 == 1) {
				ps.mulPose(Axis.YP.rotationDegrees(45f));
			}
			cube(ps.last(), vc, size, step, size, light);
			ps.popPose();
		}
	}

	/** thin rod segment per rod block, turned by 45 degrees */
	private void drawDrillRod(PoseStack ps, VertexConsumer vc, int rods, int light) {
		for (int i = 0; i < rods; i++) {
			ps.pushPose();
			ps.translate(0f, i + 0.5f, 0f);
			ps.mulPose(Axis.YP.rotationDegrees(45f));
			cube(ps.last(), vc, 0.2f, 0.5f, 0.2f, light);
			ps.popPose();
		}
	}

	/** box centered on the origin with half sizes, full texture on every face (1.12 OreDrillCube) */
	private static void cube(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, int light) {
		// top / bottom
		quad(pose, vc, light, 0, 1, 0, -x, y, -z, 0, 1, -x, y, z, 0, 0, x, y, z, 1, 0, x, y, -z, 1, 1);
		quad(pose, vc, light, 0, -1, 0, x, -y, -z, 1, 0, x, -y, z, 1, 1, -x, -y, z, 0, 1, -x, -y, -z, 0, 0);
		// north / south
		quad(pose, vc, light, 0, 0, -1, -x, -y, -z, 0, 1, -x, y, -z, 0, 0, x, y, -z, 1, 0, x, -y, -z, 1, 1);
		quad(pose, vc, light, 0, 0, 1, x, -y, z, 1, 1, x, y, z, 1, 0, -x, y, z, 0, 0, -x, -y, z, 0, 1);
		// west / east
		quad(pose, vc, light, -1, 0, 0, -x, -y, z, 1, 1, -x, y, z, 1, 0, -x, y, -z, 0, 0, -x, -y, -z, 0, 1);
		quad(pose, vc, light, 1, 0, 0, x, -y, -z, 0, 1, x, y, -z, 0, 0, x, y, z, 1, 0, x, -y, z, 1, 1);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vc, int light, float nx, float ny, float nz, float... v) {
		for (int i = 0; i < 4; i++) {
			int o = i * 5;
			vc.addVertex(pose, v[o], v[o + 1], v[o + 2]).setColor(1f, 1f, 1f, 1f).setUv(v[o + 3], v[o + 4]).setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light).setNormal(pose, nx, ny, nz);
		}
	}

	@Override
	public boolean shouldRenderOffScreen(OreDrillTileEntMaster te) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	@Override
	public AABB getRenderBoundingBox(OreDrillTileEntMaster te) {
		Direction dir = te.getDrillDirection();
		BlockPos p = te.getBlockPos();
		if (dir == null) {
			return new AABB(p);
		}
		BlockPos end = p.relative(dir, te.getTotalDrillLength() + 1);
		int r = te.getRadius() + 2;
		return new AABB(p.getX(), p.getY(), p.getZ(), end.getX() + 1, end.getY() + 1, end.getZ() + 1).inflate(r);
	}
}
