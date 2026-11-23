package techguns.items.armors;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.fml.loading.FMLEnvironment;
import techguns.Techguns;
import techguns.api.damagesystem.DamageType;
import techguns.api.damagesystem.ITGArmor;
import techguns.util.TextUtil;

/**
 * Techguns armor piece. Damage reduction uses the Techguns formula (see DamageSystem / ITGArmor); the vanilla
 * ARMOR attribute only carries the rounded physical value for the HUD. Bonuses (speed, jump, ...) are applied by
 * techguns.events.ArmorEvents through {@link ArmorBonusUtil}. Armor never breaks completely, at 1 durability left
 * it stops working.
 */
public class GenericArmor extends ArmorItem implements ITGArmor {

	protected static final DecimalFormat FORMAT_ARMOR = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	protected final TGArmorMaterial material;
	protected final EquipmentSlot slot;
	/** texture names in textures/models/armor/, one per camo */
	protected final String[] textureNames;

	/** custom 3d armor model key, null = vanilla biped armor model */
	@Nullable
	protected ResourceLocation armorModel = null;
	/** texture split in _layer_1/_layer_2 (vanilla style) or one texture for the custom model */
	protected boolean doubleTex = true;

	protected float SpeedBonus = 0.0f;
	protected float JumpBonus = 0.0f;
	protected float FallDMG = 0.0f;
	protected float FallFreeHeight = 0.0f;
	protected float MiningSpeedBonus = 0.0f;
	protected float WaterMiningBonus = 0.0f;
	protected float GunAccuracy = 0.0f;
	protected float extraHearts = 0.0f;
	protected float nightvision = 0.0f;
	protected float knockbackresistance = 0.0f;
	protected float stepassist = 0.0f;
	protected float oxygen_gear = 0.0f;
	protected float water_electrolyzer = 0.0f;
	protected float coolingsystem = 0.0f;
	protected float waterspeedbonus = 0.0f;
	protected float radresistance = 0.0f;

	protected boolean hideFaceslot = false;
	protected boolean hideBackslot = false;
	protected boolean hideGloveslot = false;

	protected Supplier<? extends Item> repairMatMetal = null;
	protected Supplier<? extends Item> repairMatCloth = null;
	protected float repairMatRatioMetal = 0.5f;
	protected int repairMatCount = 0;

	protected boolean use3dRenderHack = false;

	public GenericArmor(Holder<ArmorMaterial> vanillaMaterial, TGArmorMaterial material, String textureName, EquipmentSlot slot) {
		this(vanillaMaterial, material, new String[] { textureName }, slot);
	}

	protected GenericArmor(Holder<ArmorMaterial> vanillaMaterial, TGArmorMaterial material, String[] textureNames, EquipmentSlot slot) {
		super(vanillaMaterial, typeFor(slot), new Item.Properties().durability(material.getDurability(slot)));
		this.material = material;
		this.slot = slot;
		this.textureNames = textureNames;
	}

