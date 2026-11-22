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
import techguns.damagesystem.TGDamageSource;

/** 1.12 SuperMutantBasic; Heavy and Elite are bigger, tougher variants. All use the supermutantbasic loot table. */
public class SuperMutantBasic extends GenericNPC {

	public SuperMutantBasic(EntityType<? extends SuperMutantBasic> type, Level level) {
		super(type, level);
		setTGArmorStats(5.0f, 0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GenericNPC.createAttributes(0.30D, 35D, 7D, 50D).add(Attributes.ARMOR_TOUGHNESS, 1D);
	}

	/** Heavy and Elite share the basic loot table */
	@Override
	protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getDefaultLootTable() {
		return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, techguns.Techguns.rl("entities/supermutantbasic"));
	}

	/** texture index */
	public int getMutantType() {
		return 0;
	}

	/** model scale, the model is scaled around its origin 1.5 blocks above the feet and moved up by getModelHeightOffset */
	public float getModelScale() {
		return 1.35f;
	}

	public double getModelHeightOffset() {
		return 0.55d;
	}

	/** armor against: energy/elemental/explosion, physical/projectile, poison/radiation */
	protected float[] getArmorValues() {
		return new float[] { 10f, 7f, 15f };
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		float[] a = getArmorValues();
		return switch (dmgsrc.damageType) {
			case EXPLOSION, LIGHTNING, ENERGY, FIRE, ICE -> a[0];
			case PHYSICAL, PROJECTILE -> a[1];
			case POISON, RADIATION -> a[2];
			default -> 0f;
		};
	}

	@Override
	public float getWeaponPosX() {
		return 0.13f;
	}

	@Override
	public float getWeaponPosY() {
		return 0f;
	}

	@Override
	public float getWeaponPosZ() {
		return -0.18f;
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		int r = this.random.nextInt(5);
		equip(EquipmentSlot.MAINHAND, switch (r) {
			case 0 -> TGuns.ROCKETLAUNCHER.get();
			case 1 -> TGuns.AK47.get();
			case 2 -> TGuns.COMBATSHOTGUN.get();
			default -> TGuns.LASERGUN.get();
		});
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

	public static class Heavy extends SuperMutantBasic {
		public Heavy(EntityType<? extends Heavy> type, Level level) {
			super(type, level);
			setTGArmorStats(10.0f, 0f);
		}

		public static AttributeSupplier.Builder createAttributes() {
			return GenericNPC.createAttributes(0.25D, 45D, 9D, 50D).add(Attributes.ARMOR_TOUGHNESS, 2D);
		}

		@Override
		public int getMutantType() {
			return 1;
		}

		@Override
		public float getModelScale() {
			return 1.5f;
		}

		@Override
		public double getModelHeightOffset() {
			return 0.75d;
		}

		@Override
		protected float[] getArmorValues() {
			return new float[] { 16f, 12f, 18f };
		}

		@Override
		public float getWeaponPosX() {
			return 0.17f;
		}

		@Override
		public float getWeaponPosZ() {
			return -0.3f;
		}
	}

	public static class Elite extends SuperMutantBasic {
		public Elite(EntityType<? extends Elite> type, Level level) {
			super(type, level);
			setTGArmorStats(15.0f, 1f);
		}

		public static AttributeSupplier.Builder createAttributes() {
			return GenericNPC.createAttributes(0.20D, 60D, 11D, 50D).add(Attributes.ARMOR_TOUGHNESS, 2.5D);
		}

		@Override
		public int getMutantType() {
			return 2;
		}

		@Override
		public float getModelScale() {
			return 1.65f;
		}

		@Override
		public double getModelHeightOffset() {
			return 0.95d;
		}

		@Override
		protected float[] getArmorValues() {
			return new float[] { 14f, 10f, 16f };
		}

		@Override
		public float getWeaponPosX() {
			return 0.23f;
		}

		@Override
		public float getWeaponPosZ() {
			return -0.39f;
		}
	}
}
