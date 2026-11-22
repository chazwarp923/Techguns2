package techguns.client.render.item;

import net.minecraft.world.item.ItemDisplayContext;
import techguns.client.models.ModelMultipart;
import techguns.client.render.gl.GlStateManager;

/** Renderer for OBJ mesh guns (1.12 baked models were not flipped, only rotated around Y). */
public class RenderGunBaseObj extends RenderGunBase {

	private final float rotY;

	public RenderGunBaseObj(ModelMultipart model, int parts, float rotY) {
		super(model, parts);
		this.rotY = rotY;
	}

	@Override
	protected void setBaseRotation(ItemDisplayContext transform) {
		GlStateManager.rotate(rotY, 0f, 1.0f, 0);
	}
}
