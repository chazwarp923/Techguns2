package techguns.client.render.tileentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import techguns.tileentities.BlastFurnaceTileEnt;

/** Glowing lava behind the furnace window while it works (1.12 RenderBlastfurnace). */
public class RenderBlastFurnace implements BlockEntityRenderer<BlastFurnaceTileEnt> {

	private static final float UNIT = 1f / 16f;

	@Override
	public void render(BlastFurnaceTileEnt te, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		if (!te.isWorking()) {
			return;
		}
		TextureAtlasSprite tex = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
				.apply(IClientFluidTypeExtensions.of(Fluids.LAVA).getFlowingTexture());
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));

		ps.pushPose();
		// pane is modelled for a north facing furnace, rotate around the block center
		ps.translate(0.5f, 0f, 0.5f);
		ps.mulPose(Axis.YP.rotationDegrees(180f - te.getFacing().toYRot()));
		ps.translate(-0.5f, 0f, -0.5f);
		PoseStack.Pose pose = ps.last();
		float x0 = 3 * UNIT, x1 = 13 * UNIT, y0 = 3 * UNIT, y1 = 9 * UNIT, z = 0.06f;
		float u0 = tex.getU0(), u1 = tex.getU1(), v0 = tex.getV0(), v1 = tex.getV1();
		vertex(vc, pose, x1, y0, z, u1, v0);
		vertex(vc, pose, x0, y0, z, u0, v0);
		vertex(vc, pose, x0, y1, z, u0, v1);
		vertex(vc, pose, x1, y1, z, u1, v1);
		ps.popPose();
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
		vc.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, 0, 0, -1);
	}
}
