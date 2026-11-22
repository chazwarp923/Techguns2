package techguns.items.armors;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.curios.TGCurios;

/** Was GenericArmor.getArmorBonusForPlayer. */
public class ArmorBonusUtil {

	private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD };

	/** Sum of bonuses of worn Techguns armor pieces (powered armor uses its unpowered values without power). */
	public static ArmorPieceBonus ARMOR_PIECES = (ply, type, consume) -> {
		float bonus = 0f;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack armor = ply.getItemBySlot(slot);
			if (armor.getItem() instanceof GenericArmor ga) {
				bonus += ga.getBonus(armor, type, consume, ply);
			}
		}
		return bonus;
	};

	public static boolean isWearingTechgunsArmor(Player ply) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (ply.getItemBySlot(slot).getItem() instanceof GenericArmor) {
				return true;
			}
		}
		return false;
	}

	@FunctionalInterface
	public interface ArmorPieceBonus {
		float get(Player ply, TGArmorBonus type, boolean consumePower);
	}

	public static float getArmorBonusForPlayer(Player ply, TGArmorBonus type, boolean consumePower) {
		float bonus = ARMOR_PIECES.get(ply, type, consumePower);
		// worn Curios face / back / hand items
		for (ItemStack stack : TGCurios.getSpecialSlotItems(ply)) {
			bonus += getBonusForSlot(stack, type, consumePower, ply);
		}
		return bonus;
	}

	private static float getBonusForSlot(ItemStack stack, TGArmorBonus type, boolean consumePower, Player ply) {
		if (!stack.isEmpty() && stack.getItem() instanceof ITGSpecialSlot item) {
			// slot items at max damage are out of charge (they never break)
			if (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage()) {
				return item.getBonus(type, stack, consumePower, ply);
			}
		}
		return 0f;
	}
}
