package techguns.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import techguns.items.armors.GenericArmor;

/**
 * Grinder recipe (1.12 GrinderRecipes / GrinderRecipeChance): one input item -> several outputs. Each output has an
 * optional chance; count * chance is the expected amount, the fraction is rolled.
 * <pre>{"type":"techguns:grinder","input":{"item":"techguns:pistolrounds"},
 * "results":[{"id":"techguns:nuggetlead","chance":0.75},{"id":"minecraft:gunpowder","chance":0.0833}]}</pre>
 * Techguns armor is ground into its repair materials without a recipe, see {@link #getArmorOutputs}.
 */
public record GrinderRecipe(Ingredient input, List<ChanceOutput> results) implements Recipe<SingleRecipeInput> {

	public record ChanceOutput(ItemStack item, float chance) {

		public static final Codec<ChanceOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
				ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(o -> o.item().getItemHolder()),
				Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(o -> o.item().getCount()),
				Codec.floatRange(0f, 1f).optionalFieldOf("chance", 1f).forGetter(ChanceOutput::chance))
				.apply(i, (item, count, chance) -> new ChanceOutput(new ItemStack(item, count), chance)));

		public static final StreamCodec<RegistryFriendlyByteBuf, ChanceOutput> STREAM_CODEC = StreamCodec.composite(
				ItemStack.STREAM_CODEC, ChanceOutput::item,
				ByteBufCodecs.FLOAT, ChanceOutput::chance,
				ChanceOutput::new);

		/** rolled amount for multiplier inputs (1.12 MachineOperationChance.updateChanceRolls) */
		public ItemStack roll(int multiplier, RandomSource rand) {
			if (chance >= 1f) {
				return item.copyWithCount(item.getCount() * multiplier);
			}
			double amount = item.getCount() * (double) chance * multiplier;
			int fixed = (int) Math.floor(amount);
			if (rand.nextDouble() < amount - fixed) {
				fixed++;
			}
			return item.copyWithCount(fixed);
		}
	}

	public List<ItemStack> roll(int multiplier, RandomSource rand) {
		List<ItemStack> ret = new ArrayList<>(results.size());
		for (ChanceOutput o : results) {
			ItemStack s = o.roll(multiplier, rand);
			if (!s.isEmpty()) {
				ret.add(s);
			}
		}
		return ret;
	}

	@Override
	public boolean matches(SingleRecipeInput in, Level level) {
		return input.test(in.item());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput in, HolderLookup.Provider registries) {
		return results.isEmpty() ? ItemStack.EMPTY : results.get(0).item().copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return results.isEmpty() ? ItemStack.EMPTY : results.get(0).item();
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, input);
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.GRINDER_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.GRINDER.get();
	}

	/* ---- lookups ---- */

	public static Optional<RecipeHolder<GrinderRecipe>> find(Level level, ItemStack item) {
		if (item.isEmpty()) {
			return Optional.empty();
		}
		return level.getRecipeManager().getRecipeFor(TGRecipes.GRINDER.get(), new SingleRecipeInput(item), level);
	}

	/** 1.12 GrinderRecipeGenericArmor: the repair materials of the armor at its current durability */
	public static List<ItemStack> getArmorOutputs(ItemStack armor) {
		if (!(armor.getItem() instanceof GenericArmor ga) || !ga.canRepairOnRepairBench(armor)) {
			return List.of();
		}
		ItemStack inverted = armor.copy();
		inverted.setDamageValue(armor.getMaxDamage() - 1 - armor.getDamageValue());
		return ga.getRepairMats(inverted);
	}

	public static boolean canGrind(Level level, ItemStack item) {
		return find(level, item).isPresent() || !getArmorOutputs(item).isEmpty();
	}

	/* ---- serializer ---- */

	public static final MapCodec<GrinderRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(GrinderRecipe::input),
			ChanceOutput.CODEC.listOf().fieldOf("results").forGetter(GrinderRecipe::results)).apply(i, GrinderRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, GrinderRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, GrinderRecipe::input,
			ChanceOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), GrinderRecipe::results,
			GrinderRecipe::new);
}
