package techguns.blocks.machines.multiblocks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlocks;
import techguns.blocks.BlockOreCluster;
import techguns.blocks.machines.BlockMultiBlockSlave;
import techguns.blocks.machines.BlockOreDrillPart;
import techguns.tileentities.MultiBlockMachineTileEntMaster;
import techguns.tileentities.OreDrillTileEntMaster;

/**
 * Ore drill (1.12 OreDrillDefinition). The controller touches exactly one engine or rod, which sets the drill
 * direction. Along it: engines, then rods, then an ore cluster. Engines may be widened to a square of radius 1-3; then
 * a frame of radius+1 encloses engines and rods (edges frame blocks, faces scaffold, rod section inside air). A single
 * rod between controller and cluster makes the smallest drill.
 */
public class OreDrillSchematic extends MultiBlockSchematic {

	public static final int MAX_RADIUS = 3;
	public static final int MAX_SIZE = 16;

	public static final int TYPE_ROD = 1;
	public static final int TYPE_ENGINE = 2;
	public static final int TYPE_FRAME = 3;
	public static final int TYPE_SCAFFOLD = 4;
	public static final int TYPE_SCAFFOLD_HIDDEN = 5;

	/** the two axes across the drill direction (1.12 getSide1 / getSide2) */
	public static Direction side1(Direction dir) {
		return dir.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.DOWN;
	}

	public static Direction side2(Direction dir) {
		return switch (dir.getAxis()) {
			case X -> Direction.NORTH;
			default -> Direction.EAST;
		};
	}

	private static BlockPos at(BlockPos start, Direction dir, int h, int r1, int r2) {
		return start.relative(dir, h).relative(side1(dir), r1).relative(side2(dir), r2);
	}

	protected static List<BlockPos> engineBlocks(BlockPos masterPos, Direction dir, int radius, int len) {
		List<BlockPos> list = new ArrayList<>();
		for (int r1 = -radius; r1 <= radius; r1++) {
			for (int r2 = -radius; r2 <= radius; r2++) {
				for (int h = 0; h < len; h++) {
					list.add(at(masterPos, dir, h + 1, r1, r2));
				}
			}
		}
		return list;
	}

	protected static List<BlockPos> airBlocks(BlockPos startPos, Direction dir, int radius, int len) {
		List<BlockPos> list = new ArrayList<>();
		for (int r1 = -radius; r1 <= radius; r1++) {
			for (int r2 = -radius; r2 <= radius; r2++) {
				for (int h = 0; h < len; h++) {
					if (r1 != 0 || r2 != 0) {
						list.add(at(startPos, dir, h + 1, r1, r2));
					}
				}
			}
		}
		return list;
	}

	protected static List<BlockPos> frameBlocks(BlockPos masterPos, Direction dir, int radius, int len, boolean outer, boolean skipLastRow) {
		List<BlockPos> list = new ArrayList<>();
		for (int r1 = -radius; r1 <= radius; r1++) {
			for (int r2 = -radius; r2 <= radius; r2++) {
				for (int h = 0; h < len; h++) {
					if (outer ? isFramePos(r1, r2, h, radius, len) : isScaffoldPos(r1, r2, h, radius, len) && (!skipLastRow || h != len - 1)) {
						list.add(at(masterPos, dir, h + 1, r1, r2));
					}
				}
			}
		}
		return list;
	}

	/** inner scaffolds of the last layer, hidden while formed (the drill head turns there) */
	protected static List<BlockPos> finalScaffoldRow(BlockPos masterPos, Direction dir, int radius, int len) {
		List<BlockPos> list = new ArrayList<>();
		for (int r1 = -radius; r1 <= radius; r1++) {
			for (int r2 = -radius; r2 <= radius; r2++) {
				if (r1 != 0 || r2 != 0) {
					list.add(at(masterPos, dir, len, r1, r2));
				}
			}
		}
		return list;
	}

	protected static boolean isFramePos(int r1, int r2, int h, int radius, int len) {
		int minc = 0;
		if (Math.abs(r1) == radius) minc++;
		if (Math.abs(r2) == radius) minc++;
		if (h == 0 || h == len - 1) minc++;
		return minc >= 2;
	}

	protected static boolean isScaffoldPos(int r1, int r2, int h, int radius, int len) {
		if (h == 0) return false;
		int minc = 0;
		if (Math.abs(r1) == radius) minc++;
		if (Math.abs(r2) == radius) minc++;
		if (h == len - 1) {
			minc++;
			return minc == 1 && !(r1 == 0 && r2 == 0);
		}
		return minc == 1;
	}

