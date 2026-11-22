package techguns.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import techguns.Techguns;

public final class TextUtil {
	private TextUtil() {}

	/** Translatable component in the techguns namespace, e.g. transTG("gun.tooltip.ammo") -> "techguns.gun.tooltip.ammo". */
	public static MutableComponent transTG(String key, Object... args) {
		return Component.translatable(Techguns.MODID + "." + key, args);
	}

	public static MutableComponent trans(String key, Object... args) {
		return Component.translatable(key, args);
	}
}
