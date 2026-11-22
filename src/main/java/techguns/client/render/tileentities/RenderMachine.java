package techguns.client.render.tileentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.machines.ModelMachine;
import techguns.client.render.gl.TGRenderState;
import techguns.tileentities.BasicMachineTileEnt;

/** Animated machine model (1.12 RenderMachine), the progress drives the model animation. */
public class RenderMachine<T extends BasicMachineTileEnt> implements BlockEntityRenderer<T> {

	protected final ModelMachine model;
	protected final ResourceLocation texture;

	public RenderMachine(ModelMachine model, ResourceLocation texture) {
		this.model = model;
		this.texture = texture;
	}

	@Override
	public void render(T te, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		float progress = 0f;
		if (te.isWorking() && te.totaltime > 0) {
			progress = Math.min(1f, (te.progress + partialTicks) / te.totaltime);
		}
		renderModel(ps, buffers, light, overlay, te.rotation, progress);
	}

	public void renderModel(PoseStack ps, MultiBufferSource buffers, int light, int overlay, int rotation, float progress) {
		ps.pushPose();
		ps.translate(0.5d, 1.5d, 0.5d);
		ps.mulPose(Axis.ZP.rotationDegrees(180f));
		ps.mulPose(Axis.YP.rotationDegrees(180f));
		ps.mulPose(Axis.YP.rotationDegrees(90.0f * rotation));
		TGRenderState.begin(ps, buffers, light, overlay);
		try {
			TGRenderState.bindTexture(texture);
			this.model.render(null, 0f, 0f, 0f, 0f, 0f, 0.0625f, progress);
		} finally {
			TGRenderState.end();
		}
		ps.popPose();
	}

	/** item form of the machine, uses the builtin/entity display transforms of models/item/basicmachine.json */
	public static class ItemRenderer extends BlockEntityWithoutLevelRenderer {
		private final RenderMachine<?> renderer;

		public ItemRenderer(RenderMachine<?> renderer) {
			super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
			this.renderer = renderer;
		}

		@Override
		public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
			renderer.renderModel(ps, buffers, light, overlay, 0, 0f);
		}
	}
}
