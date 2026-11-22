package techguns.entities.npcs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.TGDamageSource;
import techguns.items.armors.GenericArmor;

/** NPCs whose armor value comes from the armor they wear (1.12 GenericNPCGearSpecificStats). */
public abstract class GenericNPCGearSpecificStats extends GenericNPC {

	protected GenericNPCGearSpecificStats(EntityType<? extends GenericNPCGearSpecificStats> type, Level level) {
		super(type, level);
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		float total = 0f;
		for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
			ItemStack stack = this.getItemBySlot(slot);
			if (stack.getItem() instanceof GenericArmor ga) {
				total += ga.getArmorValue(stack, dmgsrc.damageType);
			} else if (stack.getItem() instanceof ArmorItem a) {
				total += DamageSystem.getArmorAgainstDamageTypeDefault(this, a.getDefense(), dmgsrc.damageType);
			}
		}
		return total;
	}
}
