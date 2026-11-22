package techguns.world.gen.procedural;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import techguns.TGBlocks;
import techguns.world.gen.GenWorld;
import techguns.world.gen.LegacyBlocks;
import techguns.world.gen.MBlock;
import techguns.world.gen.PlaceContext.BiomeColorType;

/**
 * The 1.12 military camp buildings (world/structures: Tent, Containers, CampProps, Bunker, Barracks, Helipad, Tanks,
 * WatchTowerSmall, EmptyPlane), writing into a {@link GenWorld}. Coordinates are absolute; posY is the ground block layer.
 * direction: 0 = -x (west), 1 = -z (north), 2 = +x (east), 3 = +z (south).
 */
public final class CampParts {

	private CampParts() {
	}

	/** 1.12 WorldgenStructure: size limits for the camp segment it is put in; -1 max = unlimited */
	public abstract static class Part {
		public final int minX, minY, minZ, maxX, maxY, maxZ;
		public boolean canSwapXZ;

		protected Part(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			this.minX = minX;
			this.minY = minY;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxY = maxY;
			this.maxZ = maxZ;
		}

		public Part swapXZ() {
			this.canSwapXZ = true;
			return this;
		}

		public abstract void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd);
	}

	static Direction directionToFacing(int direction) {
		return switch (direction) {
			case 0 -> Direction.WEST;
			case 2 -> Direction.EAST;
			case 3 -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
	}

	static BlockState camoNet(BiomeColorType c) {
		return (c == BiomeColorType.DESERT ? TGBlocks.CAMONET_DESERT : c == BiomeColorType.SNOW ? TGBlocks.CAMONET_SNOW : TGBlocks.CAMONET).get()
				.defaultBlockState();
	}

	static BlockState camoNetTop(BiomeColorType c) {
		return (c == BiomeColorType.DESERT ? TGBlocks.CAMONET_TOP_DESERT : c == BiomeColorType.SNOW ? TGBlocks.CAMONET_TOP_SNOW : TGBlocks.CAMONET_TOP)
				.get().defaultBlockState();
	}

	static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
	static final BlockState OAK_FENCE = Blocks.OAK_FENCE.defaultBlockState();

	static BlockState[] crates() {
		BlockState[] c = new BlockState[9];
		for (int i = 0; i < 9; i++) {
			c[i] = LegacyBlocks.tg("military_crate", i);
		}
		return c;
	}

	static BlockState lamp(Direction facing) {
		return GenWorld.withFacing(TGBlocks.LAMP_YELLOW.get().defaultBlockState(), facing);
	}

	/* ------------------------------------------------------------------ Tent */

	public static class Tent extends Part {
		final int type;

		public Tent(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int type) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.type = type;
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			if (type == 0) {
				posX++;
				posZ++;
				sizeX -= 2;
				sizeZ -= 2;
			}
			// 1.12 picked a camo colour here but always placed the woodland net; the biome colour is used now
			BlockState roof = camoNetTop(color);
			BlockState wall = camoNet(color);
			for (int i = 0; i < sizeX; i++) {
				for (int j = 0; j < sizeZ; j++) {
					for (int k = 0; k < sizeY; k++) {
						if (k == 0) {
							w.set(posX + i, posY + k, posZ + j, GRAVEL);
						} else if (k < sizeY - 1) {
							if (j == 0 || j == sizeZ - 1 || i == 0 || i == sizeX - 1) {
								w.set(posX + i, posY + k, posZ + j, wall);
							}
						} else {
							w.set(posX + i, posY + k, posZ + j, roof);
						}
					}
				}
			}
			if (type == 0 || type == 1) {
				switch (direction) {
					case 0 -> {
						w.air(posX, posY + 1, posZ + (sizeZ / 2));
						w.air(posX, posY + 2, posZ + (sizeZ / 2));
						if (type == 0) w.set(posX - 1, posY, posZ + (sizeZ / 2), GRAVEL);
					}
					case 1 -> {
						w.air(posX + (sizeX / 2), posY + 1, posZ);
						w.air(posX + (sizeX / 2), posY + 2, posZ);
						if (type == 0) w.set(posX + (sizeX / 2), posY, posZ - 1, GRAVEL);
					}
					case 2 -> {
						w.air(posX + sizeX - 1, posY + 1, posZ + (sizeZ / 2));
						w.air(posX + sizeX - 1, posY + 2, posZ + (sizeZ / 2));
						if (type == 0) w.set(posX + sizeX, posY, posZ + (sizeZ / 2), GRAVEL);
					}
					default -> {
						w.air(posX + (sizeX / 2), posY + 1, posZ + sizeZ - 1);
						w.air(posX + (sizeX / 2), posY + 2, posZ + sizeZ - 1);
						if (type == 0) w.set(posX + (sizeX / 2), posY, posZ + sizeZ, GRAVEL);
					}
				}
			}
			if (type == 2) {
				for (int y = 1; y < sizeY - 1; y++) {
					if (direction == 0 || direction == 2) {
						for (int z = 1; z < sizeZ - 1; z++) {
							if (z % 3 != 0) {
								w.air(posX + ((direction == 0) ? 0 : sizeX - 1), posY + y, posZ + z);
							}
						}
					} else {
						for (int x = 1; x < sizeX - 1; x++) {
							if (x % 3 != 1) {
								w.air(posX + x, posY + y, posZ + ((direction == 1) ? 0 : sizeZ - 1));
							}
						}
					}
				}
			}
		}
	}

	/* ------------------------------------------------------------------ EmptyPlane */

	public static class EmptyPlane extends Part {
		final BlockState block;

		public EmptyPlane(BlockState block) {
			super(1, 1, 1, -1, -1, -1);
			this.block = block;
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			for (int i = 0; i < sizeX; i++) {
				for (int j = 0; j < sizeZ; j++) {
					w.set(posX + i, posY, posZ + j, block);
				}
			}
		}
	}

	/* ------------------------------------------------------------------ Containers */

	public static class Containers extends Part {
		final int containerWidth, containerHeight;
		final BlockState block;

		public Containers(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int containerWidth, int containerHeight, BlockState block) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.containerWidth = containerWidth;
			this.containerHeight = containerHeight;
			this.block = block;
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			int offset = 0;
			if (sizeX > sizeZ) {
				int count = (int) ((sizeZ + 1.0f) / (containerWidth + 1.0f));
				for (int i = 0; i < count; i++) {
					w.fill(posX, posY + 1, posZ + offset, sizeX, containerHeight, containerWidth, block);
					offset += containerWidth + 1;
				}
			} else {
				int count = (int) ((sizeX + 1.0f) / (containerWidth + 1.0f));
				for (int i = 0; i < count; i++) {
					w.fill(posX + offset, posY + 1, posZ, containerWidth, containerHeight, sizeZ, block);
					offset += containerWidth + 1;
				}
			}
		}
	}

	/* ------------------------------------------------------------------ CampProps */

	public static class CampProps extends Part {
		final int variant;
		final BlockState[] crates = crates();
		final BlockState sandbags = TGBlocks.SANDBAGS.get().defaultBlockState();
		final BlockState lantern = TGBlocks.LANTERN_YELLOW.get().defaultBlockState();

		public CampProps(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int variant) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.variant = variant;
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			BlockState camo = camoNetTop(color);
			int v = variant == -1 ? rnd.nextInt(2) : variant;
			switch (v) {
				case 1 -> {
					setRoof(w, posX, posY, posZ, sizeX, sizeY, sizeZ, camo);
					setCrates(w, posX + 1, posY, posZ + 1, sizeX - 2, sizeY - 2, sizeZ - 2, rnd);
				}
				case 2 -> setSandbags(w, posX, posY, posZ, sizeX, sizeZ, direction);
				case 3 -> {
					setRoof(w, posX, posY + 1, posZ, sizeX, sizeY - 1, sizeZ, camo);
					setSandbags(w, posX, posY, posZ, sizeX, sizeZ, direction);
				}
				case 4 -> setSandbagRow(w, posX, posY, posZ, sizeX, sizeY, sizeZ);
				case 5 -> {
					setRoof(w, posX, posY + 1, posZ, sizeX, sizeY - 1, sizeZ, camo);
					setSandbags(w, posX, posY, posZ, sizeX, sizeZ, direction);
					setCrates(w, posX + 1, posY, posZ + 1, sizeX - 2, sizeY - 2, sizeZ - 2, rnd);
				}
				default -> setCrates(w, posX, posY, posZ, sizeX, sizeY, sizeZ, rnd);
			}
		}

		private void setSandbagRow(GenWorld w, int px, int py, int pz, int sizeX, int sizeY, int sizeZ) {
			int i = 1;
			for (int y = 1; y < sizeY; y++) {
				if (sizeX > sizeZ) {
					for (int x = i; x < sizeX - i; x++) {
						w.set(px + x, py + y, pz + sizeZ / 2, sandbags);
					}
				} else {
					for (int z = i; z < sizeZ - i; z++) {
						w.set(px + sizeX / 2, py + y, pz + z, sandbags);
					}
				}
				i++;
			}
		}

		private void setSandbags(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeZ, int direction) {
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
						w.set(posX + x, posY + 1, posZ + z, sandbags);
					}
				}
			}
			switch (direction) {
				case 0 -> w.air(posX, posY + 1, posZ + sizeZ / 2);
				case 1 -> w.air(posX + sizeX / 2, posY + 1, posZ);
				case 2 -> w.air(posX + sizeX - 1, posY + 1, posZ + sizeZ / 2);
				default -> w.air(posX + sizeX / 2, posY + 1, posZ + sizeZ - 1);
			}
		}

		private void setRoof(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, BlockState camo) {
			w.fill(posX, posY + 1, posZ, 1, sizeY - 2, 1, OAK_FENCE);
			w.fill(posX + sizeX - 1, posY + 1, posZ, 1, sizeY - 2, 1, OAK_FENCE);
			w.fill(posX, posY + 1, posZ + sizeZ - 1, 1, sizeY - 2, 1, OAK_FENCE);
			w.fill(posX + sizeX - 1, posY + 1, posZ + sizeZ - 1, 1, sizeY - 2, 1, OAK_FENCE);
			w.fill(posX, posY + sizeY - 1, posZ, sizeX, 1, sizeZ, camo);
		}

		private boolean isFreeSpace(GenWorld w, int x, int y, int z) {
			BlockState s = w.get(x, y, z);
			return s.isAir() || s.is(Blocks.SNOW) || s.is(TGBlocks.LANTERN_YELLOW.get()) || s.is(TGBlocks.LAMP_YELLOW.get());
		}

		private void setCrates(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, RandomSource rnd) {
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					w.set(posX + x, posY, posZ + z, GRAVEL);
					rnd.nextFloat(); // 1.12 chest roll, the chest variant was disabled
					for (int y = 1; y < sizeY; y++) {
						if (!isFreeSpace(w, posX + x, posY + y - 1, posZ + z) && rnd.nextFloat() > 0.5f) {
							w.set(posX + x, posY + y, posZ + z, crates[rnd.nextInt(crates.length)]);
						}
					}
				}
			}
			if (((sizeX + sizeZ) / 2.0f) / 6.0f > rnd.nextFloat()) { // lantern on top of a crate
				for (int y = sizeY; y > 1; y--) {
					for (int t = 0; t < 3; t++) {
						int x = rnd.nextInt(sizeX);
						int z = rnd.nextInt(sizeZ);
						if (w.isAir(posX + x, posY + y, posZ + z) && !isFreeSpace(w, posX + x, posY + y - 1, posZ + z)) {
							w.set(posX + x, posY + y, posZ + z, lantern);
							return;
						}
					}
				}
			}
		}
	}

	/* ------------------------------------------------------------------ Bunker */

	public static class Bunker extends Part {
		static final String LOOT = "chests/militarybase_bunker";
		final int type;
		final BlockState ground = LegacyBlocks.tg("concrete", 1);
		final BlockState stairs = Blocks.STONE_STAIRS.defaultBlockState();
		final BlockState wall = LegacyBlocks.tg("concrete", 0);
		final BlockState roof = LegacyBlocks.tg("concrete", 2);
		final BlockState sandbags = TGBlocks.SANDBAGS.get().defaultBlockState();
		final BlockState ladder = TGBlocks.LADDER_METAL.get().defaultBlockState();
		final BlockState[] crates = crates();

		public Bunker(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int type) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.type = type;
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			if (sizeX <= 4 && sizeZ >= 5) {
				if (direction == 0 || direction == 2) direction = (direction + 1) % 4;
			} else if (sizeZ <= 4 && sizeX >= 5) {
				if (direction == 1 || direction == 3) direction = (direction + 1) % 4;
			}
			switch (direction) {
				case 0 -> { posX += 2; sizeX -= 2; }
				case 1 -> { posZ += 2; sizeZ -= 2; }
				case 2 -> sizeX -= 2;
				default -> sizeZ -= 2;
			}
			Direction facing = directionToFacing(direction);
			for (int y = -1; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					for (int z = 0; z < sizeZ; z++) {
						boolean edge = x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1;
						if (y == -1) {
							w.set(posX + x, posY + y, posZ + z, ground);
						} else if (y == 0) {
							if (edge) w.set(posX + x, posY + y, posZ + z, wall);
							else w.air(posX + x, posY + y, posZ + z);
						} else if (y == 1) {
							if (type == 2 || type == 3) {
								if (edge) w.set(posX + x, posY + y, posZ + z, wall);
							} else if (((x == 0 || x == sizeX - 1) && (z == 0 || z == sizeZ - 1 || z % 3 == 0))
									|| ((x == 0 || x == sizeX - 1 || x % 3 == 0) && (z == 0 || z == sizeZ - 1))) {
								w.set(posX + x, posY + y, posZ + z, wall);
							}
						} else if (y == 2) {
							w.set(posX + x, posY + y, posZ + z, edge ? wall : roof);
						} else if (y == 3 && (type == 1 || type == 3)) {
							if (edge) w.set(posX + x, posY + y, posZ + z, sandbags);
						}
					}
				}
			}
			int xo, zo, dx, dz;
			switch (direction) {
				case 0 -> { xo = 0; zo = sizeZ / 2; dx = -1; dz = 0; }
				case 1 -> { xo = sizeX / 2; zo = 0; dx = 0; dz = -1; }
				case 2 -> { xo = sizeX - 1; zo = sizeZ / 2; dx = 1; dz = 0; }
				default -> { xo = sizeX / 2; zo = sizeZ - 1; dx = 0; dz = 1; }
			}
			int sx = dx == 0 ? 1 : 0, sz = dz == 0 ? 1 : 0; // sideways
			int bx = posX + xo, bz = posZ + zo;
			w.air(bx, posY + 1, bz);
			w.air(bx, posY, bz);
			w.door(bx, posY, bz, TGBlocks.BUNKER_DOOR.get(), facing, true);
			w.set(bx + sx, posY, bz + sz, wall);
			w.set(bx + sx, posY + 1, bz + sz, wall);
			w.set(bx - sx, posY, bz - sz, wall);
			w.set(bx - sx, posY + 1, bz - sz, wall);
			int[][] ramp = { { dx == 0 ? 1 : dx, dz == 0 ? 1 : dz }, { dx == 0 ? -1 : dx, dz == 0 ? -1 : dz }, { dx == 0 ? 1 : dx * 2, dz == 0 ? 1 : dz * 2 },
					{ dx == 0 ? -1 : dx * 2, dz == 0 ? -1 : dz * 2 } };
			for (int[] r : ramp) {
				w.set(bx + r[0], posY - 1, bz + r[1], wall);
			}
			for (int[] r : ramp) {
				w.set(bx + r[0], posY, bz + r[1], wall);
			}
			w.set(bx + (dx == 0 ? 1 : dx), posY + 2, bz + (dz == 0 ? 1 : dz), lamp(facing.getOpposite()));
			w.set(bx + (dx == 0 ? -1 : dx), posY + 2, bz + (dz == 0 ? -1 : dz), lamp(facing.getOpposite()));
			w.set(bx, posY - 1, bz, ground);
			w.set(bx + dx, posY - 1, bz + dz, ground);
			w.air(bx + dx, posY, bz + dz);
			w.air(bx + dx, posY + 1, bz + dz);
			w.set(bx + dx * 2, posY, bz + dz * 2, GenWorld.withFacing(stairs, facing));
			w.air(bx + dx * 2, posY + 1, bz + dz * 2);
			if (type == 2 || type == 3) {
				w.set(bx - dx, posY + 1, bz - dz, lamp(Direction.UP));
				setCrates(w, posX + 2, posY, posZ + 2, sizeX - 4, 2, sizeZ - 4, rnd);
			} else {
				w.set(posX + sizeX / 2, posY + 1, posZ + sizeZ / 2, lamp(Direction.UP));
			}
			if (type == 1 || type == 3) {
				Direction ladderFacing;
				switch (direction) {
					case 0 -> { xo = sizeX - 2; zo = sizeZ - 2; dx = 1; dz = 0; ladderFacing = Direction.WEST; }
					case 1 -> { xo = 1; zo = sizeZ - 2; dx = 0; dz = 1; ladderFacing = Direction.NORTH; }
					case 2 -> { xo = 1; zo = 1; dx = -1; dz = 0; ladderFacing = Direction.EAST; }
					default -> { xo = sizeX - 2; zo = 1; dx = 0; dz = -1; ladderFacing = Direction.SOUTH; }
				}
				w.air(posX + xo, posY + 2, posZ + zo);
				w.set(posX + xo + dx, posY + 1, posZ + zo + dz, wall);
				BlockState l = ladder.setValue(LadderBlock.FACING, ladderFacing);
				for (int y = 0; y <= 2; y++) {
					w.set(posX + xo, posY + y, posZ + zo, l);
				}
			}
		}

		private void setCrates(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, RandomSource r) {
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					if (r.nextFloat() <= 0.1f) {
						Direction facing = Direction.NORTH;
						for (int i = r.nextInt(4); i > 0; i--) {
							facing = facing.getCounterClockWise();
						}
						w.set(posX + x, posY, posZ + z,
								new MBlock.Chest(Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, facing), LOOT));
					} else {
						for (int y = 0; y < sizeY; y++) {
							if (!w.isAir(posX + x, posY + y - 1, posZ + z) && r.nextFloat() > 0.5) {
								w.set(posX + x, posY + y, posZ + z, crates[r.nextInt(crates.length)]);
							}
						}
					}
				}
			}
		}
	}

	/* ------------------------------------------------------------------ Barracks */

	public static class Barracks extends Part {
		static final String LOOT = "chests/militarybase_barracks";
		final BlockState pillar = LegacyBlocks.vanilla("log", 0);
		final BlockState wallB = LegacyBlocks.vanilla("planks", 0);
		final BlockState floor = LegacyBlocks.vanilla("planks", 0);
		final BlockState scaffold = LegacyBlocks.vanilla("planks", 5);
		final BlockState roof = LegacyBlocks.vanilla("planks", 1);
		final BlockState roofSlab = LegacyBlocks.vanilla("wooden_slab", 1);
		final BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState();
		final BlockState window = Blocks.GLASS_PANE.defaultBlockState();

		public Barracks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		private static BlockState bed(Direction facing, boolean head) {
			return Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, head ? BedPart.HEAD : BedPart.FOOT);
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			Direction facing = directionToFacing(direction);
			switch (direction) {
				case 0 -> { posX += 2; sizeX -= 2; }
				case 1 -> { posZ += 2; sizeZ -= 2; }
				case 2 -> sizeX -= 2;
				default -> sizeZ -= 2;
			}
			sizeX -= (sizeX - 1) % 4;
			sizeZ -= (sizeZ - 1) % 4;
			sizeY = 7;
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					boolean edge = x == 0 || z == 0 || x == sizeX - 1 || z == sizeZ - 1;
					for (int y = 0; y < sizeY; y++) {
						int bx = posX + x, by = posY + y, bz = posZ + z;
						if (y == 0) {
							w.set(bx, by, bz, GRAVEL);
						} else if (y == 1) {
							if (x % 4 == 0 && z % 4 == 0) w.set(bx, by, bz, pillar);
						} else if (y == 2) {
							w.set(bx, by, bz, edge ? scaffold : floor);
						} else if (y <= 4) {
							if (edge) {
								if (x % 4 == 0 && z % 4 == 0) w.set(bx, by, bz, scaffold);
								else if (y == 4 && (x % 4 == 2 || z % 4 == 2)) w.set(bx, by, bz, window);
								else w.set(bx, by, bz, wallB);
							}
						} else if (y == 5) {
							if (edge) w.set(bx, by, bz, scaffold);
						} else if (y == 6) {
							w.set(bx, by, bz, edge ? roofSlab : roof);
						}
					}
				}
			}
			int xo, zo, dx, dz;
			switch (direction) {
				case 0 -> { xo = 0; zo = ((sizeZ / 8) * 4) + 2; dx = -1; dz = 0; }
				case 1 -> { xo = ((sizeX / 8) * 4) + 2; zo = 0; dx = 0; dz = -1; }
				case 2 -> { xo = sizeX - 1; zo = ((sizeZ / 8) * 4) + 2; dx = 1; dz = 0; }
				default -> { xo = ((sizeX / 8) * 4) + 2; zo = sizeZ - 1; dx = 0; dz = 1; }
			}
			w.air(posX + xo, posY + 4, posZ + zo);
			w.air(posX + xo, posY + 3, posZ + zo);
			w.door(posX + xo, posY + 3, posZ + zo, Blocks.OAK_DOOR, facing.getOpposite(), false);
			w.set(posX + xo + dx, posY + 5, posZ + zo + dz, lamp(facing.getOpposite()));
			w.set(posX + xo + dx, posY + 2, posZ + zo + dz, GenWorld.withFacing(stairs, facing.getOpposite()));
			w.set(posX + xo + dx * 2, posY + 1, posZ + zo + dz * 2, GenWorld.withFacing(stairs, facing.getOpposite()));
			int xHalf = sizeX / 2;
			int zHalf = sizeZ / 2;
			for (int x = 1; x < sizeX - 1; x++) {
				for (int z = 1; z < sizeZ - 1; z++) {
					for (int y = 3; y < 6; y++) {
						int bx = posX + x, by = posY + y, bz = posZ + z;
						if (x == 1 || z == 1 || x == sizeX - 2 || z == sizeZ - 2) {
							if (y == 5 && (x % 4 == 2 || z % 4 == 2)) {
								w.set(bx, by, bz, lamp(Direction.UP));
							}
							if (y == 3 && (x % 4 == 0 || z % 4 == 0) && x != xo && z != zo) {
								Direction chestrot = Direction.EAST;
								if (z % 4 == 0) {
									if (x > xHalf) chestrot = chestrot.getOpposite();
								} else if (x % 4 == 0) {
									chestrot = Direction.SOUTH;
									if (z > zHalf) chestrot = chestrot.getOpposite();
								}
								w.set(bx, by, bz, new MBlock.Chest(
										Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, chestrot), LOOT));
							}
						} else {
							if ((x == 2 || z == 2 || x == sizeX - 3 || z == sizeZ - 3) && (sizeX / 4 > 1 || sizeZ / 4 > 1) && y == 3 && x % 4 == 2
									&& z % 4 == 2 && x != xo && z != zo) {
								// 1.12 bed metas: 1 west, 3 east, 2 north, 0 south; head one block further that way
								if (x == 2) {
									w.set(bx, by, bz, bed(Direction.WEST, false));
									w.set(bx - 1, by, bz, bed(Direction.WEST, true));
								} else if (x == sizeX - 3) {
									w.set(bx, by, bz, bed(Direction.EAST, false));
									w.set(bx + 1, by, bz, bed(Direction.EAST, true));
								} else if (z == 2) {
									w.set(bx, by, bz, bed(Direction.NORTH, false));
									w.set(bx, by, bz - 1, bed(Direction.NORTH, true));
								} else if (z == sizeZ - 3) {
									w.set(bx, by, bz, bed(Direction.SOUTH, false));
									w.set(bx, by, bz + 1, bed(Direction.SOUTH, true));
								}
							}
							if (y == 5 && x % 8 == 0 && z % 8 == 0) {
								w.set(bx, by, bz, lamp(Direction.UP));
							}
						}
					}
				}
			}
		}
	}

	/* ------------------------------------------------------------------ Helipad */

	public static class Helipad extends Part {
		final BlockState glow = LegacyBlocks.tg("neonlights", 4);
		final BlockState wireframe = LegacyBlocks.tg("metalpanel", 6);
		final BlockState concrete = LegacyBlocks.tg("concrete", 3);
		final BlockState hazard = LegacyBlocks.vanilla("concrete", 4);

		public Helipad(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			if (sizeX < 7 || sizeZ < 7) {
				return;
			}
			if (sizeX > 9) {
				posX += (sizeX - 9) / 2;
				sizeX = 9;
			}
			if (sizeZ > 9) {
				posZ += (sizeZ - 9) / 2;
				sizeZ = 9;
			}
			if (sizeX == 9 && sizeZ == 9) {
				for (int z = 1; z < sizeZ - 1; z++) {
					w.set(posX, posY, posZ + z, wireframe);
					w.set(posX + sizeX - 1, posY, posZ + z, wireframe);
				}
				posX++;
				sizeX = 7;
				for (int x = 0; x < sizeX; x++) {
					w.set(posX + x, posY, posZ, wireframe);
					w.set(posX + x, posY, posZ + sizeZ - 1, wireframe);
				}
				posZ++;
			}
			sizeX = 7;
			sizeZ = 7;
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					w.set(posX + x, posY + 1, posZ + z, (x == 0 || z == 0 || x == sizeX - 1 || z == sizeZ - 1) ? hazard : concrete);
				}
			}
			int[][] h = { { 2, 2 }, { 3, 2 }, { 4, 2 }, { 3, 3 }, { 2, 4 }, { 3, 4 }, { 4, 4 } };
			for (int[] p : h) {
				w.set(posX + p[0], posY + 1, posZ + p[1], glow);
			}
		}
	}

	/* ------------------------------------------------------------------ Tanks */

	public static class Tanks extends Part {
		final int tankWidth = 4;
		final BlockState base = LegacyBlocks.tg("metalpanel", 7);
		final BlockState wall = LegacyBlocks.tg("metalpanel", 0);
		final BlockState border = LegacyBlocks.tg("metalpanel", 3);
		final BlockState fill = Blocks.LAVA.defaultBlockState();

		public Tanks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		/** one tank row along the length axis; (a, b) = (along, across) mapped to world x/z by the caller */
		private interface Setter {
			void set(int along, int y, int across, BlockState s);
		}

		private void tank(Setter s, int length) {
			int last = length - 2; // 1.12 sizeX-2 / sizeZ-2
			s.set(1, 1, 0, base);
			s.set(last, 1, 0, base);
			s.set(1, 2, 0, base);
			s.set(last, 2, 0, base);
			for (int i = 0; i < length - 2; i++) {
				s.set(1 + i, 1, 1, base);
				s.set(1 + i, 1, 2, base);
			}
			s.set(1, 1, tankWidth - 1, base);
			s.set(last, 1, tankWidth - 1, base);
			s.set(1, 2, tankWidth - 1, base);
			s.set(last, 2, tankWidth - 1, base);
			for (int i = 0; i < length - 2; i++) {
				s.set(1 + i, 2, 1, wall);
				s.set(1 + i, 2, 2, wall);
			}
			s.set(0, 2, 1, border);
			s.set(length - 1, 2, 1, border);
			s.set(0, 2, 2, border);
			s.set(length - 1, 2, 2, border);
			for (int y = 3; y <= 4; y++) {
				for (int i = 0; i < length - 2; i++) {
					s.set(1 + i, y, 0, wall);
					s.set(1 + i, y, 3, wall);
				}
				s.set(0, y, 0, border);
				s.set(length - 1, y, 0, border);
				s.set(0, y, 3, border);
				s.set(length - 1, y, 3, border);
				s.set(0, y, 1, wall);
				s.set(length - 1, y, 1, wall);
				s.set(0, y, 2, wall);
				s.set(length - 1, y, 2, wall);
			}
			for (int i = 0; i < length - 2; i++) {
				s.set(1 + i, 3, 1, fill);
				s.set(1 + i, 3, 2, fill);
				s.set(1 + i, 4, 1, fill);
				s.set(1 + i, 4, 2, fill);
			}
			for (int i = 0; i < length - 2; i++) {
				s.set(1 + i, 5, 1, wall);
				s.set(1 + i, 5, 2, wall);
			}
			s.set(0, 5, 1, border);
			s.set(length - 1, 5, 1, border);
			s.set(0, 5, 2, border);
			s.set(length - 1, 5, 2, border);
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			int offset = 0;
			if (sizeX > sizeZ) {
				int count = (int) ((sizeZ + 1.0f) / (tankWidth + 1.0f));
				for (int i = 0; i < count; i++) {
					int o = offset;
					tank((a, y, b, s) -> w.set(posX + a, posY + y, posZ + o + b, s), sizeX);
					offset += tankWidth + 1;
				}
			} else {
				int count = (int) ((sizeX + 1.0f) / (tankWidth + 1.0f));
				for (int i = 0; i < count; i++) {
					int o = offset;
					tank((a, y, b, s) -> w.set(posX + o + b, posY + y, posZ + a, s), sizeZ);
					offset += tankWidth + 1;
				}
			}
		}
	}

	/* ------------------------------------------------------------------ WatchTowerSmall */

	public static class WatchTowerSmall extends Part {
		final int towerSize;

		public WatchTowerSmall(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int towerSize) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.towerSize = towerSize;
		}

		@Override
		public void build(GenWorld w, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType color,
				RandomSource rnd) {
			if (sizeX >= towerSize + 2) posX += (sizeX - towerSize) / 2;
			if (sizeZ >= towerSize + 2) posZ += (sizeZ - towerSize) / 2;
			BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
			for (int y = 1; y < sizeY - 1; y++) {
				w.set(posX, posY + y, posZ, OAK_FENCE);
				w.set(posX + towerSize - 1, posY + y, posZ, OAK_FENCE);
				w.set(posX, posY + y, posZ + towerSize - 1, OAK_FENCE);
				w.set(posX + towerSize - 1, posY + y, posZ + towerSize - 1, OAK_FENCE);
				if (y < sizeY - 3) {
					for (int x = 1; x < towerSize - 1; x++) {
						for (int z = 1; z < towerSize - 1; z++) {
							w.set(posX + x, posY + y, posZ + z, planks);
						}
					}
				}
			}
			w.fillHollow(posX, posY + sizeY - 4, posZ, towerSize, 2, towerSize, OAK_FENCE);
			w.fill(posX, posY + sizeY - 1, posZ, towerSize, 1, towerSize, TGBlocks.CAMONET_TOP.get().defaultBlockState());
			int xo, zo;
			Direction ladder;
			switch (direction) {
				case 0 -> { xo = 0; zo = 1; ladder = Direction.WEST; }
				case 1 -> { xo = 1; zo = 0; ladder = Direction.NORTH; }
				case 2 -> { xo = towerSize - 1; zo = towerSize - 2; ladder = Direction.EAST; }
				default -> { xo = towerSize - 2; zo = towerSize - 1; ladder = Direction.SOUTH; }
			}
			for (int y = 1; y < sizeY - 3; y++) {
				w.set(posX + xo, posY + y, posZ + zo, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladder));
			}
			// 1.12 fence gate meta (direction+1)%4 = horizontal index: 0 south, 1 west, 2 north, 3 east
			Direction gate = Direction.from2DDataValue((direction + 1) % 4);
			w.set(posX + xo, posY + sizeY - 3, posZ + zo, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, gate));
		}
	}
}
