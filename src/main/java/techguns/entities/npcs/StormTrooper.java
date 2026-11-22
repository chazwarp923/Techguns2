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

/** 1.12 StormTrooper */
public class StormTrooper extends GenericNPCGearSpecificStats {

	public StormTrooper(EntityType<? extends StormTrooper> type, Level level) {
		super(type, level);
		setTGArmorStats(0f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.3D, 35D, 4D, 75D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		int camo = 1;
		this.setItemSlot(EquipmentSlot.HEAD, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T3_MINER_HELMET.get(), camo));
		this.setItemSlot(EquipmentSlot.CHEST, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T3_MINER_CHESTPLATE.get(), camo));
		this.setItemSlot(EquipmentSlot.LEGS, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T3_MINER_LEGGINGS.get(), camo));
		this.setItemSlot(EquipmentSlot.FEET, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T3_MINER_BOOTS.get(), camo));
		equip(EquipmentSlot.MAINHAND, pick(TGuns.BLASTERRIFLE.get(), TGuns.LASERGUN.get()));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource src) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.ZOMBIE_STEP;
	}
}
