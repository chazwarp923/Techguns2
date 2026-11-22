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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Metal press recipe (1.12 MetalPressRecipes): one item of each input -> result. With allow_swap the inputs may be
 * in either slot.
 * <pre>{"type":"techguns:metal_press","input1":{...},"input2":{...},"allow_swap":true,"result":{"id":"...","count":2}}</pre>
 */
public record MetalPressRecipe(Ingredient input1, Ingredient input2, boolean allowSwap, ItemStack result) implements Recipe<MetalPressRecipe.Input> {

	public record Input(ItemStack slot1, ItemStack slot2) implements RecipeInput {
		@Override
		public ItemStack getItem(int index) {
			return index == 0 ? slot1 : slot2;
		}

		@Override
		public int size() {
			return 2;
		}
	}

	public boolean isValidInput(ItemStack s1, ItemStack s2) {
		if (input1.test(s1) && input2.test(s2)) {
			return true;
		}
		return allowSwap && input1.test(s2) && input2.test(s1);
	}

	public boolean isItemPartOfRecipe(ItemStack item) {
		return input1.test(item) || input2.test(item);
	}

	@Override
	public boolean matches(Input input, Level level) {
		return isValidInput(input.slot1(), input.slot2());
	}

	@Override
	public ItemStack assemble(Input input, HolderLookup.Provider registries) {
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
		return NonNullList.of(Ingredient.EMPTY, input1, input2);
	}

	@Override
	public boolean isSpecial() {
		// keeps it out of the vanilla recipe book
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.METAL_PRESS_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.METAL_PRESS.get();
	}

	/* ---- lookups ---- */

	public static Optional<RecipeHolder<MetalPressRecipe>> find(Level level, ItemStack s1, ItemStack s2) {
		if (s1.isEmpty() || s2.isEmpty()) {
			return Optional.empty();
		}
		return level.getRecipeManager().getRecipeFor(TGRecipes.METAL_PRESS.get(), new Input(s1, s2), level);
	}

	public static ItemStack getOutputFor(Level level, ItemStack s1, ItemStack s2) {
		return find(level, s1, s2).map(r -> r.value().result()).orElse(ItemStack.EMPTY);
	}

	public static boolean hasRecipeUsing(Level level, ItemStack item) {
		if (item.isEmpty()) {
			return false;
		}
		return level.getRecipeManager().getAllRecipesFor(TGRecipes.METAL_PRESS.get()).stream().anyMatch(r -> r.value().isItemPartOfRecipe(item));
	}

	/* ---- serializer ---- */

	public static final MapCodec<MetalPressRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("input1").forGetter(MetalPressRecipe::input1),
			Ingredient.CODEC_NONEMPTY.fieldOf("input2").forGetter(MetalPressRecipe::input2),
			Codec.BOOL.optionalFieldOf("allow_swap", true).forGetter(MetalPressRecipe::allowSwap),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MetalPressRecipe::result)).apply(i, MetalPressRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, MetalPressRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, MetalPressRecipe::input1,
			Ingredient.CONTENTS_STREAM_CODEC, MetalPressRecipe::input2,
			ByteBufCodecs.BOOL, MetalPressRecipe::allowSwap,
			ItemStack.STREAM_CODEC, MetalPressRecipe::result,
			MetalPressRecipe::new);
}
