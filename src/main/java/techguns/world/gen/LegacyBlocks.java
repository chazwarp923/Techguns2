package techguns.world.gen;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Dynamic;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import techguns.TGBlocks;
import techguns.Techguns;

/**
 * Translates 1.12 block + metadata pairs (as used by the structure palettes) into 1.21 block states.
 * <ul>
 * <li>vanilla blocks: Mojang's own flattening table ({@link BlockStateData}, old numeric id = blockId &lt;&lt; 4 | meta)
 * and the block state datafixer up to the current version</li>
 * <li>Techguns blocks: the 1.12 meta encodings of the meta blocks, mapped onto the split 1.21 blocks</li>
 * </ul>
 */
public final class LegacyBlocks {

	/** data version of the flattened states BlockStateData produces (17w47a) */
	private static final int FLATTENING_VERSION = 1451;
	private static final Map<String, BlockState> CACHE = new HashMap<>();
	private static Object2IntMap<String> idByOldName;

	private LegacyBlocks() {
	}

	/** 1.12 vanilla block, name without or with "minecraft:" */
	public static synchronized BlockState vanilla(String name, int meta) {
		String full = name.contains(":") ? name : "minecraft:" + name;
		String key = full + "@" + meta;
		BlockState s = CACHE.get(key);
		if (s == null) {
			s = convertVanilla(full, meta);
			CACHE.put(key, s);
		}
		return s;
	}

