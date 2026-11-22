package techguns.client.render.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import techguns.client.models.ModelMultipart;
import techguns.client.render.gl.GlStateManager;



public class RenderItemLMGMag extends RenderItemBase {

	public RenderItemLMGMag(ModelMultipart model, ResourceLocation texture) {
		super(model, texture);
	}

	@Override
	protected void setBaseRotation(ItemDisplayContext transform) {
		GlStateManager.rotate(-180.0f, 1.0f, 0, 0);
		
		if (ItemDisplayContext.GUI == transform || ItemDisplayContext.FIXED == transform) {
			GlStateManager.rotate(90.0f, 0f, 1.0f, 0);
		} else {
			GlStateManager.rotate(180.0f, 0f, 1.0f, 0);
		}
	}
	
}
