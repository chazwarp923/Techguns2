package techguns.client.render.item;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.client.models.ModelMultipart;
import techguns.client.render.gl.TGRenderState;
import techguns.items.guns.GenericGun;
import techguns.items.guns.ammo.AmmoTypes;

/** Rocket launcher: part 1 (the loaded rocket) uses the texture of the loaded ammo variant. */
public class RenderRocketLauncher extends RenderGunBase90 {

	protected static final Map<String, ResourceLocation> textures = new HashMap<>();
	static {
		textures.put(AmmoTypes.TYPE_DEFAULT, Techguns.rl("textures/guns/rocket.png"));
		textures.put(AmmoTypes.TYPE_NUKE, Techguns.rl("textures/guns/rocket_nuke.png"));
		textures.put(AmmoTypes.TYPE_HV, Techguns.rl("textures/guns/rocket_hv.png"));
	}

	public RenderRocketLauncher(ModelMultipart model, int parts) {
		super(model, parts);
	}

	@Override
	public void bindTextureForPart(GenericGun gun, int part, ItemStack stack) {
		if (part == 0) {
			super.bindTextureForPart(gun, part, stack);
		} else if (part == 1) {
			ResourceLocation tex = textures.get(gun.getCurrentAmmoVariantKey(stack));
			if (tex != null) {
				TGRenderState.bindTexture(tex);
			}
		}
	}
}
