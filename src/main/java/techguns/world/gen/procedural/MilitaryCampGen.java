package techguns.world.gen.procedural;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import techguns.TGEntities;
import techguns.blocks.BlockTGSpawner;
import techguns.world.gen.GenWorld;
import techguns.world.gen.LegacyBlocks;
import techguns.world.gen.MBlock;
import techguns.world.gen.PlaceContext.BiomeColorType;
import techguns.world.gen.ProcGenerator;
import techguns.world.gen.TerrainView;
import techguns.world.gen.procedural.CampParts.Part;

/**
 * 1.12 MilitaryBaseStructure / MilitaryCamp: a fenced camp split recursively into segments separated by roads, each
 * segment filled with a building, a helicopter spawner in the centre and soldier spawners on the roads.
 * <p>
 * Differences to 1.12: the ground is levelled completely (1.12 allowed 4 blocks of height difference and smoothed it),
 * vanilla's terrain adaptation blends the surroundings in.
 */
public class MilitaryCampGen implements ProcGenerator {

	static final int OUTER_FENCE_WIDTH = 1;
	static final int OUTER_FENCE_HEIGHT = 2;
	static final int OUTER_FENCE_SPACING = 1;
	static final int ROAD_MAX_WIDTH = 5;
	static final int ROAD_MIN_WIDTH = 1;
	static final int MIN_BORDER_SIZE = 30, MAX_BORDER_SIZE = 80;
	static final int MIN_CORNER_SIZE = 20, MAX_CORNER_SIZE = 60;
	static final int HEIGHT_DIFF_LIMIT = 5;
	static final int SAMPLE_STEP = 8;

	private final List<Part> structures = new ArrayList<>();
	private final List<Part> borderStructures = new ArrayList<>();
	private final List<Part> cornerStructures = new ArrayList<>();
	private final BlockState fence = Blocks.IRON_BARS.defaultBlockState();
	private final BlockState road = Blocks.GRAVEL.defaultBlockState();
	private final BlockState roadSecondary = LegacyBlocks.tg("concrete", 2);

	public MilitaryCampGen() {
		structures.add(new CampParts.Tent(5, 4, 5, 7, 5, 7, 0));
		structures.add(new CampParts.Tent(3, 4, 3, 6, 5, 6, 1));
		structures.add(new CampParts.Tent(4, 4, 4, 8, 6, 8, 2));
		structures.add(new CampParts.Containers(2, 3, 2, 7, 3, 7, 2, 2, LegacyBlocks.tg("metalpanel", 0)));
		structures.add(new CampParts.CampProps(1, 3, 1, 5, 5, 5, 0));
		structures.add(new CampParts.CampProps(3, 5, 3, 6, 7, 6, 1));
		structures.add(new CampParts.Bunker(5, 3, 5, 8, 3, 8, 0));
		structures.add(new CampParts.Barracks(7, 7, 7, -1, -1, -1));
		structures.add(new CampParts.Helipad(7, 7, 7, 10, -1, 10));
		structures.add(new CampParts.Tanks(4, 5, 4, 10, -1, 10));
		structures.add(new CampParts.CampProps(3, 5, 3, 10, 7, 10, 5));
		structures.add(new CampParts.Bunker(3, 3, 5, 15, 3, 15, 2).swapXZ());
		structures.add(new CampParts.Bunker(4, 3, 6, 15, 3, 15, 3).swapXZ());

		borderStructures.add(new CampParts.Bunker(5, 3, 5, 8, 3, 8, 0));
		borderStructures.add(new CampParts.Bunker(5, 4, 5, -1, 4, -1, 1));
		borderStructures.add(new CampParts.CampProps(3, 2, 3, 8, 2, 8, 2));
		borderStructures.add(new CampParts.CampProps(3, 4, 3, 8, 5, 8, 3));
		borderStructures.add(new CampParts.CampProps(1, 2, 3, -1, 4, -1, 4).swapXZ());
		borderStructures.add(new CampParts.Bunker(3, 3, 5, 10, 3, 10, 2).swapXZ());
		borderStructures.add(new CampParts.Bunker(4, 3, 6, 10, 3, 10, 3).swapXZ());

		cornerStructures.add(new CampParts.WatchTowerSmall(3, 8, 3, 4, 8, -1, 3).swapXZ());
		cornerStructures.add(new CampParts.WatchTowerSmall(4, 10, 4, -1, 12, -1, 4));
	}

