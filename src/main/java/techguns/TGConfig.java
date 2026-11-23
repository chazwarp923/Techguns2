package techguns;

import java.util.List;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Port of the 1.12.2 Forge Configuration to NeoForge ModConfigSpec.
 * Values are baked into the same static fields the rest of the code uses.
 * Common config: config/techguns-common.toml, client: config/techguns-client.toml.
 */
public class TGConfig {
	private static final ModConfigSpec.Builder BUILDER_S = new ModConfigSpec.Builder();
	private static final ModConfigSpec.Builder BUILDER_C = new ModConfigSpec.Builder();

	public static boolean debug;
	private static final ModConfigSpec.BooleanValue debug_V = BUILDER_S.comment("Enable debug options and unfinished stuff, disable this for playing.").define("general.debug", false);
	public static int upgrade_xp_cost;
	private static final ModConfigSpec.IntValue upgrade_xp_cost_V = BUILDER_S.comment("Base XP value for Upgrade Bench recipes (enchants)").defineInRange("general.UpgradeXPCost", 20, 0, 10000);
	public static boolean limitUnsafeModeToOP;
	private static final ModConfigSpec.BooleanValue limitUnsafeModeToOP_V = BUILDER_S.comment("Only opped players can use the unsafe mode of guns, this OVERRIDES the permission setting 'techguns.allowunsafemode'").define("general.RestrictUnsafeModeToOP", false);
	public static boolean disableAutofeeder;
	private static final ModConfigSpec.BooleanValue disableAutofeeder_V = BUILDER_S.comment("Disable automatic feeding of Food in the Techguns tab. Disable autofeeding if you think it breaks the balance").define("general.disableAutofeeder", false);
	public static boolean machinesNeedNoPower;
	private static final ModConfigSpec.BooleanValue machinesNeedNoPower_V = BUILDER_S.comment("Machines don't need power, activate this if you don't want to install a mod with generators and still be able to use the machines").define("general.machinesNeedNoPower", false);
	public static boolean keepLavaRecipesWhenFuelIsPresent;
	private static final ModConfigSpec.BooleanValue keepLavaRecipesWhenFuelIsPresent_V = BUILDER_S.comment("Keep recipes with lava instead of fuel even when fuel is present. Fuels need to be added by other mods").define("general.keepLavaRecipesWhenFuelIsPresent", false);
	public static boolean addCopperIngots;
	private static final ModConfigSpec.BooleanValue addCopperIngots_V = BUILDER_S.comment("Add copper ingots.").define("enabling_items.addCopperIngot", true);
	public static boolean addCopperNuggets;
	private static final ModConfigSpec.BooleanValue addCopperNuggets_V = BUILDER_S.comment("Add copper nuggets.").define("enabling_items.addCopperNugget", true);
	public static boolean addTinIngots;
	private static final ModConfigSpec.BooleanValue addTinIngots_V = BUILDER_S.comment("Add tin ingots.").define("enabling_items.addTinIngot", true);
	public static boolean addBronzeIngots;
	private static final ModConfigSpec.BooleanValue addBronzeIngots_V = BUILDER_S.comment("Add bronze ingots.").define("enabling_items.addBronzeIngot", true);
	public static boolean addLeadIngots;
	private static final ModConfigSpec.BooleanValue addLeadIngots_V = BUILDER_S.comment("Add lead ingots.").define("enabling_items.addLeadIngot", true);
	public static boolean addLeadNuggets;
	private static final ModConfigSpec.BooleanValue addLeadNuggets_V = BUILDER_S.comment("Add Lead nuggets.").define("enabling_items.addLeadNugget", true);
	public static boolean addSteelIngots;
	private static final ModConfigSpec.BooleanValue addSteelIngots_V = BUILDER_S.comment("Adds Steel ingots.").define("enabling_items.addSteelIngot", true);
	public static boolean addSteelNuggets;
	private static final ModConfigSpec.BooleanValue addSteelNuggets_V = BUILDER_S.comment("Adds Steel nuggets.").define("enabling_items.addSteelNugget", true);
	public static int distanceSpawnLevel0;
	private static final ModConfigSpec.IntValue distanceSpawnLevel0_V = BUILDER_S.comment("Up to which distance to worldspawn only mobs with danger level up to 0 will spawn").defineInRange("npc_spawn.DistanceSpawnLevel0", 500, 0, Integer.MAX_VALUE);
	public static int distanceSpawnLevel1;
	private static final ModConfigSpec.IntValue distanceSpawnLevel1_V = BUILDER_S.comment("Up to which distance to worldspawn only mobs with danger level up to 1 will spawn").defineInRange("npc_spawn.DistanceSpawnLevel1", 1000, 0, Integer.MAX_VALUE);
	public static int distanceSpawnLevel2;
	private static final ModConfigSpec.IntValue distanceSpawnLevel2_V = BUILDER_S.comment("Up to which distance to worldspawn only mobs with danger level up to 2 will spawn").defineInRange("npc_spawn.DistanceSpawnLevel2", 2500, 0, Integer.MAX_VALUE);
	public static int spawnWeightTGOverworld;
	private static final ModConfigSpec.IntValue spawnWeightTGOverworld_V = BUILDER_S.comment("Spawn weigth of Techguns NPCs, determines how many TG npcs spawn").defineInRange("npc_spawn.Techguns_Spawnweight_Overworld", 600, 0, 10000);
	public static int spawnWeightTGNether;
	private static final ModConfigSpec.IntValue spawnWeightTGNether_V = BUILDER_S.comment("Spawn weigth of Techguns NPCs in the Nether, determines how many TG npcs spawn").defineInRange("npc_spawn.Techguns_Spawnweight_NEther", 300, 0, 10000);
	public static int spawnWeightZombieSoldier;
	private static final ModConfigSpec.IntValue spawnWeightZombieSoldier_V = BUILDER_S.comment("Spawn weight for spawning Zombie Soldiers, at 0 spawn will not be registered").defineInRange("npc_spawn.SpawnWeightZombieSoldier", 100, 0, 10000);
	public static int spawnWeightZombieFarmer;
	private static final ModConfigSpec.IntValue spawnWeightZombieFarmer_V = BUILDER_S.comment("Spawn weight for spawning Zombie Farmers, at 0 spawn will not be registered").defineInRange("npc_spawn.SpawnWeightZombieFarmer", 200, 0, 10000);
	public static int spawnWeightZombieMiner;
	private static final ModConfigSpec.IntValue spawnWeightZombieMiner_V = BUILDER_S.comment("Spawn weight for spawning Zombie Miners, at 0 spawn will not be registered").defineInRange("npc_spawn.SpawnWeightZombieMiner", 200, 0, 10000);
	public static int spawnWeightZombiePigmanSoldier;
	private static final ModConfigSpec.IntValue spawnWeightZombiePigmanSoldier_V = BUILDER_S.comment("Spawn weight for spawning Zombie Pigman Soldiers (Nether only), at 0 spawn will not be registered").defineInRange("npc_spawn.SpawnWeightZombiePigmanSoldier", 100, 0, 10000);
	public static int spawnWeightCyberDemon;
	private static final ModConfigSpec.IntValue spawnWeightCyberDemon_V = BUILDER_S.comment("Spawn weight for spawning Cyber Demons (Nether only), at 0 spawn will not be registered").defineInRange("npc_spawn.SpawnWeightCyberDemon", 30, 0, 10000);
	public static int spawnWeightBandit;
	private static final ModConfigSpec.IntValue spawnWeightBandit_V = BUILDER_S.comment("Spawn weight for spawning Bandit groups, at 0 spawn will not be registered").defineInRange("npc_spawn.SpawnWeightBandit", 50, 0, 10000);
	public static int spawnWeightSkeletonSoldier;
	private static final ModConfigSpec.IntValue spawnWeightSkeletonSoldier_V = BUILDER_S.comment("Spawn weight for spawning Skeleton Soldiers, at 0 spawn will not be registered").defineInRange("npc_spawn.SpawnWeightSkeletonSoldier", 100, 0, 10000);
	public static int spawnWeightPsychoSteve;
	private static final ModConfigSpec.IntValue spawnWeightPsychoSteve_V = BUILDER_S.comment("Spawn weight for spawning Psycho Steve, early game boss, don't set to high value, at 0 spawn will not be registered").defineInRange("npc_spawn.SpawnWeightPsychoSteve", 3, 0, 10000);
	public static String[] biomeBlacklist;
	private static final ModConfigSpec.ConfigValue<List<? extends String>> biomeBlacklist_V = BUILDER_S.comment("Biome Registry names (e.g: minecraft:mushroom_island) that are excluded from Techguns monster spawning").defineListAllowEmpty("npc_spawn.BiomeBlacklist", List.of(""), () -> "", o -> o instanceof String);
	public static float damagePvP;
	private static final ModConfigSpec.DoubleValue damagePvP_V = BUILDER_S.comment("Damage factor Techguns weapons deal when fired from players against other players, is zero when PvP is disabled").defineInRange("damage_factors.DamagePvP", (double)0.5f, (double)0.0f, (double)100.0f);
	public static float damageTurretToPlayer;
	private static final ModConfigSpec.DoubleValue damageTurretToPlayer_V = BUILDER_S.comment("Damage factor Techguns Turrets deal when hitting players").defineInRange("damage_factors.DamageTurretToPlayer", (double)0.5f, (double)0.0f, (double)100.0f);
	public static float damageFactorNPC;
	private static final ModConfigSpec.DoubleValue damageFactorNPC_V = BUILDER_S.comment("Damage factor for all NPCs other than turrets, they already have a difficulty dependent damage penalty, this can be used to further reduce their damage, or increase it").defineInRange("damage_factors.DamageFactorNPC", (double)1.0f, (double)0.0f, (double)100.0f);
	public static boolean doOreGenTin;
	private static final ModConfigSpec.BooleanValue doOreGenTin_V = BUILDER_S.comment("Generate Tin Ore, disable if other mod does").define("worldgen.doOreGenTin", true);
	public static boolean doOreGenLead;
	private static final ModConfigSpec.BooleanValue doOreGenLead_V = BUILDER_S.comment("Generate Lead Ore, disable if other mod does").define("worldgen.doOreGenLead", true);
	public static boolean doOreGenTitanium;
	private static final ModConfigSpec.BooleanValue doOreGenTitanium_V = BUILDER_S.comment("Generate Titanium, not generated by most mods mods, leave it on in most cases").define("worldgen.doOreGenTitanium", true);
	public static boolean doOreGenUranium;
	private static final ModConfigSpec.BooleanValue doOreGenUranium_V = BUILDER_S.comment("Generate Uranium, disable if other mod already adds it and you want only 1 type. OreDictEntry:'oreUranium' ").define("worldgen.doOreGenUranium", true);
	public static boolean doWorldspawn;
	private static final ModConfigSpec.BooleanValue doWorldspawn_V = BUILDER_S.comment("Should Structures (military bases) be spawned in the world?").define("worldgen.SpawnStructures", true);
	public static boolean spawnOreClusterStructures;
	private static final ModConfigSpec.BooleanValue spawnOreClusterStructures_V = BUILDER_S.comment("When worldgen is enabled, include structure spawns that contain ore clusters.").define("worldgen.SpawnOreClusterStructures", true);
	public static float explosiveChargeMaxBlockHardness;
	private static final ModConfigSpec.DoubleValue explosiveChargeMaxBlockHardness_V = BUILDER_S.comment("Highest blockHardness normal explosive charges can break, obsidian is 50.0)").defineInRange("general.ExplosiveChargeMaxHardness", (double)30.0f, (double)0.0f, (double)Double.MAX_VALUE);
	public static float explosiveChargeAdvancedMaxBlockHardness;
	private static final ModConfigSpec.DoubleValue explosiveChargeAdvancedMaxBlockHardness_V = BUILDER_S.comment("Highest blockHardness advanced explosive charges can break, obsidian is 50.0)").defineInRange("general.ExplosiveChargeAdvancedMaxHardness", (double)100.0f, (double)0.0f, (double)Double.MAX_VALUE);
	public static boolean WIP_disableRadiationSystem;
	private static final ModConfigSpec.BooleanValue WIP_disableRadiationSystem_V = BUILDER_S.comment("Disable Radiation for players. Radiation system is WIP, only use in creative for testing").define("general.WIP_disableRadiationSystem", true);
	public static String[] fluidListFuel;
	private static final ModConfigSpec.ConfigValue<List<? extends String>> fluidListFuel_V = BUILDER_S.comment("Fluids that can be used to fill up fuel tanks").defineListAllowEmpty("fluid_recipes.FluidListFuel", List.of("fuel", "refined_fuel", "biofuel", "biodiesel", "diesel", "gasoline", "fluiddiesel", "fluidnitrodiesel", "fliudnitrofuel", "refined_biofuel", "fire_water", "rocket_fuel"), () -> "", o -> o instanceof String);
	public static String[] fluidListOil;
	private static final ModConfigSpec.ConfigValue<List<? extends String>> fluidListOil_V = BUILDER_S.comment("Fluids that are treated as oil.").defineListAllowEmpty("fluid_recipes.FluidListOil", List.of("oil", "tree_oil", "crude_oil", "fluidoil", "seed_oil"), () -> "", o -> o instanceof String);
	public static String[] fluidListOilWorldspawn;
	private static final ModConfigSpec.ConfigValue<List<? extends String>> fluidListOilWorldspawn_V = BUILDER_S.comment("Fluids that are treated as oil for worlspawn and oil ore clusters.").defineListAllowEmpty("fluid_recipes.FluidListOilWorldspawn", List.of("oil", "crude_oil"), () -> "", o -> o instanceof String);
	public static float oreDrillMultiplierOres;
	private static final ModConfigSpec.DoubleValue oreDrillMultiplierOres_V = BUILDER_S.comment("Multiplier to default rate on how many ores an ore drill produces").defineInRange("ore_drills.oreDrillMultiplierOre", (double)1.0f, (double)0.001f, (double)1000.0f);
	public static float oreDrillMultiplierPower;
	private static final ModConfigSpec.DoubleValue oreDrillMultiplierPower_V = BUILDER_S.comment("Multiplier to default rate on how much power an ore drill requires").defineInRange("ore_drills.oreDrillMultiplierPower", (double)1.0f, (double)0f, (double)1000.0f);
	public static float oreDrillMultiplierFuel;
	private static final ModConfigSpec.DoubleValue oreDrillMultiplierFuel_V = BUILDER_S.comment("Multiplier to calculate value of furnace fuel burn time = RF for ore Drill. burnTime* <THIS_VALUE> = RF. Only for internal use of the ore Drill, no real RF generation.").defineInRange("ore_drills.oreDrillFuelMultiplier", (double)1000, (double)1, (double)100000);
	public static float oreDrillFuelValueFuel;
	private static final ModConfigSpec.DoubleValue oreDrillFuelValueFuel_V = BUILDER_S.comment("Fuel value for Liquid Fuel for use in ore Drills, this is per Millibucket, not Bucket, so 1/1000 of bucket value").defineInRange("ore_drills.oreDrillFuelValueFuel", (double)100, (double)1, (double)100000);
	public static int mininglevel_coal;
	private static final ModConfigSpec.IntValue mininglevel_coal_V = BUILDER_S.comment("Mining Level for coal ore clusters").defineInRange("ore_drills.cluster_mininglevel_coal", 0, 0, 10);
	public static int mininglevel_common_metal;
	private static final ModConfigSpec.IntValue mininglevel_common_metal_V = BUILDER_S.comment("Mining Level for common metal ore clusters").defineInRange("ore_drills.cluster_mininglevel_common_metal", 0, 0, 10);
	public static int mininglevel_rare_metal;
	private static final ModConfigSpec.IntValue mininglevel_rare_metal_V = BUILDER_S.comment("Mining Level for rare metal ore clusters").defineInRange("ore_drills.cluster_mininglevel_rare_metal", 1, 0, 10);
	public static int mininglevel_shiny_metal;
	private static final ModConfigSpec.IntValue mininglevel_shiny_metal_V = BUILDER_S.comment("Mining Level for shiny metal ore clusters").defineInRange("ore_drills.cluster_mininglevel_shiny_metal", 2, 0, 10);
	public static int mininglevel_uranium;
	private static final ModConfigSpec.IntValue mininglevel_uranium_V = BUILDER_S.comment("Mining Level for uranium ore clusters").defineInRange("ore_drills.cluster_mininglevel_uranium", 3, 0, 10);
	public static int mininglevel_common_gem;
	private static final ModConfigSpec.IntValue mininglevel_common_gem_V = BUILDER_S.comment("Mining Level for common gem ore clusters").defineInRange("ore_drills.cluster_mininglevel_common_gem", 1, 0, 10);
	public static int mininglevel_shiny_gem;
	private static final ModConfigSpec.IntValue mininglevel_shiny_gem_V = BUILDER_S.comment("Mining Level for shiny gem ore clusters").defineInRange("ore_drills.cluster_mininglevel_shiny_gem", 3, 0, 10);
	public static int mininglevel_nether_crystal;
	private static final ModConfigSpec.IntValue mininglevel_nether_crystal_V = BUILDER_S.comment("Mining Level for nether crystal ore clusters").defineInRange("ore_drills.cluster_mininglevel_nether_crystal", 2, 0, 10);
	public static int mininglevel_oil;
	private static final ModConfigSpec.IntValue mininglevel_oil_V = BUILDER_S.comment("Mining Level for oil clusters").defineInRange("ore_drills.cluster_mininglevel_oil", 2, 0, 10);
	public static double oremult_coal;
	private static final ModConfigSpec.DoubleValue oremult_coal_V = BUILDER_S.comment("Ore Multiplier for coal ore clusters").defineInRange("ore_drills.cluster_oremult_coal", (double)10f, (double)0.0001f, (double)1000f);
	public static double oremult_common_metal;
	private static final ModConfigSpec.DoubleValue oremult_common_metal_V = BUILDER_S.comment("Ore Multiplier for common metal ore clusters").defineInRange("ore_drills.cluster_oremult_common_metal", (double)5f, (double)0.0001f, (double)1000f);
	public static double oremult_rare_metal;
	private static final ModConfigSpec.DoubleValue oremult_rare_metal_V = BUILDER_S.comment("Ore Multiplier for rare metal ore clusters").defineInRange("ore_drills.cluster_oremult_rare_metal", (double)2.5f, (double)0.0001f, (double)1000f);
	public static double oremult_shiny_metal;
	private static final ModConfigSpec.DoubleValue oremult_shiny_metal_V = BUILDER_S.comment("Ore Multiplier for shiny metal ore clusters").defineInRange("ore_drills.cluster_oremult_shiny_metal", (double)1f, (double)0.0001f, (double)1000f);
	public static double oremult_uranium;
	private static final ModConfigSpec.DoubleValue oremult_uranium_V = BUILDER_S.comment("Ore Multiplier for uranium ore clusters").defineInRange("ore_drills.cluster_oremult_uranium", (double)0.5f, (double)0.0001f, (double)1000f);
	public static double oremult_common_gem;
	private static final ModConfigSpec.DoubleValue oremult_common_gem_V = BUILDER_S.comment("Ore Multiplier for common gem ore clusters").defineInRange("ore_drills.cluster_oremult_common_gem", (double)5f, (double)0.0001f, (double)1000f);
	public static double oremult_shiny_gem;
	private static final ModConfigSpec.DoubleValue oremult_shiny_gem_V = BUILDER_S.comment("Ore Multiplier for shiny gem ore clusters").defineInRange("ore_drills.cluster_oremult_shiny_gem", (double)0.2f, (double)0.0001f, (double)1000f);
	public static double oremult_nether_crystal;
	private static final ModConfigSpec.DoubleValue oremult_nether_crystal_V = BUILDER_S.comment("Ore Multiplier for nether crystal ore clusters").defineInRange("ore_drills.cluster_oremult_nether_crystal", (double)4f, (double)0.0001f, (double)1000f);
	public static double oremult_oil;
	private static final ModConfigSpec.DoubleValue oremult_oil_V = BUILDER_S.comment("Ore Multiplier for oil clusters").defineInRange("ore_drills.cluster_oremult_oil", (double)4f, (double)0.0001f, (double)1000f);
	public static double powermult_coal;
	private static final ModConfigSpec.DoubleValue powermult_coal_V = BUILDER_S.comment("Power Multiplier for coal ore clusters").defineInRange("ore_drills.cluster_powermult_coal", (double)0.1f, (double)0.0001f, (double)1000f);
	public static double powermult_common_metal;
	private static final ModConfigSpec.DoubleValue powermult_common_metal_V = BUILDER_S.comment("Power Multiplier for common metal ore clusters").defineInRange("ore_drills.cluster_powermult_common_metal", (double)0.2f, (double)0.0001f, (double)1000f);
	public static double powermult_rare_metal;
	private static final ModConfigSpec.DoubleValue powermult_rare_metal_V = BUILDER_S.comment("Power Multiplier for rare metal ore clusters").defineInRange("ore_drills.cluster_powermult_rare_metal", (double)0.4f, (double)0.0001f, (double)1000f);
	public static double powermult_shiny_metal;
	private static final ModConfigSpec.DoubleValue powermult_shiny_metal_V = BUILDER_S.comment("Power Multiplier for shiny metal ore clusters").defineInRange("ore_drills.cluster_powermult_shiny_metal", (double)1.0f, (double)0.0001f, (double)1000f);
	public static double powermult_uranium;
	private static final ModConfigSpec.DoubleValue powermult_uranium_V = BUILDER_S.comment("Power Multiplier for uranium ore clusters").defineInRange("ore_drills.cluster_powermult_uranium", (double)1.0f, (double)0.0001f, (double)1000f);
	public static double powermult_common_gem;
	private static final ModConfigSpec.DoubleValue powermult_common_gem_V = BUILDER_S.comment("Power Multiplier for common gem ore clusters").defineInRange("ore_drills.cluster_powermult_common_gem", (double)0.2f, (double)0.0001f, (double)1000f);
	public static double powermult_shiny_gem;
	private static final ModConfigSpec.DoubleValue powermult_shiny_gem_V = BUILDER_S.comment("Power Multiplier for shiny gem ore clusters").defineInRange("ore_drills.cluster_powermult_shiny_gem", (double)1.0f, (double)0.0001f, (double)1000f);
	public static double powermult_nether_crystal;
	private static final ModConfigSpec.DoubleValue powermult_nether_crystal_V = BUILDER_S.comment("Power Multiplier for nether crystal ore clusters").defineInRange("ore_drills.cluster_powermult_nether_crystal", (double)0.5f, (double)0.0001f, (double)1000f);
	public static double powermult_oil;
	private static final ModConfigSpec.DoubleValue powermult_oil_V = BUILDER_S.comment("Power Multiplier for oil clusters").defineInRange("ore_drills.cluster_powermult_oil", (double)1.0f, (double)0.0001f, (double)1000f);

