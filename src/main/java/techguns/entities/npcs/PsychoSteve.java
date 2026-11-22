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

/** 1.12 PsychoSteve */
public class PsychoSteve extends GenericNPC {

	public PsychoSteve(EntityType<? extends PsychoSteve> type, Level level) {
		super(type, level);
		setTGArmorStats(5f, 0f);
		this.xpReward = 25;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.6D, 75D, 7D, 60D).add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, 1D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		int camo = GenericArmorMultiCamo.getRandomCamoIndexFor((GenericArmorMultiCamo) TGArmors.T1_MINER_CHESTPLATE.get());
		this.setItemSlot(EquipmentSlot.HEAD, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T1_MINER_HELMET.get(), camo));
		this.setItemSlot(EquipmentSlot.CHEST, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T1_MINER_CHESTPLATE.get(), camo));
		this.setItemSlot(EquipmentSlot.LEGS, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T1_MINER_LEGGINGS.get(), camo));
		this.setItemSlot(EquipmentSlot.FEET, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T1_MINER_BOOTS.get(), camo));
		equip(EquipmentSlot.MAINHAND, TGuns.CHAINSAW.get());
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
