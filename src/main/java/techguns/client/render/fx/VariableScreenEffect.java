package techguns.client.render.fx;

import java.util.HashMap;
import java.util.Map;

/** Fabric port: a screen effect picked by the gun's loaded ammo variant, with a default for the rest. */
public class VariableScreenEffect implements IScreenEffect {
	public final Map<String, IScreenEffect> effects = new HashMap<>();
	public final IScreenEffect defaultEffect;

	public VariableScreenEffect(IScreenEffect defaultEffect) {
		this.defaultEffect = defaultEffect;
	}

	public VariableScreenEffect put(String ammoVariant, IScreenEffect effect) {
		this.effects.put(ammoVariant, effect);
		return this;
	}

	@Override
	public void doRender(float progress, float offsetX, float offsetY, float offsetZ, float scale, boolean is3p, String ammoVariant) {
		this.effects.getOrDefault(ammoVariant, this.defaultEffect).doRender(progress, offsetX, offsetY, offsetZ, scale, is3p);
	}

	@Override
	public void doRender(float progress, float offsetX, float offsetY, float offsetZ, float scale, float rot_x, float rot_y, float rot_z, boolean is3p) {
		this.defaultEffect.doRender(progress, offsetX, offsetY, offsetZ, scale, rot_x, rot_y, rot_z, is3p);
	}
}
