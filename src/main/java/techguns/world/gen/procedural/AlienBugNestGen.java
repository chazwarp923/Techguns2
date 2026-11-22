package techguns.world.gen.procedural;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import techguns.TGBlocks;
import techguns.TGEntities;
import techguns.blocks.BlockTGSpawner;
import techguns.world.gen.GenWorld;
import techguns.world.gen.LegacyBlocks;
import techguns.world.gen.MBlock;
import techguns.world.gen.PlaceContext.BiomeColorType;
import techguns.world.gen.ProcGenerator;
import techguns.world.gen.TerrainView;

/**
 * 1.12 AlienBugNest / AlienBugNestStructure: an underground nest of sand spheres linked by tunnels, with a shaft up to
 * the surface, egg piles and an alien bug spawner in every side chamber.
 */
public class AlienBugNestGen implements ProcGenerator {

	static final int MAX_RADIUS = 10;
	static final int MIN_RADIUS = 3;

	private final MBlock wallBlock;
	private final MBlock wallBlock2;
	private final MBlock spawner;
	private final BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

	public AlienBugNestGen() {
		MBlock sand = new MBlock(LegacyBlocks.tg("sand_hard", 0));
		MBlock eggs = new MBlock(LegacyBlocks.tg("slimy", 0));
		MBlock sandstone = new MBlock(LegacyBlocks.vanilla("sandstone", 0));
		wallBlock = new MBlock.Multi(new MBlock[] { sand, eggs, sandstone }, new int[] { 16, 1, 2 });
		wallBlock2 = new MBlock.Multi(new MBlock[] { sand, sandstone }, new int[] { 12, 1 });
		spawner = new MBlock.Spawner(BlockTGSpawner.Type.HOLE, 7, 3, 150, 1).mob(TGEntities.ALIEN_BUG, 1);
	}

	@Override
	public Layout layout(int chunkX, int chunkZ, RandomSource rnd, TerrainView terrain) {
		int x = chunkX * 16;
		int z = chunkZ * 16;
		// 1.12 getValidSpawnY over a 4x4 area with no height limit: no water, median of the corners
		int[][] corners = { { x, z }, { x + 4, z }, { x, z + 4 }, { x + 4, z + 4 } };
		int[] h = new int[4];
		for (int i = 0; i < 4; i++) {
			if (terrain.isWater(corners[i][0], corners[i][1])) {
				return null;
			}
			h[i] = terrain.ground(corners[i][0], corners[i][1]);
		}
		Arrays.sort(h);
		int y = (h[1] + h[2]) / 2;
		return new Layout(new BlockPos(x, y - 2, z), 16 + rnd.nextInt(16), 0, 16 + rnd.nextInt(16));
	}

	/* ------------------------------------------------------------------ 1.12 AlienBugNest */

	private static class Segment {
		final int x, y, z, radius;
		final int type; // 0 = main segment, 1 = regular, 2 = entrance
		int status = 0; // 0 = none, 1 = spheres, 2 = connections, 3 = interior
		final List<Segment> neighbors = new ArrayList<>();

