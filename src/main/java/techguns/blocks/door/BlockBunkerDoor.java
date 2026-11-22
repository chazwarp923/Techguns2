package techguns.blocks.door;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import techguns.TGSounds;

/** Bunker door (1.12 BlockTGDoor2x1): a metal door that opens by hand and opens / closes a double door together. */
public class BlockBunkerDoor extends DoorBlock {

	public static final BlockSetType BUNKER = new BlockSetType("techguns_bunker", true, false, false, BlockSetType.PressurePlateSensitivity.MOBS,
			SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
			SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF,
			SoundEvents.STONE_BUTTON_CLICK_ON);

	public BlockBunkerDoor(Properties props) {
		super(BUNKER, props);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		boolean open = !state.getValue(OPEN);
		setDoorOpen(level, pos, state, open);
		// the other half of a double door
		Direction facing = state.getValue(FACING);
		Direction toOther = state.getValue(HINGE) == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
		BlockPos otherPos = pos.relative(toOther);
		BlockState other = level.getBlockState(otherPos);
		if (other.is(this) && other.getValue(OPEN) == state.getValue(OPEN) && other.getValue(HALF) == state.getValue(HALF)
				&& other.getValue(FACING) == facing && other.getValue(HINGE) != state.getValue(HINGE)) {
			setDoorOpen(level, otherPos, other, open);
		}
		level.playSound(player, pos, TGSounds.BUNKER_DOOR_OPEN.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
		level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private void setDoorOpen(Level level, BlockPos pos, BlockState state, boolean open) {
		level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
		BlockPos otherHalf = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
		BlockState half = level.getBlockState(otherHalf);
		if (half.is(this)) {
			level.setBlock(otherHalf, half.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
		}
	}
}
