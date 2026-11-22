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

/** 1.12 SkeletonSoldier */
public class SkeletonSoldier extends GenericNPCUndead {

	public SkeletonSoldier(EntityType<? extends SkeletonSoldier> type, Level level) {
		super(type, level);
		setTGArmorStats(0f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.25D, 25D, 4D, 50D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		equip(EquipmentSlot.HEAD, this.random.nextBoolean() ? TGArmors.T1_SCOUT_HELMET.get() : TGArmors.T1_COMBAT_HELMET.get());
		equip(EquipmentSlot.FEET, this.random.nextBoolean() ? TGArmors.T1_SCOUT_BOOTS.get() : TGArmors.T1_COMBAT_BOOTS.get());
		equip(EquipmentSlot.MAINHAND, pick(TGuns.REVOLVER.get(), TGuns.THOMPSON.get(), TGuns.HANDCANNON.get()));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource src) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.SKELETON_STEP;
	}

	@Override
	public float getWeaponPosX() {
		return -0.06f;
	}

	@Override
	public float getWeaponPosY() {
		return -0.06f;
	}
}
