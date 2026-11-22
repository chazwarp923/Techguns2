package techguns.world.gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import techguns.Techguns;

/**
 * A 1.12 scanned structure (WorldgenStructure subclasses using BlockUtils.placeScannedStructure): the block list file in
 * assets/techguns/structures plus its palette and the placement steps (clear above, foundation, two passes).
 */
public class ScannedStructure {

	public enum Ground {
		/** on land, max height difference over the footprint */
		LAND,
		/** floating on water, 1.12 AircraftCarrier */
		WATER,
		/** nether air pocket, 1.12 spawnStructureCaveWorldgen */
		CAVE
	}

	public static final int NETHER_MIN_Y = 20;
	public static final int NETHER_MAX_Y = 100;

	public final String name;
	final String file;
	final List<MBlock> palette;
	public final int sizeX, sizeY, sizeZ;
	int cleanRange = 0;
	int foundationRange = 0;
	boolean netherFoundation = false;
	int yOffset = 0;
	/** offset of the base below the found ground height (1.12: setBlocks(y-1), carrier y-3) */
	int baseOffset = -1;
	int[] indexWeights = null;
	int indexUniform = 0;
	int heightDiffLimit = 3;
	Ground ground = Ground.LAND;
	private short[][] blocks;

	public ScannedStructure(String name, String file, int sizeX, int sizeY, int sizeZ, List<MBlock> palette) {
		this.name = name;
		this.file = file;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.palette = palette;
	}

	public ScannedStructure clean(int range) {
		this.cleanRange = range;
		return this;
	}

	public ScannedStructure foundation(int range) {
		this.foundationRange = range;
		return this;
	}

	public ScannedStructure netherFoundation(int range) {
		this.foundationRange = range;
		this.netherFoundation = true;
		this.ground = Ground.CAVE;
		return this;
	}

	public ScannedStructure yOffset(int y) {
		this.yOffset = y;
		return this;
	}

	public ScannedStructure indexRoll(int... weights) {
		this.indexWeights = weights;
		return this;
	}

	public ScannedStructure indexRollUniform(int count) {
		this.indexUniform = count;
		return this;
	}

	public ScannedStructure water() {
		this.ground = Ground.WATER;
		this.baseOffset = -3;
		return this;
	}

	public ScannedStructure heightDiff(int limit) {
		this.heightDiffLimit = limit;
		return this;
	}

	/* ------------------------------------------------------------------ file */

	private synchronized short[][] blocks() {
		if (blocks == null) {
			blocks = load(file);
		}
		return blocks;
	}

	/** 1.12 BlockUtils.loadStructureFromFile: first line count, then x,y,z,paletteIndex per line */
	static short[][] load(String file) {
		String path = "/assets/techguns/structures/" + file;
		try (InputStream in = ScannedStructure.class.getResourceAsStream(path)) {
			if (in == null) {
				Techguns.LOGGER.error("Missing structure file {}", path);
				return new short[0][4];
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			int count = Integer.parseInt(br.readLine().trim());
			short[][] out = new short[count][4];
			String line;
			int i = 0;
			while ((line = br.readLine()) != null && i < count) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				String[] s = line.split(",");
				for (int j = 0; j < s.length && j < 4; j++) {
					out[i][j] = Short.parseShort(s[j].trim());
				}
				i++;
			}
			return out;
		} catch (IOException | RuntimeException e) {
			Techguns.LOGGER.error("Could not load structure file {}", path, e);
			return new short[0][4];
		}
	}

	/* ------------------------------------------------------------------ choosing a spot */

	public int rollIndex(RandomSource rnd) {
		if (indexWeights != null) {
			int total = 0;
			for (int w : indexWeights) {
				total += w;
			}
			int roll = rnd.nextInt(total + 1);
			int sum = 0;
			for (int i = 0; i < indexWeights.length; i++) {
				sum += indexWeights[i];
				if (roll <= sum) {
					return i;
				}
			}
			return indexWeights.length - 1;
		}
		return indexUniform > 0 ? rnd.nextInt(indexUniform) : 0;
	}

