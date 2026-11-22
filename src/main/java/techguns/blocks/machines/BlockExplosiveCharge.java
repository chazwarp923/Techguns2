package techguns.blocks.machines;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import techguns.TGBlockEntities;
import techguns.tileentities.ExplosiveChargeTileEnt;

/**
 * Mining charge block (1.12 BlockExplosiveCharge, TNT and advanced / C4 variant). FACING points into the block it is
 * stuck to; the charge drops when that block is gone.
 */
public class BlockExplosiveCharge extends BlockMachine<ExplosiveChargeTileEnt> {

	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	public static final BooleanProperty ARMED = BooleanProperty.create("armed");

	/** indexed by Direction 3D data value */
	protected static final VoxelShape[] SHAPES = { box(2, 0, 2, 14, 5, 14), box(2, 11, 2, 14, 16, 14), box(2, 2, 0, 14, 14, 5),
			box(2, 2, 11, 14, 14, 16), box(0, 2, 2, 5, 14, 14), box(11, 2, 2, 16, 14, 14) };

	protected final boolean advanced;

	public BlockExplosiveCharge(Properties props, boolean advanced) {
		super(props, TGBlockEntities.EXPLOSIVE_CHARGE, RenderShape.MODEL);
		this.advanced = advanced;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ARMED, false));
	}

	public boolean isAdvanced() {
		return advanced;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(FACING, ARMED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(FACING).get3DDataValue()];
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState s = this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
		return s.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? s : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return !level.getBlockState(pos.relative(state.getValue(FACING))).isAir();
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (dir == state.getValue(FACING) && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof ExplosiveChargeTileEnt charge) {
			level.playSound(null, pos, charge.getSoundPlant(), SoundSource.BLOCKS, 1.0f, 1.0f);
		}
	}

	@Nullable
	@Override
	public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> t) {
		if (level.isClientSide || t != TGBlockEntities.EXPLOSIVE_CHARGE.get()) {
			return null;
		}
		return (l, p, s, be) -> ExplosiveChargeTileEnt.serverTick(l, p, s, (ExplosiveChargeTileEnt) be);
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
