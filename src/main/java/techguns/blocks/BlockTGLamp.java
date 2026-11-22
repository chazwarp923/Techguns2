package techguns.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wall/ceiling lamp. FACING is the direction pointing away from the supporting block
 * (legacy stored the opposite of the clicked face, i.e. towards the support; models are built
 * for that, so FACING here means "towards support" as in 1.12.2).
 */
public class BlockTGLamp extends DirectionalBlock {
	public static final MapCodec<BlockTGLamp> CODEC = simpleCodec(BlockTGLamp::new);

	protected static final double S = 4.0, SM = 12.0, H = 3.0;
	protected static final VoxelShape DOWN = Block.box(S, 0, S, SM, H, SM);
	protected static final VoxelShape UP = Block.box(S, 16 - H, S, SM, 16, SM);
	protected static final VoxelShape NORTH = Block.box(S, S, 0, SM, SM, H);
	protected static final VoxelShape SOUTH = Block.box(S, S, 16 - H, SM, SM, 16);
	protected static final VoxelShape WEST = Block.box(0, S, S, H, SM, SM);
	protected static final VoxelShape EAST = Block.box(16 - H, S, S, 16, SM, SM);

	public BlockTGLamp(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return switch (state.getValue(FACING)) {
			case UP -> UP;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
			default -> DOWN;
		};
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction toSupport = state.getValue(FACING);
		BlockPos support = pos.relative(toSupport);
		return level.getBlockState(support).isFaceSturdy(level, support, toSupport.getOpposite());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (dir == state.getValue(FACING) && !state.canSurvive(level, pos)) {
			return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
	}
}
