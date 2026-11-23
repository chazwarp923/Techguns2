package techguns.world.gen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import techguns.blocks.BlockCamoNet;
import techguns.blocks.BlockSandbags;

/**
 * Where a structure is being placed: the level, the chunk box writes are clipped to (structures are placed chunk by
 * chunk, the whole placement runs once per chunk), and the per structure random values. Random choices per block are
 * derived from the position, so every chunk pass agrees.
 */
public class PlaceContext {

	public enum BiomeColorType {
		WOODLAND, SNOW, DESERT, NETHER
	}

	public final WorldGenLevel level;
	public final BoundingBox box;
	public final long seed;
	public final BiomeColorType biome;
	public final int indexRoll;
	/** connecting blocks placed in this chunk, their shapes are recomputed once everything is placed */
	private final List<BlockPos> connecting = new ArrayList<>();

	public PlaceContext(WorldGenLevel level, BoundingBox box, long seed, BiomeColorType biome, int indexRoll) {
		this.level = level;
		this.box = box;
		this.seed = seed;
		this.biome = biome;
		this.indexRoll = indexRoll;
	}

	public boolean inside(BlockPos pos) {
		return box.isInside(pos) && pos.getY() >= level.getMinBuildHeight() + 1 && pos.getY() < level.getMaxBuildHeight();
	}

	/** deterministic random for one block position */
	public RandomSource random(BlockPos pos) {
		return RandomSource.create(Mth.getSeed(pos) ^ seed);
	}

	public BlockState get(BlockPos pos) {
		return level.getBlockState(pos);
	}

	/** sets a block (no neighbour updates, like 1.12 flag 2); connected blocks get their shape fixed when the chunk finishes */
	public boolean set(BlockPos pos, BlockState state) {
		if (!inside(pos)) {
			return false;
		}
		level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		if (connects(state.getBlock())) {
			BlockPos p = pos.immutable();
			connecting.add(p);
			// neighbours in a chunk that is not placed yet: vanilla re-runs the shape when the chunk becomes full
			level.getChunk(p).markPosForPostprocessing(p);
		}
		return true;
	}

	/** blocks whose state depends on their neighbours (fences, panes, walls, stairs, sandbags, camo nets...) */
	static boolean connects(Block b) {
		return b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock || b instanceof FenceGateBlock
				|| b instanceof BlockSandbags || b instanceof BlockCamoNet || b instanceof RedStoneWireBlock;
	}

	/** recomputes the connections of everything placed, like the block updates a player placing them would cause */
	public void finishShapes() {
		for (BlockPos pos : connecting) {
			BlockState state = level.getBlockState(pos);
			BlockState updated = Block.updateFromNeighbourShapes(state, level, pos);
			if (updated != state) {
				level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
			}
		}
		connecting.clear();
	}
}
