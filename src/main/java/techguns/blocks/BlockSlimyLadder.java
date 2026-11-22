package techguns.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Slimy trail of bug nests (1.12 BlockTGSlimyLadder): a 1px climbable slime layer on walls. */
public class BlockSlimyLadder extends LadderBlock {
	protected static final VoxelShape EAST = Block.box(0, 0, 0, 1, 16, 16);
	protected static final VoxelShape WEST = Block.box(15, 0, 0, 16, 16, 16);
	protected static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 1);
	protected static final VoxelShape NORTH = Block.box(0, 0, 15, 16, 16, 16);

	public BlockSlimyLadder(Properties props) {
		super(props);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return switch (state.getValue(FACING)) {
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			default -> EAST;
		};
	}
}
