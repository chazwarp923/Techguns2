package techguns.recipes;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import techguns.Techguns;

/**
 * Fabricator recipe (1.12 FabricatorRecipe): main input plus optional wire / powder / plate slot items. Which items fit
 * the three special slots is set by the item tags techguns:fabricator/wires, powder and plates.
 */
public record FabricatorRecipe(SizedIngredient input, Optional<SizedIngredient> wires, Optional<SizedIngredient> powder, Optional<SizedIngredient> plate,
		ItemStack result) implements Recipe<FabricatorRecipe.Input> {

	public static final int TIME = 100;

	public static final TagKey<Item> WIRES_SLOT = ItemTags.create(Techguns.rl("fabricator/wires"));
	public static final TagKey<Item> POWDER_SLOT = ItemTags.create(Techguns.rl("fabricator/powder"));
	public static final TagKey<Item> PLATES_SLOT = ItemTags.create(Techguns.rl("fabricator/plates"));

	public record Input(ItemStack input, ItemStack wires, ItemStack powder, ItemStack plate) implements RecipeInput {
		@Override
		public ItemStack getItem(int index) {
			return switch (index) {
				case 0 -> input;
				case 1 -> wires;
				case 2 -> powder;
				default -> plate;
			};
		}

		@Override
		public int size() {
			return 4;
		}
	}

	private static boolean slotMatches(Optional<SizedIngredient> ing, ItemStack stack) {
		return ing.isEmpty() || ing.get().test(stack);
	}

	@Override
	public boolean matches(Input in, Level level) {
		return input.test(in.input) && slotMatches(wires, in.wires) && slotMatches(powder, in.powder) && slotMatches(plate, in.plate);
	}

	/** how often the recipe can run at once with the given stacks, limited by maxMultiplier and the output slot */
	public int getMaxMultiplier(Input in, ItemStack outputSlot, int maxMultiplier) {
		int multi = Math.min(maxMultiplier, in.input.getCount() / input.count());
		if (wires.isPresent()) {
			multi = Math.min(multi, in.wires.getCount() / wires.get().count());
		}
		if (powder.isPresent()) {
			multi = Math.min(multi, in.powder.getCount() / powder.get().count());
		}
		if (plate.isPresent()) {
			multi = Math.min(multi, in.plate.getCount() / plate.get().count());
		}
		if (!outputSlot.isEmpty()) {
			if (!ItemStack.isSameItemSameComponents(outputSlot, result)) {
				return 0;
			}
			multi = Math.min(multi, (outputSlot.getMaxStackSize() - outputSlot.getCount()) / result.getCount());
		} else {
			multi = Math.min(multi, result.getMaxStackSize() / result.getCount());
		}
		return Math.max(0, multi);
	}

	@Override
	public ItemStack assemble(Input in, HolderLookup.Provider registries) {
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
		NonNullList<Ingredient> list = NonNullList.create();
		list.add(input.ingredient());
		wires.ifPresent(i -> list.add(i.ingredient()));
		powder.ifPresent(i -> list.add(i.ingredient()));
		plate.ifPresent(i -> list.add(i.ingredient()));
		return list;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.FABRICATOR_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.FABRICATOR.get();
	}

	public static Optional<RecipeHolder<FabricatorRecipe>> find(Level level, Input in) {
		if (level == null || in.input.isEmpty()) {
			return Optional.empty();
		}
		return level.getRecipeManager().getRecipeFor(TGRecipes.FABRICATOR.get(), in, level);
	}

	public static final MapCodec<FabricatorRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			SizedIngredient.FLAT_CODEC.fieldOf("input").forGetter(FabricatorRecipe::input),
			SizedIngredient.FLAT_CODEC.optionalFieldOf("wires").forGetter(FabricatorRecipe::wires),
			SizedIngredient.FLAT_CODEC.optionalFieldOf("powder").forGetter(FabricatorRecipe::powder),
			SizedIngredient.FLAT_CODEC.optionalFieldOf("plate").forGetter(FabricatorRecipe::plate),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FabricatorRecipe::result)).apply(i, FabricatorRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, FabricatorRecipe> STREAM_CODEC = StreamCodec.composite(
			SizedIngredient.STREAM_CODEC, FabricatorRecipe::input,
			ByteBufCodecs.optional(SizedIngredient.STREAM_CODEC), FabricatorRecipe::wires,
			ByteBufCodecs.optional(SizedIngredient.STREAM_CODEC), FabricatorRecipe::powder,
			ByteBufCodecs.optional(SizedIngredient.STREAM_CODEC), FabricatorRecipe::plate,
			ItemStack.STREAM_CODEC, FabricatorRecipe::result,
			FabricatorRecipe::new);
}