	@Override
	public Layout layout(int chunkX, int chunkZ, RandomSource rnd, TerrainView terrain) {
		int x = chunkX * 16;
		int z = chunkZ * 16;
		int sizeX = 32 + rnd.nextInt(48);
		int sizeZ = 32 + rnd.nextInt(48);
		// 1.12: retry 16 blocks smaller while at least 32x32
		while (true) {
			int y = terrain.landHeight(x, z, sizeX, sizeZ, SAMPLE_STEP, HEIGHT_DIFF_LIMIT);
			if (y > 0) {
				return new Layout(new BlockPos(x, y, z), sizeX, 12, sizeZ);
			}
			if (sizeX >= 48 && sizeZ >= 48) {
				sizeX -= 16;
				sizeZ -= 16;
			} else {
				return null;
			}
		}
	}

	@Override
	public TerrainAdjustment terrainAdjustment() {
		return TerrainAdjustment.BEARD_THIN;
	}

	@Override
	public boolean flattenGround() {
		return true;
	}

	/** everything below the levelled ground is solid, everything above is free */
	@Override
	public boolean solidFallback(BlockPos pos, Layout layout, TerrainView terrain) {
		return pos.getY() < layout.origin().getY();
	}

	@Override
	public void generate(GenWorld w, Layout layout, BiomeColorType color, RandomSource rnd, TerrainView terrain) {
		new Camp(w, layout, color, rnd).build();
	}

	/* ------------------------------------------------------------------ 1.12 MilitaryCamp */

	private class Camp {
		final GenWorld w;
		final BiomeColorType color;
		final RandomSource rand;
		final int posX, posZ, sizeX, sizeZ;
		/** y of the ground block layer (1.12 getGroundY = world height - 1, the ground is level) */
		final int groundY;
		final double campRadiusSquared;
		float borderChanceX, borderChanceZ, cornerChance;
		int minSubSegmentSize = 7, maxSubSegmentSize = 15;
		Segment rootSegment;
		Segment flagSegment;
		final List<BlockPos> spawnPositions = new ArrayList<>();

		Camp(GenWorld w, Layout layout, BiomeColorType color, RandomSource rand) {
			this.w = w;
			this.color = color;
			this.rand = rand;
			this.posX = layout.origin().getX();
			this.posZ = layout.origin().getZ();
			this.groundY = layout.origin().getY() - 1;
			this.sizeX = layout.sizeX();
			this.sizeZ = layout.sizeZ();
			double hx = sizeX / 2.0, hz = sizeZ / 2.0;
			this.campRadiusSquared = hx * hx + hz * hz;
		}

