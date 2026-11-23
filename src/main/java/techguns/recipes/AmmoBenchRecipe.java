package techguns.recipes;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
 * Ammo bench recipe (Fabric port AmmoBenchRecipe): one bullet core, one casing and one powder make the result. With the
 * same inputs every recipe matches, and the player picks one like at a stonecutter.
 * <pre>{"type":"techguns:ammo_bench","core":{...},"casing":{...},"powder":{...},"result":{"id":"...","count":12}}</pre>
 */
public record AmmoBenchRecipe(Ingredient core, Ingredient casing, Ingredient powder, ItemStack result) implements Recipe<AmmoBenchRecipe.Input> {

	public static final int SLOT_CORE = 0, SLOT_CASING = 1, SLOT_POWDER = 2;

	public record Input(ItemStack core, ItemStack casing, ItemStack powder) implements RecipeInput {
		@Override
		public ItemStack getItem(int index) {
			return switch (index) {
				case SLOT_CORE -> core;
				case SLOT_CASING -> casing;
				default -> powder;
			};
		}

		@Override
		public int size() {
			return 3;
		}
	}

	/** the ingredient for one of the three input slots */
	public Ingredient ingredient(int slot) {
		return switch (slot) {
			case SLOT_CORE -> core;
			case SLOT_CASING -> casing;
			default -> powder;
		};
	}

	@Override
	public boolean matches(Input input, Level level) {
		return core.test(input.core()) && casing.test(input.casing()) && powder.test(input.powder());
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
		return NonNullList.of(Ingredient.EMPTY, core, casing, powder);
	}

	@Override
	public boolean isSpecial() {
		// keeps it out of the vanilla recipe book
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.AMMO_BENCH_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.AMMO_BENCH.get();
	}

	/* ---- lookups ---- */

	/** the recipes the inputs make, sorted by result like the stonecutter's, so the buttons keep their places */
	public static List<RecipeHolder<AmmoBenchRecipe>> find(Level level, Input input) {
		if (input.core().isEmpty() || input.casing().isEmpty() || input.powder().isEmpty()) {
			return List.of();
		}
		return level.getRecipeManager().getRecipesFor(TGRecipes.AMMO_BENCH.get(), input, level);
	}

	/** whether the item goes into that input slot of some recipe */
	public static boolean isInput(Level level, int slot, ItemStack item) {
		return !item.isEmpty() && level.getRecipeManager().getAllRecipesFor(TGRecipes.AMMO_BENCH.get()).stream()
				.anyMatch(r -> r.value().ingredient(slot).test(item));
	}

	/* ---- serializer ---- */

	public static final MapCodec<AmmoBenchRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("core").forGetter(AmmoBenchRecipe::core),
			Ingredient.CODEC_NONEMPTY.fieldOf("casing").forGetter(AmmoBenchRecipe::casing),
			Ingredient.CODEC_NONEMPTY.fieldOf("powder").forGetter(AmmoBenchRecipe::powder),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AmmoBenchRecipe::result)).apply(i, AmmoBenchRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, AmmoBenchRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, AmmoBenchRecipe::core,
			Ingredient.CONTENTS_STREAM_CODEC, AmmoBenchRecipe::casing,
			Ingredient.CONTENTS_STREAM_CODEC, AmmoBenchRecipe::powder,
			ItemStack.STREAM_CODEC, AmmoBenchRecipe::result,
			AmmoBenchRecipe::new);
}
