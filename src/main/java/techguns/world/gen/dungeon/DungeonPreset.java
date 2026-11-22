package techguns.world.gen.dungeon;

import net.minecraft.util.RandomSource;
import techguns.world.gen.GenWorld;
import techguns.world.gen.MBlock;

/** 1.12 IDungeonPreset: which template supplies the segments of each level, path settings, spawners */
public interface DungeonPreset {

	/** template for a segment at level y of a dungeon yMax levels high; y == -1 means roof */
	DungeonTemplate template(int y, int yMax, RandomSource rand);

	int getSizeXZ();

	int getSizeY();

	/** NPC spawners per dungeon segment */
	default float getSpawnDensity() {
		return 0.1f;
	}

	/** floor height inside a segment */
	default int getSpawnYOffset() {
		return 1;
	}

	void initPath(MazeDungeonPath path);

	MBlock spawner();

	default void place(GenWorld w, SegmentType type, int y, int yMax, int seed, int px, int py, int pz, int rotation) {
		DungeonTemplate t = template(y, yMax, RandomSource.create(seed));
		DungeonTemplate.Segment seg = t.segment(type);
		if (seg != null) {
			seg.place(w, px, py, pz, rotation);
		}
	}
}
