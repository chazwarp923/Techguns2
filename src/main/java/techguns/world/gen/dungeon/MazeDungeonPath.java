package techguns.world.gen.dungeon;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import techguns.world.gen.GenWorld;

/**
 * Port of 1.12 world/dungeon/MazeDungeonPath: grows a maze of segments (corridors, ramps, rooms) in a grid of
 * sX*sY*sZ cells, then places the matching template segment for each cell. Horizontal directions use the 1.12 index
 * (0 south, 1 west, 2 north, 3 east), which is also {@link Direction#from2DDataValue}.
 */
public class MazeDungeonPath {

	final RandomSource rand;
	PathSegment[][][] volume;
	final List<PathSegment> list = new ArrayList<>();
	private int nextRoomID = 1;
	int sX, sY, sZ;

	public int startHeightLevel = 0;
	public float chanceStraight = 0.5f;
	public float chanceRamp = 0.25f;
	public float chanceRoom = 0.25f;
	public float chanceFork = 0.2f;
	public float chanceUp = 0.5f;
	public int minRoomArea = 4;
	public int maxRoomArea = 16;
	public int minRoomWidth = 2;
	public int maxRoomWidth = 5;
	public boolean usePillars = true;
	public boolean useFoundations = true;
	public boolean useRoof = false;
	public boolean useBottomLayer = false;
	public int entranceRampLength = 0;
	private int rampPlaced = 0;
	private int numSegments = 0;
	Direction startFacing;
	BlockPos startPos;

	public MazeDungeonPath(int sX, int sY, int sZ, RandomSource rand) {
		this.sX = sX;
		this.sY = sY;
		this.sZ = sZ;
		this.rand = rand;
		volume = new PathSegment[sX][sY][sZ];
	}

	public int getNumSegments() {
		return numSegments;
	}

	public void generatePath() {
		if (useBottomLayer) {
			sY -= 1;
		}
		int startX, startZ;
		int startY = (sY + startHeightLevel) % sY;
		switch (rand.nextInt(4)) {
			case 0 -> {
				startX = 0;
				startZ = sZ / 2;
				startFacing = Direction.EAST;
			}
			case 1 -> {
				startX = sX - 1;
				startZ = sZ / 2;
				startFacing = Direction.WEST;
			}
			case 2 -> {
				startX = sX / 2;
				startZ = 0;
				startFacing = Direction.SOUTH;
			}
			default -> {
				startX = sX / 2;
				startZ = sZ - 1;
				startFacing = Direction.NORTH;
			}
		}
		entranceRampLength = Math.min(entranceRampLength, startY);
		startPos = new BlockPos(startX, startY, startZ);
		generateSegment(startX, startY, startZ, startFacing.get2DDataValue(), null);
	}

	private static Direction horizontal(int dir) {
		return Direction.from2DDataValue(dir);
	}

