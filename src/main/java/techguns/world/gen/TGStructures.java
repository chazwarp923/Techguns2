package techguns.world.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.world.gen.PlaceContext.BiomeColorType;

/**
 * 1.12 Techguns structure worldgen (TGStructureSpawnRegister / WorldgenStructure) on the 1.21 structure system:
 * <ul>
 * <li>{@link LegacyStructure}: picks one of a weighted list of {@link ScannedStructure}s for a chunk</li>
 * <li>{@link LegacyPiece}: places it, clipped to each chunk as it generates</li>
 * </ul>
 * The structures and structure sets (vanilla random spread placement) are datapack JSON in data/techguns/worldgen.
 */
public final class TGStructures {

	public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Techguns.MODID);
	public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Techguns.MODID);

	public static final DeferredHolder<StructureType<?>, StructureType<LegacyStructure>> LEGACY = STRUCTURE_TYPES.register("legacy",
			() -> () -> LegacyStructure.CODEC);
	public static final DeferredHolder<StructurePieceType, StructurePieceType> LEGACY_PIECE = PIECES.register("legacy",
			() -> (StructurePieceType) LegacyPiece::new);
	public static final DeferredHolder<StructurePieceType, StructurePieceType> PROC_PIECE = PIECES.register("procedural",
			() -> (StructurePieceType) ProcPiece::new);

	private TGStructures() {
	}

	public static void register(IEventBus bus) {
		STRUCTURE_TYPES.register(bus);
		PIECES.register(bus);
	}

	/* ------------------------------------------------------------------ structure */

	public record Entry(String name, int weight) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("name").forGetter(Entry::name),
				Codec.INT.optionalFieldOf("weight", 1).forGetter(Entry::weight)).apply(i, Entry::new));
	}

	public static class LegacyStructure extends Structure {
		public static final MapCodec<LegacyStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				settingsCodec(i),
				Entry.CODEC.listOf().fieldOf("structures").forGetter(s -> s.entries)).apply(i, LegacyStructure::new));

		private final List<Entry> entries;

		public LegacyStructure(StructureSettings settings, List<Entry> entries) {
			super(settings);
			this.entries = entries;
		}

		private static boolean isOreCluster(String name) {
			return name.contains("orecluster") || name.equals("desert_oil_cluster");
		}

		@Override
		protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
			if (!TGConfig.doWorldspawn) {
				return Optional.empty();
			}
			List<Entry> pool = new ArrayList<>();
			int total = 0;
			for (Entry e : entries) {
				boolean known = TGStructureDefs.get(e.name()) != null || ProcStructures.get(e.name()) != null;
				if (e.weight() > 0 && known && (TGConfig.spawnOreClusterStructures || !isOreCluster(e.name()))) {
					pool.add(e);
					total += e.weight();
				}
			}
			if (total <= 0) {
				return Optional.empty();
			}
			RandomSource rnd = ctx.random();
			int roll = rnd.nextInt(total);
			Entry chosen = pool.get(pool.size() - 1);
			for (Entry e : pool) {
				roll -= e.weight();
				if (roll < 0) {
					chosen = e;
					break;
				}
			}
			ChunkPos cp = ctx.chunkPos();
			ProcGenerator proc = ProcStructures.get(chosen.name());
			if (proc != null) {
				return procGenerationPoint(ctx, chosen.name(), proc, rnd, cp);
			}
			ScannedStructure def = TGStructureDefs.get(chosen.name());
			int direction = rnd.nextInt(4);
			BlockPos origin = def.findOrigin(cp.x, cp.z, direction, ctx.chunkGenerator(), ctx.heightAccessor(), ctx.randomState());
			if (origin == null) {
				return Optional.empty();
			}
			int index = def.rollIndex(rnd);
			long seed = rnd.nextLong();
			BlockPos center = origin.offset(def.sizeX / 2, 1, def.sizeZ / 2);
			BiomeColorType color = colorType(ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(center.getX()), QuartPos.fromBlock(center.getY()),
					QuartPos.fromBlock(center.getZ()), ctx.randomState().sampler()));
			String name = chosen.name();
			return Optional.of(new GenerationStub(center, b -> b.addPiece(new LegacyPiece(name, def.boundingBox(origin), origin, direction, seed, color, index))));
		}

		private static Optional<GenerationStub> procGenerationPoint(GenerationContext ctx, String name, ProcGenerator proc, RandomSource rnd, ChunkPos cp) {
			TerrainView terrain = new TerrainView(ctx.chunkGenerator(), ctx.heightAccessor(), ctx.randomState());
			ProcGenerator.Layout layout = proc.layout(cp.x, cp.z, rnd, terrain);
			if (layout == null) {
				return Optional.empty();
			}
			long seed = rnd.nextLong();
			BlockPos o = layout.origin();
			BlockPos center = new BlockPos(o.getX() + layout.sizeX() / 2, o.getY(), o.getZ() + layout.sizeZ() / 2);
			BiomeColorType color = colorType(ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(center.getX()), QuartPos.fromBlock(center.getY()),
					QuartPos.fromBlock(center.getZ()), ctx.randomState().sampler()));
			GenWorld world = ProcStructures.build(name, layout, color, seed, terrain);
			if (world.isEmpty()) {
				return Optional.empty();
			}
			BoundingBox box = ProcStructures.boundingBox(proc, layout, world);
			return Optional.of(new GenerationStub(center, b -> b.addPiece(new ProcPiece(name, box, layout, seed, color))));
		}

		@Override
		public StructureType<?> type() {
			return LEGACY.get();
		}
	}

	/* ------------------------------------------------------------------ procedural piece */

	/** a code-built structure: regenerated (or taken from the cache) and copied into each chunk it covers */
	public static class ProcPiece extends StructurePiece implements net.neoforged.neoforge.common.world.PieceBeardifierModifier {
		private final String name;
		private final ProcGenerator.Layout layout;
		private final long seed;
		private final BiomeColorType color;

		public ProcPiece(String name, BoundingBox box, ProcGenerator.Layout layout, long seed, BiomeColorType color) {
			super(PROC_PIECE.get(), 0, box);
			this.name = name;
			this.layout = layout;
			this.seed = seed;
			this.color = color;
		}

		public ProcPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
			super(PROC_PIECE.get(), tag);
			this.name = tag.getString("name");
			this.layout = ProcGenerator.Layout.load(tag.getCompound("layout"));
			this.seed = tag.getLong("seed");
			BiomeColorType[] types = BiomeColorType.values();
			this.color = types[Math.floorMod(tag.getInt("color"), types.length)];
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
			tag.putString("name", name);
			tag.put("layout", layout.save());
			tag.putLong("seed", seed);
			tag.putInt("color", color.ordinal());
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box,
				ChunkPos chunkPos, BlockPos pivot) {
			ProcGenerator proc = ProcStructures.get(name);
			if (proc == null) {
				return;
			}
			TerrainView terrain = new TerrainView(generator, level, level.getLevel().getChunkSource().randomState());
			GenWorld world = ProcStructures.build(name, layout, color, seed, terrain);
			PlaceContext ctx = new PlaceContext(level, box, seed, color, 0);
			if (proc.flattenGround()) {
				ProcStructures.flatten(ctx, layout);
			}
			world.apply(ctx);
			ctx.finishShapes();
		}

		@Override
		public BoundingBox getBeardifierBox() {
			return layout.footprint();
		}

		@Override
		public net.minecraft.world.level.levelgen.structure.TerrainAdjustment getTerrainAdjustment() {
			ProcGenerator proc = ProcStructures.get(name);
			return proc == null ? net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE : proc.terrainAdjustment();
		}

		/** ground level is one above the bottom of the footprint (the ground block layer) */
		@Override
		public int getGroundLevelDelta() {
			return 1;
		}
	}

	/** 1.12 WorldgenStructure.getBiomeColorTypeFromBiome */
	public static BiomeColorType colorType(Holder<Biome> b) {
		if (b.is(BiomeTags.IS_NETHER)) {
			return BiomeColorType.NETHER;
		} else if (b.is(Tags.Biomes.IS_SNOWY) || b.is(Tags.Biomes.IS_COLD)) {
			return BiomeColorType.SNOW;
		} else if (b.is(Tags.Biomes.IS_DESERT) || b.is(Tags.Biomes.IS_SANDY) || b.is(Tags.Biomes.IS_BADLANDS) || b.is(Tags.Biomes.IS_BEACH)) {
			return BiomeColorType.DESERT;
		}
		return BiomeColorType.WOODLAND;
	}

	/* ------------------------------------------------------------------ piece */

	public static class LegacyPiece extends StructurePiece {
		private final String name;
		private final BlockPos origin;
		private final int direction;
		private final long seed;
		private final BiomeColorType color;
		private final int index;

		public LegacyPiece(String name, BoundingBox box, BlockPos origin, int direction, long seed, BiomeColorType color, int index) {
			super(LEGACY_PIECE.get(), 0, box);
			this.name = name;
			this.origin = origin;
			this.direction = direction;
			this.seed = seed;
			this.color = color;
			this.index = index;
		}

		public LegacyPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
			super(LEGACY_PIECE.get(), tag);
			this.name = tag.getString("name");
			this.origin = BlockPos.of(tag.getLong("origin"));
			this.direction = tag.getInt("dir");
			this.seed = tag.getLong("seed");
			BiomeColorType[] types = BiomeColorType.values();
			this.color = types[Math.floorMod(tag.getInt("color"), types.length)];
			this.index = tag.getInt("index");
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
			tag.putString("name", name);
			tag.putLong("origin", origin.asLong());
			tag.putInt("dir", direction);
			tag.putLong("seed", seed);
			tag.putInt("color", color.ordinal());
			tag.putInt("index", index);
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box,
				ChunkPos chunkPos, BlockPos pivot) {
			ScannedStructure def = TGStructureDefs.get(name);
			if (def != null) {
				def.place(new PlaceContext(level, box, seed, color, index), origin, direction);
			}
		}
	}
}
