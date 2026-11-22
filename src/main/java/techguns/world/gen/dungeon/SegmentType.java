package techguns.world.gen.dungeon;

/**
 * 1.12 TemplateSegment.SegmentType with its TemplateSegment data: connection pattern (8 neighbours, clockwise from the
 * -x-z corner: 0 1 2 / 7 X 3 / 6 5 4, -z up), how many rotations to try, height in levels, whether it is matched against
 * the dungeon path.
 */
public enum SegmentType {
	STRAIGHT(1, pattern(0, 0, 0, 1, 0, 0, 0, 1), 2),
	CURVE(1, pattern(0, 0, 0, 1, 0, 1, 0, 0), 4),
	FORK(1, pattern(0, 0, 0, 1, 0, 1, 0, 1), 4),
	CROSS(1, pattern(0, 1, 0, 1, 0, 1, 0, 1), 1),
	END(1, pattern(0, 0, 0, 1, 0, 0, 0, 0), 4),
	RAMP(2, null, 1),
	ENTRANCE(1, null, 1),
	ROOM_WALL(1, pattern(0, 0, 0, 1, 1, 1, 1, 1), 4),
	ROOM_CORNER(1, pattern(0, 0, 0, 1, 1, 1, 0, 0), 4),
	ROOM_INNER(1, pattern(1, 1, 1, 1, 1, 1, 1, 1), 1),
	ROOM_DOOR(1, pattern(0, 1, 0, 1, 1, 1, 1, 1), 4),
	ROOM_DOOR_CORNER1(1, pattern(0, 1, 0, 1, 1, 1, 0, 0), 4),
	ROOM_DOOR_CORNER2(1, pattern(0, 0, 0, 1, 1, 1, 0, 1), 4),
	ROOM_DOOR_CORNER_DOUBLE(1, pattern(0, 1, 0, 1, 1, 1, 0, 1), 4),
	FOUNDATION(1, null, 1),
	PILLARS(1, null, 1);

	public final int sizeY;
	public final boolean[] pattern;
	public final int rotations;
	public final boolean match;

	SegmentType(int sizeY, boolean[] pattern, int rotations) {
		this.sizeY = sizeY;
		this.pattern = pattern == null ? new boolean[8] : pattern;
		this.rotations = rotations;
		this.match = pattern != null;
	}

	private static boolean[] pattern(int... p) {
		boolean[] b = new boolean[8];
		for (int i = 0; i < 8; i++) {
			b[i] = p[i] != 0;
		}
		return b;
	}
}
