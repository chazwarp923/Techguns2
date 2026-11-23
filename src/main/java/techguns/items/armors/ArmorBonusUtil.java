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
		return ARMOR_PIECES.get(ply, type, consumePower) + getSlotBonus(ply, type, consumePower);
	}

	/** Sum of the worn Curios face / back / hand items only */
	public static float getSlotBonus(Player ply, TGArmorBonus type, boolean consumePower) {
		float bonus = 0f;
		for (ItemStack stack : TGCurios.getSpecialSlotItems(ply)) {
			bonus += getBonusForSlot(stack, type, consumePower, ply);
		}
		return bonus;
	}

	/** whether a worn slot item still works: slot items at max damage are out of charge (they never break) */
	public static boolean slotItemWorks(ItemStack stack) {
		return !stack.isEmpty() && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage());
	}

	private static float getBonusForSlot(ItemStack stack, TGArmorBonus type, boolean consumePower, Player ply) {
		if (slotItemWorks(stack) && stack.getItem() instanceof ITGSpecialSlot item) {
			return item.getBonus(type, stack, consumePower, ply);
		}
		return 0f;
	}
}