	/** where the structure goes in a chunk, or null; 1.12 spawnStructureWorldgen / spawnStructureCaveWorldgen */
	public BlockPos findOrigin(int chunkX, int chunkZ, int direction, ChunkGenerator gen, LevelHeightAccessor heights, RandomState rs) {
		if (ground == Ground.WATER) {
			int y = waterHeight(chunkX * 16, chunkZ * 16, gen, heights, rs);
			return y < 0 ? null : new BlockPos(chunkX * 16, y + baseOffset, chunkZ * 16);
		}
		int sizeXr = direction == 0 || direction == 2 ? sizeX : sizeZ;
		int sizeZr = direction == 0 || direction == 2 ? sizeZ : sizeX;
		int cx = sizeX / 2, cz = sizeZ / 2;
		int[][] corners = { rotatePoint(0, 0, direction, cx, cz), rotatePoint(sizeX, 0, direction, cx, cz), rotatePoint(0, sizeZ, direction, cx, cz),
				rotatePoint(sizeX, sizeZ, direction, cx, cz) };
		int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		for (int[] c : corners) {
			minX = Math.min(minX, c[0]);
			minZ = Math.min(minZ, c[1]);
		}
		int x = minX + chunkX * 16;
		int z = minZ + chunkZ * 16;
		int y = ground == Ground.CAVE ? caveHeight(chunkX, chunkZ, 10, gen, heights, rs) : landHeight(x, z, sizeXr, sizeZr, gen, heights, rs);
		return y < 0 ? null : new BlockPos(x, y + baseOffset, z);
	}

	public static int[] rotatePoint(int x, int z, int direction, int centerX, int centerZ) {
		int a = x - centerX;
		int b = z - centerZ;
		for (int i = 0; i < direction; i++) {
			int a2 = a;
			a = b;
			b = -a2;
		}
		return new int[] { a + centerX, b + centerZ };
	}

	/** 1.12 getValidSpawnYArea: average land height over a grid of the footprint, no water on top, flat enough */
	private int landHeight(int x, int z, int sx, int sz, ChunkGenerator gen, LevelHeightAccessor h, RandomState rs) {
		int step = 4;
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, count = 0;
		for (int ix = 0; ix <= sx; ix = next(ix, sx, step)) {
			for (int iz = 0; iz <= sz; iz = next(iz, sz, step)) {
				int surface = gen.getFirstFreeHeight(x + ix, z + iz, Heightmap.Types.WORLD_SURFACE_WG, h, rs);
				int floor = gen.getFirstFreeHeight(x + ix, z + iz, Heightmap.Types.OCEAN_FLOOR_WG, h, rs);
				if (surface > floor) {
					return -1; // liquid on top
				}
				min = Math.min(min, floor);
				max = Math.max(max, floor);
				sum += floor;
				count++;
			}
		}
		if (count == 0 || max - min > heightDiffLimit) {
			return -1;
		}
		return sum / count;
	}

	private static int next(int i, int size, int step) {
		if (i >= size) {
			return size + 1;
		}
		return Math.min(i + step, size);
	}

	/** 1.12 getValidSpawnYWater: water surface at the 4 corners, all water, flat */
	private int waterHeight(int x, int z, ChunkGenerator gen, LevelHeightAccessor h, RandomState rs) {
		int[] hs = new int[4];
		int[][] pts = { { x, z }, { x + sizeX, z }, { x, z + sizeZ }, { x + sizeX, z + sizeZ } };
		for (int i = 0; i < 4; i++) {
			int surface = gen.getFirstFreeHeight(pts[i][0], pts[i][1], Heightmap.Types.WORLD_SURFACE_WG, h, rs);
			int floor = gen.getFirstFreeHeight(pts[i][0], pts[i][1], Heightmap.Types.OCEAN_FLOOR_WG, h, rs);
			if (surface <= floor) {
				return -1; // no water
			}
			hs[i] = surface;
		}
		for (int i = 1; i < 4; i++) {
			if (Math.abs(hs[0] - hs[i]) >= heightDiffLimit) {
				return -1;
			}
		}
		java.util.Arrays.sort(hs);
		return (hs[1] + hs[2]) / 2;
	}