	public void generateSegment(int x, int y, int z, int dir, PathSegment prev) {
		Direction facing = horizontal(dir);
		PathSegment segment = new PathSegment(x, y, z);
		if (prev != null) {
			segment.setConnection(facing.getOpposite(), true);
			prev.setConnection(facing, true);
		} else {
			segment.isEntrance = true;
		}
		addSegment(segment);

		int maxRolls = 3;
		int roll = 0;
		int nextDir;
		boolean success = false;
		boolean canRollAgain = true;
		boolean fork = false;

		if (rampPlaced < entranceRampLength) {
			nextDir = dir;
			Vec3i nextPos = segment.getNextPos(nextDir);
			Vec3i offset = horizontal(nextDir).getNormal();
			Vec3i elevPos = new Vec3i(x, y - 1, z);
			Vec3i elevNextPos = new Vec3i(x + offset.getX(), y - 1, z + offset.getZ());
			if (isWithinBounds(nextPos) && isWithinBounds(elevNextPos)) {
				PathSegment rampDummy = new PathSegment(elevPos.getX(), elevPos.getY(), elevPos.getZ());
				addSegment(rampDummy);
				segment.isRamp = true;
				rampDummy.isRamp = true;
				segment.elevation = -1;
				rampDummy.elevation = 1;
				rampDummy.rampRotation = horizontal(nextDir).getOpposite().get2DDataValue();
				generateSegment(elevNextPos.getX(), elevNextPos.getY(), elevNextPos.getZ(), nextDir, rampDummy);
				success = true;
			}
			rampPlaced++;
		}

		while (!success && roll++ < maxRolls) {
			if (segment.isEntrance) {
				nextDir = dir;
				canRollAgain = false;
			} else {
				nextDir = getRandomDir(dir);
			}
			Direction nextFacing = horizontal(nextDir);
			Vec3i nextPos = segment.getNextPos(nextDir);
			if (isWithinBounds(nextPos)) {
				if (dir == nextDir && !segment.isEntrance && !fork && rand.nextFloat() < chanceRamp) {
					int dy = rand.nextFloat() < chanceUp ? 1 : -1;
					Vec3i elevPos = new Vec3i(x, y + dy, z);
					if (isWithinBounds(elevPos) && !isOccupied(elevPos)) {
						Vec3i offset = nextFacing.getNormal();
						Vec3i elevNextPos = new Vec3i(x + offset.getX(), y + dy, z + offset.getZ());
						boolean cont = false;
						// 1.12 did not bounds check this position; out of range counts as blocked here
						if (isWithinBounds(elevNextPos) && !isOccupied(elevNextPos)) {
							success = true;
							cont = true;
						} else if (isWithinBounds(elevNextPos)) {
							PathSegment seg = get(elevNextPos);
							if (seg != null && !seg.isRamp && !seg.isEntrance) {
								seg.setConnection(nextFacing.getOpposite(), true);
								success = true;
							}
						}
						if (success) {
							canRollAgain = false;
							PathSegment rampDummy = new PathSegment(elevPos.getX(), elevPos.getY(), elevPos.getZ());
							addSegment(rampDummy);
							segment.isRamp = true;
							rampDummy.isRamp = true;
							if (dy == 1) {
								segment.elevation = 1;
								rampDummy.elevation = -1;
								segment.rampRotation = nextDir;
							} else {
								segment.elevation = -1;
								rampDummy.elevation = 1;
								rampDummy.rampRotation = nextFacing.getOpposite().get2DDataValue();
							}
							if (cont) {
								generateSegment(elevNextPos.getX(), elevNextPos.getY(), elevNextPos.getZ(), nextDir, rampDummy);
							}
						}
					}
				} else if (!isOccupied(nextPos)) {
					if (rand.nextFloat() < chanceRoom) {
						int area = minRoomArea + rand.nextInt(maxRoomArea - minRoomArea);
						int w = minRoomWidth + rand.nextInt(maxRoomWidth - minRoomWidth);
						int l = minRoomWidth + rand.nextInt(maxRoomWidth - minRoomWidth);
						if (!tryGrowRoom(nextPos, segment, nextFacing, w, l, area)) {
							generateSegment(nextPos.getX(), nextPos.getY(), nextPos.getZ(), nextDir, segment);
						}
					} else {
						generateSegment(nextPos.getX(), nextPos.getY(), nextPos.getZ(), nextDir, segment);
					}
					success = true;
				} else {
					PathSegment seg = get(nextPos);
					if (seg != null && !seg.isRamp && !seg.isEntrance) {
						segment.setConnection(nextFacing, true);
						seg.setConnection(nextFacing.getOpposite(), true);
						success = true;
					}
				}
			}
			if (success && canRollAgain && rand.nextFloat() < chanceFork) {
				success = false;
				fork = true;
			}
		}
	}

