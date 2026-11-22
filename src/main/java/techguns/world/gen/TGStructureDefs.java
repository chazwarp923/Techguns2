package techguns.world.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import techguns.TGBlocks;
import techguns.TGEntities;
import techguns.TGuns;
import techguns.blocks.BlockTGSpawner;
import techguns.blocks.OreClusterType;

/**
 * The 1.12 scanned structures with their palettes (1.12 world/structures/*.java static blocks). Palette order must match
 * the indices in the structure files. Built on first use, after registries are frozen.
 */
public final class TGStructureDefs {

	private static Map<String, ScannedStructure> defs;

	private TGStructureDefs() {
	}

	public static ScannedStructure get(String name) {
		return all().get(name);
	}

	public static synchronized Map<String, ScannedStructure> all() {
		if (defs == null) {
			Map<String, ScannedStructure> m = new HashMap<>();
			for (ScannedStructure s : build()) {
				m.put(s.name, s);
			}
			defs = m;
		}
		return defs;
	}

	/* ------------------------------------------------------------------ palette helpers */

	private static MBlock v(String name, int meta) {
		return new MBlock(LegacyBlocks.vanilla(name, meta));
	}

	private static MBlock tg(String name, int meta) {
		return new MBlock(LegacyBlocks.tg(name, meta));
	}

	private static MBlock s(BlockState state) {
		return new MBlock(state);
	}

	private static final MBlock AIR = new MBlock(Blocks.AIR.defaultBlockState());

	private static MBlock air() {
		return AIR;
	}

	private static MBlock multi(MBlock[] blocks, int... weights) {
		return new MBlock.Multi(blocks, weights);
	}

	private static MBlock[] arr(MBlock... b) {
		return b;
	}

	private static MBlock chest(int meta, String loot) {
		return new MBlock.Chest(LegacyBlocks.vanilla("chest", meta), loot);
	}

	private static MBlock.Spawner spawner(BlockTGSpawner.Type type, int left, int active, int delay, int range) {
		return new MBlock.Spawner(type, left, active, delay, range);
	}

	private static MBlock lamp(Direction d) {
		return tg("lamp0", d.get3DDataValue());
	}