		void build() {
			int x = posX + OUTER_FENCE_WIDTH + OUTER_FENCE_SPACING;
			int z = posZ + OUTER_FENCE_WIDTH + OUTER_FENCE_SPACING;
			int roadWidth = Math.max(ROAD_MIN_WIDTH, Math.min(ROAD_MAX_WIDTH, Math.max(sizeX, sizeZ) / 8));
			borderChanceX = (float) (sizeX - MIN_BORDER_SIZE) / (float) (MAX_BORDER_SIZE - MIN_BORDER_SIZE);
			borderChanceZ = (float) (sizeZ - MIN_BORDER_SIZE) / (float) (MAX_BORDER_SIZE - MIN_BORDER_SIZE);
			cornerChance = (float) (((sizeX + sizeZ) / 2) - MIN_CORNER_SIZE) / (float) (MAX_CORNER_SIZE - MIN_CORNER_SIZE);
			minSubSegmentSize = Math.min(sizeX, sizeZ) < 10 ? 5 : 7;
			maxSubSegmentSize = Math.min(sizeX, sizeZ) <= 45 ? 15 : Math.min(sizeX, sizeZ) / 3;
			int border = 2 * (OUTER_FENCE_WIDTH + OUTER_FENCE_SPACING);
			rootSegment = new Segment(x, z, sizeX - border, sizeZ - border, roadWidth, 0);
			rootSegment.isCenter = true;
			rootSegment.splitStep = 0;
			rootSegment.generateSubSegments();

			if (sizeX < border + 1 || sizeZ < border + 1) {
				return;
			}
			for (int k = 1; k < OUTER_FENCE_HEIGHT + 1; k++) {
				for (int i = 0; i < sizeX; i++) {
					w.set(posX + i, groundY + k, posZ, fence);
					w.set(posX + i, groundY + k, posZ + sizeZ - 1, fence);
				}
				for (int j = 1; j < sizeZ - 1; j++) {
					w.set(posX, groundY + k, posZ + j, fence);
					w.set(posX + sizeX - 1, groundY + k, posZ + j, fence);
				}
			}
			rootSegment.setBlocks();
			placeSpawners();
		}

		/** 1.12 initCampFlagEntity: helicopter spawner in the centre segment, soldier spawners on the roads */
		void placeSpawners() {
			if (flagSegment == null) {
				return;
			}
			int fx = flagSegment.x + flagSegment.sizeX / 2;
			int fz = flagSegment.z + flagSegment.sizeZ / 2;
			w.set(fx, groundY + 1, fz, new MBlock.Spawner(BlockTGSpawner.Type.SOLDIER_SPAWN, 1, 1, 200, 0).mob(TGEntities.ATTACK_HELICOPTER, 1)
					.heightOffset(64));
			MBlock soldiers = new MBlock.Spawner(BlockTGSpawner.Type.SOLDIER_SPAWN, 3, 1, 200, 0).mob(TGEntities.ARMY_SOLDIER, 1);
			for (BlockPos p : spawnPositions) {
				w.set(p.getX(), p.getY(), p.getZ(), soldiers);
			}
		}

		void setRoadBlocks(int x, int z, int roadWidth, int roadLength, boolean alignment) {
			for (int a = 0; a < roadLength; a++) {
				for (int b = 0; b < roadWidth; b++) {
					BlockState block = (roadWidth >= 4 && b > 0 && b < roadWidth - 1) ? roadSecondary : road;
					if (alignment) {
						w.set(x + a, groundY, z + b, block);
					} else {
						w.set(x + b, groundY, z + a, block);
					}
				}
			}
			int spawnX, spawnZ;
			if (alignment) {
				spawnX = x + roadLength / 2;
				spawnZ = z + roadWidth / 2;
			} else {
				spawnZ = z + roadLength / 2;
				spawnX = x + roadWidth / 2;
			}
			spawnPositions.add(new BlockPos(spawnX, groundY + 1, spawnZ));
		}

		/** gap in the outer fence where a main road leaves the camp */
		void setFenceGateBlocks(int x, int z, int roadWidth, int length, int height, boolean alignment) {
			for (int a = 0; a < length; a++) {
				for (int b = 0; b < roadWidth; b++) {
					for (int c = 0; c < height + 10; c++) {
						int bx = alignment ? x + a : x + b;
						int bz = alignment ? z + b : z + a;
						if (w.get(bx, groundY + c + 1, bz) == fence) {
							w.air(bx, groundY + c + 1, bz);
						}
					}
				}
			}
		}

		private class Segment {
			final int x, z, sizeX, sizeZ, roadWidth, direction;
			boolean isCenter;
			Segment[] subSegments;
			boolean subAlignment = false;
			int splitStep;

