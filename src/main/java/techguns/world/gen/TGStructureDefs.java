package techguns.world.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import techguns.TGBlocks;
import techguns.blocks.OreClusterType;

/**
 * The 1.12 scanned structures (1.12 world/structures/*.java): size and placement rules here, blocks in the structure
 * templates {@code data/techguns/structure/scanned/<name>.nbt}. The blocks 1.12 chose at placement are data markers in
 * the templates; each structure names their rules with {@link ScannedStructure#marker}. Built on first use, after
 * registries are frozen.
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

	/* ------------------------------------------------------------------ rule helpers */

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

	/** weighted random block; 1.12 rolls in [0, total weight], so the first block gets one more chance */
	private static MBlock multi(MBlock[] blocks, int... weights) {
		return new MBlock.Multi(blocks, weights);
	}

	private static MBlock[] arr(MBlock... b) {
		return b;
	}

	private static MBlock stairs(BlockState stairs, Direction facing, Half half) {
		return s(stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.HALF, half));
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

	/** the rolled type's ore cluster, or (oreChance) its ore; the structure's index roll picks the type */
	private static MBlock clusterOrOre(boolean nether, float oreChance) {
		return new MBlock.OreClusterType(clusters(nether), oreChance, oreChance > 0 ? ores(nether) : null);
	}

	/* ------------------------------------------------------------------ structures */

	private static List<ScannedStructure> build() {
		List<ScannedStructure> list = new ArrayList<>();
		BlockState stoneBrickStairs = Blocks.STONE_BRICK_STAIRS.defaultBlockState();

		/* ---- overworld small ---- */
		list.add(new ScannedStructure("factory_house_small", 9, 7, 11).clean(7).foundation(3));

		list.add(new ScannedStructure("small_trainstation", 11, 6, 12)
				.marker("rail_or_air", multi(arr(v("rail", 0), air()), 2, 1))
				.marker("cobble_or_mossy", multi(arr(v("cobblestone", 0), v("mossy_cobblestone", 0)), 2, 1))
				.marker("stone_bricks_or_cracked", multi(arr(v("stonebrick", 0), v("stonebrick", 1), v("stonebrick", 2), air()), 3, 3, 3, 2))
				.marker("glass_pane_or_air", multi(arr(v("glass_pane", 4), air()), 1, 1))
				.marker("stairs_east_or_air", multi(arr(stairs(stoneBrickStairs, Direction.EAST, Half.BOTTOM), air()), 4, 1))
				.marker("stairs_west_or_air", multi(arr(stairs(stoneBrickStairs, Direction.WEST, Half.BOTTOM), air()), 4, 1))
				.marker("stairs_east_top_or_air", multi(arr(stairs(stoneBrickStairs, Direction.EAST, Half.TOP), air()), 4, 1))
				.marker("stairs_west_top_or_air", multi(arr(stairs(stoneBrickStairs, Direction.WEST, Half.TOP), air()), 4, 1))
				.clean(7).foundation(1));

		list.add(new ScannedStructure("small_mine", 17, 11, 11)
				.marker("biome_ground", new MBlock.BiomeColor(LegacyBlocks.vanilla("grass", 0), LegacyBlocks.vanilla("snow", 0),
						LegacyBlocks.vanilla("sand", 0), LegacyBlocks.vanilla("netherrack", 0)))
				.marker("biome_grass_or_air", multi(arr(new MBlock.BiomeColor(LegacyBlocks.vanilla("grass", 0), LegacyBlocks.vanilla("grass", 0),
						LegacyBlocks.vanilla("sand", 0), LegacyBlocks.vanilla("netherrack", 0)), air()), 4, 1))
				.marker("ore_or_stone", new MBlock.OreClusterOre(ores(false), v("stone", 0), 0.75f))
				.marker("cluster_or_ore", clusterOrOre(false, 0.5f))
				.marker("cluster", clusterOrOre(false, 0))
				.clean(4).yOffset(-5).indexRoll(10, 10, 10, 5, 2, 2, 2));

		list.add(new ScannedStructure("gas_station", 9, 7, 12).clean(7).foundation(3));

		/* ---- desert oil cluster: 1.12 only spawned it when a mod added a worldspawn oil fluid ---- */
		BlockState oilState = WorldspawnOil.block();
		if (oilState != null) {
			MBlock oil = s(oilState);
			list.add(new ScannedStructure("desert_oil_cluster", 11, 10, 11)
					.marker("oil", oil)
					.marker("oil_or_sand", multi(arr(oil, v("sand", 0)), 1, 1))
					.marker("cluster_or_oil", new MBlock.OreClusterType(arr(s(cluster(OreClusterType.OIL))), 0.5f, arr(oil)))
					.clean(6).yOffset(-4));
		}

		/* ---- overworld medium ---- */
		list.add(new ScannedStructure("police_station", 13, 8, 13).clean(7).foundation(3));

		list.add(new ScannedStructure("survivor_hideout", 11, 11, 19)
				.marker("metal_panel", new MBlock.IndexRoll(tg("metalpanel", 0), tg("metalpanel", 1), tg("metalpanel", 2), tg("metalpanel", 3)))
				.marker("camo_net_top", MBlock.camoNetTop())
				.clean(7).foundation(3).indexRollUniform(4));

		list.add(new ScannedStructure("orecluster_spike", 8, 7, 8)
				.marker("stone_or_air", multi(arr(v("stone", 0), air()), 1, 1))
				.marker("ore_or_stone", new MBlock.OreClusterOre(ores(false), v("stone", 0), 0.5f))
				.marker("cluster_or_ore", clusterOrOre(false, 0.5f))
				.marker("cluster", clusterOrOre(false, 0))
				.clean(6).indexRoll(5, 10, 10, 10, 5, 2, 3));

		list.add(new ScannedStructure("orecluster_meteorbase", 17, 12, 17)
				.marker("magma_or_stone", multi(arr(v("magma", 0), v("stone", 0)), 1, 1))
				.marker("cluster_or_ore", clusterOrOre(true, 0.5f))
				.marker("cluster", clusterOrOre(true, 0))
				.clean(30).yOffset(-5).indexRoll(5, 5, 5, 5, 5, 5, 5, 15));

		/* ---- overworld big ---- */
		MBlock[] crates = new MBlock[10];
		for (int i = 0; i < 9; i++) {
			crates[i] = tg("military_crate", i);
		}
		crates[9] = air();
		list.add(new ScannedStructure("aircraft_carrier", 54, 24, 21)
				.marker("supply_crate", multi(java.util.Arrays.copyOf(crates, 9), 1, 1, 1, 1, 1, 1, 1, 1, 1))
				.marker("supply_crate_or_air", multi(crates, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9))
				.water().heightDiff(2));

		/* ---- nether ---- */
		list.add(new ScannedStructure("nether_altar_small", 11, 10, 11).netherFoundation(16).yOffset(-2));
		list.add(new ScannedStructure("nether_altar_medium", 16, 9, 16).netherFoundation(16).yOffset(-1));
		list.add(new ScannedStructure("nether_soul_platform", 13, 10, 13).netherFoundation(16).yOffset(-2));
		list.add(new ScannedStructure("nether_loot_01", 6, 10, 6).netherFoundation(16).yOffset(1));
		list.add(new ScannedStructure("nether_acid_hole", 9, 6, 9)
				.marker("acid_or_netherrack", multi(arr(s(techguns.TGFluids.ACID_BLOCK.get().defaultBlockState()), v("netherrack", 0)), 1, 1))
				.netherFoundation(16).yOffset(-1));
		list.add(new ScannedStructure("nether_ghast_spawner", 10, 14, 10).netherFoundation(16).yOffset(-2));

		MBlock crystal = s(cluster(OreClusterType.NETHER_CRYSTAL));
		list.add(new ScannedStructure("nether_orecluster_small", 3, 3, 3)
				.marker("crystal_or_netherrack", multi(arr(crystal, v("netherrack", 0)), 50, 50))
				.marker("crystal_or_air", multi(arr(crystal, air()), 50, 50))
				.netherFoundation(2).yOffset(1));
		list.add(new ScannedStructure("nether_orecluster_castle", 11, 9, 11)
				.marker("crystal_or_air", multi(arr(crystal, air()), 40, 60))
				.netherFoundation(16).yOffset(-1));
		return list;
	}
}
