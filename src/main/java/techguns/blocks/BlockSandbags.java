package techguns.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Fence-like sandbag wall with filled corners. */
public class BlockSandbags extends Block {
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final BooleanProperty CORNER_NE = BooleanProperty.create("corner_ne");
	public static final BooleanProperty CORNER_ES = BooleanProperty.create("corner_es");
	public static final BooleanProperty CORNER_SW = BooleanProperty.create("corner_sw");
	public static final BooleanProperty CORNER_WN = BooleanProperty.create("corner_wn");

	protected static final VoxelShape PILLAR = Block.box(4, 0, 4, 12, 16, 12);
	protected static final VoxelShape S_NORTH = Block.box(4, 0, 0, 12, 16, 4);
	protected static final VoxelShape S_SOUTH = Block.box(4, 0, 12, 12, 16, 16);
	protected static final VoxelShape S_WEST = Block.box(0, 0, 4, 4, 16, 12);
	protected static final VoxelShape S_EAST = Block.box(12, 0, 4, 16, 16, 12);

	public BlockSandbags(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
				.setValue(WEST, false).setValue(CORNER_NE, false).setValue(CORNER_ES, false).setValue(CORNER_SW, false).setValue(CORNER_WN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, CORNER_NE, CORNER_ES, CORNER_SW, CORNER_WN);
	}

	protected boolean canConnect(BlockGetter level, BlockPos pos, Direction dir) {
		BlockPos other = pos.relative(dir);
		BlockState s = level.getBlockState(other);
		Block b = s.getBlock();
		if (b instanceof BlockSandbags || b instanceof FenceBlock || b instanceof FenceGateBlock) {
			return true;
		}
		return !isExceptionForConnection(s) && s.isFaceSturdy(level, other, dir.getOpposite());
	}

	protected boolean canConnectCorner(BlockGetter level, BlockPos pos, Direction f1, Direction f2) {
		return canConnect(level, pos, f1) && canConnect(level, pos, f2)
				&& (canConnect(level, pos.relative(f1), f2) || canConnect(level, pos.relative(f2), f1));
	}

	protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
		return state.setValue(NORTH, canConnect(level, pos, Direction.NORTH))
				.setValue(EAST, canConnect(level, pos, Direction.EAST))
				.setValue(SOUTH, canConnect(level, pos, Direction.SOUTH))
				.setValue(WEST, canConnect(level, pos, Direction.WEST))
				.setValue(CORNER_NE, canConnectCorner(level, pos, Direction.NORTH, Direction.EAST))
				.setValue(CORNER_ES, canConnectCorner(level, pos, Direction.EAST, Direction.SOUTH))
				.setValue(CORNER_SW, canConnectCorner(level, pos, Direction.SOUTH, Direction.WEST))
				.setValue(CORNER_WN, canConnectCorner(level, pos, Direction.WEST, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return computeState(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		// Corners depend on diagonal neighbours, so recompute everything on any horizontal change.
		return dir.getAxis().isHorizontal() ? computeState(state, level, pos) : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		VoxelShape shape = PILLAR;
		if (state.getValue(NORTH)) shape = Shapes.or(shape, S_NORTH);
		if (state.getValue(EAST)) shape = Shapes.or(shape, S_EAST);
		if (state.getValue(SOUTH)) shape = Shapes.or(shape, S_SOUTH);
		if (state.getValue(WEST)) shape = Shapes.or(shape, S_WEST);
		return shape;
	}
}
