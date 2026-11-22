package techguns.blocks.machines;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import techguns.tileentities.MultiBlockMachineTileEntMaster;
import techguns.tileentities.MultiBlockSlaveTileEnt;

/**
 * Housing / glass part of a multiblock machine (1.12 MultiBlockMachine slave types). While formed the block model is
 * empty, the controller renders the whole machine; clicking a formed part opens the controller gui. Which parts forward
 * capabilities and their formed shape is decided by the controller.
 */
public class BlockMultiBlockSlave extends BaseEntityBlock {

	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	public BlockMultiBlockSlave(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		throw new UnsupportedOperationException("multiblock blocks are not data driven");
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MultiBlockSlaveTileEnt(pos, state);
	}

	/** the formed shape comes from the controller */
	@Override
	public boolean hasDynamicShape() {
		return true;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		if (state.getValue(FORMED) && level.getBlockEntity(pos) instanceof MultiBlockSlaveTileEnt slave) {
			MultiBlockMachineTileEntMaster m = slave.getMaster();
			if (m != null) {
				return m.getSlaveShape(slave.getSlaveType(), pos);
			}
		}
		return Shapes.block();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(FORMED) || !(level.getBlockEntity(pos) instanceof MultiBlockSlaveTileEnt slave) || !slave.hasMaster()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide && player instanceof ServerPlayer sp && slave.getMaster() instanceof MultiBlockMachineTileEntMaster master) {
			if (master.isUseableByPlayer(player)) {
				BlockPos mp = master.getBlockPos();
				sp.openMenu(master, buf -> buf.writeBlockPos(mp));
			} else {
				player.displayClientMessage(Component.translatable("techguns.container.security.denied"), true);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MultiBlockSlaveTileEnt slave) {
			slave.onBlockBreak();
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** formed parts stay in place */
	@Override
	@Nullable
	public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
		return state.getValue(FORMED) ? net.minecraft.world.level.material.PushReaction.BLOCK : null;
	}
}