	public static ArmorItem.Type typeFor(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> ArmorItem.Type.HELMET;
			case CHEST -> ArmorItem.Type.CHESTPLATE;
			case LEGS -> ArmorItem.Type.LEGGINGS;
			default -> ArmorItem.Type.BOOTS;
		};
	}

	public static boolean isTechgunArmor(ItemStack i) {
		return i != null && i.getItem() instanceof GenericArmor;
	}

	/** At 1 durability left the armor no longer works (1.12: damage &gt;= maxDamage-1). */
	public static boolean isBroken(ItemStack stack) {
		return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
	}

	// ------------------------------------------------------------------ builder

	public GenericArmor setArmorModel(ResourceLocation key, boolean doubleTex) {
		this.armorModel = key;
		this.doubleTex = doubleTex;
		return this;
	}

	/** backupTex was the 1.12 inventory model override, inventory icons are plain item models now */
	public GenericArmor setArmorModel(ResourceLocation key, boolean doubleTex, ResourceLocation backupTex) {
		return setArmorModel(key, doubleTex);
	}

	public GenericArmor setSpeedBoni(float speed, float jump) {
		this.SpeedBonus = speed;
		this.JumpBonus = jump;
		return this;
	}

	public GenericArmor setFallProtection(float multiplier, float freeheight) {
		this.FallDMG = multiplier;
		this.FallFreeHeight = freeheight;
		return this;
	}

	public GenericArmor setMiningBoni(float bonus) {
		this.MiningSpeedBonus = bonus;
		return this;
	}

	public GenericArmor setMiningBoniWater(float bonus) {
		this.WaterMiningBonus = bonus;
		return this;
	}

	public GenericArmor setGunBonus(float acc) {
		this.GunAccuracy = acc;
		return this;
	}

	public GenericArmor setHealthBonus(int bonusHearts) {
		this.extraHearts = bonusHearts;
		return this;
	}

	public GenericArmor setKnockbackResistance(float resistpercent) {
		this.knockbackresistance = resistpercent;
		return this;
	}

	public GenericArmor setRADResistance(float radresistance) {
		this.radresistance = radresistance;
		return this;
	}

	public GenericArmor setStepAssist(float stepassist) {
		this.stepassist = stepassist;
		return this;
	}

	public GenericArmor setOxygenGear(float value) {
		this.oxygen_gear = value;
		return this;
	}

	public GenericArmor setCoolingSystem(float value) {
		this.coolingsystem = value;
		return this;
	}

	public GenericArmor setWaterspeedBonus(float value) {
		this.waterspeedbonus = value;
		return this;
	}

	public GenericArmor setUseRenderHack() {
		this.use3dRenderHack = true;
		return this;
	}

	public GenericArmor setHideFaceslot(boolean hideFaceslot) {
		this.hideFaceslot = hideFaceslot;
		return this;
	}

	public GenericArmor setHideBackslot(boolean hideBackslot) {
		this.hideBackslot = hideBackslot;
		return this;
	}

	public GenericArmor setHideGloveslot(boolean hideGloveslot) {
		this.hideGloveslot = hideGloveslot;
		return this;
	}

	/** metal/cloth may be null; Supplier so DeferredItems can be passed */
	public GenericArmor setRepairMats(@Nullable Supplier<? extends Item> metal, @Nullable Supplier<? extends Item> cloth, float metalpercent,
			int totalmats) {
		this.repairMatMetal = metal;
		this.repairMatCloth = cloth;
		this.repairMatCount = totalmats;
		this.repairMatRatioMetal = metalpercent;
		return this;
	}

	// ------------------------------------------------------------------ stats

	public float getBonus(TGArmorBonus type) {
		return switch (type) {
			case SPEED -> this.SpeedBonus;
			case JUMP -> this.JumpBonus;
			case FALLDMG -> this.FallDMG;
			case FREEHEIGHT -> this.FallFreeHeight;
			case BREAKSPEED -> this.MiningSpeedBonus;
			case BREAKSPEED_WATER -> this.WaterMiningBonus;
			case GUN_ACCURACY -> this.GunAccuracy;
			case EXTRA_HEART -> this.extraHearts;
			case NIGHTVISION -> this.nightvision;
			case KNOCKBACK_RESISTANCE -> this.knockbackresistance;
			case STEPASSIST -> this.stepassist;
			case OXYGEN_GEAR -> this.oxygen_gear;
			case WATER_ELECTROLYZER -> this.water_electrolyzer;
			case COOLING_SYSTEM -> this.coolingsystem;
			case SPEED_WATER -> this.waterspeedbonus;
			case RAD_RESISTANCE -> this.radresistance;
			default -> 0.0f;
		};
	}

	/** Bonus of this worn piece; powered armor overrides this to handle power. */
	public float getBonus(ItemStack stack, TGArmorBonus type, boolean consumePower, net.minecraft.world.entity.player.Player ply) {
		return isBroken(stack) ? 0f : getBonus(type);
	}

	@Override
	public float getArmorValue(ItemStack stack, DamageType type) {
		if (isBroken(stack)) {
			return 0f;
		}
		return this.material.getArmorValueSlot(this.slot, type);
	}

	public TGArmorMaterial getTGMaterial() {
		return material;
	}

	public EquipmentSlot getSlot() {
		return slot;
	}

	@Nullable
	public ResourceLocation getArmorModelKey() {
		return armorModel;
	}

	public boolean isHideFaceslot() {
		return hideFaceslot;
	}

	public boolean isHideBackslot() {
		return hideBackslot;
	}

	public boolean isHideGloveslot() {
		return hideGloveslot;
	}

	public boolean shouldUseRenderHack(ItemStack stack) {
		return this.use3dRenderHack;
	}

	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
		if (isBroken(stack)) {
			return ItemAttributeModifiers.EMPTY;
		}
		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(this.slot);
		ResourceLocation id = ResourceLocation.withDefaultNamespace("armor." + this.type.getName());
		ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
		int av = Math.round(this.material.getArmorValueSlot(this.slot, DamageType.PHYSICAL));
		if (av > 0) {
			b.add(Attributes.ARMOR, new AttributeModifier(id, av, AttributeModifier.Operation.ADD_VALUE), group);
		}
		if (this.material.getToughness() > 0) {
			b.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, this.material.getToughness(), AttributeModifier.Operation.ADD_VALUE), group);
		}
		if (this.radresistance > 0) {
			b.add(techguns.radiation.TGRadiationSystem.RADIATION_RESISTANCE, new AttributeModifier(id, this.radresistance, AttributeModifier.Operation.ADD_VALUE),
					group);
		}
		// passive bonuses as vanilla / NeoForge attributes; powered armor gives its unpowered values without power
		// (a change of power changes the stack, so the game re-applies these)
		addBonus(b, stack, id, group, TGArmorBonus.EXTRA_HEART, Attributes.MAX_HEALTH, 1.0, AttributeModifier.Operation.ADD_VALUE);
		addBonus(b, stack, id, group, TGArmorBonus.KNOCKBACK_RESISTANCE, Attributes.KNOCKBACK_RESISTANCE, 1.0, AttributeModifier.Operation.ADD_VALUE);
		addBonus(b, stack, id, group, TGArmorBonus.JUMP, Attributes.JUMP_STRENGTH, 1.0, AttributeModifier.Operation.ADD_VALUE);
		addBonus(b, stack, id, group, TGArmorBonus.FREEHEIGHT, Attributes.SAFE_FALL_DISTANCE, 1.0, AttributeModifier.Operation.ADD_VALUE);
		// ADD_MULTIPLIED_BASE adds up like the 1.12 bonuses did: base 1 * (1 + sum of all pieces)
		addBonus(b, stack, id, group, TGArmorBonus.FALLDMG, Attributes.FALL_DAMAGE_MULTIPLIER, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		addBonus(b, stack, id, group, TGArmorBonus.BREAKSPEED, Attributes.BLOCK_BREAK_SPEED, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		addBonus(b, stack, id, group, TGArmorBonus.BREAKSPEED_WATER, Attributes.SUBMERGED_MINING_SPEED, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		if (getBonus(stack, TGArmorBonus.CREATIVE_FLIGHT, false, null) > 0) {
			b.add(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT, new AttributeModifier(id, 1.0, AttributeModifier.Operation.ADD_VALUE), group);
		}
		return b.build();
	}

	private void addBonus(ItemAttributeModifiers.Builder b, ItemStack stack, ResourceLocation id, EquipmentSlotGroup group, TGArmorBonus type,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double sign, AttributeModifier.Operation op) {
		float v = getBonus(stack, type, false, null);
		if (v != 0.0f) {
			b.add(attr, new AttributeModifier(id, sign * v, op), group);
		}
	}

	// ------------------------------------------------------------------ durability

	/** 1.12 damageArmor: never break the piece, stop at 1 durability left. */
	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
		int maxDmg = stack.getMaxDamage() - 1 - stack.getDamageValue();
		return Math.max(0, Math.min(amount, maxDmg));
	}

	public boolean canRepairOnRepairBench(ItemStack item) {
		return this.repairMatCount > 0;
	}

	public List<ItemStack> getRepairMats(ItemStack item) {
		List<ItemStack> mats = new ArrayList<>();
		if (item.getDamageValue() > 0) {
			float dmgpercent = item.getDamageValue() / (float) (item.getMaxDamage() - 1);
			int count = (int) Math.ceil(this.repairMatCount * dmgpercent);
			int metalcount = (int) Math.ceil(count * this.repairMatRatioMetal);
			int clothcount = count - metalcount;
			if (this.repairMatMetal != null && metalcount > 0) {
				mats.add(new ItemStack(this.repairMatMetal.get(), metalcount));
			}
			if (this.repairMatCloth != null && clothcount > 0) {
				mats.add(new ItemStack(this.repairMatCloth.get(), clothcount));
			}
		}
		return mats;
	}

	@Override
	public boolean isValidRepairItem(ItemStack item, ItemStack mat) {
		Supplier<? extends Item> rep = this.repairMatMetal != null ? this.repairMatMetal : this.repairMatCloth;
		return rep != null && mat.is(rep.get());
	}

	// ------------------------------------------------------------------ textures

	public int getCamoCount() {
		return textureNames.length;
	}

	public int getCurrentCamoIndex(ItemStack item) {
		return item.getOrDefault(techguns.TGDataComponents.CAMO.get(), 0);
	}

	protected boolean hasDoubleTexture() {
		return this.doubleTex;
	}

	@Override
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		int i = getCurrentCamoIndex(stack);
		if (i < 0 || i >= this.textureNames.length) {
			i = 0;
		}
		String suffix = this.hasDoubleTexture() ? (slot == EquipmentSlot.LEGS ? "_layer_2" : "_layer_1") : "";
		return Techguns.rl("textures/models/armor/" + this.textureNames[i] + suffix + ".png");
	}

	// ------------------------------------------------------------------ tooltip

	public static String formatAV(float armorValue) {
		return FORMAT_ARMOR.format(armorValue);
	}

	protected void addArmorvaluesInformation(ItemStack item, List<Component> list) {
		Object[][] rows = { { ChatFormatting.DARK_GRAY, "AR", DamageType.PHYSICAL }, { ChatFormatting.GRAY, "PR", DamageType.PROJECTILE },
				{ ChatFormatting.DARK_RED, "EX", DamageType.EXPLOSION }, { ChatFormatting.DARK_AQUA, "E", DamageType.ENERGY },
				{ ChatFormatting.RED, "F", DamageType.FIRE }, { ChatFormatting.AQUA, "I", DamageType.ICE }, { ChatFormatting.YELLOW, "L", DamageType.LIGHTNING },
				{ ChatFormatting.DARK_GREEN, "P", DamageType.POISON }, { ChatFormatting.DARK_GRAY, "D", DamageType.DARK },
				{ ChatFormatting.GREEN, "RAD", DamageType.RADIATION } };
		for (Object[] r : rows) {
			list.add(Component.literal(" " + r[1] + ": " + formatAV(this.material.getArmorValueSlot(this.slot, (DamageType) r[2])))
					.withStyle((ChatFormatting) r[0]));
		}
	}

	protected static boolean isShiftDown() {
		return FMLEnvironment.dist.isClient() && net.minecraft.client.gui.screens.Screen.hasShiftDown();
	}

	private static String pct(float f) {
		return formatAV(f * 100.0f) + "%";
	}

	@Override
	public void appendHoverText(ItemStack item, TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(item, context, list, flag);
		if (isShiftDown()) {
			list.add(TextUtil.transTG("armorTooltip.durability").append(": " + (item.getMaxDamage() - item.getDamageValue()) + "/" + item.getMaxDamage()));
			if (this.material.getToughness() > 0) {
				list.add(TextUtil.transTG("armorTooltip.toughness").append(": " + this.material.getToughness()).withStyle(ChatFormatting.GRAY));
			}
			list.add(TextUtil.transTG("armorTooltip.resistances").append(":").withStyle(ChatFormatting.BLUE));
			this.addArmorvaluesInformation(item, list);
			// health, jump, fall, mining and knockback bonuses are attributes now: the vanilla tooltip lists them
			if (getBonus(TGArmorBonus.SPEED) > 0.0f) {
				list.add(TextUtil.transTG("armorTooltip.movespeed").append(": +" + pct(getBonus(TGArmorBonus.SPEED))));
			}
			if (getBonus(TGArmorBonus.NIGHTVISION) > 0.0f) {
				list.add(TextUtil.transTG("armorTooltip.nightvision"));
			}
			if (getBonus(TGArmorBonus.STEPASSIST) > 0.0f) {
				list.add(TextUtil.transTG("armorTooltip.stepassist"));
			}
			if (getBonus(TGArmorBonus.OXYGEN_GEAR) > 0.0f) {
				list.add(TextUtil.transTG("armorTooltip.oxygengear"));
			}
			if (getBonus(TGArmorBonus.COOLING_SYSTEM) > 0.0f) {
				list.add(TextUtil.transTG("armorTooltip.coolingsystem"));
			}
			this.addCamoInformation(item, list);
		} else {
			list.add(TextUtil.transTG("armorTooltip.durability").append(": " + (item.getMaxDamage() - item.getDamageValue()) + "/" + item.getMaxDamage()));
			list.add(TextUtil.transTG("gun.tooltip.shift1").append(" ").append(TextUtil.transTG("gun.tooltip.shift2").withStyle(ChatFormatting.GREEN))
					.append(" ").append(TextUtil.transTG("gun.tooltip.shift3").withStyle(ChatFormatting.GRAY)));
		}
	}

	protected void addCamoInformation(ItemStack item, List<Component> list) {
	}

	/** lang key for a camo name, the 1.12 key form "techguns.item.&lt;texture0&gt;.&lt;suffix&gt;camoname.N" */
	protected static Component camoName(String texture0, String suffix, int camo) {
		String key = Techguns.MODID + ".item." + texture0 + "." + suffix + "camoname." + camo;
		if (!Language.getInstance().has(key)) {
			String alt = "item." + Techguns.MODID + "." + texture0 + "." + suffix + "camoname." + camo;
			if (Language.getInstance().has(alt)) {
				key = alt;
			}
		}
		return Component.translatable(key);
	}
}
