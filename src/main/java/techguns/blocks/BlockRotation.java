package techguns.blocks;

import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Block.rotate / mirror helpers. Structures turn their blocks with these (MBlock.rotatedState), so a block that
 * stores a direction or per-side connections needs them, or rotated structures keep the unrotated state.
 */
public final class BlockRotation {
	private BlockRotation() {
	}

	public static BlockState rotate(BlockState state, DirectionProperty facing, Rotation rot) {
		return state.setValue(facing, rot.rotate(state.getValue(facing)));
	}

	public static BlockState mirror(BlockState state, DirectionProperty facing, Mirror mirror) {
		return state.setValue(facing, mirror.mirror(state.getValue(facing)));
	}

	/** turns the north/east/south/west booleans (PipeBlock.PROPERTY_BY_DIRECTION) the state has */
	public static BlockState rotateSides(BlockState state, Rotation rot) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BooleanProperty from = PipeBlock.PROPERTY_BY_DIRECTION.get(d);
			if (state.hasProperty(from)) {
				out = out.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(rot.rotate(d)), state.getValue(from));
			}
		}
		return out;
	}

	public static BlockState mirrorSides(BlockState state, Mirror mirror) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BooleanProperty from = PipeBlock.PROPERTY_BY_DIRECTION.get(d);
			if (state.hasProperty(from)) {
				out = out.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(mirror.mirror(d)), state.getValue(from));
			}
		}
		return out;
	}

	/**
	 * Turns corner booleans, each keyed by the side it starts at going clockwise (the north-east corner by NORTH).
	 */
	public static BlockState rotateCorners(BlockState state, Map<Direction, BooleanProperty> corners, Rotation rot) {
		BlockState out = state;
		for (var e : corners.entrySet()) {
			out = out.setValue(corners.get(rot.rotate(e.getKey())), state.getValue(e.getValue()));
		}
		return out;
	}

	public static BlockState mirrorCorners(BlockState state, Map<Direction, BooleanProperty> corners, Mirror mirror) {
		BlockState out = state;
		for (var e : corners.entrySet()) {
			Direction a = mirror.mirror(e.getKey());
			Direction b = mirror.mirror(e.getKey().getClockWise());
			// the mirrored corner lies between a and b; its key is the one of them the other follows clockwise
			Direction key = a.getClockWise() == b ? a : b;
			out = out.setValue(corners.get(key), state.getValue(e.getValue()));
		}
		return out;
	}
}
