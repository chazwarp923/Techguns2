package techguns.items.armors;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import techguns.Techguns;
import techguns.api.damagesystem.DamageType;

/**
 * Techguns armor material: one armor value per Techguns damage type plus per-slot split factors.
 * The vanilla {@link ArmorMaterial} built from it ({@link #createVanillaMaterial()}) only carries
 * toughness, enchantability, equip sound and the texture; damage reduction is done by the Techguns formula.
 */
public class TGArmorMaterial {

	public static final List<TGArmorMaterial> MATERIALS = new ArrayList<>();

	/** The sum of default (1.12) durability weight values */
	private static final int VANILLA_DURABILITY_FACTOR_SUM = 55;

	public final String name;

	protected float armorPhys = 0;
	protected float armorProjectile = 0;
	protected float armorExplosion = 0;
	protected float armorEnergy = 0;
	protected float armorFire = 0;
	protected float armorLightning = 0;
	protected float armorIce = 0;
	protected float armorDark = 0;
	protected float armorPoison = 0;
	protected float armorRadiation = 0;

	protected float factorHead = 0.25f;
	protected float factorBoots = 0.20f;
	protected float factorLegs = 0.25f;
	protected float factorChest = 0.30f;

	protected float durHead = 0.25f;
	protected float durBoots = 0.25f;
	protected float durLegs = 0.25f;
	protected float durChest = 0.25f;

	protected float toughness;
	protected Holder<SoundEvent> equipSound;
	protected int baseDur;
	protected int enchantability;
	protected String texture;

	/**
	 * Set the armor values with standard factors, phys and proj are equal to armor, rad is 0, all others are 0.75*armor;
	 */
	public TGArmorMaterial(String name, int baseDurability, int enchantability, float armor, Holder<SoundEvent> equipSound, float toughness) {
		this.name = name;
		this.baseDur = baseDurability;
		this.enchantability = enchantability;
		float f = 0.75f;
		this.armorPhys = armor;
		this.armorProjectile = armor;
		this.armorFire = armor * f;
		this.armorExplosion = armor * f;
		this.armorEnergy = armor * f;
		this.armorIce = armor * f;
		this.armorLightning = armor * f;
		this.armorDark = armor * f;
		this.armorPoison = armor * f;
		this.armorRadiation = 0.0f;
		this.toughness = toughness;
		this.equipSound = equipSound;
		this.texture = name.toLowerCase();
		MATERIALS.add(this);
	}

	/** Set all 'Elemental' values: fire, lightning, ice and energy */
	public TGArmorMaterial setArmorElemental(float value) {
		this.armorEnergy = value;
		this.armorFire = value;
		this.armorIce = value;
		this.armorLightning = value;
		return this;
	}

	public TGArmorMaterial setPartArmorSplit(float helm, float chest, float legs, float boots) {
		this.factorHead = helm;
		this.factorChest = chest;
		this.factorLegs = legs;
		this.factorBoots = boots;
		return this;
	}

	public TGArmorMaterial setPartDurabilitySplit(float helm, float chest, float legs, float boots) {
		this.durHead = helm;
		this.durChest = chest;
		this.durLegs = legs;
		this.durBoots = boots;
		return this;
	}

	/** 1.12 leftover, replaced by toughness; kept so the material definitions read like the original. */
	@Deprecated
	public TGArmorMaterial setPenetrationResistance(float penresist) {
		return this;
	}

	public float getArmorValueTotal(DamageType damageType) {
		return switch (damageType) {
			case ENERGY -> this.armorEnergy;
			case EXPLOSION -> this.armorExplosion;
			case FIRE -> this.armorFire;
			case ICE -> this.armorIce;
			case LIGHTNING -> this.armorLightning;
			case PHYSICAL -> this.armorPhys;
			case POISON -> this.armorPoison;
			case PROJECTILE -> this.armorProjectile;
			case RADIATION -> this.armorRadiation;
			case DARK -> this.armorDark;
			default -> 0;
		};
	}

	public float getArmorValueSlot(EquipmentSlot slot, DamageType type) {
		float materialArmor = this.getArmorValueTotal(type);
		return switch (slot) {
			case HEAD -> this.factorHead * materialArmor;
			case CHEST -> this.factorChest * materialArmor;
			case LEGS -> this.factorLegs * materialArmor;
			case FEET -> this.factorBoots * materialArmor;
			default -> 0;
		};
	}

	public int getDurability(EquipmentSlot slot) {
		float f = switch (slot) {
			case HEAD -> this.durHead;
			case CHEST -> this.durChest;
			case LEGS -> this.durLegs;
			case FEET -> this.durBoots;
			default -> 0;
		};
		return Math.round(f * VANILLA_DURABILITY_FACTOR_SUM * this.baseDur);
	}

	public float getToughness() {
		return toughness;
	}

	/** Vanilla material: no defense values (Techguns formula handles damage), texture techguns:&lt;name&gt; */
	public ArmorMaterial createVanillaMaterial() {
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		for (ArmorItem.Type t : ArmorItem.Type.values()) {
			defense.put(t, 0);
		}
		return new ArmorMaterial(defense, enchantability, equipSound, () -> Ingredient.EMPTY,
				List.of(new ArmorMaterial.Layer(Techguns.rl(this.texture))), toughness, 0f);
	}

	public TGArmorMaterial setArmorProjectile(float v) {
		this.armorProjectile = v;
		return this;
	}

	public TGArmorMaterial setArmorExplosion(float v) {
		this.armorExplosion = v;
		return this;
	}

	public TGArmorMaterial setArmorEnergy(float v) {
		this.armorEnergy = v;
		return this;
	}

	public TGArmorMaterial setArmorFire(float v) {
		this.armorFire = v;
		return this;
	}

	public TGArmorMaterial setArmorLightning(float v) {
		this.armorLightning = v;
		return this;
	}

	public TGArmorMaterial setArmorIce(float v) {
		this.armorIce = v;
		return this;
	}

	public TGArmorMaterial setArmorPoison(float v) {
		this.armorPoison = v;
		return this;
	}

	public TGArmorMaterial setArmorRadiation(float v) {
		this.armorRadiation = v;
		return this;
	}

	public TGArmorMaterial setArmorPhys(float v) {
		this.armorPhys = v;
		return this;
	}
}
