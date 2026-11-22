package techguns.world.gen;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import techguns.Techguns;
import techguns.blocks.BlockTGLamp;
import techguns.blocks.BlockTGLantern;
import techguns.blocks.BlockTGSpawner;
import techguns.tileentities.TGSpawnerTileEnt;

/**
 * One palette entry of a 1.12 scanned structure (1.12 techguns.util.MBlock and subclasses). Pass 0 blocks are placed
 * first, pass 1 blocks (torches, doors, ladders... anything attached to other blocks) afterwards.
 */
public class MBlock {

	protected final BlockState state;
	protected int pass;

	public MBlock(BlockState state) {
		this.state = state;
		this.pass = needsSupport(state) ? 1 : 0;
	}

	/** 1.12 BlockData layers: attached blocks go into the second pass */
	static boolean needsSupport(BlockState s) {
		Block b = s.getBlock();
		return b instanceof TorchBlock || b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof LadderBlock || b instanceof BaseRailBlock
				|| b instanceof LeverBlock || b instanceof ButtonBlock || b instanceof SignBlock || b instanceof BannerBlock || b instanceof AnvilBlock
				|| b instanceof LiquidBlock || b instanceof RedStoneWireBlock || b instanceof BlockTGLamp || b instanceof BlockTGLantern;
	}

	public int getPass() {
		return pass;
	}

