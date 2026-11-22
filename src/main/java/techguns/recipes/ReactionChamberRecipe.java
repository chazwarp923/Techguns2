package techguns.recipes;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * Reaction chamber recipe (1.12 ReactionChamberRecipe). The input item is hit by the beam of the focus inside a tank
 * filled with exactly liquid_level buckets of the fluid. Every reaction tick (3s) the reaction advances when the
 * intensity setting equals the wanted intensity, which may jump around for unstable recipes. When the required
 * completion is not reached in time, the risk happens.
 * <pre>{"type":"techguns:reaction_chamber","input":{"item":"minecraft:wheat_seeds"},"focus":"techguns:rcuvemitter",
 * "fluid":{"tag":"minecraft:water"},"results":[{"id":"minecraft:wheat"}],"ticks":10,"required_completion":5,"intensity":3,
 * "intensity_margin":1,"liquid_level":1,"liquid_consumption":1000,"instability":0.5,"risk":"explosion_low","power":50000}</pre>
 */
public record ReactionChamberRecipe(Ingredient input, Item focus, FluidIngredient fluid, List<ItemStack> results, int ticks, int requiredCompletion,
		int intensity, int intensityMargin, int liquidLevel, int liquidConsumption, float instability, RiskType risk, int power)
		implements Recipe<ReactionChamberRecipe.Input> {

	public static final int MAX_LEVEL = 10;

	public record Input(ItemStack input, ItemStack focus, FluidStack fluid, int liquidLevel, int intensity) implements RecipeInput {
		@Override
		public ItemStack getItem(int index) {
			return index == 0 ? input : index == 1 ? focus : ItemStack.EMPTY;
		}

		@Override
		public int size() {
			return 2;
		}
	}

	public enum RiskType implements StringRepresentable {
		NONE("none", ChatFormatting.WHITE), BREAK_ITEM("breakItem", ChatFormatting.GREEN), EXPLOSION_LOW("expLow", ChatFormatting.YELLOW),
		EXPLOSION_MEDIUM("expMed", ChatFormatting.RED);

		public static final Codec<RiskType> CODEC = StringRepresentable.fromEnum(RiskType::values);

		private final String key;
		private final ChatFormatting color;

		RiskType(String key, ChatFormatting color) {
			this.key = key;
			this.color = color;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}

		public Component getDisplayName() {
			return Component.translatable("techguns.reactionChamber.RiskType." + key).withStyle(color);
		}

		public boolean isExplosion() {
			return this == EXPLOSION_LOW || this == EXPLOSION_MEDIUM;
		}
	}

	public boolean isStable() {
		return instability == 0.0f || intensityMargin == 0;
	}

	public int getFluidAmount() {
		return liquidLevel * 1000;
	}

	@Override
	public boolean matches(Input in, Level level) {
		return !in.input().isEmpty() && input.test(in.input()) && in.focus().is(focus) && in.liquidLevel() == liquidLevel
				&& in.fluid().getAmount() == getFluidAmount() && fluid.test(in.fluid()) && in.intensity() == intensity;
	}

	public boolean usesItem(ItemStack stack) {
		return input.test(stack);
	}

	@Override
	public ItemStack assemble(Input input, HolderLookup.Provider registries) {
		return getResultItem(registries).copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, input, Ingredient.of(focus));
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.REACTION_CHAMBER_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.REACTION_CHAMBER.get();
	}

	public static Optional<RecipeHolder<ReactionChamberRecipe>> find(Level level, Input in) {
		if (level == null || in.input().isEmpty() || in.focus().isEmpty() || in.fluid().isEmpty()) {
			return Optional.empty();
		}
		return level.getRecipeManager().getRecipeFor(TGRecipes.REACTION_CHAMBER.get(), in, level);
	}

	private static final Codec<Integer> LEVEL = Codec.intRange(0, MAX_LEVEL);

	public static final MapCodec<ReactionChamberRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ReactionChamberRecipe::input),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("focus").forGetter(ReactionChamberRecipe::focus),
			FluidIngredient.CODEC_NON_EMPTY.fieldOf("fluid").forGetter(ReactionChamberRecipe::fluid),
			ItemStack.STRICT_CODEC.listOf().fieldOf("results").forGetter(ReactionChamberRecipe::results),
			Codec.intRange(1, 127).fieldOf("ticks").forGetter(ReactionChamberRecipe::ticks),
			Codec.intRange(1, 127).fieldOf("required_completion").forGetter(ReactionChamberRecipe::requiredCompletion),
			LEVEL.fieldOf("intensity").forGetter(ReactionChamberRecipe::intensity),
			LEVEL.optionalFieldOf("intensity_margin", 0).forGetter(ReactionChamberRecipe::intensityMargin),
			Codec.intRange(1, MAX_LEVEL).fieldOf("liquid_level").forGetter(ReactionChamberRecipe::liquidLevel),
			Codec.INT.optionalFieldOf("liquid_consumption", 0).forGetter(ReactionChamberRecipe::liquidConsumption),
			Codec.floatRange(0f, 1f).optionalFieldOf("instability", 0f).forGetter(ReactionChamberRecipe::instability),
			RiskType.CODEC.optionalFieldOf("risk", RiskType.NONE).forGetter(ReactionChamberRecipe::risk),
			Codec.INT.fieldOf("power").forGetter(ReactionChamberRecipe::power)).apply(i, ReactionChamberRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ReactionChamberRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
}
