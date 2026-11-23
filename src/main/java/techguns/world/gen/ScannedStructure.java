package techguns.world.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import techguns.Techguns;

/**
 * A 1.12 scanned structure (WorldgenStructure subclasses using BlockUtils.placeScannedStructure), now a vanilla
 * structure template, {@code data/techguns/structure/scanned/<name>.nbt}, with the 1.12 rules for finding a spot and
 * placing it (clear above, foundation, two passes, rotation around the centre). Blocks that 1.12 chose at placement
 * (weighted random blocks, the rolled ore type, biome blocks) are structure blocks in data mode in the template; their
 * metadata names a rule set with {@link #marker}. The templates can be loaded and edited with structure blocks
 * (load {@code techguns:scanned/<name>}), and a datapack can replace them.
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
	/** rules for the template's data markers (structure blocks in data mode), by their metadata */
	final Map<String, MBlock> markers = new LinkedHashMap<>();
	private StructureTemplate compiledFrom;
	private List<Placed> compiled = List.of();
	private boolean warnedMissing;

	/** one template block: a fixed block (with its block entity data) or a marker's rule */
	record Placed(int x, int y, int z, MBlock block) {
	}

	public ScannedStructure(String name, int sizeX, int sizeY, int sizeZ) {
		this.name = name;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
	}

	/** the rule for the template's data markers with this metadata */
	public ScannedStructure marker(String key, MBlock rule) {
		markers.put(key, rule);
		return this;
	}

	public ResourceLocation templateId() {
		return Techguns.rl("scanned/" + name);
	}

	/** the template's blocks, read again when the template manager has a new one (datapack reload) */
	synchronized List<Placed> placements(StructureTemplateManager manager) {
		Optional<StructureTemplate> t = manager.get(templateId());
		if (t.isEmpty()) {
			if (!warnedMissing) {
				warnedMissing = true;
				Techguns.LOGGER.error("Missing structure template {}", templateId());
			}
			return List.of();
		}
		if (t.get() != compiledFrom) {
			compiled = compile(t.get());
			compiledFrom = t.get();
		}
		return compiled;
	}

	private List<Placed> compile(StructureTemplate template) {
		CompoundTag tag = template.save(new CompoundTag());
		ListTag pal = tag.contains("palettes", Tag.TAG_LIST) ? tag.getList("palettes", Tag.TAG_LIST).getList(0) : tag.getList("palette", Tag.TAG_COMPOUND);
		HolderGetter<Block> lookup = BuiltInRegistries.BLOCK.asLookup();
		List<BlockState> states = new ArrayList<>();
		for (int i = 0; i < pal.size(); i++) {
			states.add(NbtUtils.readBlockState(lookup, pal.getCompound(i)));
		}
		Map<BlockState, MBlock> plain = new HashMap<>();
		List<Placed> out = new ArrayList<>();
		ListTag list = tag.getList("blocks", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag b = list.getCompound(i);
			ListTag pos = b.getList("pos", Tag.TAG_INT);
			BlockState state = states.get(b.getInt("state"));
			CompoundTag nbt = b.contains("nbt", Tag.TAG_COMPOUND) ? b.getCompound("nbt") : null;
			MBlock mb;
			if (state.is(Blocks.STRUCTURE_BLOCK) && nbt != null && nbt.getString("mode").equals(StructureMode.DATA.name())) {
				String key = nbt.getString("metadata");
				mb = markers.get(key);
				if (mb == null) {
					Techguns.LOGGER.error("Structure template {}: no rule for the data marker \"{}\" at {} {} {}, left out", templateId(), key, pos.getInt(0),
							pos.getInt(1), pos.getInt(2));
					continue;
				}
			} else if (nbt != null) {
				mb = new MBlock.WithData(state, nbt);
			} else {
				mb = plain.computeIfAbsent(state, MBlock::new);
			}
			out.add(new Placed(pos.getInt(0), pos.getInt(1), pos.getInt(2), mb));
		}
		return List.copyOf(out);
	}

	/** data marker metadata in the template, for the tests */
	public List<String> templateMarkers(StructureTemplateManager manager) {
		List<String> out = new ArrayList<>();
		manager.get(templateId()).ifPresent(t -> {
			CompoundTag tag = t.save(new CompoundTag());
			for (Tag e : tag.getList("blocks", Tag.TAG_COMPOUND)) {
				CompoundTag b = (CompoundTag) e;
				if (b.contains("nbt", Tag.TAG_COMPOUND) && b.getCompound("nbt").getString("mode").equals(StructureMode.DATA.name())) {
					out.add(b.getCompound("nbt").getString("metadata"));
				}
			}
		});
		return out;
	}

	public java.util.Set<String> markerRules() {
		return markers.keySet();
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

	/** places the structure from its template; the 1.12 rules for clearing, foundation and the two passes */
	public void place(PlaceContext ctx, BlockPos origin, int direction) {
		List<Placed> bl = placements(ctx.level.getServer().getStructureManager());
		int posX = origin.getX(), posY = origin.getY(), posZ = origin.getZ();
		int centerX = sizeX / 2, centerZ = sizeZ / 2;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		int ax = posX + centerX, az = posZ + centerZ;
		if (cleanRange > 0) {
			for (Placed b : bl) {
				if (b.y() == 0) {
					for (int o = 1; o <= cleanRange; o++) {
						rotated(p, posX + b.x(), posY + o, posZ + b.z(), ax, az, direction);
						ctx.set(p, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		int baseY = posY + yOffset;
		if (foundationRange > 0) {
			for (Placed b : bl) {
				MBlock mb = b.block();
				if (b.y() != 0 || mb.getPass() != 0) {
					continue;
				}
				int solid = 0;
				for (int o = 1; o <= foundationRange; o++) {
					rotated(p, posX + b.x(), baseY - o, posZ + b.z(), ax, az, direction);
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
		for (int pass = 0; pass <= 1; pass++) {
			for (Placed b : bl) {
				if (b.block().getPass() == pass) {
					rotated(p, posX + b.x(), baseY + b.y(), posZ + b.z(), ax, az, direction);
					if (ctx.inside(p)) {
						b.block().place(ctx, p.immutable(), direction);
					}
				}
			}
		}
		ctx.finishShapes();
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
