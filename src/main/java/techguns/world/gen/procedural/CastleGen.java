package techguns.world.gen.procedural;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import techguns.world.gen.GenWorld;
import techguns.world.gen.PlaceContext.BiomeColorType;
import techguns.world.gen.ProcGenerator;
import techguns.world.gen.TerrainView;
import techguns.world.gen.dungeon.Dungeon;
import techguns.world.gen.dungeon.DungeonPreset;
import techguns.world.gen.dungeon.PresetCastle;

/**
 * 1.12 CastleStructure: a maze dungeon built from the castle templates (ncdung1, nclower1, ncmid1, ncupper1, nctop1,
 * ncroof1). The lowest level is sunk into the ground, the entrance is on the level above it.
 */
public class CastleGen implements ProcGenerator {

	public static final DungeonPreset PRESET = new PresetCastle();

	static final int HEIGHT_DIFF_LIMIT = 10;
	static final int SAMPLE_STEP = 8;

	@Override
	public Layout layout(int chunkX, int chunkZ, RandomSource rnd, TerrainView terrain) {
		int x = chunkX * 16;
		int z = chunkZ * 16;
		int sizeX = 32 + rnd.nextInt(16);
		int sizeZ = 32 + rnd.nextInt(16);
		int sizeY = 24 + rnd.nextInt(16);
		int y = terrain.landHeight(x, z, sizeX, sizeZ, SAMPLE_STEP, HEIGHT_DIFF_LIMIT);
		if (y <= 0) {
			return null;
		}
		return new Layout(new BlockPos(x, y, z), sizeX, sizeY, sizeZ);
	}

	@Override
	public void generate(GenWorld w, Layout layout, BiomeColorType color, RandomSource rnd, TerrainView terrain) {
		BlockPos o = layout.origin();
		// 1.12: posY+1-sizeY with posY the top ground block, so the entrance level's floor replaces the first air layer
		Dungeon.generate(PRESET, w, rnd, o.getX(), o.getY() - PRESET.getSizeY(), o.getZ(), layout.sizeX(), layout.sizeY(), layout.sizeZ());
	}
}
