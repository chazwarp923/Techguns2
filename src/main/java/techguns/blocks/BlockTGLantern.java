package techguns.blocks;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Lantern variant of the Techguns lamp: draws brackets towards every sturdy neighbour face. */
public class BlockTGLantern extends Block {
	public static final Map<Direction, BooleanProperty> PROPS = PipeBlock.PROPERTY_BY_DIRECTION;
	protected static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);

	public BlockTGLantern(Properties props) {
		super(props);
		BlockState s = this.stateDefinition.any();
		for (BooleanProperty p : PROPS.values()) {
			s = s.setValue(p, false);
		}
		this.registerDefaultState(s);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		PROPS.values().forEach(builder::add);
	}

	protected boolean connects(BlockGetter level, BlockPos pos, Direction dir) {
		BlockPos other = pos.relative(dir);
		return level.getBlockState(other).isFaceSturdy(level, other, dir.getOpposite());
	}

	protected BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			state = state.setValue(PROPS.get(d), connects(level, pos, d));
		}
		return state;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return state.setValue(PROPS.get(dir), connects(level, pos, dir));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
