package techguns.client.render.tileentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import techguns.Techguns;
import techguns.client.models.machines.ModelFabricator;
import techguns.client.models.machines.ModelMachine;
import techguns.client.render.gl.TGRenderState;
import techguns.tileentities.FabricatorTileEntMaster;

/** Formed fabricator (1.12 RenderFabricator): the whole machine model plus the item being worked on. */
public class RenderFabricator implements BlockEntityRenderer<FabricatorTileEntMaster> {

	protected static final ModelMachine MODEL = new ModelFabricator();
	protected static final ResourceLocation TEXTURE = Techguns.rl("textures/blocks/fabricator.png");

	@Override
	public void render(FabricatorTileEntMaster te, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		if (!te.isFormed()) {
			return;
		}
		float progress = te.isWorking() && te.totaltime > 0 ? Math.min(1f, (te.progress + partialTicks) / te.totaltime) : 0f;
		Direction direction = te.getMultiblockDirection();

		ps.pushPose();
		ps.translate(0.5d, 1.5d, 0.5d);
		ps.mulPose(Axis.ZP.rotationDegrees(180f));
		ps.mulPose(Axis.YP.rotationDegrees(180f + direction.get2DDataValue() * 90f));

		ItemStack itemStack = ItemStack.EMPTY;
		if (te.currentOperation != null) {
			if (progress < 0.8f) {
				itemStack = te.currentOperation.getItemInputI(FabricatorTileEntMaster.SLOT_PLATE);
				if (itemStack.isEmpty()) {
					itemStack = te.currentOperation.getItemInputI(FabricatorTileEntMaster.SLOT_INPUT1);
				}
			} else {
				itemStack = te.currentOperation.getItemOutput0();
			}
		}

		TGRenderState.begin(ps, buffers, light, overlay);
		try {
			TGRenderState.bindTexture(TEXTURE);
			MODEL.render(null, 0f, 0f, 0f, 0f, 0f, 0.0625f, progress);
		} finally {
			TGRenderState.end();
		}

		if (!itemStack.isEmpty()) {
			// 1.12: rotate 90 around x (item lies flat), item entity at (-0.5, 0, -0.6). The 1.12 item entity renderer ignored
			// the yaw argument and lifted the item by bob (0.1) + 0.25 * ground scale (0.5)
			ps.pushPose();
			ps.mulPose(Axis.XP.rotationDegrees(90f));
			ps.translate(-0.5d, 0.225d, -0.6d);
			Minecraft.getInstance().getItemRenderer().renderStatic(itemStack.copyWithCount(1), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
					ps, buffers, te.getLevel(), 0);
			ps.popPose();
		}
		ps.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(FabricatorTileEntMaster te) {
		BlockPos p = te.getBlockPos();
		return new AABB(p).inflate(2.5d);
	}
}
