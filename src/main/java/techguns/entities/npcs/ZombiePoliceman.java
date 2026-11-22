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

/** 1.12 ZombiePoliceman */
public class ZombiePoliceman extends GenericNPCUndead {

	public ZombiePoliceman(EntityType<? extends ZombiePoliceman> type, Level level) {
		super(type, level);
		setTGArmorStats(5f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.25D, 25D, 4D, 50D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		int camo = 5;
		equipChance(EquipmentSlot.HEAD, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T2_COMBAT_HELMET.get(), camo), 0.5);
		equipChance(EquipmentSlot.CHEST, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T2_COMBAT_CHESTPLATE.get(), camo), 0.5);
		equipChance(EquipmentSlot.LEGS, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T2_COMBAT_LEGGINGS.get(), camo), 0.5);
		equipChance(EquipmentSlot.FEET, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T2_COMBAT_BOOTS.get(), camo), 0.5);
		equip(EquipmentSlot.MAINHAND, pick(TGuns.REVOLVER.get(), TGuns.PISTOL.get()));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ZOMBIE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource src) {
		return SoundEvents.ZOMBIE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ZOMBIE_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.ZOMBIE_STEP;
	}
}
