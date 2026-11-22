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

/** 1.12 Commando */
public class Commando extends GenericNPC {

	public Commando(EntityType<? extends Commando> type, Level level) {
		super(type, level);
		setTGArmorStats(8f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.35D, 30D, 5D, 75D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		equip(EquipmentSlot.HEAD, TGArmors.T2_COMMANDO_HELMET.get());
		equip(EquipmentSlot.CHEST, TGArmors.T2_COMMANDO_CHESTPLATE.get());
		equip(EquipmentSlot.LEGS, TGArmors.T2_COMMANDO_LEGGINGS.get());
		equip(EquipmentSlot.FEET, TGArmors.T2_COMMANDO_BOOTS.get());
		equip(EquipmentSlot.MAINHAND, TGuns.M4_INFILTRATOR.get());
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
		return SoundEvents.ZOMBIE_VILLAGER_STEP;
	}
}
