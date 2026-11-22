package techguns.world.gen.dungeon;

import java.util.List;

import net.minecraft.util.RandomSource;
import techguns.TGEntities;
import techguns.blocks.BlockTGSpawner;
import techguns.world.gen.MBlock;

/** 1.12 PresetCastle: nether brick style castle, underground level, roof */
public class PresetCastle implements DungeonPreset {

	private static final String LOOT = "chests/castle";

	private List<DungeonTemplate> underground, lower, mid, upper, top, roof;

	private void init() {
		if (mid == null) {
			underground = List.of(DungeonTemplate.get("ncdung1", LOOT));
			lower = List.of(DungeonTemplate.get("nclower1", LOOT));
			upper = List.of(DungeonTemplate.get("ncupper1", LOOT));
			top = List.of(DungeonTemplate.get("nctop1", LOOT));
			roof = List.of(DungeonTemplate.get("ncroof1", LOOT));
			mid = List.of(DungeonTemplate.get("ncmid1", LOOT));
		}
	}

	private static DungeonTemplate pick(List<DungeonTemplate> list, RandomSource rand) {
		return list.get(rand.nextInt(list.size()));
	}

	@Override
	public synchronized DungeonTemplate template(int y, int yMax, RandomSource rand) {
		init();
		int yMin = 0;
		if (y == -1) return pick(roof, rand);
		if (y == yMin) return pick(underground, rand);
		if (y <= yMin + 1) return pick(lower, rand);
		if (y < yMax - 2) return pick(mid, rand);
		if (y < yMax - 1) return pick(upper, rand);
		return pick(top, rand);
	}

	@Override
	public synchronized int getSizeXZ() {
		init();
		return mid.get(0).sizeXZ;
	}

	@Override
	public synchronized int getSizeY() {
		init();
		return mid.get(0).sizeY;
	}

	@Override
	public void initPath(MazeDungeonPath path) {
		path.startHeightLevel = 1;
		path.chanceStraight = 0.8f;
		path.chanceRamp = 0.5f;
		path.chanceRoom = 0.25f;
		path.chanceFork = 0.4f;
		path.chanceUp = 0.65f;
		path.useFoundations = true;
		path.usePillars = true;
		path.useRoof = true;
	}

	@Override
	public MBlock spawner() {
		return new MBlock.Spawner(BlockTGSpawner.Type.HOLE, 2, 2, 200, 2).mob(TGEntities.ZOMBIE_SOLDIER, 1).mob(TGEntities.SKELETON_SOLDIER, 1);
	}
}