	public static boolean checkDrillSize(int rods, int engines, int radius) {
		return rods >= (radius * 2 + 1) && rods >= engines && engines > radius;
	}

	public static boolean isOreCluster(Level level, BlockPos p) {
		return level.getBlockState(p).getBlock() instanceof BlockOreCluster;
	}

	protected static boolean isUnformed(Level level, BlockPos p, Block block) {
		BlockState s = level.getBlockState(p);
		return s.is(block) && !s.getValue(BlockMultiBlockSlave.FORMED);
	}

	protected static boolean isEngine(Level level, BlockPos p) {
		return isUnformed(level, p, TGBlocks.OREDRILL_ENGINE.get());
	}

	protected static boolean isRod(Level level, BlockPos p) {
		return isUnformed(level, p, TGBlocks.OREDRILL_ROD.get());
	}

	/** the only neighbour engine or rod, null if none or several */
	protected static Direction findDirection(Level level, Player player, BlockPos masterPos) {
		Direction dir = null;
		for (Direction f : Direction.values()) {
			BlockPos p = masterPos.relative(f);
			if (isEngine(level, p) || isRod(level, p)) {
				if (dir == null) {
					dir = f;
				} else {
					sendErrorPing(level, p, player);
					return null;
				}
			}
		}
		return dir;
	}

	/** [engines, rods]; negative values are errors (1.12 checkLength) */
	protected static int[] checkLength(Level level, BlockPos p, Direction dir) {
		int engines = 0;
		int rods = 0;
		if (isRod(level, p.relative(dir, 1))) {
			return isOreCluster(level, p.relative(dir, 2)) ? new int[] { 0, 1 } : new int[] { -1, -1 };
		} else if (isEngine(level, p.relative(dir, 1))) {
			engines++;
			while (isEngine(level, p.relative(dir, engines + 1)) && engines < MAX_SIZE) {
				engines++;
			}
			if (isRod(level, p.relative(dir, engines + 1))) {
				rods++;
				while (isRod(level, p.relative(dir, engines + rods + 1)) && rods < MAX_SIZE) {
					rods++;
				}
				return isOreCluster(level, p.relative(dir, engines + rods + 1)) ? new int[] { engines, rods } : new int[] { engines, -rods };
			}
			return new int[] { -engines, -1 };
		}
		return new int[] { -1, -1 };
	}

	/** engine square radius (1.12 checkWidth) */
	protected static int checkWidth(Level level, BlockPos p, Direction dir) {
		BlockPos enginePos = p.relative(dir, 1);
		int rad = 0;
		for (Direction f : Direction.values()) {
			if (f.getAxis() == dir.getAxis()) {
				continue;
			}
			int count = 0;
			while (count < MAX_RADIUS && isEngine(level, enginePos.relative(f, count + 1))) {
				count++;
			}
			rad = Math.max(rad, count);
		}
		return rad;
	}

	@Override
	public boolean canFormFromSide(Direction side) {
		return true;
	}

	@Override
	public boolean checkForm(Level level, Player player, BlockPos masterPos, Direction side) {
		Direction dir = findDirection(level, player, masterPos);
		if (dir == null) {
			sendErrorMsg(player, "techguns.msg.error.oredrill.controlleralone");
			return false;
		}
		int[] length = checkLength(level, masterPos, dir);
		int engines = length[0];
		int rods = length[1];
		if (engines < 0 || rods <= 0 || rods + engines > MAX_SIZE) {
			if (engines < 0) {
				sendErrorPing(level, masterPos.relative(dir, -engines + 1), player);
			} else if (rods < 0) {
				sendErrorPing(level, masterPos.relative(dir, -rods + engines + 1), player);
			}
			sendErrorMsg(player, "techguns.msg.error.multiblockinvalid");
			return false;
		}
		if (engines == 0 && rods == 1) {
			return true;
		}
		int rad = checkWidth(level, masterPos, dir);
		if (!checkDrillSize(rods, engines, rad)) {
			sendErrorMsg(player, "techguns.msg.error.oredrill.rodsize");
			return false;
		}
		if (!allBlocksMatch(level, player, engineBlocks(masterPos, dir, rad, engines), TGBlocks.OREDRILL_ENGINE.get())) {
			sendErrorMsg(player, "techguns.multiblock.oredrill.invalid.engines");
			return false;
		}
		if (!allBlocksMatch(level, player, frameBlocks(masterPos, dir, rad + 1, engines + rods, true, true), TGBlocks.OREDRILL_FRAME.get())) {
			sendErrorMsg(player, "techguns.multiblock.oredrill.invalid.frame");
			return false;
		}
		if (!allBlocksMatch(level, player, frameBlocks(masterPos, dir, rad + 1, engines + rods, false, true), TGBlocks.OREDRILL_SCAFFOLD.get())) {
			sendErrorMsg(player, "techguns.multiblock.oredrill.invalid.scaffold");
			return false;
		}
		boolean air = true;
		for (BlockPos p : airBlocks(masterPos.relative(dir, engines), dir, rad, rods - 1)) {
			if (!level.getBlockState(p).isAir()) {
				sendErrorPing(level, p, player);
				air = false;
			}
		}
		if (!air) {
			sendErrorMsg(player, "techguns.multiblock.oredrill.invalid.air");
		}
		return air;
	}