	private static MBlock stairs(BlockState stairs, Direction facing, Half half) {
		return s(stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.HALF, half));
	}

	private static MBlock bunkerDoor(Direction facing, DoorHingeSide hinge, DoubleBlockHalf half) {
		return s(TGBlocks.BUNKER_DOOR.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
				.setValue(BlockStateProperties.DOOR_HINGE, hinge).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, half));
	}

	private static BlockState cluster(OreClusterType t) {
		return TGBlocks.ORE_CLUSTERS.get(t).get().defaultBlockState();
	}

	/** 1.12 ore lists of the ore cluster structures, one per cluster type */
	private static MBlock[] ores(boolean nether) {
		List<MBlock> o = new ArrayList<>(List.of(v("coal_ore", 0),
				multi(arr(v("iron_ore", 0), tg("basicore", 0), tg("basicore", 1)), 1, 1, 1),
				multi(arr(v("redstone_ore", 0), v("lapis_ore", 0)), 1, 1),
				tg("basicore", 2),
				multi(arr(v("gold_ore", 0), tg("basicore", 3)), 1, 1),
				multi(arr(v("diamond_ore", 0), v("emerald_ore", 0), v("stone", 0)), 2, 1, 3),
				multi(arr(tg("basicore", 4), v("stone", 0)), 1, 1)));
		if (nether) {
			o.add(multi(arr(v("netherrack", 0), v("quartz_ore", 0), v("glowstone", 0)), 2, 1, 1));
		}
		return o.toArray(new MBlock[0]);
	}

	/** cluster types in the order of the 1.12 type arrays (coal, common metal, common gem, rare metal, shiny metal, shiny gem, uranium[, nether]) */
	private static MBlock[] clusters(boolean nether) {
		List<OreClusterType> t = new ArrayList<>(List.of(OreClusterType.COAL, OreClusterType.COMMON_METAL, OreClusterType.COMMON_GEM,
				OreClusterType.RARE_METAL, OreClusterType.SHINY_METAL, OreClusterType.SHINY_GEM, OreClusterType.URANIUM));
		if (nether) {
			t.add(OreClusterType.NETHER_CRYSTAL);
		}
		return t.stream().map(c -> s(cluster(c))).toArray(MBlock[]::new);
	}

	private static Supplier<ItemStack> boltAction() {
		return () -> new ItemStack(TGuns.BOLTACTION.get());
	}

	private static <T extends EntityType<?>> Supplier<T> e(Supplier<T> t) {
		return t;
	}

	/* ------------------------------------------------------------------ structures */

	private static List<ScannedStructure> build() {
		List<ScannedStructure> list = new ArrayList<>();
		BlockState stoneBrickStairs = Blocks.STONE_BRICK_STAIRS.defaultBlockState();
		MBlock cobbleMossy = multi(arr(v("cobblestone", 0), v("mossy_cobblestone", 0)), 2, 1);
		MBlock stoneBricksCracked = multi(arr(v("stonebrick", 0), v("stonebrick", 1), v("stonebrick", 2), air()), 3, 3, 3, 2);

		/* ---- overworld small ---- */
		list.add(new ScannedStructure("factory_house_small", "factory_building_small", 9, 7, 11, List.of(
				tg("concrete", 2), tg("metalpanel", 7), v("brick_block", 0), v("glass_pane", 0), tg("concrete", 1), tg("metalpanel", 0),
				chest(5, "chests/factory_building"), air(), lamp(Direction.NORTH), lamp(Direction.WEST), v("hardened_clay", 0),
				v("crafting_table", 0), bunkerDoor(Direction.SOUTH, DoorHingeSide.LEFT, DoubleBlockHalf.LOWER),
				bunkerDoor(Direction.SOUTH, DoorHingeSide.LEFT, DoubleBlockHalf.UPPER), lamp(Direction.UP), lamp(Direction.SOUTH), v("furnace", 3),
				lamp(Direction.EAST), tg("ladder0", 0), v("iron_bars", 0), lamp(Direction.WEST),
				spawner(BlockTGSpawner.Type.HOLE, 5, 2, 150, 2).mob(e(TGEntities.ZOMBIE_MINER), 1)))
				.clean(7).foundation(3));

		list.add(new ScannedStructure("small_trainstation", "small_trainstation", 11, 6, 12, List.of(
				v("gravel", 0), multi(arr(v("rail", 0), air()), 2, 1), cobbleMossy,
				multi(arr(stairs(stoneBrickStairs, Direction.EAST, Half.BOTTOM), air()), 4, 1), v("stone", 0), v("brick_block", 0), air(),
				multi(arr(v("glass_pane", 4), air()), 1, 1), multi(arr(stairs(stoneBrickStairs, Direction.WEST, Half.TOP), air()), 4, 1),
				stoneBricksCracked, v("fence", 0), v("wooden_pressure_plate", 0), v("wooden_slab", 8), v("ladder", 3),
				chest(2, "chests/small_trainstation"), stairs(stoneBrickStairs, Direction.SOUTH, Half.TOP),
				stairs(stoneBrickStairs, Direction.NORTH, Half.TOP), v("furnace", 4), v("crafting_table", 0),
				multi(arr(stairs(stoneBrickStairs, Direction.EAST, Half.TOP), air()), 4, 1),
				multi(arr(stairs(stoneBrickStairs, Direction.WEST, Half.BOTTOM), air()), 4, 1),
				spawner(BlockTGSpawner.Type.HOLE, 3, 2, 200, 1).mob(e(TGEntities.ZOMBIE_MINER), 1)))
				.clean(7).foundation(1));

		list.add(new ScannedStructure("small_mine", "small_mine", 17, 11, 11, List.of(
				air(), v("stone", 0), v("log", 0),
				new MBlock.BiomeColor(LegacyBlocks.vanilla("grass", 0), LegacyBlocks.vanilla("snow", 0), LegacyBlocks.vanilla("sand", 0),
						LegacyBlocks.vanilla("netherrack", 0)),
				v("log", 8),
				multi(arr(new MBlock.BiomeColor(LegacyBlocks.vanilla("grass", 0), LegacyBlocks.vanilla("grass", 0), LegacyBlocks.vanilla("sand", 0),
						LegacyBlocks.vanilla("netherrack", 0)), air()), 4, 1),
				v("rail", 1), v("log", 4), v("stone_stairs", 1), v("rail", 3), v("torch", 3), v("torch", 4), v("torch", 2),
				new MBlock.OreClusterOre(ores(false), v("stone", 0), 0.75f),
				spawner(BlockTGSpawner.Type.HOLE, 2, 1, 200, 1).mob(e(TGEntities.ZOMBIE_MINER), 1),
				new MBlock.OreClusterType(clusters(false), 0.5f, ores(false)), new MBlock.OreClusterType(clusters(false), 0, null)))
				.clean(4).yOffset(-5).indexRoll(10, 10, 10, 5, 2, 2, 2));

		list.add(new ScannedStructure("gas_station", "gasstation", 9, 7, 12, List.of(
				tg("concrete", 3), air(), air(), v("quartz_block", 0), v("lever", 3), v("quartz_stairs", 1), tg("lamp0", 11), v("stonebrick", 0),
				v("stone", 6), v("brick_block", 0), v("wooden_door", 2), v("stone", 6), v("glass_pane", 0), v("wooden_door", 8), v("stone_slab", 0),
				v("wooden_slab", 8), tg("lamp0", 7), v("double_stone_slab", 0), v("stone_slab", 8),
				spawner(BlockTGSpawner.Type.HOLE, 3, 2, 200, 1).mob(e(TGEntities.ZOMBIE_SOLDIER), 1).mob(e(TGEntities.ZOMBIE_FARMER), 1)
						.mob(e(TGEntities.ZOMBIE_MINER), 1),
				v("trapdoor", 10), v("trapdoor", 14), v("planks", 0), chest(4, "chests/gasstation")))
				.clean(7).foundation(3));

		/* ---- desert oil cluster: 1.12 only spawned it when a mod added a worldspawn oil fluid ---- */
		BlockState oilState = WorldspawnOil.block();
		if (oilState != null) {
			MBlock oil = s(oilState);
			MBlock[] oilCluster = { s(cluster(OreClusterType.OIL)) };
			list.add(new ScannedStructure("desert_oil_cluster", "desert_oil_cluster", 11, 10, 11, List.of(
					v("sandstone", 0), tg("sandbags", 0), air(),
					spawner(BlockTGSpawner.Type.SOLDIER_SPAWN, 3, 1, 200, 1).mob(e(TGEntities.ARMY_SOLDIER), 4).mob(e(TGEntities.COMMANDO), 1),
					multi(arr(oil, v("sand", 0)), 1, 1),
					new MBlock.OreClusterType(oilCluster, 0.5f, new MBlock[] { oil }), oil,
					new MBlock.OreClusterType(oilCluster, 0, null), oil))
					.clean(6).yOffset(-4));
		}

		/* ---- overworld medium ---- */
		list.add(new ScannedStructure("police_station", "policestation", 13, 8, 13, List.of(
				tg("concrete", 0), v("iron_block", 0), v("iron_bars", 0), air(), tg("sandbags", 0), tg("lamp0", 11), v("stonebrick", 0),
				v("concrete", 11), tg("bunker_door", 2), tg("bunker_door", 9), v("glass_pane", 0), v("planks", 0), chest(2, "chests/policestation"),
				v("stone", 6), spawner(BlockTGSpawner.Type.SOLDIER_SPAWN, 3, 1, 200, 1).mob(e(TGEntities.ZOMBIE_POLICEMAN), 1), tg("lamp0", 7),
				v("rail", 6), v("rail", 9), tg("ladder0", 4), v("rail", 7), v("rail", 8), v("stone_slab", 8), chest(3, "chests/policestation"),
				v("crafting_table", 0), v("fence_gate", 0), tg("bunker_door", 1), tg("bunker_door", 8)))
				.clean(7).foundation(3));

		list.add(new ScannedStructure("survivor_hideout", "survivor_hideout", 11, 11, 19, List.of(
				v("dirt", 1), v("fence", 0), air(), v("iron_block", 0), tg("sandbags", 0), v("iron_block", 0), tg("lamp0", 12),
				chest(2, "chests/survivor_hideout"), v("stonebrick", 0), spawner(BlockTGSpawner.Type.HOLE, 3, 2, 200, 1).mob(e(TGEntities.BANDIT), 1),
				v("planks", 0), chest(4, "chests/survivor_hideout"), v("oak_stairs", 0),
				new MBlock.IndexRoll(tg("metalpanel", 0), tg("metalpanel", 1), tg("metalpanel", 2), tg("metalpanel", 3)), v("planks", 1),
				v("stone_slab", 3), MBlock.camoNetTop(), v("bed", 10), v("bed", 2), v("torch", 1), v("crafting_table", 0),
				spawner(BlockTGSpawner.Type.HOLE, 2, 1, 200, 1).mob(e(TGEntities.BANDIT), 1).weapon(boltAction()), v("furnace", 3), tg("ladder0", 0),
				tg("simplemachine", 9), chest(3, "chests/survivor_hideout")))
				.clean(7).foundation(3).indexRollUniform(4));

		list.add(new ScannedStructure("orecluster_spike", "orecluster_spike", 8, 7, 8, List.of(
				air(), spawner(BlockTGSpawner.Type.HOLE, 4, 2, 200, 1).mob(e(TGEntities.ALIEN_BUG), 1), multi(arr(v("stone", 0), air()), 1, 1),
				tg("slimyladder", 4), tg("slimy", 0), v("stone", 0), new MBlock.OreClusterOre(ores(false), v("stone", 0), 0.5f), tg("slimyladder", 2),
				new MBlock.OreClusterType(clusters(false), 0.5f, ores(false)), tg("slimyladder", 3),
				new MBlock.OreClusterType(clusters(false), 0, null)))
				.clean(6).indexRoll(5, 10, 10, 10, 5, 2, 3));

		list.add(new ScannedStructure("orecluster_meteorbase", "orecluster_meteorbase", 17, 12, 17, List.of(
				v("stone", 0), v("gravel", 0), tg("sandbags", 0), v("iron_bars", 0), air(),
				spawner(BlockTGSpawner.Type.HOLE, 2, 1, 200, 1).mob(e(TGEntities.ARMY_SOLDIER), 4).mob(e(TGEntities.COMMANDO), 1), tg("metalpanel", 4),
				multi(arr(v("magma", 0), v("stone", 0)), 1, 1), tg("concrete", 5), tg("lamp0", 6), v("glass_pane", 15), tg("metalpanel", 7),
				tg("bunker_door", 1), tg("bunker_door", 8), tg("lamp0", 7), v("magma", 0),
				new MBlock.OreClusterType(clusters(true), 0.5f, ores(true)), tg("ladder0", 12), new MBlock.OreClusterType(clusters(true), 0, null),
				tg("ladder0", 0), air()))
				.clean(30).yOffset(-5).indexRoll(5, 5, 5, 5, 5, 5, 5, 15));

		/* ---- overworld big ---- */
		MBlock supplyCrates = multi(arr(tg("military_crate", 0), tg("military_crate", 1), tg("military_crate", 2), tg("military_crate", 3),
				tg("military_crate", 4), tg("military_crate", 5), tg("military_crate", 6), tg("military_crate", 7), tg("military_crate", 8)), 1, 1, 1, 1,
				1, 1, 1, 1, 1);
		MBlock supplyCratesChance = multi(arr(tg("military_crate", 0), tg("military_crate", 1), tg("military_crate", 2), tg("military_crate", 3),
				tg("military_crate", 4), tg("military_crate", 5), tg("military_crate", 6), tg("military_crate", 7), tg("military_crate", 8), air()), 1, 1,
				1, 1, 1, 1, 1, 1, 1, 9);
		String carrierLoot = "chests/aircraftcarrier";
		BlockState metalStairs = TGBlocks.STAIRS_METAL.get().defaultBlockState();
		list.add(new ScannedStructure("aircraft_carrier", "aircraft_carrier", 54, 24, 21, List.of(
				v("concrete", 4), v("concrete", 8), v("concrete", 7), v("hardened_clay", 0), tg("concrete", 3), v("crafting_table", 0),
				chest(5, carrierLoot), lamp(Direction.NORTH), lamp(Direction.WEST), chest(5, carrierLoot), air(), supplyCrates, supplyCrates,
				v("glass_pane", 0), tg("concrete", 4), lamp(Direction.SOUTH), lamp(Direction.EAST), tg("bunker_door", 2), tg("bunker_door", 9),
				tg("bunker_door", 8), tg("metalpanel", 4), v("glass", 0), v("iron_bars", 0), tg("metalpanel", 7), lamp(Direction.UP),
				tg("bunker_door", 0), tg("bunker_door", 1), tg("bunker_door", 3), chest(3, carrierLoot), chest(2, carrierLoot),
				stairs(metalStairs, Direction.WEST, Half.BOTTOM), stairs(metalStairs, Direction.EAST, Half.BOTTOM), tg("ladder0", 0), v("iron_block", 0),
				tg("metalpanel", 6), chest(3, carrierLoot), tg("metalpanel", 6), chest(4, carrierLoot),
				spawner(BlockTGSpawner.Type.HOLE, 6, 2, 150, 2).mob(e(TGEntities.ARMY_SOLDIER), 1).mob(e(TGEntities.COMMANDO), 1),
				spawner(BlockTGSpawner.Type.SOLDIER_SPAWN, 1, 1, 200, 0).mob(e(TGEntities.ATTACK_HELICOPTER), 1), supplyCratesChance))
				.water().heightDiff(2));

		/* ---- nether ---- */
		list.add(new ScannedStructure("nether_altar_small", "nether_altar_small", 11, 10, 11, List.of(
				tg("nethermetal", 0), air(), tg("nethermetal", 8), tg("nethermetal", 9), v("nether_brick_stairs", 0), v("nether_brick_fence", 0),
				tg("nethermetal", 7), v("nether_brick_stairs", 2), v("nether_brick_stairs", 3),
				spawner(BlockTGSpawner.Type.HOLE, 3, 2, 200, 1).mob(e(TGEntities.CYBER_DEMON), 1), v("nether_brick_stairs", 1)))
				.netherFoundation(16).yOffset(-2));

		list.add(new ScannedStructure("nether_altar_medium", "nether_altar_medium", 16, 9, 16, List.of(
				tg("nethermetal", 0), tg("nethermetal", 6), v("nether_brick_fence", 0), air(), tg("nethermetal", 2), tg("nethermetal", 8),
				tg("nethermetal", 1), tg("nethermetal", 7), tg("nethermetal", 9), v("nether_brick_stairs", 0),
				spawner(BlockTGSpawner.Type.HOLE, 3, 1, 200, 1).mob(e(TGEntities.CYBER_DEMON), 1), v("nether_brick_stairs", 2),
				v("nether_brick_stairs", 3), v("nether_brick", 0), v("nether_brick_stairs", 1)))
				.netherFoundation(16).yOffset(-1));

		list.add(new ScannedStructure("nether_soul_platform", "nether_soul_platform", 13, 10, 13, List.of(
				tg("nethermetal", 3), air(), tg("nethermetal", 4), v("skull", 1), tg("nethermetal", 5), v("soul_sand", 0), tg("nethermetal", 7),
				v("glowstone", 0), v("nether_brick_fence", 0), spawner(BlockTGSpawner.Type.HOLE, 3, 2, 200, 1).mob(e(TGEntities.GHASTLING), 1)))
				.netherFoundation(16).yOffset(-2));

		list.add(new ScannedStructure("nether_loot_01", "nether_loot_01", 6, 10, 6, List.of(
				v("netherrack", 0), tg("nethermetal", 3), air(), v("nether_brick_fence", 0), v("netherrack", 0), v("skull", 1),
				chest(5, "chests/factory_building"), spawner(BlockTGSpawner.Type.HOLE, 2, 1, 200, 1).mob(e(TGEntities.ZOMBIE_PIGMAN_SOLDIER), 1)))
				.netherFoundation(16).yOffset(1));

		MBlock acid = s(techguns.TGFluids.ACID_BLOCK.get().defaultBlockState());
		list.add(new ScannedStructure("nether_acid_hole", "nether_acid_hole", 9, 6, 9, List.of(
				v("netherrack", 0), air(), v("soul_sand", 0), multi(arr(acid, v("netherrack", 0)), 1, 1), acid))
				.netherFoundation(16).yOffset(-1));

		list.add(new ScannedStructure("nether_ghast_spawner", "nether_ghast_spawner", 10, 14, 10, List.of(
				v("netherrack", 0), air(), v("netherrack", 0), v("netherrack", 0), v("glowstone", 0), tg("nethermetal", 3), v("iron_bars", 0),
				v("skull", 1), v("soul_sand", 0), new MBlock.VanillaSpawner(e(TGEntities.GHASTLING)), v("netherrack", 0), v("netherrack", 0),
				chest(3, "chests/factory_building")))
				.netherFoundation(16).yOffset(-2));

		MBlock crystal = s(cluster(OreClusterType.NETHER_CRYSTAL));
		list.add(new ScannedStructure("nether_orecluster_small", "nether_orecluster_small", 3, 3, 3, List.of(
				v("magma", 0), air(), multi(arr(crystal, v("netherrack", 0)), 50, 50), crystal, multi(arr(crystal, air()), 50, 50)))
				.netherFoundation(2).yOffset(1));

		list.add(new ScannedStructure("nether_orecluster_castle", "nether_orecluster_castle", 11, 9, 11, List.of(
				tg("nethermetal", 0), tg("nethermetal", 7), v("nether_brick", 0), air(), v("nether_brick_fence", 0), tg("nethermetal", 6),
				spawner(BlockTGSpawner.Type.HOLE, 2, 1, 200, 0).mob(e(TGEntities.ZOMBIE_PIGMAN_SOLDIER), 1).weapon(boltAction()),
				multi(arr(crystal, air()), 40, 60), crystal))
				.netherFoundation(16).yOffset(-1));
		return list;
	}
}
