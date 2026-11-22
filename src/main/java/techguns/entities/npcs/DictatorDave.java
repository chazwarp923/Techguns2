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

/** 1.12 DictatorDave */
public class DictatorDave extends GenericNPC {

	public DictatorDave(EntityType<? extends DictatorDave> type, Level level) {
		super(type, level);
		setTGArmorStats(15f, 0f);
		this.xpReward = 35;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.4D, 100D, 7D, 60D).add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, 2D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		equip(EquipmentSlot.HEAD, TGArmors.BERET.get());
		equip(EquipmentSlot.CHEST, TGArmors.T1_COMBAT_CHESTPLATE.get());
		equip(EquipmentSlot.LEGS, TGArmors.T1_SCOUT_LEGGINGS.get());
		equip(EquipmentSlot.FEET, TGArmors.T1_COMBAT_BOOTS.get());
		equip(EquipmentSlot.MAINHAND, TGuns.GOLDENREVOLVER.get());
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