	@Override
	public void form(Level level, MultiBlockMachineTileEntMaster master, Direction side) {
		if (!(master instanceof OreDrillTileEntMaster drill)) {
			return;
		}
		BlockPos masterPos = master.getBlockPos();
		Direction dir = findDirection(level, null, masterPos);
		if (dir == null) {
			return;
		}
		int[] length = checkLength(level, masterPos, dir);
		int engines = length[0];
		int rods = length[1];
		if (engines < 0 || rods <= 0) {
			return;
		}
		int rad = engines == 0 ? 0 : checkWidth(level, masterPos, dir);
		drill.formOreDrill(engines, rods, rad, dir);
		for (int i = engines + 1; i <= engines + rods; i++) {
			linkSlave(level, masterPos.relative(dir, i), TYPE_ROD, masterPos);
		}
		if (engines == 0) {
			return;
		}
		engineBlocks(masterPos, dir, rad, engines).forEach(p -> linkSlave(level, p, TYPE_ENGINE, masterPos));
		frameBlocks(masterPos, dir, rad + 1, engines + rods, true, true).forEach(p -> linkSlave(level, p, TYPE_FRAME, masterPos));
		frameBlocks(masterPos, dir, rad + 1, engines + rods, false, true).forEach(p -> linkSlave(level, p, TYPE_SCAFFOLD, masterPos));
		finalScaffoldRow(masterPos, dir, rad, engines + rods).forEach(p -> {
			if (level.getBlockState(p).is(TGBlocks.OREDRILL_SCAFFOLD.get())) {
				linkSlave(level, p, TYPE_SCAFFOLD_HIDDEN, masterPos);
				level.setBlock(p, level.getBlockState(p).setValue(BlockOreDrillPart.Scaffold.HIDDEN, true), 3);
			}
		});
	}

	@Override
	public void unform(Level level, MultiBlockMachineTileEntMaster master) {
		if (!(master instanceof OreDrillTileEntMaster drill)) {
			return;
		}
		BlockPos masterPos = master.getBlockPos();
		Direction dir = drill.getDrillDirection();
		int engines = drill.getEngines();
		int rods = drill.getRods();
		int rad = drill.getRadius();
		drill.unformOreDrill();
		if (dir == null) {
			return;
		}
		List<BlockPos> parts = new ArrayList<>();
		for (int i = engines + 1; i <= engines + rods; i++) {
			parts.add(masterPos.relative(dir, i));
		}
		if (engines > 0) {
			parts.addAll(engineBlocks(masterPos, dir, rad, engines));
			parts.addAll(frameBlocks(masterPos, dir, rad + 1, engines + rods, true, true));
			parts.addAll(frameBlocks(masterPos, dir, rad + 1, engines + rods, false, true));
			parts.addAll(finalScaffoldRow(masterPos, dir, rad, engines + rods));
		}
		for (BlockPos p : parts) {
			unlinkSlave(level, p, masterPos);
			BlockState s = level.getBlockState(p);
			if (s.is(TGBlocks.OREDRILL_SCAFFOLD.get()) && s.getValue(BlockOreDrillPart.Scaffold.HIDDEN)) {
				level.setBlock(p, s.setValue(BlockOreDrillPart.Scaffold.HIDDEN, false), 3);
			}
		}
	}
}