	/* Client */
	public static boolean cl_enableDeathFX;
	private static final ModConfigSpec.BooleanValue cl_enableDeathFX_V = BUILDER_C.comment("Enable Death Effects, pure clientside check.").define("client.EnableDeathEffects", true);
	public static boolean cl_enableDeathFX_Gore;
	private static final ModConfigSpec.BooleanValue cl_enableDeathFX_Gore_V = BUILDER_C.comment("Enable the gore Death Effect, requires DeathEffects to be enabled, pure clientside check.").define("client.EnableGoreDeathEffect", true);
	public static boolean cl_lockSpeedFov;
	private static final ModConfigSpec.BooleanValue cl_lockSpeedFov_V = BUILDER_C.comment("Counters the speed dependant FOV change. This also stops FOV changes while sprinting. Don't activate if another mod does this too, pure clientside check.").define("client.LockSpeedDependantFov", true);
	public static float cl_fixedSprintFov;
	private static final ModConfigSpec.DoubleValue cl_fixedSprintFov_V = BUILDER_C.comment("Multiply the FOV while sprinting by this value independent from the actual speed, has no effect when LockSpeedDependantFov is false, pure clientside check.").defineInRange("client.FixedSprintFovMultiplier", (double)1.15f, (double)1.0f, (double)10.0f);
	public static int cl_sortPassesPerTick;
	private static final ModConfigSpec.IntValue cl_sortPassesPerTick_V = BUILDER_C.comment("How many bubble sort passes should be performed each tick on particles. 0=off. Clientside").defineInRange("client.ParticleDepthSortPasses", 10, 0, 20);

