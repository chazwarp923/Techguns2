package techguns.recipes;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Camo bench block recipe (1.12 CamoBenchRecipes): the 1.12 metadata variants are separate blocks now, a recipe is the
 * ordered list of items the camo bench cycles through. Items with {@link techguns.items.armors.ICamoChangeable} cycle
 * their own camos and need no recipe.
 * <pre>{"type":"techguns:camo_bench","items":["minecraft:white_wool","minecraft:orange_wool",...]}</pre>
 */
public record CamoBenchRecipe(List<Holder<Item>> items) implements Recipe<SingleRecipeInput> {

	public int indexOf(ItemStack stack) {
		for (int i = 0; i < items.size(); i++) {
			if (stack.is(items.get(i))) {
				return i;
			}
		}
		return -1;
	}

	/** the stack turned into the next / previous item of the cycle, components (banner patterns...) are kept */
	public ItemStack cycle(ItemStack stack, boolean back) {
		int i = indexOf(stack);
		if (i < 0) {
			return stack;
		}
		int n = items.size();
		int next = Math.floorMod(i + (back ? -1 : 1), n);
		return stack.transmuteCopy(items.get(next).value(), stack.getCount());
	}

	@Override
	public boolean matches(SingleRecipeInput input, Level level) {
		return indexOf(input.item()) >= 0;
	}

	@Override
	public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
		return cycle(input.item(), false);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get(0));
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.CAMO_BENCH_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.CAMO_BENCH.get();
	}

	public static Optional<RecipeHolder<CamoBenchRecipe>> find(Level level, ItemStack stack) {
		if (stack.isEmpty() || level == null) {
			return Optional.empty();
		}
		return level.getRecipeManager().getRecipeFor(TGRecipes.CAMO_BENCH.get(), new SingleRecipeInput(stack), level);
	}

	public static final MapCodec<CamoBenchRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.ITEM.holderByNameCodec().listOf().fieldOf("items").forGetter(CamoBenchRecipe::items)).apply(i, CamoBenchRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CamoBenchRecipe> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.holderRegistry(Registries.ITEM).apply(ByteBufCodecs.list()), CamoBenchRecipe::items, CamoBenchRecipe::new);
}
