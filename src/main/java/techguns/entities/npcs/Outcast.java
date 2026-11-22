package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import techguns.TGArmors;
import techguns.TGuns;
import techguns.items.armors.GenericArmorMultiCamo;

/** 1.12 Outcast */
public class Outcast extends GenericNPCGearSpecificStats {

	public Outcast(EntityType<? extends Outcast> type, Level level) {
		super(type, level);
		setTGArmorStats(0f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.2D, 30D, 8D, 60D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		this.setItemSlot(EquipmentSlot.HEAD, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T3_POWER_HELMET.get(), 1 + this.random.nextInt(2)));
		this.setItemSlot(EquipmentSlot.CHEST, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T3_POWER_CHESTPLATE.get(), 1 + this.random.nextInt(2)));
		this.setItemSlot(EquipmentSlot.LEGS, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T3_POWER_LEGGINGS.get(), 1 + this.random.nextInt(2)));
		this.setItemSlot(EquipmentSlot.FEET, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T3_POWER_BOOTS.get(), 1 + this.random.nextInt(2)));
		equip(EquipmentSlot.MAINHAND, pick(TGuns.LASERGUN.get(), TGuns.GRIMREAPER.get(), TGuns.MINIGUN.get(), TGuns.BIOGUN.get(), TGuns.FLAMETHROWER.get(), TGuns.TESLAGUN.get(),
				TGuns.LMG.get()));
	}
}
