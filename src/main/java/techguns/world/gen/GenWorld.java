package techguns.world.gen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * In-memory stand-in for the 1.12 World a procedural structure generator wrote into. The generator runs once per
 * structure (deterministic from the structure seed), every chunk of the structure then copies the part inside it into the
 * real level. Reads of positions the generator did not write come from a fallback (usually "air above the ground,
 * solid below") instead of the real world, so they give the same answer no matter which chunk is generating.
 */
public class GenWorld {

	private final Map<BlockPos, Object> blocks = new HashMap<>();
	private final Predicate<BlockPos> solidFallback;
	public final RandomSource rand;
	private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

	private static final BlockState FALLBACK_SOLID = Blocks.STONE.defaultBlockState();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	public GenWorld(RandomSource rand, Predicate<BlockPos> solidFallback) {
		this.rand = rand;
		this.solidFallback = solidFallback;
	}

	private void put(BlockPos pos, Object value) {
		blocks.put(pos, value);
		minX = Math.min(minX, pos.getX());
		minY = Math.min(minY, pos.getY());
		minZ = Math.min(minZ, pos.getZ());
		maxX = Math.max(maxX, pos.getX());
		maxY = Math.max(maxY, pos.getY());
		maxZ = Math.max(maxZ, pos.getZ());
	}

	public void set(int x, int y, int z, BlockState state) {
		put(new BlockPos(x, y, z), state);
	}

	public void set(BlockPos pos, BlockState state) {
		put(pos.immutable(), state);
	}

	/** an MBlock with block entity setup or a random choice (chests, spawners, multi blocks), unrotated */
	public void set(int x, int y, int z, MBlock block) {
		put(new BlockPos(x, y, z), block);
	}

	public void air(int x, int y, int z) {
		set(x, y, z, AIR);
	}

	public BlockState get(int x, int y, int z) {
		return get(new BlockPos(x, y, z));
	}

	public BlockState get(BlockPos pos) {
		Object o = blocks.get(pos);
		if (o instanceof BlockState s) {
			return s;
		} else if (o instanceof MBlock mb) {
			return mb.state;
		}
		return solidFallback.test(pos) ? FALLBACK_SOLID : AIR;
	}

	public boolean isAir(int x, int y, int z) {
		return get(x, y, z).isAir();
	}

	/** a full block something can be attached to (not air, plants, ladders or fluids) */
	public boolean isSolid(BlockPos pos) {
		return get(pos).isSolid();
	}

	/** 1.12 BlockUtils.fillBlocks */
	public void fill(int x, int y, int z, int sizeX, int sizeY, int sizeZ, BlockState state) {
		for (int i = 0; i < sizeX; i++) {
			for (int j = 0; j < sizeZ; j++) {
				for (int k = 0; k < sizeY; k++) {
					set(x + i, y + k, z + j, state);
				}
			}
		}
	}

	/** 1.12 BlockUtils.fillBlocksHollow: only the side walls */
	public void fillHollow(int x, int y, int z, int sizeX, int sizeY, int sizeZ, BlockState state) {
		for (int i = 0; i < sizeX; i++) {
			for (int j = 0; j < sizeZ; j++) {
				if (j == 0 || j == sizeZ - 1 || i == 0 || i == sizeX - 1) {
					for (int k = 0; k < sizeY; k++) {
						set(x + i, y + k, z + j, state);
					}
				}
			}
		}
	}

	/** both halves of a door, as ItemDoor.placeDoor */
	public void door(int x, int y, int z, Block door, Direction facing, boolean rightHinge) {
		BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing)
				.setValue(DoorBlock.HINGE, rightHinge ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
		set(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		set(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** 1.12 BlockRotator.getWithFacing */
	public static BlockState withFacing(BlockState state, Direction facing) {
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return facing.getAxis().isHorizontal() ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing) : state;
		} else if (state.hasProperty(BlockStateProperties.FACING)) {
			return state.setValue(BlockStateProperties.FACING, facing);
		}
		return state;
	}

	public boolean isEmpty() {
		return blocks.isEmpty();
	}

	public BoundingBox bounds() {
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	/** copies the part of the structure inside the context's chunk box into the level */
	private static BlockState stateOf(Object o) {
		if (o instanceof BlockState s) {
			return s;
		}
		return o != null && o.getClass() == MBlock.class || o instanceof MBlock.Chest ? ((MBlock) o).state : null;
	}

	/**
	 * For blocks converted from 1.12 data one by one: each door half takes the properties 1.12 only stored in the other
	 * half, and single chests side by side with the same facing become double chests (as ScannedStructure does).
	 */
	public void mergeLegacyPairs() {
		Map<BlockPos, Object> changes = new HashMap<>();
		for (Map.Entry<BlockPos, Object> e : blocks.entrySet()) {
			BlockState s = stateOf(e.getValue());
			if (s == null || !(s.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)) {
				continue;
			}
			BlockPos pos = e.getKey();
			boolean upper = s.getValue(net.minecraft.world.level.block.DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
			BlockState other = stateOf(blocks.get(upper ? pos.below() : pos.above()));
			if (other == null || other.getBlock() != s.getBlock()
					|| other.getValue(net.minecraft.world.level.block.DoorBlock.HALF) == s.getValue(net.minecraft.world.level.block.DoorBlock.HALF)) {
				continue;
			}
			BlockState lower = upper ? other : s;
			BlockState top = upper ? s : other;
			changes.put(pos, s.setValue(net.minecraft.world.level.block.DoorBlock.FACING, lower.getValue(net.minecraft.world.level.block.DoorBlock.FACING))
					.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, lower.getValue(net.minecraft.world.level.block.DoorBlock.OPEN))
					.setValue(net.minecraft.world.level.block.DoorBlock.HINGE, top.getValue(net.minecraft.world.level.block.DoorBlock.HINGE))
					.setValue(net.minecraft.world.level.block.DoorBlock.POWERED, top.getValue(net.minecraft.world.level.block.DoorBlock.POWERED)));
		}
		blocks.putAll(changes);

		java.util.Set<BlockPos> paired = new java.util.HashSet<>();
		java.util.List<BlockPos> chests = new java.util.ArrayList<>();
		for (Map.Entry<BlockPos, Object> e : blocks.entrySet()) {
			BlockState s = stateOf(e.getValue());
			if (s != null && s.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
				chests.add(e.getKey());
			}
		}
		chests.sort(java.util.Comparator.comparingLong(BlockPos::asLong)); // fixed order: same pairs every time
		for (BlockPos pos : chests) {
			if (paired.contains(pos)) {
				continue;
			}
			BlockState s = stateOf(blocks.get(pos));
			net.minecraft.core.Direction facing = s.getValue(net.minecraft.world.level.block.ChestBlock.FACING);
			BlockPos otherPos = pos.relative(facing.getClockWise());
			BlockState o = paired.contains(otherPos) ? null : stateOf(blocks.get(otherPos));
			if (o != null && o.getBlock() == s.getBlock() && o.getValue(net.minecraft.world.level.block.ChestBlock.FACING) == facing) {
				setChestType(pos, net.minecraft.world.level.block.state.properties.ChestType.LEFT);
				setChestType(otherPos, net.minecraft.world.level.block.state.properties.ChestType.RIGHT);
				paired.add(pos);
				paired.add(otherPos);
			}
		}
	}

	private void setChestType(BlockPos pos, net.minecraft.world.level.block.state.properties.ChestType type) {
		Object o = blocks.get(pos);
		BlockState s = stateOf(o).setValue(net.minecraft.world.level.block.ChestBlock.TYPE, type);
		blocks.put(pos, o instanceof MBlock.Chest c ? c.withState(s) : s);
	}

	public void apply(PlaceContext ctx) {
		for (Map.Entry<BlockPos, Object> e : blocks.entrySet()) {
			BlockPos pos = e.getKey();
			if (!ctx.inside(pos)) {
				continue;
			}
			if (e.getValue() instanceof BlockState s) {
				ctx.set(pos, s);
			} else if (e.getValue() instanceof MBlock mb) {
				mb.place(ctx, pos, 0);
			}
		}
	}
}
