package techguns.world.gen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

/**
 * A structure built by code (1.12 MilitaryCamp, AlienBugNest...) instead of from a scanned block list. It picks its
 * layout for a chunk, then writes its blocks into a {@link GenWorld}.
 */
public interface ProcGenerator {

	/** where and how big; origin is the min corner at ground level (first free y) */
	record Layout(BlockPos origin, int sizeX, int sizeY, int sizeZ) {
		CompoundTag save() {
			CompoundTag t = new CompoundTag();
			t.putLong("origin", origin.asLong());
			t.putInt("sx", sizeX);
			t.putInt("sy", sizeY);
			t.putInt("sz", sizeZ);
			return t;
		}

		static Layout load(CompoundTag t) {
			return new Layout(BlockPos.of(t.getLong("origin")), t.getInt("sx"), t.getInt("sy"), t.getInt("sz"));
		}

		/** footprint from the ground block layer up */
		public BoundingBox footprint() {
			return new BoundingBox(origin.getX(), origin.getY() - 1, origin.getZ(), origin.getX() + sizeX - 1, origin.getY() + sizeY,
					origin.getZ() + sizeZ - 1);
		}
	}

	/** layout for a structure starting in this chunk, or null if the spot is unsuitable */
	@Nullable
	Layout layout(int chunkX, int chunkZ, RandomSource rnd, TerrainView terrain);

	/** writes the structure; must only depend on the layout, the random and the terrain view */
	void generate(GenWorld world, Layout layout, PlaceContext.BiomeColorType color, RandomSource rnd, TerrainView terrain);

	/** reads of unwritten positions: solid or not */
	default boolean solidFallback(BlockPos pos, Layout layout, TerrainView terrain) {
		return pos.getY() < terrain.ground(pos.getX(), pos.getZ());
	}

	/** vanilla terrain shaping around the footprint during noise generation */
	default TerrainAdjustment terrainAdjustment() {
		return TerrainAdjustment.NONE;
	}

	/** level the real ground of the footprint to the layout height before placing (1.12 flattenArea / removeJunkInArea) */
	default boolean flattenGround() {
		return false;
	}
}
