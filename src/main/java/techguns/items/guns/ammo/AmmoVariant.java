package techguns.items.guns.ammo;

import java.util.Arrays;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * One variant (default, incendiary, ...) of an {@link AmmoType}. Items are stored as
 * {@link ItemLike} references and only turned into stacks on demand, since in 1.21 items
 * do not exist yet when the gun definitions are built. A null entry means "no item".
 */
public class AmmoVariant {
	protected final String key;
	protected final ItemLike[] ammo;
	protected final ItemLike[] bullet;

	public AmmoVariant(ItemLike[] ammo, ItemLike[] bullet) {
		this(AmmoTypes.TYPE_DEFAULT, ammo, bullet);
	}

	public AmmoVariant(String key, ItemLike[] ammo, ItemLike[] bullet) {
		this.key = key;
		this.ammo = ammo;
		this.bullet = bullet;
	}

	public String getKey() {
		return key;
	}

	public ItemStack[] getAmmo() {
		return AmmoType.toStacks(ammo);
	}

	public ItemStack[] getBullet() {
		return AmmoType.toStacks(bullet);
	}

	@Override
	public String toString() {
		return "AmmoVariant[" + key + "," + Arrays.toString(ammo) + "]";
	}
}