	public static Rotation rotation(int steps) {
		return switch (((steps % 4) + 4) % 4) {
			case 1 -> Rotation.COUNTERCLOCKWISE_90;
			case 2 -> Rotation.CLOCKWISE_180;
			case 3 -> Rotation.CLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	public void place(PlaceContext ctx, BlockPos pos, int rotation) {
		BlockState target = ctx.stateOverrides.get(pos);
		if (target == null) {
			target = rotatedState(rotation);
		}
		if (ctx.set(pos, target)) {
			afterPlace(ctx, pos, target);
		}
	}

	public BlockState rotatedState(int rotation) {
		return state.rotate(rotation(rotation));
	}

	/** places only into replaceable blocks (foundations) */
	public void placeReplaceableOnly(PlaceContext ctx, BlockPos pos, int rotation) {
		if (ctx.inside(pos) && ctx.get(pos).canBeReplaced()) {
			place(ctx, pos, rotation);
		}
	}

	/** block entity setup after placement */
	protected void afterPlace(PlaceContext ctx, BlockPos pos, BlockState placed) {
	}

	/* ------------------------------------------------------------------ variants */

	/** weighted random choice per block (1.12 MultiMBlock / MultiMMBlock) */
	public static class Multi extends MBlock {
		protected final MBlock[] blocks;
		protected final int[] weights;
		protected final int total;

		public Multi(MBlock[] blocks, int[] weights) {
			super(blocks[0].state);
			this.blocks = blocks;
			this.weights = weights;
			int t = 0;
			for (int w : weights) {
				t += w;
			}
			this.total = t;
			this.pass = blocks[0].pass;
		}

		public Multi(BlockState[] states, int[] weights) {
			this(wrap(states), weights);
		}

		static MBlock[] wrap(BlockState[] states) {
			MBlock[] out = new MBlock[states.length];
			for (int i = 0; i < states.length; i++) {
				out[i] = new MBlock(states[i]);
			}
			return out;
		}

		/** 1.12 rollBlockIndex: roll in [0, total], first cumulative weight &gt;= roll */
		protected int roll(RandomSource rnd) {
			int roll = rnd.nextInt(total + 1);
			int sum = 0;
			for (int i = 0; i < blocks.length; i++) {
				sum += weights[i];
				if (roll <= sum) {
					return i;
				}
			}
			return blocks.length - 1;
		}

		@Override
		public void place(PlaceContext ctx, BlockPos pos, int rotation) {
			if (ctx.inside(pos)) {
				blocks[roll(ctx.random(pos))].place(ctx, pos, rotation);
			}
		}
	}

	/** one entry chosen per structure by the structure's index roll (1.12 MultiMMBlockIndexRoll) */
	public static class IndexRoll extends MBlock {
		protected final MBlock[] blocks;

		public IndexRoll(MBlock... blocks) {
			super(blocks[0].state);
			this.blocks = blocks;
			this.pass = blocks[0].pass;
		}

		protected MBlock pick(PlaceContext ctx) {
			return blocks[Math.min(Math.max(ctx.indexRoll, 0), blocks.length - 1)];
		}

		@Override
		public void place(PlaceContext ctx, BlockPos pos, int rotation) {
			pick(ctx).place(ctx, pos, rotation);
		}
	}

	/** ore cluster of the rolled type, sometimes the matching ore instead (1.12 MBlockOreclusterType) */
	public static class OreClusterType extends IndexRoll {
		private final float oreChance;
		private final MBlock[] ores;

		public OreClusterType(MBlock[] clusters, float oreChance, MBlock[] ores) {
			super(clusters);
			this.oreChance = oreChance;
			this.ores = ores;
		}

		@Override
		public void place(PlaceContext ctx, BlockPos pos, int rotation) {
			if (!ctx.inside(pos)) {
				return;
			}
			int i = Math.min(Math.max(ctx.indexRoll, 0), blocks.length - 1);
			if (oreChance > 0 && ores != null && ctx.random(pos).nextFloat() < oreChance) {
				ores[i].place(ctx, pos, rotation);
			} else {
				blocks[i].place(ctx, pos, rotation);
			}
		}
	}

	/** the rolled ore, sometimes stone instead (1.12 MBlockOreClusterTypeOre) */
	public static class OreClusterOre extends IndexRoll {
		private final MBlock stone;
		private final float stoneChance;

		public OreClusterOre(MBlock[] ores, MBlock stone, float stoneChance) {
			super(ores);
			this.stone = stone;
			this.stoneChance = stoneChance;
		}

		@Override
		public void place(PlaceContext ctx, BlockPos pos, int rotation) {
			if (!ctx.inside(pos)) {
				return;
			}
			if (stoneChance > 0 && ctx.random(pos).nextFloat() < stoneChance) {
				stone.place(ctx, pos, rotation);
			} else {
				pick(ctx).place(ctx, pos, rotation);
			}
		}
	}

	/** block depending on the biome color type: woodland, snow, desert, nether (1.12 MBlockBiomeColorType) */
	public static class BiomeColor extends MBlock {
		private final MBlock[] byBiome;

		public BiomeColor(BlockState... states) {
			super(states[0]);
			this.byBiome = Multi.wrap(states);
		}

		@Override
		public void place(PlaceContext ctx, BlockPos pos, int rotation) {
			int i = ctx.biome.ordinal();
			byBiome[i < byBiome.length ? i : 0].place(ctx, pos, rotation);
		}
	}

	/** chest with a loot table (1.12 MBlockChestLoottable) */
	public static class Chest extends MBlock {
		private final ResourceKey<LootTable> loot;

		public Chest(BlockState chest, String lootTable) {
			super(chest);
			this.loot = ResourceKey.create(Registries.LOOT_TABLE, Techguns.rl(lootTable));
		}

		private Chest(BlockState chest, ResourceKey<LootTable> loot) {
			super(chest);
			this.loot = loot;
		}

		/** same loot table, other state */
		public Chest withState(BlockState chest) {
			return new Chest(chest, loot);
		}

		/** placed as the double chest half the structure pass worked out */
		@Override
		public void place(PlaceContext ctx, BlockPos pos, int rotation) {
			BlockState target = rotatedState(rotation);
			net.minecraft.world.level.block.state.properties.ChestType type = ctx.chestTypes.get(pos);
			if (type != null && target.hasProperty(ChestBlock.TYPE)) {
				target = target.setValue(ChestBlock.TYPE, type);
			}
			if (ctx.set(pos, target)) {
				afterPlace(ctx, pos, target);
			}
		}

		@Override
		protected void afterPlace(PlaceContext ctx, BlockPos pos, BlockState placed) {
			if (ctx.level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity c) {
				c.setLootTable(loot, ctx.random(pos).nextLong());
			}
		}
	}

	/** Techguns dungeon spawner with its mob list (1.12 MBlockTGSpawner) */
	public static class Spawner extends MBlock {
		private final int mobsLeft, maxActive, spawnDelay, spawnRange;
		private final java.util.List<Supplier<? extends EntityType<?>>> types = new java.util.ArrayList<>();
		private final java.util.List<Integer> weights = new java.util.ArrayList<>();
		private Supplier<ItemStack> weapon = () -> ItemStack.EMPTY;
		private int heightOffset = 0;

		public Spawner(BlockTGSpawner.Type type, int mobsLeft, int maxActive, int spawnDelay, int spawnRange) {
			super(techguns.TGBlocks.TG_SPAWNER.get().defaultBlockState().setValue(BlockTGSpawner.TYPE, type));
			this.mobsLeft = mobsLeft;
			this.maxActive = maxActive;
			this.spawnDelay = spawnDelay;
			this.spawnRange = spawnRange;
		}

		public Spawner mob(Supplier<? extends EntityType<?>> type, int weight) {
			types.add(type);
			weights.add(weight);
			return this;
		}

		public Spawner weapon(Supplier<ItemStack> weapon) {
			this.weapon = weapon;
			return this;
		}

		/** spawn this many blocks above the spawner (helicopters) */
		public Spawner heightOffset(int offset) {
			this.heightOffset = offset;
			return this;
		}

		@Override
		protected void afterPlace(PlaceContext ctx, BlockPos pos, BlockState placed) {
			if (ctx.level.getBlockEntity(pos) instanceof TGSpawnerTileEnt spawner) {
				spawner.setParams(mobsLeft, maxActive, spawnDelay, spawnRange);
				if (heightOffset != 0) {
					spawner.setSpawnHeightOffset(heightOffset);
				}
				ItemStack w = weapon.get();
				if (!w.isEmpty()) {
					spawner.setWeaponOverride(w);
				}
				for (int i = 0; i < types.size(); i++) {
					spawner.addMobType(types.get(i).get(), weights.get(i));
				}
			}
		}
	}

	/** vanilla mob spawner for one mob (1.12 MBlockVanillaSpawner) */
	public static class VanillaSpawner extends MBlock {
		private final Supplier<? extends EntityType<?>> type;

		public VanillaSpawner(Supplier<? extends EntityType<?>> type) {
			super(Blocks.SPAWNER.defaultBlockState());
			this.type = type;
		}

		@Override
		protected void afterPlace(PlaceContext ctx, BlockPos pos, BlockState placed) {
			if (ctx.level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
				spawner.setEntityId(type.get(), ctx.random(pos));
			}
		}
	}

	/** camo net roof in the biome's camo (1.12 MBlockCamoNetTop) */
	public static MBlock camoNetTop() {
		return new BiomeColor(techguns.TGBlocks.CAMONET_TOP.get().defaultBlockState(), techguns.TGBlocks.CAMONET_TOP_SNOW.get().defaultBlockState(),
				techguns.TGBlocks.CAMONET_TOP_DESERT.get().defaultBlockState(), techguns.TGBlocks.CAMONET_TOP.get().defaultBlockState());
	}
}
