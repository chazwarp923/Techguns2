package techguns.world.gen.dungeon;

import net.minecraft.util.RandomSource;
import techguns.world.gen.GenWorld;

/** 1.12 world/dungeon/Dungeon: best of 5 maze paths, then segments and spawners */
public final class Dungeon {

	private static final int GENERATE_ATTEMPTS = 5;

	private Dungeon() {
	}

	public static void generate(DungeonPreset preset, GenWorld w, RandomSource rand, int posX, int posY, int posZ, int maxSizeX,
			int maxSizeY, int maxSizeZ) {
		int sX = maxSizeX / preset.getSizeXZ();
		int sY = maxSizeY / preset.getSizeY();
		int sZ = maxSizeZ / preset.getSizeXZ();
		MazeDungeonPath path = null;
		for (int i = 0; i < GENERATE_ATTEMPTS; i++) {
			MazeDungeonPath p = new MazeDungeonPath(sX, sY, sZ, rand);
			preset.initPath(p);
			p.generatePath();
			if (path == null || p.getNumSegments() > path.getNumSegments()) {
				path = p;
			}
		}
		path.generateDungeon(w, posX, posY, posZ, preset);
		path.generateNPCSpawners(w, posX, posY, posZ, preset);
		w.mergeLegacyPairs();
	}
}