	private boolean tryGrowRoom(Vec3i pos, PathSegment prev, Direction dir, int maxWidth, int maxLength, int preferredArea) {
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		Vec3i vFront = dir.getNormal();
		Vec3i vRight = dir.getClockWise().getNormal();
		Vec3i vLeft = dir.getCounterClockWise().getNormal();
		int l = 0, bestl = 0, bestwL = 0, bestwR = 0;
		float bestValue = Float.MAX_VALUE;
		int maxwL = Integer.MAX_VALUE, maxwR = Integer.MAX_VALUE;
		while (l < maxLength) {
			Vec3i posF = new Vec3i(x + vFront.getX() * l, y, z + vFront.getZ() * l);
			if (!isWithinBounds(posF) || isOccupied(posF)) {
				break;
			}
			int wL = 0, wR = 0;
			while (wL < maxWidth && wL < maxwL) {
				Vec3i p = new Vec3i(x + vLeft.getX() * (wL + 1) + vFront.getX() * l, y, z + vLeft.getZ() * (wL + 1) + vFront.getZ() * l);
				if (!isWithinBounds(p) || isOccupied(p)) {
					maxwL = wL;
					break;
				}
				wL++;
			}
			while (wR < maxWidth && wR < maxwR) {
				Vec3i p = new Vec3i(x + vRight.getX() * (wR + 1) + vFront.getX() * l, y, z + vRight.getZ() * (wR + 1) + vFront.getZ() * l);
				if (!isWithinBounds(p) || isOccupied(p)) {
					maxwR = wR;
					break;
				}
				wR++;
			}
			int width = wL + wR + 1;
			int area = width * (l + 1);
			float areaRatio = (float) Math.max(area, preferredArea) / (float) Math.min(area, preferredArea);
			float aspect = (float) Math.max(width, l + 1) / (float) Math.min(width, l + 1);
			float value = areaRatio + aspect;
			if (value < bestValue) {
				bestValue = value;
				bestl = l;
				bestwL = wL;
				bestwR = wR;
			}
			l++;
		}
		if (bestl < 2 || bestwL + bestwR + 1 < 2) {
			return false;
		}
		Vec3i p1 = new Vec3i(x + vLeft.getX() * bestwL, y, z + vLeft.getZ() * bestwL);
		Vec3i p2 = new Vec3i(x + vRight.getX() * bestwR + vFront.getX() * bestl, y, z + vRight.getZ() * bestwR + vFront.getZ() * bestl);
		Vec3i min = new Vec3i(Math.min(p1.getX(), p2.getX()), y, Math.min(p1.getZ(), p2.getZ()));
		Vec3i max = new Vec3i(Math.max(p1.getX(), p2.getX()), y, Math.max(p1.getZ(), p2.getZ()));
		placeRoomSegments(min, max);
		prev.setConnection(dir, true);
		PathSegment segment = get(pos);
		if (segment != null) {
			segment.setConnection(dir.getOpposite(), true);
		}
		return true;
	}

