package techguns.world.gen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import techguns.world.gen.PlaceContext.BiomeColorType;
import techguns.world.gen.procedural.AlienBugNestGen;
import techguns.world.gen.procedural.MilitaryCampGen;

/**
 * The code-built structures by name (structure JSON entries), and a small cache of their generated blocks: a structure
 * is generated when its start is created, then again for each chunk it covers unless it is still cached.
 */
public final class ProcStructures {

	private static final Map<String, ProcGenerator> GENERATORS = Map.of(
			"alien_bug_nest", new AlienBugNestGen(),
			"military_base", new MilitaryCampGen(),
			"castle", new techguns.world.gen.procedural.CastleGen());

	private static final int CACHE_SIZE = 12;
	private static final Map<String, GenWorld> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, GenWorld> eldest) {
			return size() > CACHE_SIZE;
		}
	};

	/** how far the ground levelling reaches above and below the layout height */
	public static final int FLATTEN_UP = 40, FLATTEN_DOWN = 24;

	private ProcStructures() {
	}

	public static ProcGenerator get(String name) {
		return GENERATORS.get(name);
	}

	public static GenWorld build(String name, ProcGenerator.Layout layout, BiomeColorType color, long seed, TerrainView terrain) {
		String key = name + "|" + seed + "|" + layout.origin().asLong();
		synchronized (CACHE) {
			GenWorld cached = CACHE.get(key);
			if (cached != null) {
				return cached;
			}
		}
		ProcGenerator gen = GENERATORS.get(name);
		RandomSource rnd = RandomSource.create(seed);
		GenWorld world = new GenWorld(rnd, p -> gen.solidFallback(p, layout, terrain));
		gen.generate(world, layout, color, rnd, terrain);
		synchronized (CACHE) {
			CACHE.put(key, world);
		}
		return world;
	}

	/** every block the structure may change, including the levelled ground */
	public static BoundingBox boundingBox(ProcGenerator gen, ProcGenerator.Layout layout, GenWorld world) {
		BoundingBox box = world.isEmpty() ? layout.footprint() : world.bounds();
		if (gen.flattenGround()) {
			BoundingBox fp = layout.footprint();
			int y = layout.origin().getY();
			box = BoundingBox.encapsulatingBoxes(java.util.List.of(box,
					new BoundingBox(fp.minX(), y - FLATTEN_DOWN, fp.minZ(), fp.maxX(), y + FLATTEN_UP, fp.maxZ()))).orElse(box);
		}
		return box;
	}

	private static boolean isGround(BlockState s) {
		return !s.isAir() && !s.canBeReplaced() && s.getFluidState().isEmpty() && !s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES);
	}

	/**
	 * Levels the real ground of the footprint inside this chunk: clears everything from the layout height up (trees,
	 * hills) and fills holes and water below it with the ground block found underneath, grass on top.
	 */
	public static void flatten(PlaceContext ctx, ProcGenerator.Layout layout) {
		BoundingBox fp = layout.footprint();
		int g = layout.origin().getY();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = Math.max(fp.minX(), ctx.box.minX()); x <= Math.min(fp.maxX(), ctx.box.maxX()); x++) {
			for (int z = Math.max(fp.minZ(), ctx.box.minZ()); z <= Math.min(fp.maxZ(), ctx.box.maxZ()); z++) {
				for (int y = g; y < g + FLATTEN_UP; y++) {
					if (!ctx.get(p.set(x, y, z)).isAir()) {
						ctx.set(p.immutable(), air);
					}
				}
				BlockState found = null;
				for (int y = g - 1; y > g - FLATTEN_DOWN; y--) {
					BlockState s = ctx.get(p.set(x, y, z));
					if (isGround(s)) {
						found = s;
						break;
					}
				}
				BlockState top = found == null || found.is(Blocks.GRASS_BLOCK) || found.is(Blocks.DIRT) ? Blocks.GRASS_BLOCK.defaultBlockState() : found;
				BlockState fill = top.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT.defaultBlockState() : top;
				for (int y = g - 1; y > g - FLATTEN_DOWN; y--) {
					BlockState s = ctx.get(p.set(x, y, z));
					if (isGround(s) && y < g - 1) {
						break;
					}
					if (!isGround(s) || y == g - 1) {
						ctx.set(p.immutable(), y == g - 1 ? top : fill);
					}
				}
			}
		}
	}
}
