package techguns.client.render.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.ModelMultipart;
import techguns.client.render.gl.TGRenderState;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GenericGunMeleeCharge;

/** Mining tool whose head part (part 1) texture depends on the installed mining head. */
public class RenderMiningToolMultiTexture extends RenderGunBase90 {

	protected final ResourceLocation[] textures;

	public RenderMiningToolMultiTexture(ModelMultipart model, int parts, ResourceLocation[] textures) {
		super(model, parts);
		this.textures = textures;
	}

	@Override
	public void bindTextureForPart(GenericGun gun, int part, ItemStack stack) {
		if (part == 1 && gun instanceof GenericGunMeleeCharge g) {
			int textureIndex = g.getMiningHeadLevel(stack);
			if (textureIndex < textures.length) {
				TGRenderState.bindTexture(textures[textureIndex]);
				return;
			}
		}
		super.bindTextureForPart(gun, part, stack);
	}
}
