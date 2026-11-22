package techguns.tileentities.operation;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import techguns.TGItems;
import techguns.TGSounds;

/**
 * Reaction chamber beam focus (1.12 ReactionBeamFocus): the sound played every reaction tick and the radiation the
 * reaction gives off.
 */
public record ReactionBeamFocus(Supplier<? extends Item> item, Supplier<SoundEvent> sound, float radiationFactorFailure, float radiationFactorSuccess,
		float radRadiusInner, float radRadiusOuter) {

	public ReactionBeamFocus(Supplier<? extends Item> item, Supplier<SoundEvent> sound) {
		this(item, sound, 0.5f, 0f, 4.0f, 6.0f);
	}

	public static final List<ReactionBeamFocus> FOCI = List.of(new ReactionBeamFocus(TGItems.RC_HEAT_RAY, TGSounds.REACTION_CHAMBER_HEATRAY_WORK),
			new ReactionBeamFocus(TGItems.RC_UV_EMITTER, TGSounds.CHEM_LAB_WORK));

	@Nullable
	public static ReactionBeamFocus get(ItemStack stack) {
		if (!stack.isEmpty()) {
			for (ReactionBeamFocus f : FOCI) {
				if (stack.is(f.item().get())) {
					return f;
				}
			}
		}
		return null;
	}
}
