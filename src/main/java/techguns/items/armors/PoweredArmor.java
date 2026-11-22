package techguns.items.armors;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import techguns.TGDataComponents;
import techguns.TGSounds;
import techguns.util.InventoryUtil;
import techguns.util.TextUtil;

/**
 * Armor that needs power for its bonuses. The chestplate holds the "battery" (compressed air tank / energy cell)
 * and recharges the other pieces of the same power type; empty chest = reload from the player's inventory.
 */
public class PoweredArmor extends GenericArmorMultiCamo {

	protected final ArmorPowerType powerType;
	protected Supplier<? extends Item> battery = null;
	protected Supplier<? extends Item> battery_empty = null;
	public final int maxpower;

	protected float SpeedBonusUnpowered = 0.0f;
	protected float JumpBonusUnpowered = 0.0f;
	protected float FallDMGUnpowered = 0.0f;
	protected float FallFreeHeightUnpowered = 0.0f;
	protected float MiningSpeedBonusUnpowered = 0.0f;
	protected float WaterMiningBonusUnpowered = 0.0f;
	protected float GunAccuracyUnpowered = 0.0f;
	protected float extraHeartsUnpowered = 0.0f;
	protected float nightvisionUnpowered = 0.0f;
	protected float knockbackresistanceUnpowered = 0.0f;
	protected float stepassistUnpowerd = 0.0f;
	protected float oxygen_gearUnpowered = 0.0f;
	protected float water_electrolyzerUnpowered = 0.0f;
	protected float coolingsystemUnpowered = 0.0f;
	protected float waterspeedbonusUnpowered = 0.0f;

	public PoweredArmor(Holder<ArmorMaterial> vanillaMaterial, TGArmorMaterial material, String[] textureNames, EquipmentSlot slot,
			ArmorPowerType powerType, int maxpower) {
		super(vanillaMaterial, material, textureNames, slot);
		this.powerType = powerType;
		this.maxpower = maxpower;
	}

	public PoweredArmor setBattery(Supplier<? extends Item> battery) {
		this.battery = battery;
		return this;
	}

	public PoweredArmor setEmptyBattery(Supplier<? extends Item> emptyBattery) {
		this.battery_empty = emptyBattery;
		return this;
	}

	public float getBonusUnpowered(TGArmorBonus type) {
		return switch (type) {
			case SPEED -> this.SpeedBonusUnpowered;
			case JUMP -> this.JumpBonusUnpowered;
			case FALLDMG -> this.FallDMGUnpowered;
			case FREEHEIGHT -> this.FallFreeHeightUnpowered;
			case BREAKSPEED -> this.MiningSpeedBonusUnpowered;
			case BREAKSPEED_WATER -> this.WaterMiningBonusUnpowered;
			case GUN_ACCURACY -> this.GunAccuracyUnpowered;
			case EXTRA_HEART -> this.extraHeartsUnpowered;
			case NIGHTVISION -> this.nightvisionUnpowered;
			case KNOCKBACK_RESISTANCE -> this.knockbackresistanceUnpowered;
			case STEPASSIST -> this.stepassistUnpowerd;
			case OXYGEN_GEAR -> this.oxygen_gearUnpowered;
			case WATER_ELECTROLYZER -> this.water_electrolyzerUnpowered;
			case COOLING_SYSTEM -> this.coolingsystemUnpowered;
			case SPEED_WATER -> this.waterspeedbonusUnpowered;
			// rad resistance works without power
			case RAD_RESISTANCE -> this.radresistance;
			default -> 0.0f;
		};
	}

	@Override
	public float getBonus(ItemStack stack, TGArmorBonus type, boolean consumePower, Player ply) {
		if (isBroken(stack)) {
			return 0f;
		}
		if (hasPower(stack)) {
			float v = getBonus(type);
			if (consumePower && v > 0.0f) {
				consumePower(stack, this.applyPowerConsumptionOnAction(type, ply));
			}
			return v;
		}
		return getBonusUnpowered(type);
	}

	public PoweredArmor setSpeedBoni(float speed, float jump, float speed_unpowered, float jump_unpowered) {
		this.SpeedBonus = speed;
		this.JumpBonus = jump;
		this.SpeedBonusUnpowered = speed_unpowered;
		this.JumpBonusUnpowered = jump_unpowered;
		return this;
	}

	public PoweredArmor setFallProtection(float multiplier, float freeheight, float multiplier_unpowered, float freeheight_unpowered) {
		this.FallDMG = multiplier;
		this.FallFreeHeight = freeheight;
		this.FallDMGUnpowered = multiplier_unpowered;
		this.FallFreeHeightUnpowered = freeheight_unpowered;
		return this;
	}

	public PoweredArmor setMiningBoni(float bonus, float bonus_unpowered) {
		this.MiningSpeedBonus = bonus;
		this.MiningSpeedBonusUnpowered = bonus_unpowered;
		return this;
	}

	public PoweredArmor setMiningBoniWater(float bonus, float bonus_unpowered) {
		this.WaterMiningBonus = bonus;
		this.WaterMiningBonusUnpowered = bonus_unpowered;
		return this;
	}

	public PoweredArmor setGunBonus(float acc, float acc_unpowered) {
		this.GunAccuracy = acc;
		this.GunAccuracyUnpowered = acc_unpowered;
		return this;
	}

	public PoweredArmor setHealthBonus(int bonusHearts, int bonusHeartsUnpowered) {
		this.extraHearts = bonusHearts;
		this.extraHeartsUnpowered = bonusHeartsUnpowered;
		return this;
	}

