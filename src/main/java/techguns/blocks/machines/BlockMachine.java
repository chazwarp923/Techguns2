package techguns.blocks.machines;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import techguns.tileentities.BasicInventoryTileEnt;
import techguns.tileentities.BasicMachineTileEnt;
import techguns.tileentities.BasicOwnedTileEnt;
import techguns.tileentities.BasicRedstoneTileEnt;
import techguns.tileentities.BasicTGTileEntity;

/**
 * Block of one machine (the 1.12 BasicMachine / SimpleMachine metadata blocks are flattened to one block per
 * machine). The machine itself is the block entity; rotation is stored there like in 1.12.
 */
public class BlockMachine<T extends BasicTGTileEntity> extends BaseEntityBlock {

	protected final Supplier<BlockEntityType<T>> type;
	protected final RenderShape renderShape;

	public BlockMachine(Properties props, Supplier<BlockEntityType<T>> type, RenderShape renderShape) {
		super(props);
		this.type = type;
		this.renderShape = renderShape;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		throw new UnsupportedOperationException("machine blocks are not data driven");
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return type.get().create(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return renderShape;
	}

	@Nullable
	@Override
	public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> t) {
		if (t != type.get()) {
			return null;
		}
		return (l, p, s, be) -> {
			if (be instanceof BasicMachineTileEnt m) {
				BasicMachineTileEnt.tick(l, p, s, m);
			}
		};
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		BlockEntity be = level.getBlockEntity(pos);
		if (placer instanceof Player ply && be instanceof BasicOwnedTileEnt owned) {
			owned.setOwner(ply);
		}
		if (placer != null && be instanceof BasicTGTileEntity tg && tg.hasRotation()) {
			tg.rotation = (byte) (Mth.floor(placer.getYRot() * 4.0F / 360.0F + 0.5D) & 3);
		}
		if (be instanceof BasicRedstoneTileEnt rs) {
			rs.onNeighborBlockChange();
		}
		if (be instanceof BasicTGTileEntity tg) {
			tg.needUpdate();
		}
	}

	/** fluid containers fill / drain machines with tanks, everything else opens the gui */
	@Override
	protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof techguns.tileentities.IFluidTankTile tanks
				&& net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent()) {
			if (level.isClientSide || tanks.onFluidContainerInteract(player, hand)) {
				return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
		}
		return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof BasicTGTileEntity tile)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide && player instanceof ServerPlayer sp) {
			if (tile.isUseableByPlayer(player)) {
				sp.openMenu(tile, buf -> buf.writeBlockPos(pos));
			} else {
				player.displayClientMessage(Component.translatable("techguns.container.security.denied"), true);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);
		if (level.getBlockEntity(pos) instanceof BasicRedstoneTileEnt rs) {
			rs.onNeighborBlockChange();
		}
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BasicInventoryTileEnt inv) {
			inv.onBlockBreak();
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
