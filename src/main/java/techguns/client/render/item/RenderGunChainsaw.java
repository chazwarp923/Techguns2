package techguns.client.render.item;

import net.minecraft.world.item.ItemStack;
import techguns.client.models.ModelMultipart;
import techguns.client.render.gl.GlStateManager;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GenericGunMeleeCharge;

/** Chainsaw: blade part tinted by installed blade (obsidian / carbon). */
public class RenderGunChainsaw extends RenderGunBase90 {

	public RenderGunChainsaw(ModelMultipart model, int parts) {
		super(model, parts);
	}

	@Override
	protected void setGLColorForPart(GenericGun gun, int part, ItemStack stack) {
		if (part == 1 && gun instanceof GenericGunMeleeCharge c) {
			int i = c.getMiningHeadLevel(stack);
			if (i == 1) {
				GlStateManager.color(0.9f, 0.55f, 1.0f, 1.0f);
			} else if (i == 2) {
				GlStateManager.color(0.4f, 0.4f, 0.4f, 1.0f);
			}
		}
	}
}