	public PoweredArmor setKnockbackResistance(float resistpercent, float resistpercent_unpowered) {
		this.knockbackresistance = resistpercent;
		this.knockbackresistanceUnpowered = resistpercent_unpowered;
		return this;
	}

	public PoweredArmor setStepAssist(float stepassist, float stepassist_unpowered) {
		this.stepassist = stepassist;
		this.stepassistUnpowerd = stepassist_unpowered;
		return this;
	}

	public PoweredArmor setOxygenGear(float value, float unpowered_value) {
		this.oxygen_gear = value;
		this.oxygen_gearUnpowered = unpowered_value;
		return this;
	}

	public PoweredArmor setCoolingSystem(float value, float unpowered_value) {
		this.coolingsystem = value;
		this.coolingsystemUnpowered = unpowered_value;
		return this;
	}

	public PoweredArmor setWaterspeedBonus(float value, float unpowered_value) {
		this.waterspeedbonus = value;
		this.waterspeedbonusUnpowered = unpowered_value;
		return this;
	}

	// ------------------------------------------------------------------ power

	public static int getPower(ItemStack armor) {
		return armor.getOrDefault(TGDataComponents.ARMOR_POWER.get(), 0);
	}

	protected static boolean hasPower(ItemStack armor) {
		return getPower(armor) > 0;
	}

	public static void consumePower(ItemStack armor, int amount) {
		if (amount <= 0) {
			return;
		}
		armor.set(TGDataComponents.ARMOR_POWER.get(), Math.max(0, getPower(armor) - amount));
	}

	/** charges up to max power into this piece; @return amount actually charged */
	public int setPowered(ItemStack armor, int max) {
		int power = getPower(armor);
		int missing = maxpower - power;
		if (missing > max) {
			armor.set(TGDataComponents.ARMOR_POWER.get(), power + max);
			return max;
		}
		armor.set(TGDataComponents.ARMOR_POWER.get(), maxpower);
		return missing;
	}

	protected void setFullyPowered(ItemStack armor) {
		armor.set(TGDataComponents.ARMOR_POWER.get(), maxpower);
	}

	private static final EquipmentSlot[] NON_CHEST = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.HEAD };

	protected static void powerSlots(Player player, ArmorPowerType power, ItemStack chest, boolean consumeTick) {
		for (EquipmentSlot s : NON_CHEST) {
			ItemStack armor = player.getItemBySlot(s);
			if (armor.getItem() instanceof PoweredArmor pa) {
				if (pa.powerType == power) {
					int charged = pa.setPowered(armor, getPower(chest));
					consumePower(chest, charged);
				}
				if (consumeTick) {
					consumePower(armor, 1);
				}
			}
		}
		if (consumeTick && chest.getItem() instanceof PoweredArmor) {
			consumePower(chest, 1);
		}
	}

	public int applyPowerConsumptionOnAction(TGArmorBonus type, Player player) {
		if (type == TGArmorBonus.JUMP) {
			if (this.slot == EquipmentSlot.LEGS) {
				if (!player.level().isClientSide) {
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TGSounds.STEAM_JUMP.get(), SoundSource.PLAYERS, 1f, 1f);
				}
				return 5;
			}
		} else if (type == TGArmorBonus.FALLDMG) {
			if (this.slot == EquipmentSlot.FEET) {
				return 5;
			}
		} else if (type == TGArmorBonus.COOLING_SYSTEM) {
			if (this.slot == EquipmentSlot.CHEST) {
				return 1;
			}
		}
		return 0;
	}

	/** Called from the tick handler (server): power drain while moving and chest battery reload. */
	public static void calculateConsumptionTick(Player player) {
		float f = 0.001f;
		var m = player.getDeltaMovement();
		boolean moving = m.x > f || m.z > f || m.x < -f || m.z < -f;

		ArmorPowerType power = null;
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		PoweredArmor powerChest = chest.getItem() instanceof PoweredArmor pa ? pa : null;
		if (powerChest != null) {
			power = powerChest.powerType;
		}
		int modTick = (int) (player.level().getGameTime() % 20);
		powerSlots(player, power, chest, modTick == 0 && moving);

		if (powerChest != null && !hasPower(chest) && powerChest.battery != null) {
			// chest is empty, try reload
			if (InventoryUtil.consumeAmmoPlayer(player, new ItemStack(powerChest.battery.get()))) {
				if (powerChest.battery_empty != null) {
					int amount = InventoryUtil.addAmmoToPlayerInventory(player, new ItemStack(powerChest.battery_empty.get()));
					if (amount > 0 && !player.level().isClientSide) {
						player.level().addFreshEntity(
								new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(powerChest.battery_empty.get(), amount)));
					}
				}
				powerChest.setFullyPowered(chest);
				powerSlots(player, power, chest, false);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack item, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(TextUtil.transTG("tooltip.power").append(": " + getPower(item) + "/" + maxpower).withStyle(ChatFormatting.AQUA));
		Component type = TextUtil.transTG("tooltip.powertype." + (powerType == ArmorPowerType.STEAM ? "Steam" : "RF"));
		if (this.slot == EquipmentSlot.CHEST && this.battery != null) {
			list.add(TextUtil.transTG("tooltip.powerType").append(": ").append(type).append(" (").append(this.battery.get().getDescription()).append(")")
					.withStyle(ChatFormatting.AQUA));
		} else {
			list.add(TextUtil.transTG("tooltip.powerType").append(": ").append(type).withStyle(ChatFormatting.AQUA));
		}
		super.appendHoverText(item, context, list, flag);
	}
}