	@SuppressWarnings("unchecked")
	private static BlockState convertVanilla(String name, int meta) {
		if (name.equals("minecraft:skull")) {
			// the flattening table can't convert skulls (1.12 kept the skull type in the block entity): skeleton skull,
			// meta 1 on the floor, 2-5 on a wall facing north, south, west, east
			int f = meta & 7;
			if (f >= 2 && f <= 5) {
				return Blocks.SKELETON_WALL_SKULL.defaultBlockState().setValue(net.minecraft.world.level.block.WallSkullBlock.FACING,
						net.minecraft.core.Direction.from3DDataValue(f));
			}
			return Blocks.SKELETON_SKULL.defaultBlockState();
		}
		try {
			if (idByOldName == null) {
				Field f = BlockStateData.class.getDeclaredField("ID_BY_OLD_NAME");
				f.setAccessible(true);
				idByOldName = (Object2IntMap<String>) f.get(null);
			}
			int base = idByOldName.getInt(name);
			if (base < 0) {
				Techguns.LOGGER.warn("Unknown 1.12 block {} in a structure palette", name);
				return Blocks.AIR.defaultBlockState();
			}
			int id = (base & ~15) | (meta & 15);
			Dynamic<Tag> flat = (Dynamic<Tag>) BlockStateData.getTag(id);
			Dynamic<Tag> current = DataFixers.getDataFixer().update(References.BLOCK_STATE, flat, FLATTENING_VERSION,
					SharedConstants.getCurrentVersion().getDataVersion().getVersion());
			return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), (CompoundTag) current.getValue());
		} catch (ReflectiveOperationException | RuntimeException e) {
			Techguns.LOGGER.error("Could not convert 1.12 block {}:{}", name, meta, e);
			return Blocks.STONE.defaultBlockState();
		}
	}

	/** a 1.21 block with the properties (facing, half, hinge...) of a converted 1.12 vanilla block */
	public static BlockState like(String vanillaName, int meta, Block target) {
		BlockState src = vanilla(vanillaName, meta);
		BlockState out = target.defaultBlockState();
		for (Property<?> p : src.getProperties()) {
			Property<?> tp = target.getStateDefinition().getProperty(p.getName());
			if (tp != null) {
				out = copy(out, tp, valueName(p, src));
			}
		}
		return out;
	}

	private static <T extends Comparable<T>> String valueName(Property<T> p, BlockState s) {
		return p.getName(s.getValue(p));
	}

	private static <T extends Comparable<T>> BlockState copy(BlockState s, Property<T> p, String value) {
		return p.getValue(value).map(v -> s.setValue(p, v)).orElse(s);
	}

	private static Block block(String name) {
		return BuiltInRegistries.BLOCK.get(Techguns.rl(name));
	}

	private static BlockState facing(Block b, Direction d) {
		BlockState s = b.defaultBlockState();
		if (s.hasProperty(BlockStateProperties.FACING)) {
			return s.setValue(BlockStateProperties.FACING, d);
		}
		if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && d.getAxis().isHorizontal()) {
			return s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
		}
		return s;
	}

	private static final String[] CONCRETE = { "concrete_brown", "concrete_brown_light", "concrete_grey", "concrete_grey_dark", "concrete_brown_pipes",
			"concrete_brown_light_scaff" };
	private static final String[] METAL_PANEL = { "container_red", "container_green", "container_blue", "container_orange", "panel_large_border",
			"steelframe_blue", "steelframe_dark", "steelframe_scaffold" };
	private static final String[] NETHER_METAL = { "panel", "grate1", "grate2", "grey_dark", "grey", "grey_tiles", "border_red", "plate_black",
			"plate_red", "border_lava" };
	private static final String[] LADDERS = { "ladder_metal", "ladder_shiny", "ladder_rusty", "ladder_carbon" };
	private static final String[] CRATES = { "ammo", "gun", "armor", "medical", "explosive", "generic_oak", "generic_jungle", "generic_birch",
			"generic_spruce" };
	private static final String[] NEON = { "neontubes2", "neontubes2_rotated", "neontubes4", "neontubes4_rotated", "neonsquare_white" };
	private static final String[] ORE_CLUSTERS = { "coal", "common_metal", "rare_metal", "shiny_metal", "uranium", "common_gem", "shiny_gem",
			"nether_crystal", "oil" };

	/** 1.12 Techguns block (registry name without "techguns:") */
	public static synchronized BlockState tg(String name, int meta) {
		// no computeIfAbsent: some conversions recurse into vanilla(), which writes the same cache
		String key = "techguns:" + name + "@" + meta;
		BlockState s = CACHE.get(key);
		if (s == null) {
			s = convertTG(name, meta);
			CACHE.put(key, s);
		}
		return s;
	}

	private static BlockState convertTG(String name, int meta) {
		switch (name) {
			case "concrete":
				return block(CONCRETE[meta % CONCRETE.length]).defaultBlockState();
			case "metalpanel":
				return block(METAL_PANEL[meta % METAL_PANEL.length]).defaultBlockState();
			case "nethermetal":
				return block("nethermetal_" + NETHER_METAL[meta % NETHER_METAL.length]).defaultBlockState();
			case "neonlights":
				return block(NEON[meta % NEON.length]).defaultBlockState();
			case "basicore":
				return switch (meta) {
					case 0 -> Blocks.COPPER_ORE.defaultBlockState();
					case 1 -> TGBlocks.ORE_TIN.get().defaultBlockState();
					case 2 -> TGBlocks.ORE_LEAD.get().defaultBlockState();
					case 3 -> TGBlocks.ORE_TITANIUM.get().defaultBlockState();
					default -> TGBlocks.ORE_URANIUM.get().defaultBlockState();
				};
			case "lamp0":
				// 0-5 yellow lamp facing, 6-11 white lamp facing, 12/13 lanterns
				if (meta < 6) {
					return facing(TGBlocks.LAMP_YELLOW.get(), Direction.from3DDataValue(meta));
				} else if (meta < 12) {
					return facing(TGBlocks.LAMP_WHITE.get(), Direction.from3DDataValue(meta - 6));
				}
				return (meta == 12 ? TGBlocks.LANTERN_YELLOW : TGBlocks.LANTERN_WHITE).get().defaultBlockState();
			case "ladder0":
				// horizontal index << 2 | type
				return facing(block(LADDERS[meta & 3]), Direction.from2DDataValue(meta >> 2));
			case "military_crate":
				return block("military_crate_" + CRATES[meta % CRATES.length]).defaultBlockState();
			case "sandbags":
				return TGBlocks.SANDBAGS.get().defaultBlockState();
			case "bunker_door":
				return like("wooden_door", meta, block("bunker_door"));
			case "slimyladder":
				return like("ladder", meta, TGBlocks.SLIMY_LADDER.get());
			case "stairs_metal":
				return like("stone_stairs", meta, TGBlocks.STAIRS_METAL.get());
			case "sand_hard":
				return TGBlocks.BUGNEST_SAND.get().defaultBlockState();
			case "slimy":
				return TGBlocks.BUGNEST_EGGS.get().defaultBlockState();
			case "orecluster":
				return block("ore_cluster_" + ORE_CLUSTERS[meta % ORE_CLUSTERS.length]).defaultBlockState();
			case "simplemachine": {
				// horizontal index << 2 | type: camo bench, repair bench, charging station, blast furnace
				String[] machines = { "camo_bench", "repair_bench", "charging_station", "blast_furnace" };
				return facing(block(machines[meta & 3]), Direction.from2DDataValue(meta >> 2));
			}
			case "camonet_top":
				return TGBlocks.CAMONET_TOP.get().defaultBlockState();
			case "camonet":
				return TGBlocks.CAMONET.get().defaultBlockState();
			case "debugblock":
				// 1.12 template scanner markers, never meant to be placed
				return Blocks.AIR.defaultBlockState();
			default:
				Techguns.LOGGER.warn("Unknown 1.12 Techguns block {} in a structure palette", name);
				return Blocks.STONE.defaultBlockState();
		}
	}

	/** checks the translation table resolves to real blocks (no air from unknown registry names) */
	static boolean isKnown(ResourceLocation id) {
		return BuiltInRegistries.BLOCK.containsKey(id);
	}
}
