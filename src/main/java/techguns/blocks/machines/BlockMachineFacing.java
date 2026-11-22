package techguns.blocks.machines;

import java.util.function.Supplier;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import techguns.tileentities.BasicTGTileEntity;

/**
 * Machine with a json block model and a horizontal facing blockstate (1.12 SimpleMachine / SimpleMachine2 blocks).
 * The front faces the player who placed it.
 */
public class BlockMachineFacing<T extends BasicTGTileEntity> extends BlockMachine<T> {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public BlockMachineFacing(Properties props, Supplier<BlockEntityType<T>> type) {
		super(props, type, RenderShape.MODEL);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
