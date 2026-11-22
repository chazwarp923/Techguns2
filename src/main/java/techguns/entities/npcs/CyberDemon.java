package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import techguns.TGSounds;
import techguns.TGuns;

/** 1.12 CyberDemon: nether undead with a nether blaster built into its arm (no aimed arm pose). */
public class CyberDemon extends GenericNPCUndead {

	public CyberDemon(EntityType<? extends CyberDemon> type, Level level) {
		super(type, level);
		setTGArmorStats(10.0f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.35D, 30D, 7D, 50D).add(Attributes.ARMOR_TOUGHNESS, 1D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		equip(EquipmentSlot.MAINHAND, TGuns.NETHERBLASTER.get());
	}

	@Override
	protected boolean shouldBurnInDay() {
		return false;
	}

	@Override
	public boolean hasWeaponArmPose() {
		return false;
	}

	@Override
	public float getWeaponPosX() {
		return 0.16f;
	}

	@Override
	public float getWeaponPosY() {
		return 0.72f;
	}

	@Override
	public float getWeaponPosZ() {
		return 0.1f;
	}

	@Override
	public float getGunScale() {
		return 1.5f;
	}

	@Override
	public float getBulletOffsetSide() {
		return 0.3f;
	}

	@Override
	public float getBulletOffsetHeight() {
		return -0.59f;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return TGSounds.CYBERDEMON_IDLE.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource src) {
		return TGSounds.CYBERDEMON_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TGSounds.CYBERDEMON_DEATH.get();
	}

	@Override
	protected SoundEvent getStepSound() {
		return TGSounds.CYBERDEMON_STEP.get();
	}
}
