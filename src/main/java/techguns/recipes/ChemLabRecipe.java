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
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Chemical laboratory recipe (1.12 ChemLabRecipes): up to two items, a "bottle" item and an input fluid make an
 * item and / or a fluid. A missing item input requires that slot to be empty. With allow_swap the two item inputs may
 * be in either slot.
 * <pre>{"type":"techguns:chem_lab","input1":{"tag":"c:dusts/redstone"},"input2":{"item":"minecraft:coal"},
 * "fluid_input":{"tag":"minecraft:water","amount":250},"result":{"id":"minecraft:gunpowder"},"allow_swap":true,"power_per_tick":5}</pre>
 */
public record ChemLabRecipe(Optional<SizedIngredient> input1, Optional<SizedIngredient> input2, Optional<SizedIngredient> bottle,
		Optional<SizedFluidIngredient> fluidInput, ItemStack result, FluidStack fluidResult, boolean allowSwap, int powerPerTick)
		implements Recipe<ChemLabRecipe.Input> {

	/** chem lab work time, the same for every recipe like in 1.12 */
	public static final int TIME = 100;

	public record Input(ItemStack slot1, ItemStack slot2, ItemStack bottle, FluidStack fluid) implements RecipeInput {
		@Override
		public ItemStack getItem(int index) {
			return switch (index) {
				case 0 -> slot1;
				case 1 -> slot2;
				case 2 -> bottle;
				default -> ItemStack.EMPTY;
			};
		}

		@Override
		public int size() {
			return 3;
		}
	}

	public enum Match {
		INVALID, VALID, VALID_SWAPPED;
	}

	private static boolean matchSlot(Optional<SizedIngredient> ing, ItemStack stack) {
		if (ing.isEmpty()) {
			return stack.isEmpty();
		}
		return ing.get().test(stack);
	}

	/** item matches the ingredient type, ignoring the amount */
	private static boolean matchType(Optional<SizedIngredient> ing, ItemStack stack) {
		return ing.isPresent() && !stack.isEmpty() && ing.get().ingredient().test(stack);
	}

	private boolean matchFluid(FluidStack fluid) {
		return fluidInput.isEmpty() || fluidInput.get().test(fluid);
	}

	public Match match(Input in) {
		if (!matchSlot(bottle, in.bottle()) || !matchFluid(in.fluid())) {
			return Match.INVALID;
		}
		if (matchSlot(input1, in.slot1()) && matchSlot(input2, in.slot2())) {
			return Match.VALID;
		}
		if (allowSwap && matchSlot(input1, in.slot2()) && matchSlot(input2, in.slot1())) {
			return Match.VALID_SWAPPED;
		}
		return Match.INVALID;
	}

	public boolean usesItem(ItemStack stack) {
		return matchType(input1, stack) || matchType(input2, stack) || matchType(bottle, stack);
	}

	/** item could go in slot 2 next to slot1 (1.12 isValidInputSlot1_2) */
	public boolean validPair(ItemStack slot1, ItemStack slot2) {
		boolean straight = (input1.isEmpty() ? slot1.isEmpty() : matchType(input1, slot1)) && matchType(input2, slot2);
		boolean swapped = allowSwap && matchType(input1, slot2) && (input2.isEmpty() ? slot1.isEmpty() : matchType(input2, slot1));
		return straight || swapped;
	}

	public static int count(Optional<SizedIngredient> ing) {
		return ing.map(SizedIngredient::count).orElse(0);
	}

	@Override
	public boolean matches(Input input, Level level) {
		return match(input) != Match.INVALID;
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
		NonNullList<Ingredient> l = NonNullList.create();
		input1.ifPresent(i -> l.add(i.ingredient()));
		input2.ifPresent(i -> l.add(i.ingredient()));
		bottle.ifPresent(i -> l.add(i.ingredient()));
		return l;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.CHEM_LAB_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.CHEM_LAB.get();
	}

	/* ---- lookups ---- */

	public static Optional<RecipeHolder<ChemLabRecipe>> find(Level level, Input in) {
		if (level == null) {
			return Optional.empty();
		}
		return level.getRecipeManager().getAllRecipesFor(TGRecipes.CHEM_LAB.get()).stream().filter(r -> r.value().match(in) != Match.INVALID)
				.findFirst();
	}

	public static boolean allowInBottleSlot(Level level, ItemStack stack) {
		return level != null && level.getRecipeManager().getAllRecipesFor(TGRecipes.CHEM_LAB.get()).stream()
				.anyMatch(r -> matchType(r.value().bottle, stack));
	}

	public static boolean hasRecipeUsing(Level level, ItemStack stack) {
		return level != null && level.getRecipeManager().getAllRecipesFor(TGRecipes.CHEM_LAB.get()).stream()
				.anyMatch(r -> r.value().usesItem(stack));
	}

	public static boolean allowAsInput2(Level level, ItemStack slot1, ItemStack stack) {
		return level != null && level.getRecipeManager().getAllRecipesFor(TGRecipes.CHEM_LAB.get()).stream()
				.anyMatch(r -> r.value().validPair(slot1, stack));
	}

	/* ---- serializer ---- */

	public static final MapCodec<ChemLabRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			SizedIngredient.FLAT_CODEC.optionalFieldOf("input1").forGetter(ChemLabRecipe::input1),
			SizedIngredient.FLAT_CODEC.optionalFieldOf("input2").forGetter(ChemLabRecipe::input2),
			SizedIngredient.FLAT_CODEC.optionalFieldOf("bottle").forGetter(ChemLabRecipe::bottle),
			SizedFluidIngredient.FLAT_CODEC.optionalFieldOf("fluid_input").forGetter(ChemLabRecipe::fluidInput),
			ItemStack.STRICT_CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(ChemLabRecipe::result),
			FluidStack.CODEC.optionalFieldOf("fluid_result", FluidStack.EMPTY).forGetter(ChemLabRecipe::fluidResult),
			Codec.BOOL.optionalFieldOf("allow_swap", false).forGetter(ChemLabRecipe::allowSwap),
			Codec.INT.optionalFieldOf("power_per_tick", 5).forGetter(ChemLabRecipe::powerPerTick)).apply(i, ChemLabRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ChemLabRecipe> STREAM_CODEC = StreamCodec.of(ChemLabRecipe::write,
			ChemLabRecipe::read);

	private static final StreamCodec<RegistryFriendlyByteBuf, Optional<SizedIngredient>> OPT_ING = ByteBufCodecs
			.optional(SizedIngredient.STREAM_CODEC);
	private static final StreamCodec<RegistryFriendlyByteBuf, Optional<SizedFluidIngredient>> OPT_FLUID = ByteBufCodecs
			.optional(SizedFluidIngredient.STREAM_CODEC);

	private static void write(RegistryFriendlyByteBuf buf, ChemLabRecipe r) {
		OPT_ING.encode(buf, r.input1);
		OPT_ING.encode(buf, r.input2);
		OPT_ING.encode(buf, r.bottle);
		OPT_FLUID.encode(buf, r.fluidInput);
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.result);
		FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, r.fluidResult);
		buf.writeBoolean(r.allowSwap);
		buf.writeVarInt(r.powerPerTick);
	}

	private static ChemLabRecipe read(RegistryFriendlyByteBuf buf) {
		return new ChemLabRecipe(OPT_ING.decode(buf), OPT_ING.decode(buf), OPT_ING.decode(buf), OPT_FLUID.decode(buf),
				ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), FluidStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readVarInt());
	}
}
