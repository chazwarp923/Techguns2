package techguns.api.guns;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IGenericGun {
	boolean isShootWithLeftClick();

	boolean isSemiAuto();

	/** Client only. */
	boolean isZooming();

	void shootGunPrimary(ItemStack stack, Level level, Player player, boolean zooming, InteractionHand hand, Entity target);

	int getAmmoLeft(ItemStack stack);

	GunHandType getGunHandType();

	boolean isHoldZoom();

	float getZoomMult();

	default boolean canCharge() {
		return false;
	}

	ResourceLocation getCurrentTexture(ItemStack stack);
}
