package techguns.api.render;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Techguns item renderer. Called with the matrix state 1.12.2's ItemRenderHack saw: the hand / GUI
 * transform applied by vanilla, without model display transforms. Draws via the GlStateManager shim.
 */
public interface IItemRenderer {
	void renderItem(@Nonnull ItemDisplayContext transform, @Nonnull ItemStack stack, @Nullable LivingEntity elb, boolean leftHanded);
}
