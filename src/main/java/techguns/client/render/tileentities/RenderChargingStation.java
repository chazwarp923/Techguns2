package techguns.client.render.tileentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.tileentities.ChargingStationTileEnt;

/** The item being charged, inside the station (1.12 RenderChargingStation). */
public class RenderChargingStation implements BlockEntityRenderer<ChargingStationTileEnt> {

	@Override
	public void render(ChargingStationTileEnt te, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		ItemStack item = te.getDisplayedItem();
		if (item.isEmpty()) {
			return;
		}
		ps.pushPose();
		ps.translate(0.5d, 0.4d, 0.5d);
		ps.mulPose(Axis.YP.rotationDegrees(-te.getFacing().toYRot()));
		Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, ps, buffers,
				te.getLevel(), (int) te.getBlockPos().asLong());
		ps.popPose();
	}
}
