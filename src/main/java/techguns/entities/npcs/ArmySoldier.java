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

/** 1.12 ArmySoldier */
public class ArmySoldier extends GenericNPC {

	public ArmySoldier(EntityType<? extends ArmySoldier> type, Level level) {
		super(type, level);
		setTGArmorStats(8f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.3D, 25D, 4D, 75D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		equip(EquipmentSlot.HEAD, this.random.nextDouble() <= 0.5 ? TGArmors.T2_COMBAT_HELMET.get() : TGArmors.BERET.get());
		equipChance(EquipmentSlot.CHEST, TGArmors.T2_COMBAT_CHESTPLATE.get(), 0.5);
		equipChance(EquipmentSlot.LEGS, TGArmors.T2_COMBAT_LEGGINGS.get(), 0.5);
		equipChance(EquipmentSlot.FEET, TGArmors.T2_COMBAT_BOOTS.get(), 0.5);
		equip(EquipmentSlot.MAINHAND, pick(TGuns.M4.get(), TGuns.COMBATSHOTGUN.get(), TGuns.BOLTACTION.get()));
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
