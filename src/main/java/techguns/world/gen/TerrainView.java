package techguns.world.gen;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Terrain heights straight from the chunk generator (noise only, no features), so they are the same whenever and
 * wherever they are asked for: when a structure start is created and later when each of its chunks is placed.
 */
public class TerrainView {

	private final ChunkGenerator gen;
	private final LevelHeightAccessor heights;
	private final RandomState randomState;
	private final Map<Long, int[]> cache = new HashMap<>();

	public TerrainView(ChunkGenerator gen, LevelHeightAccessor heights, RandomState randomState) {
		this.gen = gen;
		this.heights = heights;
		this.randomState = randomState;
	}

	/** {first free y above all blocks incl. water, first free y above solid ground} */
	private int[] column(int x, int z) {
		return cache.computeIfAbsent(((long) x << 32) ^ (z & 0xffffffffL), k -> new int[] {
				gen.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState),
				gen.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState) });
	}

	/** first free y above the solid ground */
	public int ground(int x, int z) {
		return column(x, z)[1];
	}

	public boolean isWater(int x, int z) {
		int[] c = column(x, z);
		return c[0] > c[1];
	}

	public int minY() {
		return heights.getMinBuildHeight();
	}

	/**
	 * 1.12 BlockUtils.getValidSpawnYArea: average ground height over a grid of the area with this step, -1 if water is on
	 * top anywhere or the heights differ by more than the limit
	 */
	public int landHeight(int x, int z, int sizeX, int sizeZ, int step, int limit) {
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, count = 0;
		for (int ix = 0; ix <= sizeX; ix = next(ix, sizeX, step)) {
			for (int iz = 0; iz <= sizeZ; iz = next(iz, sizeZ, step)) {
				if (isWater(x + ix, z + iz)) {
					return -1;
				}
				int h = ground(x + ix, z + iz);
				min = Math.min(min, h);
				max = Math.max(max, h);
				sum += h;
				count++;
			}
		}
		if (count == 0 || max - min > limit) {
			return -1;
		}
		return sum / count;
	}

	private static int next(int i, int size, int step) {
		return i >= size ? size + 1 : Math.min(i + step, size);
	}
}
