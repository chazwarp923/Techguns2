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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Blast furnace recipe (1.12 BlastFurnaceRecipes): input1 x count + input2 x count -> result.
 * <pre>{"type":"techguns:blast_furnace","input1":{"item":"minecraft:iron_ingot","count":4},"input2":{"tag":"minecraft:coals"},
 * "result":{"id":"techguns:ingotsteel","count":4},"power_per_tick":10,"time":800}</pre>
 */
public record BlastFurnaceRecipe(SizedIngredient input1, SizedIngredient input2, ItemStack result, int powerPerTick, int time)
		implements Recipe<MetalPressRecipe.Input> {

	/** both inputs present in at least the needed amount */
	public boolean isValidInput(ItemStack s1, ItemStack s2) {
		return input1.test(s1) && input2.test(s2);
	}

	@Override
	public boolean matches(MetalPressRecipe.Input input, Level level) {
		return isValidInput(input.slot1(), input.slot2());
	}

	@Override
	public ItemStack assemble(MetalPressRecipe.Input input, HolderLookup.Provider registries) {
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
		return NonNullList.of(Ingredient.EMPTY, input1.ingredient(), input2.ingredient());
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.BLAST_FURNACE_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.BLAST_FURNACE.get();
	}

	/* ---- lookups ---- */

	public static Optional<RecipeHolder<BlastFurnaceRecipe>> find(Level level, ItemStack s1, ItemStack s2) {
		if (s1.isEmpty() || s2.isEmpty()) {
			return Optional.empty();
		}
		return level.getRecipeManager().getRecipeFor(TGRecipes.BLAST_FURNACE.get(), new MetalPressRecipe.Input(s1, s2), level);
	}

	/** slot 0 or 1: is the item used in that slot by any recipe (ignoring the amount) */
	public static boolean hasRecipeWithInputForSlot(Level level, ItemStack item, int slot) {
		if (item.isEmpty() || level == null) {
			return false;
		}
		return level.getRecipeManager().getAllRecipesFor(TGRecipes.BLAST_FURNACE.get()).stream()
				.anyMatch(r -> (slot == 0 ? r.value().input1 : r.value().input2).ingredient().test(item));
	}

	/* ---- serializer ---- */

	public static final MapCodec<BlastFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			SizedIngredient.FLAT_CODEC.fieldOf("input1").forGetter(BlastFurnaceRecipe::input1),
			SizedIngredient.FLAT_CODEC.fieldOf("input2").forGetter(BlastFurnaceRecipe::input2),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(BlastFurnaceRecipe::result),
			Codec.INT.optionalFieldOf("power_per_tick", 10).forGetter(BlastFurnaceRecipe::powerPerTick),
			Codec.INT.optionalFieldOf("time", 200).forGetter(BlastFurnaceRecipe::time)).apply(i, BlastFurnaceRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> STREAM_CODEC = StreamCodec.composite(
			SizedIngredient.STREAM_CODEC, BlastFurnaceRecipe::input1,
			SizedIngredient.STREAM_CODEC, BlastFurnaceRecipe::input2,
			ItemStack.STREAM_CODEC, BlastFurnaceRecipe::result,
			ByteBufCodecs.VAR_INT, BlastFurnaceRecipe::powerPerTick,
			ByteBufCodecs.VAR_INT, BlastFurnaceRecipe::time,
			BlastFurnaceRecipe::new);
}
