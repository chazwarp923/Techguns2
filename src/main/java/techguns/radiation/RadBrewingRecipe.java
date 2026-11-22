package techguns.radiation;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

/** potion + (tag) ingredient -> potion, for drinkable, splash and lingering potions */
public record RadBrewingRecipe(Holder<Potion> input, Ingredient ingredient, Holder<Potion> output) implements IBrewingRecipe {

	@Override
	public boolean isInput(ItemStack stack) {
		return (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))
				&& stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(input);
	}

	@Override
	public boolean isIngredient(ItemStack stack) {
		return ingredient.test(stack);
	}

	@Override
	public ItemStack getOutput(ItemStack in, ItemStack ing) {
		if (!isInput(in) || !isIngredient(ing)) {
			return ItemStack.EMPTY;
		}
		return PotionContents.createItemStack(in.getItem(), output);
	}
}