			Segment(int x, int z, int sizeX, int sizeZ, int roadWidth, int direction) {
				this.x = x;
				this.z = z;
				this.sizeX = sizeX;
				this.sizeZ = sizeZ;
				this.roadWidth = Math.max(ROAD_MIN_WIDTH, roadWidth);
				this.direction = direction;
			}

			void setBlocks() {
				if (subSegments != null && subSegments.length >= 2) {
					for (int i = 0; i < subSegments.length; i++) {
						subSegments[i].setBlocks();
						if (i < subSegments.length - 1) {
							Segment s = subSegments[i];
							if (this == rootSegment) { // main road through the fence
								int extra = OUTER_FENCE_WIDTH + OUTER_FENCE_SPACING + 1;
								if (subAlignment) {
									setRoadBlocks(this.x - extra, s.z + s.sizeZ, roadWidth, sizeX + 2 * extra, true);
									setFenceGateBlocks(x - (OUTER_FENCE_SPACING + OUTER_FENCE_WIDTH), s.z + s.sizeZ, roadWidth, OUTER_FENCE_WIDTH,
											OUTER_FENCE_HEIGHT, true);
									setFenceGateBlocks(x + sizeX + OUTER_FENCE_SPACING, s.z + s.sizeZ, roadWidth, OUTER_FENCE_WIDTH, OUTER_FENCE_HEIGHT, true);
								} else {
									setRoadBlocks(s.x + s.sizeX, this.z - extra, roadWidth, sizeZ + 2 * extra, false);
									setFenceGateBlocks(s.x + s.sizeX, z - (OUTER_FENCE_SPACING + OUTER_FENCE_WIDTH), roadWidth, OUTER_FENCE_WIDTH,
											OUTER_FENCE_HEIGHT, false);
									setFenceGateBlocks(s.x + s.sizeX, z + sizeZ + OUTER_FENCE_SPACING, roadWidth, OUTER_FENCE_WIDTH, OUTER_FENCE_HEIGHT, false);
								}
							} else if (subAlignment) {
								setRoadBlocks(this.x - 1, s.z + s.sizeZ, roadWidth, sizeX + 2, true);
							} else {
								setRoadBlocks(s.x + s.sizeX, this.z - 1, roadWidth, sizeZ + 2, false);
							}
						}
					}
				} else if (isCenter && flagSegment == null) {
					flagSegment = this;
				} else {
					Part structure;
					if (shouldPlaceCornerStructure()) {
						structure = getMatchingStructure(cornerStructures);
					} else if (shouldPlaceBorderStructure()) {
						structure = getMatchingStructure(borderStructures);
					} else {
						structure = getMatchingStructure(structures);
					}
					int height;
					if (structure.maxY == -1 || structure.maxY < structure.minY) {
						height = structure.minY;
					} else {
						height = structure.minY + rand.nextInt(structure.maxY + 1 - structure.minY);
					}
					structure.build(w, x, groundY, z, sizeX, height, sizeZ, direction, color, rand);
				}
			}

			private Part getMatchingStructure(List<Part> list) {
				List<Part> matching = new ArrayList<>();
				for (Part str : list) {
					boolean fits = sizeX >= str.minX && sizeZ >= str.minZ && (str.maxX == -1 || sizeX <= str.maxX) && (str.maxZ == -1 || sizeZ <= str.maxZ);
					if (str.canSwapXZ) {
						fits = ((sizeX >= str.minX && sizeZ >= str.minZ) || (sizeZ >= str.minX && sizeX >= str.minZ))
								&& (((str.maxX == -1 || sizeX <= str.maxX) && (str.maxZ == -1 || sizeZ <= str.maxZ))
										|| ((str.maxX == -1 || sizeZ <= str.maxX) && (str.maxZ == -1 || sizeX <= str.maxZ)));
					}
					if (fits) {
						matching.add(str);
					}
				}
				if (!matching.isEmpty()) {
					return matching.get(rand.nextInt(matching.size()));
				}
				return new CampParts.EmptyPlane(Blocks.COBBLESTONE.defaultBlockState());
			}

