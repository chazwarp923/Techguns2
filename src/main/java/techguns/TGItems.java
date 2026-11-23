package techguns;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.api.tginventory.TGSlotType;
import techguns.items.GenericItemShared;
import techguns.items.guns.ammo.AmmoType;
import techguns.items.guns.ammo.AmmoTypes;

/**
 * Item registry. The 1.12.2 "itemshared" metadata item has been flattened: every former
 * variant is now its own item, registered under the old variant name (e.g. techguns:pistolrounds).
 */
public class TGItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Techguns.MODID);

	/** All items that should show up in the creative tab, in registration order. */
	public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ITEMS = new ArrayList<>();

	/** Former shared (metadata) items. */
	public static final DeferredItem<GenericItemShared> STONE_BULLETS = shared("stonebullets", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> PISTOL_ROUNDS = shared("pistolrounds", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> SHOTGUN_ROUNDS = shared("shotgunrounds", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> RIFLE_ROUNDS = shared("riflerounds", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> SNIPER_ROUNDS = shared("sniperrounds", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> GRENADE_40MM = shared("40mmgrenade", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> ADVANCED_ROUNDS = shared("advancedrounds", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> ROCKET = shared("rocket", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> RIFLE_ROUNDS_STACK = shared("rifleroundsstack", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> SMG_MAGAZINE = shared("smgmagazine", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> SMG_MAGAZINE_EMPTY = shared("smgmagazineempty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> PISTOL_MAGAZINE = shared("pistolmagazine", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> PISTOL_MAGAZINE_EMPTY = shared("pistolmagazineempty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> ASSAULTRIFLE_MAGAZINE = shared("assaultriflemagazine", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> ASSAULTRIFLE_MAGAZINE_EMPTY = shared("assaultriflemagazineempty", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> LMG_MAGAZINE = shared("lmgmagazine", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> LMG_MAGAZINE_EMPTY = shared("lmgmagazineempty", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> MINIGUN_DRUM = shared("minigundrum", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> MINIGUN_DRUM_EMPTY = shared("minigundrumempty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> AS50_MAGAZINE = shared("as50magazine", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> AS50_MAGAZINE_EMPTY = shared("as50magazineempty", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> ADVANCED_MAGAZINE = shared("advancedmagazine", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> ADVANCED_MAGAZINE_EMPTY = shared("advancedmagazineempty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> COMPRESSED_AIR_TANK = shared("compressedairtank", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> COMPRESSED_AIR_TANK_EMPTY = shared("compressedairtankempty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> BIO_TANK = shared("biotank", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> BIO_TANK_EMPTY = shared("biotankempty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> FUEL_TANK = shared("fueltank", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> FUEL_TANK_EMPTY = shared("fueltankempty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> ENERGY_CELL = shared("energycell", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> ENERGY_CELL_EMPTY = shared("energycellempty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> NUCLEAR_POWERCELL = shared("nuclearpowercell", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> NUCLEAR_POWERCELL_EMPTY = shared("nuclearpowercelldepleted", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> RECEIVER_IRON = shared("ironreceiver", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RECEIVER_STEEL = shared("steelreceiver", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RECEIVER_OBSIDIAN_STEEL = shared("obsidiansteelreceiver", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RECEIVER_CARBON = shared("carbonreceiver", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> BARREL_STONE = shared("stonebarrel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> BARREL_IRON = shared("ironbarrel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> BARREL_OBSIDIAN_STEEL = shared("obsidiansteelbarrel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> BARREL_CARBON = shared("carbonbarrel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> BARREL_LASER = shared("laserbarrel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> STOCK_WOOD = shared("woodstock", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> STOCK_PLASTIC = shared("plasticstock", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> STOCK_CARBON = shared("carbonstock", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> STEAMARMOR_PLATE = shared("steamarmorplate", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_IRON = shared("plateiron", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_COPPER = shared("platecopper", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_TIN = shared("platetin", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_BRONZE = shared("platebronze", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_STEEL = shared("platesteel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_OBSIDIAN_STEEL = shared("plateobsidiansteel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_LEAD = shared("platelead", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_CARBON = shared("platecarbon", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLATE_TITANIUM = shared("platetitanium", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLASTIC_SHEET = shared("plasticsheet", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RUBBER_BAR = shared("rubberbar", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> MECHANICAL_PARTS_IRON = shared("mechanicalpartsiron", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> MECHANICAL_PARTS_OBSIDIAN_STEEL = shared("mechanicalpartsobsidiansteel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> MECHANICAL_PARTS_CARBON = shared("mechanicalpartscarbon", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> HEAVY_CLOTH = shared("heavycloth", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> BIOMASS = shared("biomass", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> WIRE_COPPER = shared("copperwire", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> WIRE_GOLD = shared("goldwire", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> CARBON_FIBERS = shared("carbonfibers", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> CIRCUIT_BOARD_BASIC = shared("circuitboard", TGSlotType.NORMAL);
	/** between the basic and the elite circuit board (Fabric port) */
	public static final DeferredItem<GenericItemShared> CIRCUIT_BOARD_ADVANCED = shared("circuitboardadvanced", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> CIRCUIT_BOARD_ELITE = shared("circuitboardelite", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> POWER_ARMOR_PLATING = shared("powerplating", TGSlotType.NORMAL);
	/** T4 power armor plating (Fabric port) */
	public static final DeferredItem<GenericItemShared> POWER_ARMOR_PLATING_MK2 = shared("powerplating_mk2", TGSlotType.NORMAL);
	/** praetor armor plating (Fabric port) */
	public static final DeferredItem<GenericItemShared> NETHER_ARMOR_PLATING = shared("netherarmorplating", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> COIL = shared("coil", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> CYBERNETIC_PARTS = shared("cyberneticparts", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> ELECTRIC_ENGINE = shared("electricengine", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> LASER_FOCUS = shared("laserfocus", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PUMP_MECHANISM = shared("pumpmechanism", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RAD_EMITTER = shared("rademitter", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> SONIC_EMITTER = shared("sonicemitter", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> TGX = shared("tgx", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> NETHER_CHARGE = shared("nethercharge", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> TREATED_LEATHER = shared("treatedleather", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> ORE_TITANIUM = shared("oretitanium", TGSlotType.NORMAL);
	/** raw ores, dropped by the ores like vanilla's (Fabric port); titanium ore drops the existing oretitanium */
	public static final DeferredItem<GenericItemShared> RAW_TIN = shared("raw_tin", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RAW_LEAD = shared("raw_lead", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RAW_URANIUM = shared("raw_uranium", TGSlotType.NORMAL);
	/** Techguns copper ingot replaced by vanilla copper (added in 1.17). */
	public static final ItemLike INGOT_COPPER = Items.COPPER_INGOT;
	public static final DeferredItem<GenericItemShared> INGOT_TIN = shared("ingottin", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> INGOT_BRONZE = shared("ingotbronze", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> INGOT_LEAD = shared("ingotlead", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> INGOT_STEEL = shared("ingotsteel", TGSlotType.NORMAL);
	/** crafted from iron and coal, blasted into steel (Fabric port) */
	public static final DeferredItem<GenericItemShared> RAW_STEEL_INGOT = shared("raw_steel_ingot", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> INGOT_OBSIDIAN_STEEL = shared("ingotobsidiansteel", TGSlotType.NORMAL);
	/** crafted from steel and obsidian, blasted into obsidian steel (Fabric port) */
	public static final DeferredItem<GenericItemShared> RAW_OBSIDIAN_STEEL_INGOT = shared("raw_obsidian_steel_ingot", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> INGOT_TITANIUM = shared("ingottitanium", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> NUGGET_COPPER = shared("nuggetcopper", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> NUGGET_LEAD = shared("nuggetlead", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> NUGGET_STEEL = shared("nuggetsteel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> GAS_MASK_FILTER = shared("gasmaskfilter", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> GLIDER_BACKBACK = shared("gliderbackpack", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> GLIDER_WING = shared("gliderwing", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> ANTI_GRAV_CORE = shared("antigravcore", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> OXYGEN_MASK = shared("oxygenmask", TGSlotType.FACESLOT, 1, false);
	public static final DeferredItem<GenericItemShared> MACHINE_UPGRADE_STACK = shared("machinestackupgrade", TGSlotType.NORMAL, 7, false);
	public static final DeferredItem<GenericItemShared> RAW_RUBBER = shared("rawrubber", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RAW_PLASTIC = shared("rawplastic", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> YELLOWCAKE = shared("yellowcake", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> ENRICHED_URANIUM = shared("enricheduranium", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> TURRET_ARMOR_IRON = shared("turretarmoriron", TGSlotType.TURRETARMOR);
	public static final DeferredItem<GenericItemShared> TURRET_ARMOR_STEEL = shared("turretarmorsteel", TGSlotType.TURRETARMOR);
	public static final DeferredItem<GenericItemShared> TURRET_ARMOR_OBSIDIAN_STEEL = shared("turretarmorobsidiansteel", TGSlotType.TURRETARMOR);
	public static final DeferredItem<GenericItemShared> TURRET_ARMOR_CARBON = shared("turretarmorcarbon", TGSlotType.TURRETARMOR);
	public static final DeferredItem<GenericItemShared> QUARTZ_ROD = shared("quartzrod", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RC_HEAT_RAY = shared("rcheatray", TGSlotType.REACTION_CHAMBER_FOCUS, 1, false);
	public static final DeferredItem<GenericItemShared> RC_UV_EMITTER = shared("rcuvemitter", TGSlotType.REACTION_CHAMBER_FOCUS, 1, false);
	public static final DeferredItem<GenericItemShared> SHOTGUN_ROUNDS_INCENDIARY = shared("shotgunrounds_incendiary", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> AS50_MAGAZINE_INCENDIARY = shared("as50magazine_incendiary", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> SNIPER_ROUNDS_INCENDIARY = shared("sniperrounds_incendiary", TGSlotType.AMMOSLOT);
	/** magic rifle ammo (Fabric port): arcane, fire and lightning */
	public static final DeferredItem<GenericItemShared> MAGIC_BULLET = shared("magic_bullet", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> MAGIC_BULLET_FIRE = shared("magic_bullet_fire", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> MAGIC_BULLET_LIGHTNING = shared("magic_bullet_lightning", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> PISTOL_ROUNDS_INCENDIARY = shared("pistolrounds_incendiary", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> RIFLE_ROUNDS_INCENDIARY = shared("riflerounds_incendiary", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> RIFLE_ROUNDS_STACK_INCENDIARY = shared("rifleroundsstack_incendiary", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> SMG_MAGAZINE_INCENDIARY = shared("smgmagazine_incendiary", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> PISTOL_MAGAZINE_INCENDIARY = shared("pistolmagazine_incendiary", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> MINIGUN_DRUM_INCENDIARY = shared("minigundrum_incendiary", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> ASSAULTRIFLE_MAGAZINE_INCENDIARY = shared("assaultriflemagazine_incendiary", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> LMG_MAGAZINE_INCENDIARY = shared("lmgmagazine_incendiary", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> ROCKET_NUKE = shared("rocket_nuke", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> TACTICAL_NUKE_WARHEAD = shared("tacticalnukewarhead", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> MININGDRILLHEAD_OBSIDIAN = shared("miningdrillhead_obsidian", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> MININGDRILLHEAD_CARBON = shared("miningdrillhead_carbon", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> POWERHAMMERHEAD_OBSIDIAN = shared("powerhammerhead_obsidian", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> POWERHAMMERHEAD_CARBON = shared("powerhammerhead_carbon", TGSlotType.NORMAL);
	/** the power hammer's built-in head, crafted into a new power hammer (Fabric port) */
	public static final DeferredItem<GenericItemShared> POWERHAMMERHEAD_STEEL = shared("powerhammerhead_steel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> CHAINSAWBLADES_OBSIDIAN = shared("chainsawblades_obsidian", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> CHAINSAWBLADES_CARBON = shared("chainsawblades_carbon", TGSlotType.NORMAL);
	/** the chainsaw's built-in blades, crafted into a new chainsaw (Fabric port) */
	public static final DeferredItem<GenericItemShared> CHAINSAWBLADES_STEEL = shared("chainsawblades_steel", TGSlotType.NORMAL);
	/** Fabric port: bundles for ammo, 4 and 8 stacks; reloading takes ammo from them */
	public static final DeferredItem<techguns.items.ItemAmmoBox> AMMO_BOX = register("ammo_box",
			() -> new techguns.items.ItemAmmoBox(new Item.Properties(), 4));
	public static final DeferredItem<techguns.items.ItemAmmoBox> AMMO_BOX_LARGE = register("ammo_box_large",
			() -> new techguns.items.ItemAmmoBox(new Item.Properties(), 8));
	public static final DeferredItem<GenericItemShared> REDSTONE_BATTERY = shared("redstone_battery", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> REDSTONE_BATTERY_EMPTY = shared("redstone_battery_empty", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> GAUSSRIFLE_SLUGS = shared("gaussrifleslugs", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> BARREL_GAUSS = shared("gaussbarrel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> BARREL_TITANIUM = shared("shieldedtitaniumbarrel", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> RECEIVER_TITANIUM = shared("titaniumreceiver", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> PLASMA_GENERATOR = shared("plasmagenerator", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> WORKING_GLOVES = shared("workinggloves", TGSlotType.HANDSLOT, 1, false);
	public static final DeferredItem<GenericItemShared> PROTECTIVE_FIBER = shared("protectivefiber", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_STEEL = shared("oredrillsmall_steel", TGSlotType.DRILL_SMALL, 1, false);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_OBSIDIANSTEEL = shared("oredrillsmall_obsidiansteel", TGSlotType.DRILL_SMALL, 1, false);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_CARBON = shared("oredrillsmall_carbon", TGSlotType.DRILL_SMALL, 1, false);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_MEDIUM_STEEL = shared("oredrillmedium_steel", TGSlotType.DRILL_MEDIUM, 1, false);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_MEDIUM_OBSIDIANSTEEL = shared("oredrillmedium_obsidiansteel", TGSlotType.DRILL_MEDIUM, 1, false);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_MEDIUM_CARBON = shared("oredrillmedium_carbon", TGSlotType.DRILL_MEDIUM, 1, false);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_LARGE_STEEL = shared("oredrilllarge_steel", TGSlotType.DRILL_LARGE, 1, false);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_LARGE_OBSIDIANSTEEL = shared("oredrilllarge_obsidiansteel", TGSlotType.DRILL_LARGE, 1, false);
	public static final DeferredItem<GenericItemShared> OREDRILLHEAD_LARGE_CARBON = shared("oredrilllarge_carbon", TGSlotType.DRILL_LARGE, 1, false);
	public static final DeferredItem<GenericItemShared> AS50_MAGAZINE_EXPLOSIVE = shared("as50magazine_explosive", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> SNIPER_ROUNDS_EXPLOSIVE = shared("sniperrounds_explosive", TGSlotType.AMMOSLOT);
	public static final DeferredItem<GenericItemShared> ROCKET_HIGH_VELOCITY = shared("rocket_high_velocity", TGSlotType.AMMOSLOT, 64, true);
	public static final DeferredItem<GenericItemShared> INFUSION_BAG = shared("infusionbag", TGSlotType.NORMAL);
	public static final DeferredItem<GenericItemShared> UPGRADE_PROTECTION_1 = shared("upgrade_protection_1", TGSlotType.ARMOR_UPGRADE, 1, false);
	public static final DeferredItem<GenericItemShared> UPGRADE_PROJECTILE_PROTECTION_1 = shared("upgrade_projectile_protection_1", TGSlotType.ARMOR_UPGRADE, 1, false);
	public static final DeferredItem<GenericItemShared> UPGRADE_BLAST_PROTECTION_1 = shared("upgrade_blast_protection_1", TGSlotType.ARMOR_UPGRADE, 1, false);
	public static final DeferredItem<GenericItemShared> UPGRADE_PROTECTION_2 = shared("upgrade_protection_2", TGSlotType.ARMOR_UPGRADE, 1, false);
	public static final DeferredItem<GenericItemShared> UPGRADE_PROJECTILE_PROTECTION_2 = shared("upgrade_projectile_protection_2", TGSlotType.ARMOR_UPGRADE, 1, false);
	public static final DeferredItem<GenericItemShared> UPGRADE_BLAST_PROTECTION_2 = shared("upgrade_blast_protection_2", TGSlotType.ARMOR_UPGRADE, 1, false);
	public static final DeferredItem<GenericItemShared> UPGRADE_PROTECTION_3 = shared("upgrade_protection_3", TGSlotType.ARMOR_UPGRADE, 1, false);
	public static final DeferredItem<GenericItemShared> UPGRADE_PROJECTILE_PROTECTION_3 = shared("upgrade_projectile_protection_3", TGSlotType.ARMOR_UPGRADE, 1, false);
	public static final DeferredItem<GenericItemShared> UPGRADE_BLAST_PROTECTION_3 = shared("upgrade_blast_protection_3", TGSlotType.ARMOR_UPGRADE, 1, false);

	/* Tools */
	public static final Tier TG_STEEL = new Tier() {
		@Override public int getUses() { return 1000; }
		@Override public float getSpeed() { return 7.5f; }
		@Override public float getAttackDamageBonus() { return 2.5f; }
		@Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
		@Override public int getEnchantmentValue() { return 12; }
		@Override public Ingredient getRepairIngredient() { return Ingredient.of(INGOT_STEEL.get()); }
	};

	public static final DeferredItem<SwordItem> COMBAT_KNIFE = register("combatknife",
			() -> new SwordItem(TG_STEEL, new Item.Properties().attributes(SwordItem.createAttributes(TG_STEEL, 3, -2.4f))));
	public static final DeferredItem<PickaxeItem> CROWBAR = register("crowbar",
			() -> new PickaxeItem(TG_STEEL, new Item.Properties().attributes(PickaxeItem.createAttributes(TG_STEEL, 1.0f, -2.8f))));

	// face / back slot items (Curios)
	public static final DeferredItem<techguns.items.ItemRadConsumable> RAD_AWAY = register("radaway",
			() -> new techguns.items.ItemRadConsumable(new Item.Properties(),
					() -> java.util.List.of(new net.minecraft.world.effect.MobEffectInstance(techguns.radiation.TGRadiationSystem.RAD_REGENERATION, 400, 14, false, false)),
					INFUSION_BAG, () -> net.minecraft.sounds.SoundEvents.PLAYER_BREATH, net.minecraft.world.item.UseAnim.BOW,
					java.util.List.of(net.minecraft.network.chat.Component.translatable("techguns.radregeneration").append(" XV (-300)"))));
	public static final DeferredItem<techguns.items.ItemRadConsumable> RAD_PILLS = register("radpills",
			() -> new techguns.items.ItemRadConsumable(new Item.Properties(),
					() -> java.util.List.of(new net.minecraft.world.effect.MobEffectInstance(techguns.radiation.TGRadiationSystem.RAD_REGENERATION, 500, 1, false, false),
							new net.minecraft.world.effect.MobEffectInstance(techguns.radiation.TGRadiationSystem.RAD_RESISTANCE, 3600, 2, false, false)),
					() -> net.minecraft.world.item.Items.GLASS_BOTTLE, () -> net.minecraft.sounds.SoundEvents.PLAYER_BURP, net.minecraft.world.item.UseAnim.EAT,
					java.util.List.of(net.minecraft.network.chat.Component.translatable("techguns.radresistance").append(" III (180s)"),
							net.minecraft.network.chat.Component.translatable("techguns.radregeneration").append(" II (-50)"))));
	public static final DeferredItem<techguns.items.additionalslots.ItemGasMask> GAS_MASK = register("gasmask",
			() -> new techguns.items.additionalslots.ItemGasMask(1, 300).setRadresist(1f));
	public static final DeferredItem<techguns.items.additionalslots.ItemGlider> GLIDER = register("glider",
			() -> new techguns.items.additionalslots.ItemGlider(1, -1));
	public static final DeferredItem<techguns.items.additionalslots.ItemJumpPack> JUMPPACK = register("jumppack",
			() -> new techguns.items.additionalslots.ItemJumpPack(4, 1000));
	public static final DeferredItem<techguns.items.additionalslots.ItemScubaTanks> SCUBA_TANKS = register("scubatanks",
			() -> new techguns.items.additionalslots.ItemScubaTanks(1, 600));
	public static final DeferredItem<techguns.items.additionalslots.ItemNightVisionGoggles> NIGHTVISION_GOGGLES = register("nightvisiongoggles",
			() -> new techguns.items.additionalslots.ItemNightVisionGoggles(1, 24000));
	public static final DeferredItem<techguns.items.additionalslots.ItemJetpack> JETPACK = register("jetpack",
			() -> new techguns.items.additionalslots.ItemJetpack(4, 5000));
	public static final DeferredItem<techguns.items.additionalslots.ItemTacticalMask> TACTICAL_MASK = register("tacticalmask",
			() -> new techguns.items.additionalslots.ItemTacticalMask(4, 24000));
	public static final DeferredItem<techguns.items.additionalslots.ItemAntiGravPack> ANTI_GRAV_PACK = register("antigravpack",
			() -> new techguns.items.additionalslots.ItemAntiGravPack(5, 12000));

	public static DeferredItem<GenericItemShared> shared(String name, TGSlotType slot) {
		return shared(name, slot, 64, false);
	}

	public static DeferredItem<GenericItemShared> shared(String name, TGSlotType slot, int maxStackSize, boolean useRenderHack) {
		return register(name, () -> new GenericItemShared(new Item.Properties().stacksTo(maxStackSize), slot, useRenderHack));
	}

	public static <T extends Item> DeferredItem<T> register(String name, java.util.function.Supplier<T> sup) {
		DeferredItem<T> item = ITEMS.register(name, sup);
		CREATIVE_TAB_ITEMS.add(item);
		return item;
	}

	/** Registers an item that does not get added to the creative tab automatically. */
	public static <T extends Item> DeferredItem<T> registerHidden(String name, java.util.function.Supplier<T> sup) {
		return ITEMS.register(name, sup);
	}

	public static ItemStack newStack(ItemLike item, int size) {
		return new ItemStack(item, size);
	}

	public static ItemStack newStack(ItemStack stack, int size) {
		return stack.copyWithCount(size);
	}

	/** Called from common setup: wire up magazine unloading (sneak right click). */
	public static void init() {
		AmmoTypes.init();
		setAmmoTypeEntryForMagazine(PISTOL_MAGAZINE, AmmoTypes.PISTOL_MAGAZINE, AmmoTypes.TYPE_DEFAULT);
		setAmmoTypeEntryForMagazine(ASSAULTRIFLE_MAGAZINE, AmmoTypes.ASSAULT_RIFLE_MAGAZINE, AmmoTypes.TYPE_DEFAULT);
		setAmmoTypeEntryForMagazine(AS50_MAGAZINE, AmmoTypes.AS50_MAGAZINE, AmmoTypes.TYPE_DEFAULT);
		setAmmoTypeEntryForMagazine(SMG_MAGAZINE, AmmoTypes.SMG_MAGAZINE, AmmoTypes.TYPE_DEFAULT);
		setAmmoTypeEntryForMagazine(LMG_MAGAZINE, AmmoTypes.LMG_MAGAZINE, AmmoTypes.TYPE_DEFAULT);
		setAmmoTypeEntryForMagazine(MINIGUN_DRUM, AmmoTypes.MINIGUN_AMMO_DRUM, AmmoTypes.TYPE_DEFAULT);

		setAmmoTypeEntryForMagazine(PISTOL_MAGAZINE_INCENDIARY, AmmoTypes.PISTOL_MAGAZINE, AmmoTypes.TYPE_INCENDIARY);
		setAmmoTypeEntryForMagazine(ASSAULTRIFLE_MAGAZINE_INCENDIARY, AmmoTypes.ASSAULT_RIFLE_MAGAZINE, AmmoTypes.TYPE_INCENDIARY);
		setAmmoTypeEntryForMagazine(AS50_MAGAZINE_INCENDIARY, AmmoTypes.AS50_MAGAZINE, AmmoTypes.TYPE_INCENDIARY);
		setAmmoTypeEntryForMagazine(SMG_MAGAZINE_INCENDIARY, AmmoTypes.SMG_MAGAZINE, AmmoTypes.TYPE_INCENDIARY);
		setAmmoTypeEntryForMagazine(LMG_MAGAZINE_INCENDIARY, AmmoTypes.LMG_MAGAZINE, AmmoTypes.TYPE_INCENDIARY);
		setAmmoTypeEntryForMagazine(MINIGUN_DRUM_INCENDIARY, AmmoTypes.MINIGUN_AMMO_DRUM, AmmoTypes.TYPE_INCENDIARY);

		setAmmoTypeEntryForMagazine(AS50_MAGAZINE_EXPLOSIVE, AmmoTypes.AS50_MAGAZINE, AmmoTypes.TYPE_EXPLOSIVE);

		setAmmoTypeEntryForMagazine(ADVANCED_MAGAZINE, AmmoTypes.ADVANCED_MAGAZINE, AmmoTypes.TYPE_DEFAULT);
	}

	public static void setAmmoTypeEntryForMagazine(DeferredItem<GenericItemShared> fullMag, AmmoType ammoType, String variant) {
		ItemStack bullet = ammoType.getBullet(ammoType.getIDforVariantKey(variant))[0];
		ItemStack mag = ammoType.getEmptyMag()[0];
		fullMag.get().setAmmoEntry(bullet.getItem(), mag.getItem(), ammoType.getBulletsPerMag());
	}

	public static boolean isMachineUpgrade(ItemStack stack) {
		return !stack.isEmpty() && stack.is(MACHINE_UPGRADE_STACK.get());
	}
}
