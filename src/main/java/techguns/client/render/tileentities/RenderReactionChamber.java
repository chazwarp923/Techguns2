package techguns.client.render.tileentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import techguns.Techguns;
import techguns.client.render.gl.TGRenderTypes;
import techguns.tileentities.ReactionChamberTileEntMaster;

/**
 * Formed reaction chamber (1.12 RenderReactionChamber). The chamber itself is the formed controller block model; this
 * draws the item in the chamber, the beam while working and the fluid column.
 */
public class RenderReactionChamber implements BlockEntityRenderer<ReactionChamberTileEntMaster> {

	protected static final ResourceLocation HEATRAY_TEXTURE = Techguns.rl("textures/fx/heatray.png");

	/** half width of the fluid column / beam */
	private static final float W = 1.875f * 0.5f;
	private static final float H1 = 0.5f;
	private static final float H2 = 2.9375f;

	@Override
	public void render(ReactionChamberTileEntMaster te, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		if (!te.isFormed() || te.getLevel() == null) {
			return;
		}
		Direction dir = te.getMultiblockDirection();
		BlockPos centerPos = te.getCenterPos().above();
		int centerLight = LevelRenderer.getLightColor(te.getLevel(), centerPos);
		// chamber center relative to the controller
		float cx = dir.getStepX() + 0.5f;
		float cz = dir.getStepZ() + 0.5f;

		ItemStack item = te.isWorking() ? te.currentOperation.getItemInputI(0) : te.getInput();
		if (!item.isEmpty()) {
			ps.pushPose();
			ps.translate(cx, 1.5f, cz);
			ps.mulPose(Axis.YP.rotationDegrees(-90.0f * dir.getOpposite().get2DDataValue()));
			Minecraft.getInstance().getItemRenderer().renderStatic(item.copyWithCount(1), ItemDisplayContext.GROUND, centerLight, OverlayTexture.NO_OVERLAY,
					ps, buffers, te.getLevel(), 0);
			ps.popPose();
		}

		if (te.isWorking() && !te.getFocus().isEmpty()) {
			renderBeam(te, partialTicks, ps, buffers, cx, cz);
		}

		FluidStack fluid = te.inputTank.getFluid();
		if (!fluid.isEmpty()) {
			renderFluid(te, fluid, ps, buffers, cx, cz, centerLight);
		}
	}

	/** pulsing beam quad in the chamber center, turned to the camera */
	private void renderBeam(ReactionChamberTileEntMaster te, float partialTicks, PoseStack ps, MultiBufferSource buffers, float cx, float cz) {
		double d = ((te.progress % 10) + partialTicks) / 10.0d;
		d = 0.875d + 0.125d * (1.0d - Math.cos(Math.PI * d * 2.0d));
		float w = (float) (W * d);
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		BlockPos p = te.getBlockPos();
		float angle = (float) Math.toDegrees(Math.atan2(p.getX() + cx - cam.x, p.getZ() + cz - cam.z));

		ps.pushPose();
		ps.translate(cx, 0, cz);
		ps.mulPose(Axis.YP.rotationDegrees(angle));
		PoseStack.Pose pose = ps.last();
		VertexConsumer vc = buffers.getBuffer(TGRenderTypes.additive(HEATRAY_TEXTURE));
		beamVertex(vc, pose, -w, H1, 0, 1);
		beamVertex(vc, pose, -w, H2, 0, 0);
		beamVertex(vc, pose, w, H2, 1, 0);
		beamVertex(vc, pose, w, H1, 1, 1);
		ps.popPose();
	}

	private static void beamVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v) {
		vc.addVertex(pose, x, y, 0).setColor(1f, 1f, 1f, 1f).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, 0, 0, 1);
	}

	/** fluid column, height by tank fill; flowing texture while working */
	private void renderFluid(ReactionChamberTileEntMaster te, FluidStack fluid, PoseStack ps, MultiBufferSource buffers, float cx, float cz, int light) {
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
		ResourceLocation texLoc = te.isWorking() ? ext.getFlowingTexture(fluid) : ext.getStillTexture(fluid);
		if (texLoc == null) {
			return;
		}
		TextureAtlasSprite tex = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texLoc);
		int color = ext.getTintColor(fluid);
		int a = (color >> 24) & 0xFF;
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		if (a == 0) {
			a = 255;
		}
		if (fluid.getFluid().getFluidType().getLightLevel(fluid) > 0) {
			light = LightTexture.FULL_BRIGHT;
		}
		float level = Math.min(1f, (float) fluid.getAmount() / te.inputTank.getCapacity());
		float height = H2 * level;
		float x0 = cx - W, x1 = cx + W, z0 = cz - W, z1 = cz + W;

		VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
		PoseStack.Pose pose = ps.last();
		// sides in segments of at most one block so the texture is not stretched vertically
		for (float y = 0; y < height; y += 1f) {
			float yb = H1 + y;
			float yt = H1 + Math.min(height, y + 1f);
			float vBottom = tex.getV0() + (tex.getV1() - tex.getV0()) * (yt - yb);
			side(vc, pose, tex, x0, z0, x1, z0, yb, yt, vBottom, r, g, b, a, light, 0, 0, -1);
			side(vc, pose, tex, x1, z1, x0, z1, yb, yt, vBottom, r, g, b, a, light, 0, 0, 1);
			side(vc, pose, tex, x0, z1, x0, z0, yb, yt, vBottom, r, g, b, a, light, -1, 0, 0);
			side(vc, pose, tex, x1, z0, x1, z1, yb, yt, vBottom, r, g, b, a, light, 1, 0, 0);
		}
		float top = H1 + height;
		vertex(vc, pose, x0, top, z0, tex.getU0(), tex.getV0(), r, g, b, a, light, 0, 1, 0);
		vertex(vc, pose, x0, top, z1, tex.getU0(), tex.getV1(), r, g, b, a, light, 0, 1, 0);
		vertex(vc, pose, x1, top, z1, tex.getU1(), tex.getV1(), r, g, b, a, light, 0, 1, 0);
		vertex(vc, pose, x1, top, z0, tex.getU1(), tex.getV0(), r, g, b, a, light, 0, 1, 0);
	}

	/** vertical quad from (xa, za) to (xb, zb), front face seen with a on the right */
	private static void side(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite tex, float xa, float za, float xb, float zb, float yb, float yt,
			float vBottom, int r, int g, int b, int a, int light, float nx, float ny, float nz) {
		// texture top at the top edge, so flowing textures run downwards
		vertex(vc, pose, xa, yb, za, tex.getU1(), vBottom, r, g, b, a, light, nx, ny, nz);
		vertex(vc, pose, xa, yt, za, tex.getU1(), tex.getV0(), r, g, b, a, light, nx, ny, nz);
		vertex(vc, pose, xb, yt, zb, tex.getU0(), tex.getV0(), r, g, b, a, light, nx, ny, nz);
		vertex(vc, pose, xb, yb, zb, tex.getU0(), vBottom, r, g, b, a, light, nx, ny, nz);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int r, int g, int b, int a, int light,
			float nx, float ny, float nz) {
		vc.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(u, v).setLight(light).setNormal(pose, nx, ny, nz);
	}

	@Override
	public boolean shouldRenderOffScreen(ReactionChamberTileEntMaster te) {
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(ReactionChamberTileEntMaster te) {
		BlockPos c = te.getCenterPos();
		return new AABB(c.getX() - 1, c.getY(), c.getZ() - 1, c.getX() + 2, c.getY() + 4, c.getZ() + 2);
	}
}