			boolean generateSubSegments() {
				int size;
				if (sizeX >= sizeZ) {
					size = sizeX;
					subAlignment = false;
				} else {
					size = sizeZ;
					subAlignment = true;
				}
				if (size <= 0) {
					return false;
				}
				double s = Math.sqrt((double) (size - minSubSegmentSize) / (double) (maxSubSegmentSize - minSubSegmentSize));
				if (size < minSubSegmentSize || (size < maxSubSegmentSize && rand.nextFloat() > (s * 0.75f + getCenterFactor() * 0.25f))) {
					subSegments = null;
					return false;
				}
				int count = 2;
				if (this != rootSegment && rand.nextFloat() >= 0.5) {
					count = Math.max(2, Math.round(1.0f + rand.nextFloat() * 0.5f * ((size - ((minSubSegmentSize + 1) / 2.0f)) / (roadWidth + 2))));
				}
				double[] parts = new double[count];
				double sum = 0;
				for (int i = 0; i < count; i++) {
					parts[i] = 1.0 + rand.nextFloat();
					sum += parts[i];
				}
				int sizeTotal = 0;
				subSegments = new Segment[count];
				for (int i = 0; i < count; i++) {
					int splitSize;
					int dir;
					if (i >= count - 1) {
						splitSize = size - sizeTotal;
						dir = subAlignment ? 1 : 0;
					} else {
						splitSize = (int) Math.round((parts[i] / sum) * (double) (size - (roadWidth * count)));
						dir = subAlignment ? 3 : 2;
					}
					if (!subAlignment) {
						subSegments[i] = new Segment(x + sizeTotal, z, splitSize, sizeZ, roadWidth - 1, dir);
					} else {
						subSegments[i] = new Segment(x, z + sizeTotal, sizeX, splitSize, roadWidth - 1, dir);
					}
					sizeTotal += splitSize + roadWidth;
					if (this.isCenter && this.splitStep <= 1 && i == (count - 1) / 2) {
						subSegments[i].isCenter = true;
					} else if (this.isCenter && i == count - 1) {
						subSegments[i].isCenter = true;
					}
					subSegments[i].splitStep = this.splitStep + 1;
					subSegments[i].generateSubSegments();
				}
				return true;
			}

			float getCenterFactor() {
				double segX = x + sizeX / 2.0, segZ = z + sizeZ / 2.0;
				double campX = posX + Camp.this.sizeX / 2.0, campZ = posZ + Camp.this.sizeZ / 2.0;
				double d = (campX - segX) * (campX - segX) + (campZ - segZ) * (campZ - segZ);
				return 1.0f - (float) (d / campRadiusSquared);
			}

			boolean shouldPlaceBorderStructure() {
				int border = OUTER_FENCE_WIDTH + OUTER_FENCE_SPACING;
				boolean isBorderX = x <= posX + border || x + sizeX >= posX + Camp.this.sizeX - border;
				boolean isBorderZ = z <= posZ + border || z + sizeZ >= posZ + Camp.this.sizeZ - border;
				return (isBorderX && borderChanceX > rand.nextFloat()) || (isBorderZ && borderChanceZ > rand.nextFloat());
			}

			boolean shouldPlaceCornerStructure() {
				int border = OUTER_FENCE_WIDTH + OUTER_FENCE_SPACING;
				boolean minX = x <= posX + border, maxX = x + sizeX >= posX + Camp.this.sizeX - border;
				boolean minZ = z <= posZ + border, maxZ = z + sizeZ >= posZ + Camp.this.sizeZ - border;
				boolean isCorner = (minX || maxX) && (minZ || maxZ);
				return isCorner && cornerChance > rand.nextFloat();
			}
		}
	}
}
