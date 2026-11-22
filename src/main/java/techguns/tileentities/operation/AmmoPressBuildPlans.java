package techguns.tileentities.operation;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import techguns.TGItems;
import techguns.Techguns;
import techguns.tileentities.AmmoPressTileEnt;

/**
 * Ammo press plans (1.12 AmmoPressBuildPlans). The input lists are item tags now:
 * techguns:ammo_press/metal1 (bullet), techguns:ammo_press/metal2 (2x casing), techguns:ammo_press/powder.
 */
public class AmmoPressBuildPlans {

	public static final int AMMOUNT_PISTOL = 12;
	public static final int AMMOUNT_SHOTGUN = 16;
	public static final int AMMOUNT_RIFLE = 8;
	public static final int AMMOUNT_SNIPER = 4;

	public static final int PLAN_COUNT = 4;

	public static final TagKey<Item> METAL1 = TagKey.create(Registries.ITEM, Techguns.rl("ammo_press/metal1"));
	public static final TagKey<Item> METAL2 = TagKey.create(Registries.ITEM, Techguns.rl("ammo_press/metal2"));
	public static final TagKey<Item> POWDER = TagKey.create(Registries.ITEM, Techguns.rl("ammo_press/powder"));

	public static boolean isValidFor(ItemStack input, int slot) {
		return switch (slot) {
			case AmmoPressTileEnt.SLOT_METAL1 -> input.is(METAL1);
			case AmmoPressTileEnt.SLOT_METAL2 -> input.is(METAL2);
			case AmmoPressTileEnt.SLOT_POWDER -> input.is(POWDER);
			default -> false;
		};
	}

	public static ItemStack getResult(int plan) {
		return switch (plan) {
			case 0 -> new ItemStack(TGItems.PISTOL_ROUNDS.get(), AMMOUNT_PISTOL);
			case 1 -> new ItemStack(TGItems.SHOTGUN_ROUNDS.get(), AMMOUNT_SHOTGUN);
			case 2 -> new ItemStack(TGItems.RIFLE_ROUNDS.get(), AMMOUNT_RIFLE);
			case 3 -> new ItemStack(TGItems.SNIPER_ROUNDS.get(), AMMOUNT_SNIPER);
			default -> ItemStack.EMPTY;
		};
	}

	public static List<ItemStack> getPossibleResults() {
		return List.of(getResult(0), getResult(1), getResult(2), getResult(3));
	}

	public static ItemStack getOutputFor(ItemStack metal1, ItemStack metal2, ItemStack powder, int plan) {
		if (isValidFor(metal1, AmmoPressTileEnt.SLOT_METAL1) && isValidFor(metal2, AmmoPressTileEnt.SLOT_METAL2)
				&& isValidFor(powder, AmmoPressTileEnt.SLOT_POWDER) && metal1.getCount() >= 1 && metal2.getCount() >= 2 && powder.getCount() >= 1) {
			return getResult(plan);
		}
		return ItemStack.EMPTY;
	}

	public static int getTotalPower(int plan) {
		return 100 * AmmoPressTileEnt.POWER_PER_TICK;
	}
}
