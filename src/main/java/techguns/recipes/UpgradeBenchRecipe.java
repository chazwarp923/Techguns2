package techguns.recipes;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import techguns.TGConfig;
import techguns.items.armors.GenericArmor;

/**
 * Upgrade bench recipe (1.12 UpgradeBenchRecipes): an upgrade item puts an enchantment of a level on a piece of Techguns
 * armor, for TGConfig.upgrade_xp_cost xp points per level gained.
 * <pre>{"type":"techguns:upgrade_bench","upgrade":{"item":"techguns:upgrade_protection_1"},"enchantment":"minecraft:protection","level":1}</pre>
 */
public record UpgradeBenchRecipe(Ingredient upgrade, Holder<Enchantment> enchantment, int level) implements Recipe<MetalPressRecipe.Input> {

	/** Techguns armor the enchantment can go on */
	public boolean isValidArmor(ItemStack armor) {
		if (armor.isEmpty() || !(armor.getItem() instanceof GenericArmor)) {
			return false;
		}
		// the armor is not in the vanilla enchantable tags, accept the armor enchantments too
		return enchantment.value().canEnchant(armor) || enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE);
	}

	public int currentLevel(ItemStack armor) {
		return EnchantmentHelper.getItemEnchantmentLevel(enchantment, armor);
	}

	/** the upgraded armor, empty if the armor already has this level or higher */
	public ItemStack apply(ItemStack armor) {
		if (currentLevel(armor) >= level) {
			return ItemStack.EMPTY;
		}
		ItemStack out = armor.copyWithCount(1);
		EnchantmentHelper.updateEnchantments(out, m -> m.set(enchantment, level));
		return out;
	}

	public int xpCost(ItemStack armor) {
		int dif = level - currentLevel(armor);
		return dif > 0 ? TGConfig.upgrade_xp_cost * dif : 0;
	}

	@Override
	public boolean matches(MetalPressRecipe.Input input, Level level) {
		return isValidArmor(input.slot1()) && upgrade.test(input.slot2());
	}

	@Override
	public ItemStack assemble(MetalPressRecipe.Input input, HolderLookup.Provider registries) {
		return apply(input.slot1());
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return ItemStack.EMPTY;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, upgrade);
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TGRecipes.UPGRADE_BENCH_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TGRecipes.UPGRADE_BENCH.get();
	}

	public static Optional<RecipeHolder<UpgradeBenchRecipe>> find(Level level, ItemStack armor, ItemStack upgrade) {
		if (level == null || armor.isEmpty() || upgrade.isEmpty()) {
			return Optional.empty();
		}
		return level.getRecipeManager().getRecipeFor(TGRecipes.UPGRADE_BENCH.get(), new MetalPressRecipe.Input(armor, upgrade), level);
	}

	public static boolean isUpgradeItem(Level level, ItemStack stack) {
		return level != null && !stack.isEmpty()
				&& level.getRecipeManager().getAllRecipesFor(TGRecipes.UPGRADE_BENCH.get()).stream().anyMatch(r -> r.value().upgrade.test(stack));
	}

	public static final MapCodec<UpgradeBenchRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("upgrade").forGetter(UpgradeBenchRecipe::upgrade),
			Enchantment.CODEC.fieldOf("enchantment").forGetter(UpgradeBenchRecipe::enchantment),
			Codec.intRange(1, 255).fieldOf("level").forGetter(UpgradeBenchRecipe::level)).apply(i, UpgradeBenchRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeBenchRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, UpgradeBenchRecipe::upgrade,
			Enchantment.STREAM_CODEC, UpgradeBenchRecipe::enchantment,
			ByteBufCodecs.VAR_INT, UpgradeBenchRecipe::level,
			UpgradeBenchRecipe::new);
}
