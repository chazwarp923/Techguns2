package techguns.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Metal ladders; 2px thick instead of vanilla 3px. */
public class BlockTGLadder extends LadderBlock {
	protected static final double SIZE = 2.0;
	protected static final VoxelShape EAST = Block.box(0, 0, 0, SIZE, 16, 16);
	protected static final VoxelShape WEST = Block.box(16 - SIZE, 0, 0, 16, 16, 16);
	protected static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, SIZE);
	protected static final VoxelShape NORTH = Block.box(0, 0, 16 - SIZE, 16, 16, 16);

	public BlockTGLadder(Properties props) {
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
