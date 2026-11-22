package techguns.blocks.machines.multiblocks;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import techguns.blocks.machines.BlockMultiBlockSlave;
import techguns.tileentities.MultiBlockMachineTileEntMaster;
import techguns.tileentities.MultiBlockSlaveTileEnt;

/** Layout of a multiblock machine (1.12 MultiBlockMachineSchematic). */
public abstract class MultiBlockSchematic {

	/** can the multiblock be formed by clicking this side of the controller */
	public abstract boolean canFormFromSide(Direction side);

	/** server side: check the structure, ping invalid blocks and tell the player */
	public abstract boolean checkForm(Level level, Player player, BlockPos masterPos, Direction direction);

	public abstract void form(Level level, MultiBlockMachineTileEntMaster master, Direction direction);

	public abstract void unform(Level level, MultiBlockMachineTileEntMaster master);

	/** all positions must hold an unformed part of the given block */
	protected static boolean allBlocksMatch(Level level, Player player, List<BlockPos> positions, Block block) {
		boolean valid = true;
		for (BlockPos p : positions) {
			BlockState s = level.getBlockState(p);
			if (!s.is(block) || s.getValue(BlockMultiBlockSlave.FORMED)) {
				sendErrorPing(level, p, player);
				valid = false;
			}
		}
		return valid;
	}

	/** 1.12 MultiblockInvalidPing particle, only for this player */
	protected static void sendErrorPing(Level level, BlockPos p, Player player) {
		if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
			sl.sendParticles(sp, ParticleTypes.ANGRY_VILLAGER, true, p.getX() + 0.5d, p.getY() + 0.5d, p.getZ() + 0.5d, 3, 0.25d, 0.25d, 0.25d, 0d);
		}
	}

	protected static void sendErrorMsg(Player player, String key) {
		player.displayClientMessage(Component.translatable(key), true);
	}

	protected static void linkSlave(Level level, BlockPos pos, int type, BlockPos masterPos) {
		BlockState s = level.getBlockState(pos);
		if (s.getBlock() instanceof BlockMultiBlockSlave) {
			level.setBlock(pos, s.setValue(BlockMultiBlockSlave.FORMED, true), 3);
		}
		if (level.getBlockEntity(pos) instanceof MultiBlockSlaveTileEnt slave) {
			slave.form(masterPos, (byte) type);
		}
	}

	protected static void unlinkSlave(Level level, BlockPos pos, BlockPos masterPos) {
		if (level.getBlockEntity(pos) instanceof MultiBlockSlaveTileEnt slave && masterPos.equals(slave.getMasterPos())) {
			slave.unform();
			BlockState s = level.getBlockState(pos);
			if (s.getBlock() instanceof BlockMultiBlockSlave && s.getValue(BlockMultiBlockSlave.FORMED)) {
				level.setBlock(pos, s.setValue(BlockMultiBlockSlave.FORMED, false), 3);
			}
		}
	}
}
