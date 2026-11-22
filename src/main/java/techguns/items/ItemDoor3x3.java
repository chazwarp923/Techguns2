package techguns.items;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import techguns.blocks.door.BlockDoor3x3;
import techguns.tileentities.Door3x3TileEnt;

/**
 * Places a 3x3 door (1.12 ItemTGDoor3x3): the clicked position is the bottom middle, the door faces away from the
 * player.
 */
public class ItemDoor3x3 extends BlockItem {

	public ItemDoor3x3(BlockDoor3x3 block, Properties props) {
		super(block, props);
	}

	@Override
	public InteractionResult place(BlockPlaceContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		BlockPos pos = ctx.getClickedPos();
		Direction facing = ctx.getHorizontalDirection();
		boolean zplane = facing.getAxis() == Direction.Axis.X;
		BlockPos master = pos.above();
		List<BlockPos> slaves = BlockDoor3x3.slaves(master, zplane);
		ItemStack stack = ctx.getItemInHand();
		if (!canPlace(ctx, master) || !slaves.stream().allMatch(p -> canPlace(ctx, p))) {
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide) {
			BlockState slave = this.getBlock().defaultBlockState().setValue(BlockDoor3x3.ZPLANE, zplane).setValue(BlockDoor3x3.MASTER, false);
			for (BlockPos p : slaves) {
				level.setBlock(p, slave, Block.UPDATE_ALL);
			}
			level.setBlock(master, slave.setValue(BlockDoor3x3.MASTER, true), Block.UPDATE_ALL);
			if (level.getBlockEntity(master) instanceof Door3x3TileEnt door) {
				if (player != null) {
					door.setOwner(player);
				}
				door.needUpdate();
			}
		}
		SoundType st = this.getBlock().defaultBlockState().getSoundType();
		level.playSound(player, pos, st.getPlaceSound(), SoundSource.BLOCKS, (st.getVolume() + 1.0f) / 2.0f, st.getPitch() * 0.8f);
		if (player == null || !player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static boolean canPlace(BlockPlaceContext ctx, BlockPos p) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		return level.isInWorldBounds(p) && level.getBlockState(p).canBeReplaced() && (player == null || player.mayUseItemAt(p, Direction.UP, ctx.getItemInHand()))
				&& level.getWorldBorder().isWithinBounds(p);
	}
}