	public static final ModConfigSpec COMMON_SPEC = BUILDER_S.build();
	public static final ModConfigSpec CLIENT_SPEC = BUILDER_C.build();

	public static void bakeCommon() {
		debug = debug_V.get();
		upgrade_xp_cost = upgrade_xp_cost_V.get();
		limitUnsafeModeToOP = limitUnsafeModeToOP_V.get();
		disableAutofeeder = disableAutofeeder_V.get();
		machinesNeedNoPower = machinesNeedNoPower_V.get();
		keepLavaRecipesWhenFuelIsPresent = keepLavaRecipesWhenFuelIsPresent_V.get();
		addCopperIngots = addCopperIngots_V.get();
		addCopperNuggets = addCopperNuggets_V.get();
		addTinIngots = addTinIngots_V.get();
		addBronzeIngots = addBronzeIngots_V.get();
		addLeadIngots = addLeadIngots_V.get();
		addLeadNuggets = addLeadNuggets_V.get();
		addSteelIngots = addSteelIngots_V.get();
		addSteelNuggets = addSteelNuggets_V.get();
		distanceSpawnLevel0 = distanceSpawnLevel0_V.get();
		distanceSpawnLevel1 = distanceSpawnLevel1_V.get();
		distanceSpawnLevel2 = distanceSpawnLevel2_V.get();
		spawnWeightTGOverworld = spawnWeightTGOverworld_V.get();
		spawnWeightTGNether = spawnWeightTGNether_V.get();
		spawnWeightZombieSoldier = spawnWeightZombieSoldier_V.get();
		spawnWeightZombieFarmer = spawnWeightZombieFarmer_V.get();
		spawnWeightZombieMiner = spawnWeightZombieMiner_V.get();
		spawnWeightZombiePigmanSoldier = spawnWeightZombiePigmanSoldier_V.get();
		spawnWeightCyberDemon = spawnWeightCyberDemon_V.get();
		spawnWeightBandit = spawnWeightBandit_V.get();
		spawnWeightSkeletonSoldier = spawnWeightSkeletonSoldier_V.get();
		spawnWeightPsychoSteve = spawnWeightPsychoSteve_V.get();
		biomeBlacklist = biomeBlacklist_V.get().toArray(new String[0]);
		damagePvP = (float)damagePvP_V.get().doubleValue();
		damageTurretToPlayer = (float)damageTurretToPlayer_V.get().doubleValue();
		damageFactorNPC = (float)damageFactorNPC_V.get().doubleValue();
		doOreGenTin = doOreGenTin_V.get();
		doOreGenLead = doOreGenLead_V.get();
		doOreGenTitanium = doOreGenTitanium_V.get();
		doOreGenUranium = doOreGenUranium_V.get();
		doWorldspawn = doWorldspawn_V.get();
		spawnOreClusterStructures = spawnOreClusterStructures_V.get();
		explosiveChargeMaxBlockHardness = (float)explosiveChargeMaxBlockHardness_V.get().doubleValue();
		explosiveChargeAdvancedMaxBlockHardness = (float)explosiveChargeAdvancedMaxBlockHardness_V.get().doubleValue();
		WIP_disableRadiationSystem = WIP_disableRadiationSystem_V.get();
		fluidListFuel = fluidListFuel_V.get().toArray(new String[0]);
		fluidListOil = fluidListOil_V.get().toArray(new String[0]);
		fluidListOilWorldspawn = fluidListOilWorldspawn_V.get().toArray(new String[0]);
		oreDrillMultiplierOres = (float)oreDrillMultiplierOres_V.get().doubleValue();
		oreDrillMultiplierPower = (float)oreDrillMultiplierPower_V.get().doubleValue();
		oreDrillMultiplierFuel = (float)oreDrillMultiplierFuel_V.get().doubleValue();
		oreDrillFuelValueFuel = (float)oreDrillFuelValueFuel_V.get().doubleValue();
		mininglevel_coal = mininglevel_coal_V.get();
		mininglevel_common_metal = mininglevel_common_metal_V.get();
		mininglevel_rare_metal = mininglevel_rare_metal_V.get();
		mininglevel_shiny_metal = mininglevel_shiny_metal_V.get();
		mininglevel_uranium = mininglevel_uranium_V.get();
		mininglevel_common_gem = mininglevel_common_gem_V.get();
		mininglevel_shiny_gem = mininglevel_shiny_gem_V.get();
		mininglevel_nether_crystal = mininglevel_nether_crystal_V.get();
		mininglevel_oil = mininglevel_oil_V.get();
		oremult_coal = oremult_coal_V.get();
		oremult_common_metal = oremult_common_metal_V.get();
		oremult_rare_metal = oremult_rare_metal_V.get();
		oremult_shiny_metal = oremult_shiny_metal_V.get();
		oremult_uranium = oremult_uranium_V.get();
		oremult_common_gem = oremult_common_gem_V.get();
		oremult_shiny_gem = oremult_shiny_gem_V.get();
		oremult_nether_crystal = oremult_nether_crystal_V.get();
		oremult_oil = oremult_oil_V.get();
		powermult_coal = powermult_coal_V.get();
		powermult_common_metal = powermult_common_metal_V.get();
		powermult_rare_metal = powermult_rare_metal_V.get();
		powermult_shiny_metal = powermult_shiny_metal_V.get();
		powermult_uranium = powermult_uranium_V.get();
		powermult_common_gem = powermult_common_gem_V.get();
		powermult_shiny_gem = powermult_shiny_gem_V.get();
		powermult_nether_crystal = powermult_nether_crystal_V.get();
		powermult_oil = powermult_oil_V.get();
	}

	public static void bakeClient() {
		cl_enableDeathFX = cl_enableDeathFX_V.get();
		cl_enableDeathFX_Gore = cl_enableDeathFX_Gore_V.get();
		cl_lockSpeedFov = cl_lockSpeedFov_V.get();
		cl_fixedSprintFov = (float)cl_fixedSprintFov_V.get().doubleValue();
		cl_sortPassesPerTick = cl_sortPassesPerTick_V.get();
	}

	@SubscribeEvent
	public static void onLoad(ModConfigEvent event) {
		if (event.getConfig().getSpec() == COMMON_SPEC) {
			bakeCommon();
		} else if (event.getConfig().getSpec() == CLIENT_SPEC) {
			bakeClient();
		}
	}
}
