package techguns.client.render.tileentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.machines.ModelTurretBase;
import techguns.client.render.gl.TGRenderState;
import techguns.entities.npcs.NPCTurret;
import techguns.tileentities.TurretTileEnt;

/** Turret base block (1.12 RenderTurret), textured by the turret armor tier. */
public class RenderTurret implements BlockEntityRenderer<TurretTileEnt> {

	protected static final ModelTurretBase MODEL = new ModelTurretBase();

	@Override
	public void render(TurretTileEnt te, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		renderModel(ps, buffers, light, overlay, te.getFacing(), NPCTurret.getTexture(te.getTurretTier()));
	}

	public static void renderModel(PoseStack ps, MultiBufferSource buffers, int light, int overlay, Direction facing, ResourceLocation tex) {
		ps.pushPose();
		switch (facing) {
			case DOWN -> {
				ps.translate(0.5d, -0.5d, 0.5d);
				ps.mulPose(Axis.YP.rotationDegrees(180f));
			}
			case EAST -> {
				ps.translate(1.5d, 0.5d, 0.5d);
				ps.mulPose(Axis.ZP.rotationDegrees(90f));
			}
			case NORTH -> {
				ps.translate(0.5d, 0.5d, -0.5d);
				ps.mulPose(Axis.XP.rotationDegrees(90f));
			}
			case SOUTH -> {
				ps.translate(0.5d, 0.5d, 1.5d);
				ps.mulPose(Axis.XP.rotationDegrees(-90f));
			}
			case WEST -> {
				ps.translate(-0.5d, 0.5d, 0.5d);
				ps.mulPose(Axis.ZP.rotationDegrees(-90f));
			}
			default -> {
				ps.translate(0.5d, 1.5d, 0.5d);
				ps.mulPose(Axis.ZP.rotationDegrees(180f));
				ps.mulPose(Axis.YP.rotationDegrees(180f));
			}
		}
		TGRenderState.begin(ps, buffers, light, overlay);
		try {
			TGRenderState.bindTexture(tex);
			MODEL.render(null, 0f, 0f, 0f, 0f, 0f, 0.0625f);
		} finally {
			TGRenderState.end();
		}
		ps.popPose();
	}

	/** item form, uses the builtin/entity display transforms of models/item/basicmachine.json */
	public static class ItemRenderer extends BlockEntityWithoutLevelRenderer {
		public ItemRenderer() {
			super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		}

		@Override
		public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
			renderModel(ps, buffers, light, overlay, Direction.UP, NPCTurret.getTexture(0));
		}
	}
}
