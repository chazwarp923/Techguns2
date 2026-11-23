package techguns.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Camo netting (1.12 BlockTGCamoNet / BlockTGCamoNetTop). The wall version is a pole with nets to neighbouring nets, the
 * top version a thin roof that grows towards neighbouring tops. Any camo type connects to any other. Waterloggable.
 */
public class BlockCamoNet extends Block implements SimpleWaterloggedBlock {

	public static final BooleanProperty NORTH = PipeBlock.NORTH;
	public static final BooleanProperty EAST = PipeBlock.EAST;
	public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
	public static final BooleanProperty WEST = PipeBlock.WEST;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape POLE = box(7, 0, 7, 9, 16, 9);
	private static final VoxelShape POLE_N = box(7, 0, 0, 9, 16, 7);
	private static final VoxelShape POLE_E = box(9, 0, 7, 16, 16, 9);
	private static final VoxelShape POLE_S = box(7, 0, 9, 9, 16, 16);
	private static final VoxelShape POLE_W = box(0, 0, 7, 7, 16, 9);

	/** indexed by n*8 + e*4 + s*2 + w (1.12 EnumConnectionType) */
	private static final VoxelShape[] TOP_SHAPES = { box(4, 0, 4, 12, 1, 12), box(0, 0, 4, 9, 1, 12), box(4, 0, 7, 12, 1, 16),
			box(0, 0, 7, 9, 1, 16), box(7, 0, 4, 16, 1, 12), box(0, 0, 4, 16, 1, 12), box(7, 0, 7, 16, 1, 16), box(0, 0, 7, 16, 1, 16),
			box(4, 0, 0, 12, 1, 9), box(0, 0, 0, 9, 1, 9), box(4, 0, 0, 12, 1, 16), box(0, 0, 0, 9, 1, 16), box(7, 0, 0, 16, 1, 9),
			box(0, 0, 0, 16, 1, 9), box(7, 0, 0, 16, 1, 16), box(0, 0, 0, 16, 1, 16) };
	private static final VoxelShape[] POLE_SHAPES = new VoxelShape[16];

	static {
		for (int i = 0; i < 16; i++) {
			VoxelShape s = POLE;
			if ((i & 8) != 0) s = Shapes.or(s, POLE_N);
			if ((i & 4) != 0) s = Shapes.or(s, POLE_E);
			if ((i & 2) != 0) s = Shapes.or(s, POLE_S);
			if ((i & 1) != 0) s = Shapes.or(s, POLE_W);
			POLE_SHAPES[i] = s;
		}
	}

	protected final boolean top;

	public BlockCamoNet(boolean top, Properties props) {
		super(props);
		this.top = top;
		this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
				.setValue(WATERLOGGED, false));
	}

	public boolean isTop() {
		return top;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED);
	}

	protected boolean connectsTo(BlockState other) {
		return other.getBlock() instanceof BlockCamoNet net && net.top == this.top;
	}

	protected BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
		return state.setValue(NORTH, connectsTo(level.getBlockState(pos.north()))).setValue(EAST, connectsTo(level.getBlockState(pos.east())))
				.setValue(SOUTH, connectsTo(level.getBlockState(pos.south()))).setValue(WEST, connectsTo(level.getBlockState(pos.west())));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return withConnections(this.defaultBlockState().setValue(WATERLOGGED, placedInWater(ctx)), ctx.getLevel(), ctx.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		tickWater(state, level, pos);
		if (dir.getAxis().isHorizontal()) {
			return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), connectsTo(neighbour));
		}
		return state;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	/** waterlogged when placed into water, like vanilla fences and walls */
	private static boolean placedInWater(BlockPlaceContext ctx) {
		return ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER;
	}

	/** keeps the water flowing out of a waterlogged block */
	private static void tickWater(BlockState state, LevelAccessor level, BlockPos pos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
	}

	private static int index(BlockState s) {
		return (s.getValue(NORTH) ? 8 : 0) | (s.getValue(EAST) ? 4 : 0) | (s.getValue(SOUTH) ? 2 : 0) | (s.getValue(WEST) ? 1 : 0);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return top ? TOP_SHAPES[index(state)] : POLE_SHAPES[index(state)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return top ? TOP_SHAPES[index(state)] : POLE_SHAPES[index(state)];
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