	private void placeRoomSegments(Vec3i min, Vec3i max) {
		int roomID = nextRoomID++;
		int y = min.getY();
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int z = min.getZ(); z <= max.getZ(); z++) {
				PathSegment segment = new PathSegment(x, y, z);
				segment.roomID = roomID;
				addSegment(segment);
				if (x > min.getX()) segment.setConnection(7, true);
				if (x < max.getX()) segment.setConnection(3, true);
				if (z > min.getZ()) segment.setConnection(1, true);
				if (z < max.getZ()) segment.setConnection(5, true);
				if (x != min.getX() && z != min.getZ()) segment.setConnection(0, true);
				if (x != min.getX() && z != max.getZ()) segment.setConnection(6, true);
				if (x != max.getX() && z != min.getZ()) segment.setConnection(2, true);
				if (x != max.getX() && z != max.getZ()) segment.setConnection(4, true);
			}
		}
		int length = max.getZ() - min.getZ() + 1;
		int numDoors = rand.nextInt(length);
		for (int i = 0; i < numDoors; i++) {
			Vec3i p = new Vec3i(min.getX(), y, min.getZ() + rand.nextInt(length));
			placeDoor(p, Direction.WEST, get(p));
		}
		numDoors = rand.nextInt(length);
		for (int i = 0; i < numDoors; i++) {
			Vec3i p = new Vec3i(max.getX(), y, min.getZ() + rand.nextInt(length));
			placeDoor(p, Direction.EAST, get(p));
		}
		length = max.getX() - min.getX() + 1;
		numDoors = rand.nextInt(length);
		for (int i = 0; i < numDoors; i++) {
			Vec3i p = new Vec3i(min.getX() + rand.nextInt(length), y, min.getZ());
			placeDoor(p, Direction.NORTH, get(p));
		}
		numDoors = rand.nextInt(length);
		for (int i = 0; i < numDoors; i++) {
			Vec3i p = new Vec3i(min.getX() + rand.nextInt(length), y, max.getZ());
			placeDoor(p, Direction.SOUTH, get(p));
		}
	}

	private void placeDoor(Vec3i pos, Direction facing, PathSegment segment) {
		Vec3i offset = facing.getNormal();
		Vec3i nextPos = new Vec3i(pos.getX() + offset.getX(), pos.getY(), pos.getZ() + offset.getZ());
		if (isWithinBounds(nextPos)) {
			if (!isOccupied(nextPos)) {
				generateSegment(nextPos.getX(), nextPos.getY(), nextPos.getZ(), facing.get2DDataValue(), segment);
			} else {
				PathSegment seg = get(nextPos);
				if (seg != null && !seg.isRamp && !seg.isEntrance && segment != null) {
					segment.setConnection(facing, true);
					seg.setConnection(facing.getOpposite(), true);
				}
			}
		}
	}

	private void addSegment(PathSegment seg) {
		if (seg.x >= 0 && seg.x < sX && seg.y >= 0 && seg.y < sY && seg.z >= 0 && seg.z < sZ) {
			volume[seg.x][seg.y][seg.z] = seg;
			list.add(seg);
			numSegments++;
		}
	}

	private boolean isOccupied(Vec3i pos) {
		return volume[pos.getX()][pos.getY()][pos.getZ()] != null;
	}

	private int getRandomDir(int dir) {
		float f = rand.nextFloat();
		if (f < chanceStraight) {
			return dir;
		} else if (f < chanceStraight + (1.0f - chanceStraight) * 0.5f) {
			return horizontal(dir).getClockWise().get2DDataValue();
		}
		return horizontal(dir).getCounterClockWise().get2DDataValue();
	}

	private boolean isWithinBounds(Vec3i pos) {
		int maxY = entranceRampLength > rampPlaced ? sY : sY - entranceRampLength;
		return pos.getX() >= 0 && pos.getX() < sX && pos.getY() >= 0 && pos.getY() < maxY && pos.getZ() >= 0 && pos.getZ() < sZ;
	}

	private PathSegment get(Vec3i index) {
		return volume[index.getX()][index.getY()][index.getZ()];
	}

	public static class PathSegment {
		boolean isEntrance;
		int x, y, z;
		boolean isRamp = false;
		int elevation = 0;
		int rampRotation = -1;
		int roomID = -1;
		boolean[] pattern = new boolean[8];

		PathSegment(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		Vec3i getNextPos(int dir) {
			Vec3i o = horizontal(dir).getNormal();
			return new Vec3i(x + o.getX(), y + o.getY(), z + o.getZ());
		}

		void setConnection(int index, boolean connected) {
			pattern[index] = connected;
		}

		void setConnection(Direction facing, boolean connected) {
			pattern[((facing.get2DDataValue() + 2) % 4) * 2 + 1] = connected;
		}

		boolean match(boolean[] o, int rotations) {
			int r = rotations * 2;
			for (int i = 0; i < 8; i++) {
				if (pattern[i] != o[(r + i) % 8]) {
					return false;
				}
			}
			return true;
		}

		boolean allowSpawner() {
			return !(isEntrance || isRamp);
		}
	}

	/** 1.12 generateDungeon: the template segment for every cell, plus foundations, pillars and roofs */
	public void generateDungeon(GenWorld w, int posX, int posY, int posZ, DungeonPreset preset) {
		if (useBottomLayer) {
			cloneBottomLayer();
		}
		int sxz = preset.getSizeXZ(), sy = preset.getSizeY();
		for (int i = 0; i < sX; i++) {
			for (int j = 0; j < sY; j++) {
				for (int k = 0; k < sZ; k++) {
					boolean above = false;
					for (int y = j + 1; y < sY; y++) {
						if (volume[i][y][k] != null) {
							above = true;
							break;
						}
					}
					boolean below = false;
					for (int y = j - 1; y >= 0; y--) {
						if (volume[i][y][k] != null) {
							below = true;
							break;
						}
					}
					int px = posX + i * sxz, py = posY + j * sy, pz = posZ + k * sxz;
					PathSegment segment = volume[i][j][k];
					if (segment != null) {
						if (segment.isRamp) {
							if (segment.elevation == 1) {
								int r = segment.rampRotation;
								if (r == 0 || r == 2) r = (r + 2) % 4;
								preset.place(w, SegmentType.RAMP, segment.y, sY, rand.nextInt(), px, py, pz, (r + 1) % 4);
							} else {
								int r = volume[i][j - 1][k].rampRotation;
								if (r == 0 || r == 2) r = (r + 2) % 4;
								if (useRoof && !above && segment.y + 1 < sY) {
									int seed = segment.roomID > 0 ? segment.roomID : rand.nextInt();
									preset.place(w, SegmentType.END, -1, sY, seed, px, posY + (j + 1) * sy, pz, (r + 1) % 4);
								}
							}
						} else {
							for (SegmentType type : SegmentType.values()) {
								if (!type.match) continue;
								int r = 0;
								while (r < type.rotations && !segment.match(type.pattern, r)) {
									r++;
								}
								if (r < type.rotations) {
									if (type == SegmentType.END && j == (sY + startHeightLevel) % sY) {
										preset.place(w, SegmentType.ENTRANCE, segment.y, sY, rand.nextInt(), px, py, pz, r);
									} else {
										int seed = segment.roomID > 0 ? segment.roomID : rand.nextInt();
										preset.place(w, type, segment.y, sY, seed, px, py, pz, r);
									}
									if (useRoof && !above && segment.y >= startHeightLevel && segment.y + 1 < sY) {
										int seed = segment.roomID > 0 ? segment.roomID : rand.nextInt();
										preset.place(w, type, -1, sY, seed, px, posY + (j + 1) * sy, pz, r);
									}
									break;
								}
							}
						}
					} else if (useFoundations && above && !below) {
						preset.place(w, SegmentType.FOUNDATION, j, sY, rand.nextInt(), px, py, pz, 0);
					} else if (usePillars && above) {
						preset.place(w, SegmentType.PILLARS, j, sY, rand.nextInt(), px, py, pz, 0);
					}
				}
			}
		}
	}

	private void cloneBottomLayer() {
		PathSegment[][][] v2 = new PathSegment[sX][sY + 1][sZ];
		for (int i = 0; i < sX; i++) {
			for (int j = 0; j < sY; j++) {
				for (int k = 0; k < sZ; k++) {
					if (volume[i][j][k] != null) {
						v2[i][j + 1][k] = volume[i][j][k];
						v2[i][j + 1][k].y++;
						if (j == 0) {
							PathSegment seg2 = new PathSegment(i, j, k);
							seg2.pattern = volume[i][j][k].pattern;
							v2[i][j][k] = seg2;
						}
					}
				}
			}
		}
		volume = v2;
		sY++;
		startPos = startPos.above();
	}

	/** 1.12 generateNPCSpawners: spawners in random non-ramp, non-entrance cells */
	public void generateNPCSpawners(GenWorld w, int posX, int posY, int posZ, DungeonPreset preset) {
		int numSpawns = (int) (numSegments * preset.getSpawnDensity());
		List<PathSegment> spawners = new ArrayList<>();
		int tries = 0;
		while (numSpawns > 0 && tries < 5 && !list.isEmpty()) {
			PathSegment seg = list.get(rand.nextInt(list.size()));
			if (!spawners.contains(seg) && seg.allowSpawner()) {
				spawners.add(seg);
				numSpawns--;
				tries = 0;
			} else {
				tries++;
			}
		}
		int sxz = preset.getSizeXZ();
		for (PathSegment seg : spawners) {
			int px = posX + seg.x * sxz + (int) (sxz * 0.5f);
			int py = posY + seg.y * preset.getSizeY() + preset.getSpawnYOffset();
			int pz = posZ + seg.z * sxz + (int) (sxz * 0.5f);
			w.set(px, py, pz, preset.spawner());
		}
	}
}
