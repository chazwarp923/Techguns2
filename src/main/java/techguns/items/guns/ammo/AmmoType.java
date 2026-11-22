package techguns.items.guns.ammo;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class AmmoType {

	protected ItemLike[] emptyMag = { null };
	protected final ArrayList<AmmoVariant> variants = new ArrayList<>();
	protected final HashMap<String, Integer> ammoVariantIDs = new HashMap<>();
	protected int bulletsPerMag = 0;

	public AmmoType(ItemLike... ammo) {
		this.variants.add(new AmmoVariant(ammo, ammo));
		ammoVariantIDs.put(AmmoTypes.TYPE_DEFAULT, 0);
	}

	public AmmoType(ItemLike ammo, ItemLike emptyMag, ItemLike bullet, int bulletsPerMag) {
		this(new ItemLike[] { ammo }, new ItemLike[] { emptyMag }, new ItemLike[] { bullet }, bulletsPerMag);
	}

	public AmmoType(ItemLike[] ammo, ItemLike[] emptyMag, ItemLike[] bullet, int bulletsPerMag) {
		this.variants.add(new AmmoVariant(ammo, bullet));
		ammoVariantIDs.put(AmmoTypes.TYPE_DEFAULT, 0);
		this.emptyMag = emptyMag;
		this.bulletsPerMag = bulletsPerMag;
	}

	static ItemStack[] toStacks(ItemLike[] items) {
		ItemStack[] out = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++) {
			out[i] = items[i] == null ? ItemStack.EMPTY : new ItemStack(items[i]);
		}
		return out;
	}

	public AmmoType addVariant(String key, ItemLike ammo, ItemLike bullet) {
		return this.addVariant(key, new ItemLike[] { ammo }, new ItemLike[] { bullet });
	}

	public AmmoType addVariant(String key, ItemLike... ammo) {
		return this.addVariant(key, ammo, ammo);
	}

	public AmmoType addVariant(String key, ItemLike[] ammo, ItemLike[] bullet) {
		this.variants.add(new AmmoVariant(key, ammo, bullet));
		ammoVariantIDs.put(key, this.variants.size() - 1);
		return this;
	}

	public int getIDforVariantKey(String key) {
		return ammoVariantIDs.getOrDefault(key, 0);
	}

	public ItemStack[] getAmmo(int variant) {
		return this.variants.get(variant).getAmmo();
	}

	public ItemStack[] getEmptyMag() {
		return toStacks(emptyMag);
	}

	public ItemStack[] getBullet(int variant) {
		return this.variants.get(variant).getBullet();
	}

	public String getAmmoVariantKeyfor(ItemStack stack, int index) {
		for (AmmoVariant v : variants) {
			ItemLike a = v.ammo[index];
			if (a != null && stack.is(a.asItem())) {
				return v.key;
			}
		}
		return AmmoTypes.TYPE_DEFAULT;
	}

	public ArrayList<AmmoVariant> getVariants() {
		return variants;
	}

	public boolean hasMultipleVariants() {
		return this.variants.size() > 1;
	}

	public float getShotsPerBullet(int clipsize, int ammoCount) {
		if (ammoCount == 1 && bulletsPerMag == 0) {
			return clipsize;
		}
		float bpm = bulletsPerMag == 0 ? (float) clipsize / (float) ammoCount : bulletsPerMag;
		return ((float) clipsize) / bpm;
	}

	public int getBulletsPerMag() {
		return bulletsPerMag;
	}
}
