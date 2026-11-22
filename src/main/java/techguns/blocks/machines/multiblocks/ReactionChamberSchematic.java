package techguns.blocks.machines.multiblocks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import techguns.TGBlocks;
import techguns.tileentities.MultiBlockMachineTileEntMaster;

/**
 * Reaction chamber (1.12 ReactionChamberDefinition): 3x4x3. The controller sits in the middle of the bottom front row;
 * the clicked side is the front and the chamber extends away from it. Bottom layer and top layer are housing, the two
 * layers in between glass. The housings next to the bottom center connect items and fluids, the top center one energy.
 */
public class ReactionChamberSchematic extends MultiBlockSchematic {

	/** glass and housing without connection */
	public static final int TYPE_PLAIN = 1;
	public static final int TYPE_SIDE_CONNECTOR = 2;
	public static final int TYPE_ENERGY_CONNECTOR = 3;

	protected static List<BlockPos> glassBlocks(BlockPos masterPos, Direction dir) {
		Direction left = dir.getClockWise();
		List<BlockPos> list = new ArrayList<>();
		BlockPos.betweenClosed(masterPos.relative(left).above(), masterPos.relative(left.getOpposite()).relative(dir, 2).above(2))
				.forEach(p -> list.add(p.immutable()));
		return list;
	}

	protected static List<BlockPos> energyConnector(BlockPos masterPos, Direction dir) {
		return List.of(masterPos.relative(dir).above(3));
	}

	protected static List<BlockPos> sideConnectors(BlockPos masterPos, Direction dir) {
		BlockPos center = masterPos.relative(dir);
		Direction left = dir.getClockWise();
		return List.of(center.relative(dir), center.relative(left), center.relative(left.getOpposite()));
	}

	protected static List<BlockPos> plainHousings(BlockPos masterPos, Direction dir) {
		Direction left = dir.getClockWise();
		BlockPos leftp = masterPos.relative(left);
		BlockPos rightp = masterPos.relative(left.getOpposite());
		BlockPos centerp = masterPos.relative(dir);
		BlockPos centeru = centerp.above(3);
		return List.of(leftp, rightp, leftp.relative(dir, 2), rightp.relative(dir, 2), centerp, leftp.above(3), rightp.above(3),
				leftp.relative(dir, 2).above(3), rightp.relative(dir, 2).above(3), centeru.relative(dir), centeru.relative(dir.getOpposite()),
				centeru.relative(left), centeru.relative(left.getOpposite()));
	}

	protected static List<BlockPos> housingBlocks(BlockPos masterPos, Direction dir) {
		List<BlockPos> list = new ArrayList<>(plainHousings(masterPos, dir));
		list.addAll(energyConnector(masterPos, dir));
		list.addAll(sideConnectors(masterPos, dir));
		return list;
	}

	@Override
	public boolean canFormFromSide(Direction side) {
		return side != null && side.getAxis().isHorizontal();
	}

	@Override
	public boolean checkForm(Level level, Player player, BlockPos masterPos, Direction side) {
		if (canFormFromSide(side)) {
			Direction dir = side.getOpposite();
			boolean glass = allBlocksMatch(level, player, glassBlocks(masterPos, dir), TGBlocks.REACTIONCHAMBER_GLASS.get());
			boolean housing = allBlocksMatch(level, player, housingBlocks(masterPos, dir), TGBlocks.REACTIONCHAMBER_HOUSING.get());
			if (glass && housing) {
				return true;
			}
		}
		sendErrorMsg(player, "techguns.msg.error.multiblockinvalid");
		return false;
	}

	@Override
	public void form(Level level, MultiBlockMachineTileEntMaster master, Direction side) {
		BlockPos masterPos = master.getBlockPos();
		Direction dir = side.getOpposite();
		master.setFormed(true, dir);
		glassBlocks(masterPos, dir).forEach(p -> linkSlave(level, p, TYPE_PLAIN, masterPos));
		plainHousings(masterPos, dir).forEach(p -> linkSlave(level, p, TYPE_PLAIN, masterPos));
		sideConnectors(masterPos, dir).forEach(p -> linkSlave(level, p, TYPE_SIDE_CONNECTOR, masterPos));
		energyConnector(masterPos, dir).forEach(p -> linkSlave(level, p, TYPE_ENERGY_CONNECTOR, masterPos));
	}

	@Override
	public void unform(Level level, MultiBlockMachineTileEntMaster master) {
		BlockPos masterPos = master.getBlockPos();
		Direction dir = master.getMultiblockDirection();
		master.setFormed(false, dir);
		glassBlocks(masterPos, dir).forEach(p -> unlinkSlave(level, p, masterPos));
		housingBlocks(masterPos, dir).forEach(p -> unlinkSlave(level, p, masterPos));
	}
}
