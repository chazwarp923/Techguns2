package techguns.recipes;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Charging station recipe (1.12 ChargingStationRecipe): charges input into result, taking charge_amount FE.
 * Items with an energy capability are charged directly and need no recipe.
 * <pre>{"type":"techguns:charging_station","input":{"item":"techguns:energycellempty"},"result":{"id":"techguns:energycell"},
 * "charge_amount":50000}</pre>
 */
public record ChargingStationRecipe(SizedIngredient input, ItemStack result, int chargeAmount) implements Recipe<SingleRecipeInput> {

	@Override
	public boolean matches(SingleRecipeInput in, Level level) {
		return input.test(in.item());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput in, HolderLookup.Provider registries) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, input.ingredient());
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.CHARGING_STATION_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.CHARGING_STATION.get();
	}

	public static Optional<RecipeHolder<ChargingStationRecipe>> find(Level level, ItemStack stack) {
		if (stack.isEmpty() || level == null) {
			return Optional.empty();
		}
		return level.getRecipeManager().getRecipeFor(TGRecipes.CHARGING_STATION.get(), new SingleRecipeInput(stack), level);
	}

	/** item used by any recipe, ignoring the amount */
	public static boolean isInput(Level level, ItemStack stack) {
		if (stack.isEmpty() || level == null) {
			return false;
		}
		return level.getRecipeManager().getAllRecipesFor(TGRecipes.CHARGING_STATION.get()).stream()
				.anyMatch(r -> r.value().input.ingredient().test(stack));
	}

	public static final MapCodec<ChargingStationRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			SizedIngredient.FLAT_CODEC.fieldOf("input").forGetter(ChargingStationRecipe::input),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ChargingStationRecipe::result),
			Codec.INT.fieldOf("charge_amount").forGetter(ChargingStationRecipe::chargeAmount)).apply(i, ChargingStationRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ChargingStationRecipe> STREAM_CODEC = StreamCodec.composite(
			SizedIngredient.STREAM_CODEC, ChargingStationRecipe::input,
			ItemStack.STREAM_CODEC, ChargingStationRecipe::result,
			ByteBufCodecs.VAR_INT, ChargingStationRecipe::chargeAmount,
			ChargingStationRecipe::new);
}
