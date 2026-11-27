package techguns.blocks.machines;

import techguns.blocks.BlockRotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import techguns.blocks.machines.multiblocks.MultiBlockSchematic;
import techguns.tileentities.MultiBlockMachineTileEntMaster;

/**
 * Controller of a multiblock machine. Right click a side while unformed (not sneaking) to form the multiblock with that
 * side as front; formed or sneaking it opens the gui.
 */
public class BlockMultiBlockController<T extends MultiBlockMachineTileEntMaster> extends BlockMachine<T> {

	public static final BooleanProperty FORMED = BlockMultiBlockSlave.FORMED;
	/** front of the formed multiblock */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public BlockMultiBlockController(Properties props, Supplier<BlockEntityType<T>> type) {
		super(props, type, RenderShape.MODEL);
		this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false).setValue(FACING, Direction.SOUTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED, FACING);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(FORMED) && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MultiBlockMachineTileEntMaster master) {
			MultiBlockSchematic schematic = master.getSchematic();
			Direction side = hit.getDirection();
			if (schematic != null && schematic.canFormFromSide(side)) {
				if (!level.isClientSide) {
					if (!master.isUseableByPlayer(player)) {
						player.displayClientMessage(net.minecraft.network.chat.Component.translatable("techguns.container.security.denied"), true);
					} else if (schematic.checkForm(level, player, pos, side)) {
						schematic.form(level, master, side);
					}
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
		}
		return super.useWithoutItem(state, level, pos, player, hit);
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.BLOCK;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rot) {
		return BlockRotation.rotate(state, FACING, rot);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return BlockRotation.mirror(state, FACING, mirror);
	}
}
