package techguns.blocks.machines.multiblocks;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import techguns.TGBlocks;
import techguns.tileentities.MultiBlockMachineTileEntMaster;

/**
 * Fabricator (1.12 FabricatorDefinition): 2x2x2, the controller sits in the bottom front corner, the other 3 bottom
 * blocks are housing, the top 4 glass. Formed by clicking a side of the controller, that side becomes the front; the
 * machine extends behind it and to the left as seen from the front.
 */
public class FabricatorSchematic extends MultiBlockSchematic {

	public static final int TYPE_GLASS = 1;
	public static final int TYPE_HOUSING = 2;

	private static List<BlockPos> bottomRow(BlockPos masterPos, Direction direction) {
		BlockPos behind = masterPos.relative(direction.getOpposite());
		Direction left = direction.getClockWise();
		return List.of(behind, masterPos.relative(left), behind.relative(left));
	}

	private static List<BlockPos> topRow(BlockPos masterPos, Direction direction) {
		BlockPos behind = masterPos.relative(direction.getOpposite());
		Direction left = direction.getClockWise();
		return List.of(masterPos.above(), behind.above(), masterPos.relative(left).above(), behind.relative(left).above());
	}

	@Override
	public boolean canFormFromSide(Direction side) {
		return side != null && side.getAxis().isHorizontal();
	}

	@Override
	public boolean checkForm(Level level, Player player, BlockPos masterPos, Direction direction) {
		boolean bottom = allBlocksMatch(level, player, bottomRow(masterPos, direction), TGBlocks.FABRICATOR_HOUSING.get());
		boolean top = allBlocksMatch(level, player, topRow(masterPos, direction), TGBlocks.FABRICATOR_GLASS.get());
		if (canFormFromSide(direction) && bottom && top) {
			return true;
		}
		sendErrorMsg(player, "techguns.msg.error.multiblockinvalid");
		return false;
	}

	@Override
	public void form(Level level, MultiBlockMachineTileEntMaster master, Direction direction) {
		BlockPos masterPos = master.getBlockPos();
		master.setFormed(true, direction);
		topRow(masterPos, direction).forEach(p -> linkSlave(level, p, TYPE_GLASS, masterPos));
		bottomRow(masterPos, direction).forEach(p -> linkSlave(level, p, TYPE_HOUSING, masterPos));
	}

	@Override
	public void unform(Level level, MultiBlockMachineTileEntMaster master) {
		BlockPos masterPos = master.getBlockPos();
		Direction direction = master.getMultiblockDirection();
		master.setFormed(false, direction);
		topRow(masterPos, direction).forEach(p -> unlinkSlave(level, p, masterPos));
		bottomRow(masterPos, direction).forEach(p -> unlinkSlave(level, p, masterPos));
	}
}