	/** 1.12 getCaveHeight: an air pocket of 10+ blocks above solid ground at the 4 chunk corners, level enough */
	private static int caveHeight(int cx, int cz, int airH, ChunkGenerator gen, LevelHeightAccessor h, RandomState rs) {
		int[] hs = { airHeight(cx * 16, cz * 16, airH, gen, h, rs), airHeight(cx * 16 + 15, cz * 16, airH, gen, h, rs),
				airHeight(cx * 16, cz * 16 + 15, airH, gen, h, rs), airHeight(cx * 16 + 15, cz * 16 + 15, airH, gen, h, rs) };
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0;
		for (int v : hs) {
			if (v < NETHER_MIN_Y || v > NETHER_MAX_Y) {
				return -1;
			}
			min = Math.min(min, v);
			max = Math.max(max, v);
			sum += v;
		}
		return max - min <= airH ? sum / 4 : -1;
	}

	private static int airHeight(int x, int z, int airH, ChunkGenerator gen, LevelHeightAccessor h, RandomState rs) {
		NoiseColumn col = gen.getBaseColumn(x, z, h, rs);
		int countAir = 0;
		for (int y = NETHER_MAX_Y; y > NETHER_MIN_Y; y--) {
			if (col.getBlock(y).isAir()) {
				countAir++;
			} else {
				if (countAir >= airH) {
					return y;
				}
				countAir = 0;
			}
		}
		return -1;
	}

	/** box of every block the placement may touch, around the rotation axis */
	public BoundingBox boundingBox(BlockPos origin) {
		int r = Math.max(sizeX, sizeZ) + 2;
		int ax = origin.getX() + sizeX / 2, az = origin.getZ() + sizeZ / 2;
		int below = Math.max(foundationRange, 1) + Math.max(-yOffset, 0) + 1;
		int above = Math.max(sizeY, 1) + Math.max(cleanRange, 0) + Math.max(yOffset, 0) + 2;
		return new BoundingBox(ax - r, origin.getY() - below, az - r, ax + r, origin.getY() + above, az + r);
	}

	/* ------------------------------------------------------------------ placement (1.12 setBlocks) */

