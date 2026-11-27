package techguns.recipes;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import techguns.TGConfig;
import techguns.Techguns;

/**
 * Recipe condition {@code {"type": "techguns:keep_lava_recipes"}}. The Chem Lab recipes that use lava in place of fuel
 * load only while no fuel fluid exists (the {@code techguns:fuel} fluid tag is empty), or while the config option
 * {@code general.keepLavaRecipesWhenFuelIsPresent} is on. As in 1.12 TGMachineRecipes. Checked whenever recipes load,
 * so a changed option or tag applies on the next /reload.
 */
public final class KeepLavaRecipesCondition implements ICondition {
	public static final KeepLavaRecipesCondition INSTANCE = new KeepLavaRecipesCondition();
	public static final MapCodec<KeepLavaRecipesCondition> CODEC = MapCodec.unit(INSTANCE);
	public static final TagKey<Fluid> FUEL = TagKey.create(Registries.FLUID, Techguns.rl("fuel"));

	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Techguns.MODID);
	public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<KeepLavaRecipesCondition>> KEEP_LAVA_RECIPES = CONDITIONS
			.register("keep_lava_recipes", () -> CODEC);

	private KeepLavaRecipesCondition() {
	}

	public static void register(IEventBus modBus) {
		CONDITIONS.register(modBus);
	}

	/** Whether the lava recipes load, given whether any fuel fluid exists. */
	public static boolean keepLava(boolean fuelPresent) {
		return !fuelPresent || TGConfig.keepLavaRecipesWhenFuelIsPresent;
	}

	@Override
	public boolean test(IContext context) {
		return keepLava(!context.getTag(FUEL).isEmpty());
	}

	@Override
	public MapCodec<? extends ICondition> codec() {
		return CODEC;
	}

	@Override
	public String toString() {
		return "techguns:keep_lava_recipes";
	}
}