		Segment(int x, int y, int z, int radius, int type) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = radius;
			this.type = type;
		}
	}

	@Override
	public void generate(GenWorld w, Layout layout, BiomeColorType color, RandomSource rand, TerrainView terrain) {
		BlockPos o = layout.origin();
		int segments = 5;
		boolean largeNest = true;
		int startRadius = Math.min(MAX_RADIUS, MIN_RADIUS + (segments / 2));
		int x = o.getX() + (layout.sizeX() / 2);
		int y = o.getY() + (layout.sizeY() / 2);
		int z = o.getZ() + (layout.sizeZ() / 2);

		Segment entrance = new Segment(x, y, z, 2, 2);
		int mainY = y - (startRadius + 5 + rand.nextInt(7));
		Segment main = new Segment(x + rand.nextInt(3), mainY, z + rand.nextInt(3), startRadius, 0);
		main.neighbors.add(entrance);

		float angle = (float) ((Math.PI * 2.0) / segments);
		Segment prev = main;
		for (int i = 0; i < segments; i++) {
			int radius = Math.max(MIN_RADIUS, startRadius - rand.nextInt(i + 1));
			double dist = startRadius + radius + 5.0 + rand.nextDouble() * 8.0;
			double sx = x + dist * Math.cos(angle * i);
			double sz = z + dist * Math.sin(angle * i);
			int segY = mainY + rand.nextInt(startRadius * 3) - startRadius * 2;
			Segment seg = new Segment((int) sx, segY, (int) sz, radius, 1);
			if (prev == main) {
				main.neighbors.add(seg);
			} else if (rand.nextBoolean()) {
				prev.neighbors.add(seg);
				if (rand.nextBoolean()) {
					main.neighbors.add(seg);
				}
			} else {
				main.neighbors.add(seg);
			}
			if (largeNest && rand.nextBoolean()) {
				radius = Math.max(MIN_RADIUS, startRadius - rand.nextInt(i + 1));
				dist = startRadius + radius + 5.0 + rand.nextDouble() * 8.0;
				double a2 = (angle * i) + (Math.PI * 0.5 * (rand.nextDouble() - 0.5));
				double s2x = sx + dist * Math.cos(a2);
				double s2z = sz + dist * Math.sin(a2);
				segY = mainY + rand.nextInt(startRadius * 3) - startRadius * 2;
				seg.neighbors.add(new Segment((int) s2x, segY, (int) s2z, radius, 1));
			}
			prev = seg;
		}

		placeSegment(w, main);
		placeConnections(w, main, rand);
		placeInterior(w, main, rand);
	}

	private void placeSegment(GenWorld w, Segment segment) {
		if (segment.status >= 1 || segment.type == 2) {
			return;
		}
		fillSphere2(w, segment.x, segment.y, segment.z, segment.radius, 1.0f, null, wallBlock);
		segment.status = 1;
		for (Segment seg : segment.neighbors) {
			placeSegment(w, seg);
		}
	}

	private void placeConnections(GenWorld w, Segment segment, RandomSource rand) {
		if (segment.status >= 2) {
			return;
		}
		segment.status = 2;
		for (Segment seg : segment.neighbors) {
			Vec3 pos1 = new Vec3(segment.x, segment.y, segment.z);
			Vec3 pos2 = new Vec3(seg.x, seg.y, seg.z);
			Vec3 v2v1 = pos2.subtract(pos1);
			double distance = v2v1.length();
			Vec3 dir = v2v1.normalize();
			double d2 = distance - seg.radius;
			if (seg.type == 2) {
				d2 += 5.0;
			}
			int x1 = (int) (segment.x + dir.x * segment.radius);
			int y1 = (int) (segment.y + dir.y * segment.radius);
			int z1 = (int) (segment.z + dir.z * segment.radius);
			int x2 = (int) (segment.x + dir.x * d2);
			int y2 = (int) (segment.y + dir.y * d2);
			int z2 = (int) (segment.z + dir.z * d2);
			int r = Math.min(segment.radius, Math.min(2 + rand.nextInt(2), seg.radius));
			fillCylinder(w, x1, y1, z1, x2, y2, z2, r, wallBlock2);
			if (seg.type == 2) {
				createSlimeLadder(w, x1, y1, z1, x2, y2, z2, r - 1);
			}
			placeConnections(w, seg, rand);
		}
	}

	private void placeInterior(GenWorld w, Segment segment, RandomSource rand) {
		if (segment.status >= 3 || segment.type == 2) {
			return;
		}
		fillSphere(w, segment.x, segment.y, segment.z, segment.radius - 1);
		segment.status = 3;
		if (segment.type == 1) {
			placeEggs(w, segment, rand);
		}
		for (Segment seg : segment.neighbors) {
			if (segment.type == 0 && seg.type == 2) { // slime line from the main chamber up towards the entrance
				double a = rand.nextDouble() * 2.0 * Math.PI;
				double r = 1.0 + (rand.nextDouble() * 0.66);
				int xo = (int) (r * Math.cos(a));
				int zo = (int) (r * Math.sin(a));
				Vec3 dir = new Vec3(seg.x - segment.x, seg.y - segment.y, seg.z - segment.z).normalize();
				double d2 = (segment.radius - 1) * 2;
				int x1 = (int) (segment.x + xo + dir.x * -(segment.radius + 1));
				int y1 = (int) (segment.y + dir.y * -(segment.radius + 1));
				int z1 = (int) (segment.z + zo + dir.z * -(segment.radius + 1));
				int x2 = (int) (segment.x + xo + dir.x * d2);
				int y2 = (int) (segment.y + dir.y * d2);
				int z2 = (int) (segment.z + zo + dir.z * d2);
				drawSlimeLine(w, x1, y1, z1, x2, y2, z2);
			}
			placeInterior(w, seg, rand);
		}
	}

	private void placeEggs(GenWorld w, Segment segment, RandomSource rand) {
		int r = segment.radius - 2;
		int ri = r > 1 ? r - 1 : r;
		int spawnerX = rand.nextInt(2 * ri) - ri;
		int spawnerZ = rand.nextInt(2 * ri) - ri;
		BlockState eggs = LegacyBlocks.tg("slimy", 0);
		for (int i = -r; i < r; i++) {
			for (int j = -r; j < r; j++) {
				if (i == spawnerX && j == spawnerZ) {
					w.set(segment.x + i, segment.y - (segment.radius - 2), segment.z + j, spawner);
				} else {
					float f1 = (float) (r - Math.abs(i)) / (float) r;
					float f2 = (float) (r - Math.abs(j)) / (float) r;
					if (rand.nextDouble() < ((f1 + f2) / 2.0f)) {
						int x = segment.x + i;
						int z = segment.z + j;
						for (int k = 0; k < segment.radius; k++) {
							int y = segment.y - (segment.radius - 1) + k;
							if (w.isAir(x, y, z)) {
								w.set(x, y, z, eggs);
							}
							if (rand.nextDouble() > (f1 + f2) / 3.0f) {
								break;
							}
						}
					}
				}
			}
		}
	}

	/* ------------------------------------------------------------------ 1.12 BlockUtils shapes */

	/** sphere: inside (radius - thickness) air when block1 is null, the shell block2 */
	private void fillSphere2(GenWorld w, int px, int py, int pz, float radius, float thickness, MBlock block1, MBlock block2) {
		float rsq = radius * radius;
		float rsq2 = (radius - thickness) * (radius - thickness);
		int r = (int) radius;
		for (int i = -r; i <= r; i++) {
			for (int j = -r; j <= r; j++) {
				for (int k = -r; k <= r; k++) {
					float d = i * i + j * j + k * k;
					if (d < rsq2) {
						if (block1 == null) {
							w.set(px + i, py + j, pz + k, air);
						} else {
							w.set(px + i, py + j, pz + k, block1);
						}
					} else if (d < rsq) {
						w.set(px + i, py + j, pz + k, block2);
					}
				}
			}
		}
	}

	private void fillSphere(GenWorld w, int px, int py, int pz, float radius) {
		float rsq = radius * radius;
		int r = (int) radius;
		for (int i = -r; i <= r; i++) {
			for (int j = -r; j <= r; j++) {
				for (int k = -r; k <= r; k++) {
					if (i * i + j * j + k * k < rsq) {
						w.set(px + i, py + j, pz + k, air);
					}
				}
			}
		}
	}

	/** tunnel: air inside radius-1, wall block shell */
	private void fillCylinder(GenWorld w, int x1, int y1, int z1, int x2, int y2, int z2, float radius, MBlock wall) {
		Vec3 v1 = new Vec3(x1, y1, z1);
		Vec3 v2 = new Vec3(x2, y2, z2);
		Vec3 v2v1 = v1.subtract(v2);
		double l = v2v1.length();
		int tmp;
		if (x1 > x2) { tmp = x2; x2 = x1; x1 = tmp; }
		if (y1 > y2) { tmp = y2; y2 = y1; y1 = tmp; }
		if (z1 > z2) { tmp = z2; z2 = z1; z1 = tmp; }
		int r = (int) radius;
		for (int x = x1 - r; x < x2 + r; x++) {
			for (int y = y1 - r; y < y2 + r; y++) {
				for (int z = z1 - r; z < z2 + r; z++) {
					Vec3 v1v0 = v1.subtract(x, y, z);
					double distance = l == 0 ? v1v0.length() : v2v1.cross(v1v0).length() / l;
					if (distance < radius - 1.0f) {
						w.set(x, y, z, air);
					} else if (distance < radius) {
						w.set(x, y, z, wall);
					}
				}
			}
		}
	}

	/**
	 * 1.12 createSlimeLadder: its distance check always came out 0, so it covers every free spot next to a wall in the
	 * box around the shaft; kept that way, it is what made the climbable shaft.
	 */
	private void createSlimeLadder(GenWorld w, int x1, int y1, int z1, int x2, int y2, int z2, float radius) {
		int tmp;
		if (x1 > x2) { tmp = x2; x2 = x1; x1 = tmp; }
		if (y1 > y2) { tmp = y2; y2 = y1; y1 = tmp; }
		if (z1 > z2) { tmp = z2; z2 = z1; z1 = tmp; }
		int r = (int) radius;
		for (int x = x1 - r; x < x2 + r; x++) {
			for (int y = y1 - r; y < y2 + r; y++) {
				for (int z = z1 - r; z < z2 + r; z++) {
					placeSlimeLadder(w, new BlockPos(x, y, z));
				}
			}
		}
	}

	/** slimy ladder if the spot is free and has a wall, facing like 1.12 BlockLadder.getStateForPlacement */
	private void placeSlimeLadder(GenWorld w, BlockPos p) {
		BlockState here = w.get(p);
		if (!here.canBeReplaced()) {
			return;
		}
		for (Direction d : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
			if (w.isSolid(p.relative(d.getOpposite()))) {
				w.set(p, TGBlocks.SLIMY_LADDER.get().defaultBlockState().setValue(LadderBlock.FACING, d));
				return;
			}
		}
	}

	/** 1.12 MBlockSlime: bug nest sand with slimy ladders around it */
	private void slime(GenWorld w, int x, int y, int z) {
		w.set(x, y, z, LegacyBlocks.tg("sand_hard", 0));
		BlockPos p = new BlockPos(x, y, z);
		for (Direction d : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
			BlockPos n = p.relative(d);
			if (w.get(n).isAir()) {
				placeSlimeLadder(w, n);
			}
		}
	}

	/** 1.12 BlockUtils.drawLine (3D Bresenham, 4-connected when going up) with the slime block */
	private void drawSlimeLine(GenWorld w, int x1, int y1, int z1, int x2, int y2, int z2) {
		int dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
		int ax = Math.abs(dx) << 1, ay = Math.abs(dy) << 1, az = Math.abs(dz) << 1;
		int sx = Integer.signum(dx), sy = Integer.signum(dy), sz = Integer.signum(dz);
		int x = x1, y = y1, z = z1;
		if (ax >= Math.max(ay, az)) {
			int deltay = ay - (ax >> 1), deltaz = az - (ax >> 1);
			while (true) {
				slime(w, x, y, z);
				if (x == x2) {
					return;
				}
				if (deltay >= 0) { y += sy; deltay -= ax; }
				if (deltaz >= 0) { z += sz; deltaz -= ax; }
				x += sx;
				deltay += ay;
				deltaz += az;
			}
		} else if (ay >= Math.max(ax, az)) {
			int deltax = ax - (ay >> 1), deltaz = az - (ay >> 1);
			while (true) {
				slime(w, x, y, z);
				if (y == y2) {
					return;
				}
				if (deltax >= 0) { slime(w, x + sx, y, z); x += sx; deltax -= ay; }
				if (deltaz >= 0) { slime(w, x, y, z + sz); z += sz; deltaz -= ay; }
				y += sy;
				deltax += ax;
				deltaz += az;
			}
		} else {
			int deltax = ax - (az >> 1), deltay = ay - (az >> 1);
			while (true) {
				slime(w, x, y, z);
				if (z == z2) {
					return;
				}
				if (deltax >= 0) { x += sx; deltax -= az; }
				if (deltay >= 0) { y += sy; deltay -= az; }
				z += sz;
				deltax += ax;
				deltay += ay;
			}
		}
	}
}