	public void place(PlaceContext ctx, BlockPos origin, int direction) {
		short[][] bl = blocks();
		int posX = origin.getX(), posY = origin.getY(), posZ = origin.getZ();
		int centerX = sizeX / 2, centerZ = sizeZ / 2;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		int ax = posX + centerX, az = posZ + centerZ;
		if (cleanRange > 0) {
			BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
			for (short[] b : bl) {
				if (b[1] == 0) {
					for (int o = 1; o <= cleanRange; o++) {
						rotated(q, posX + b[0], posY + b[1] + o, posZ + b[2], ax, az, direction);
						ctx.set(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		int baseY = posY + yOffset;
		if (foundationRange > 0) {
			for (short[] b : bl) {
				if (b[1] != 0 || b[3] >= palette.size()) {
					continue;
				}
				MBlock mb = palette.get(b[3]);
				if (mb.getPass() != 0) {
					continue;
				}
				int solid = 0;
				for (int o = 1; o <= foundationRange; o++) {
					rotated(p, posX + b[0], baseY - o, posZ + b[2], ax, az, direction);
					if (netherFoundation) {
						if (!ctx.inside(p)) {
							continue;
						}
						if (ctx.get(p).canBeReplaced()) {
							mb.place(ctx, p.immutable(), direction);
							solid = 0;
						} else if (++solid >= 2) {
							break;
						}
					} else {
						mb.placeReplaceableOnly(ctx, p.immutable(), direction);
					}
				}
			}
		}
		pairChests(ctx, bl, posX, baseY, posZ, ax, az, direction);
		matchDoorHalves(ctx, bl, posX, baseY, posZ, ax, az, direction);
		for (int pass = 0; pass <= 1; pass++) {
			for (short[] b : bl) {
				if (b[3] >= palette.size()) {
					continue;
				}
				MBlock mb = palette.get(b[3]);
				if (mb.getPass() == pass) {
					rotated(p, posX + b[0], baseY + b[1], posZ + b[2], ax, az, direction);
					if (ctx.inside(p)) {
						mb.place(ctx, p.immutable(), direction);
					}
				}
			}
		}
		ctx.finishShapes();
	}

	/**
	 * Single chests side by side with the same facing become a double chest, as when a player places them. Worked out from
	 * the structure data, not the world, so a pair split over two chunks gets matching halves.
	 */
	private void pairChests(PlaceContext ctx, short[][] bl, int posX, int baseY, int posZ, int ax, int az, int direction) {
		java.util.Map<BlockPos, BlockState> chests = new java.util.LinkedHashMap<>();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (short[] b : bl) {
			if (b[3] < palette.size() && palette.get(b[3]) instanceof MBlock.Chest chest) {
				rotated(p, posX + b[0], baseY + b[1], posZ + b[2], ax, az, direction);
				chests.put(p.immutable(), chest.rotatedState(direction));
			}
		}
		for (java.util.Map.Entry<BlockPos, BlockState> e : chests.entrySet()) {
			BlockPos pos = e.getKey();
			BlockState s = e.getValue();
			if (ctx.chestTypes.containsKey(pos) || !s.hasProperty(ChestBlock.FACING)) {
				continue;
			}
			Direction facing = s.getValue(ChestBlock.FACING);
			BlockPos other = pos.relative(facing.getClockWise());
			BlockState o = chests.get(other);
			if (o != null && !ctx.chestTypes.containsKey(other) && o.hasProperty(ChestBlock.FACING) && o.getValue(ChestBlock.FACING) == facing) {
				ctx.chestTypes.put(pos, ChestType.LEFT);
				ctx.chestTypes.put(other, ChestType.RIGHT);
			}
		}
	}

	/**
	 * 1.12 doors kept the facing and open state only in the lower half's metadata and the hinge and powered state only in
	 * the upper half's, so each half converts with defaults for the rest. Every half takes the missing properties from its
	 * partner, as the vanilla world upgrade does.
	 */
	private void matchDoorHalves(PlaceContext ctx, short[][] bl, int posX, int baseY, int posZ, int ax, int az, int direction) {
		java.util.Map<BlockPos, BlockState> doors = new java.util.HashMap<>();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (short[] b : bl) {
			if (b[3] < palette.size()) {
				MBlock mb = palette.get(b[3]);
				if (mb.getClass() == MBlock.class && mb.state.getBlock() instanceof DoorBlock) {
					rotated(p, posX + b[0], baseY + b[1], posZ + b[2], ax, az, direction);
					doors.put(p.immutable(), mb.rotatedState(direction));
				}
			}
		}
		for (java.util.Map.Entry<BlockPos, BlockState> e : doors.entrySet()) {
			BlockPos pos = e.getKey();
			BlockState s = e.getValue();
			boolean upper = s.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
			BlockState other = doors.get(upper ? pos.below() : pos.above());
			if (other == null || other.getBlock() != s.getBlock() || other.getValue(DoorBlock.HALF) == s.getValue(DoorBlock.HALF)) {
				continue;
			}
			BlockState lower = upper ? other : s;
			BlockState top = upper ? s : other;
			ctx.stateOverrides.put(pos, s.setValue(DoorBlock.FACING, lower.getValue(DoorBlock.FACING)).setValue(DoorBlock.OPEN, lower.getValue(DoorBlock.OPEN))
					.setValue(DoorBlock.HINGE, top.getValue(DoorBlock.HINGE)).setValue(DoorBlock.POWERED, top.getValue(DoorBlock.POWERED)));
		}
	}

	/** 1.12 BlockUtils.rotateAroundY */
	private static void rotated(BlockPos.MutableBlockPos out, int x, int y, int z, int ax, int az, int steps) {
		int ox = x - ax, oz = z - az;
		for (int i = steps; i > 0; i--) {
			int tmp = ox;
			ox = oz;
			oz = -tmp;
		}
		out.set(ax + ox, y, az + oz);
	}
}
