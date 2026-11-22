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

/** 1.12 ZombieMiner */
public class ZombieMiner extends GenericNPCUndead {

	public ZombieMiner(EntityType<? extends ZombieMiner> type, Level level) {
		super(type, level);
		setTGArmorStats(0f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.2D, 20D, 4D, 50D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		int camo = GenericArmorMultiCamo.getRandomCamoIndexFor((GenericArmorMultiCamo) TGArmors.T1_MINER_CHESTPLATE.get());
		this.setItemSlot(EquipmentSlot.HEAD, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T1_MINER_HELMET.get(), camo));
		equipChance(EquipmentSlot.CHEST, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T1_MINER_CHESTPLATE.get(), camo), 0.5);
		equipChance(EquipmentSlot.LEGS, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T1_MINER_LEGGINGS.get(), camo), 0.5);
		equipChance(EquipmentSlot.FEET, GenericArmorMultiCamo.getNewWithCamo(TGArmors.T1_MINER_BOOTS.get(), camo), 0.5);
		equip(EquipmentSlot.MAINHAND, pick(Items.STONE_PICKAXE, Items.IRON_PICKAXE, TGuns.HANDCANNON.get()));
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
