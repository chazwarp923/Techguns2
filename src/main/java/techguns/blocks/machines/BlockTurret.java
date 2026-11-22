package techguns.blocks.machines;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import techguns.TGBlockEntities;
import techguns.tileentities.TurretTileEnt;

/**
 * Turret base (1.12 BasicMachine TURRET). Placed against the bottom of a block the turret hangs below it, otherwise it
 * stands on top.
 */
public class BlockTurret extends BlockMachine<TurretTileEnt> {

	public static final DirectionProperty FACING = BlockStateProperties.VERTICAL_DIRECTION;

	public BlockTurret(Properties props) {
		super(props, TGBlockEntities.TURRET, RenderShape.ENTITYBLOCK_ANIMATED);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getClickedFace() == Direction.DOWN ? Direction.DOWN : Direction.UP);
	}

	@Nullable
	@Override
	public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> t) {
		if (level.isClientSide || t != type.get()) {
			return null;
		}
		return (l, p, s, be) -> {
			if (be instanceof TurretTileEnt te) {
				TurretTileEnt.tick(l, p, s, te);
			}
		};
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurretTileEnt te && !te.turretDeath) {
			te.spawnTurret(-1f);
		}
	}
}
