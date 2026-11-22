package techguns.entities.spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import techguns.TGConfig;
import techguns.TGEntities;
import techguns.Techguns;
import techguns.entities.npcs.GenericNPC;
import techguns.entities.npcs.TGDummySpawn;

/**
 * Techguns NPC spawning (1.12 TGSpawnManager): biomes spawn {@link TGDummySpawn}, which is replaced by an NPC picked
 * by weight from all NPCs up to the danger level of the position (distance to world spawn + biome type).
 */
@EventBusSubscriber(modid = Techguns.MODID)
public final class TGSpawnManager {

	public static final int MAX_DANGER = 5;

	/** an NPC that can spawn naturally, weight from the config */
	public record NpcSpawn(Supplier<? extends EntityType<? extends GenericNPC>> type, IntSupplier weight, int danger) {
	}

	public static final List<NpcSpawn> OVERWORLD = new ArrayList<>();
	public static final List<NpcSpawn> NETHER = new ArrayList<>();

	static {
		OVERWORLD.add(new NpcSpawn(TGEntities.ZOMBIE_SOLDIER, () -> TGConfig.spawnWeightZombieSoldier, 1));
		OVERWORLD.add(new NpcSpawn(TGEntities.ZOMBIE_FARMER, () -> TGConfig.spawnWeightZombieFarmer, 0));
		OVERWORLD.add(new NpcSpawn(TGEntities.ZOMBIE_MINER, () -> TGConfig.spawnWeightZombieMiner, 0));
		OVERWORLD.add(new NpcSpawn(TGEntities.SKELETON_SOLDIER, () -> TGConfig.spawnWeightSkeletonSoldier, 1));
		OVERWORLD.add(new NpcSpawn(TGEntities.PSYCHO_STEVE, () -> TGConfig.spawnWeightPsychoSteve, 1));
		OVERWORLD.add(new NpcSpawn(TGEntities.BANDIT, () -> TGConfig.spawnWeightBandit, 2));
		NETHER.add(new NpcSpawn(TGEntities.ZOMBIE_PIGMAN_SOLDIER, () -> TGConfig.spawnWeightZombiePigmanSoldier, 0));
		NETHER.add(new NpcSpawn(TGEntities.CYBER_DEMON, () -> TGConfig.spawnWeightCyberDemon, 0));
	}

	public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS = DeferredRegister
			.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Techguns.MODID);

	public static final Supplier<MapCodec<NpcSpawnsModifier>> NPC_SPAWNS = BIOME_MODIFIERS.register("npc_spawns",
			() -> RecordCodecBuilder.mapCodec(i -> i.group(Biome.LIST_CODEC.fieldOf("biomes").forGetter(NpcSpawnsModifier::biomes),
					com.mojang.serialization.Codec.BOOL.optionalFieldOf("nether", false).forGetter(NpcSpawnsModifier::nether))
					.apply(i, NpcSpawnsModifier::new)));

	private TGSpawnManager() {
	}

	public static void register(IEventBus modBus) {
		BIOME_MODIFIERS.register(modBus);
	}

	/** adds the dummy spawn with the configured weight to all non blacklisted biomes of the set */
	public record NpcSpawnsModifier(HolderSet<Biome> biomes, boolean nether) implements BiomeModifier {

		@Override
		public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
			if (phase != Phase.ADD || !biomes.contains(biome) || isBlacklisted(biome)) {
				return;
			}
			int weight = nether ? TGConfig.spawnWeightTGNether : TGConfig.spawnWeightTGOverworld;
			if (weight > 0) {
				builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(TGEntities.DUMMY_SPAWN.get(), weight, 1, 3));
			}
		}

		@Override
		public MapCodec<? extends BiomeModifier> codec() {
			return NPC_SPAWNS.get();
		}
	}

	private static boolean isBlacklisted(Holder<Biome> biome) {
		if (TGConfig.biomeBlacklist == null) {
			return false;
		}
		String id = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
		for (String s : TGConfig.biomeBlacklist) {
			if (!s.isEmpty() && s.equals(id)) {
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public static void onJoin(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof TGDummySpawn dummy) {
			event.setCanceled(true);
			if (event.getLevel() instanceof ServerLevel level) {
				handleSpawn(level, dummy);
			}
		}
	}

	public static void handleSpawn(ServerLevel level, TGDummySpawn dummy) {
		boolean nether = level.dimension() == Level.NETHER;
		List<NpcSpawn> table = nether ? NETHER : OVERWORLD;
		BlockPos pos = dummy.blockPosition();
		int danger = getDistanceDanger(level, pos, nether) + getBiomeDanger(level.getBiome(pos));

		int total = 0;
		for (NpcSpawn s : table) {
			if (s.danger() <= danger) {
				total += Math.max(0, s.weight().getAsInt());
			}
		}
		if (total <= 0) {
			return;
		}
		int roll = level.random.nextInt(total);
		NpcSpawn chosen = null;
		total = 0;
		for (NpcSpawn s : table) {
			if (s.danger() <= danger) {
				total += Math.max(0, s.weight().getAsInt());
				if (total > roll) {
					chosen = s;
					break;
				}
			}
		}
		if (chosen == null) {
			return;
		}
		GenericNPC npc = chosen.type().get().create(level);
		if (npc == null) {
			return;
		}
		npc.moveTo(dummy.getX(), dummy.getY(), dummy.getZ(), dummy.getYRot(), 0f);
		npc.onSpawnByManager(Math.min(danger, MAX_DANGER));
		level.addFreshEntity(npc);
	}

	private static int getBiomeDanger(Holder<Biome> b) {
		if (b.is(Tags.Biomes.IS_NETHER) || b.is(Tags.Biomes.IS_END)) {
			return 0;
		}
		if (b.is(Tags.Biomes.IS_WASTELAND) || b.is(Tags.Biomes.IS_SANDY) || b.is(Tags.Biomes.IS_SNOWY) || b.is(Tags.Biomes.IS_SWAMP)
				|| b.is(Tags.Biomes.IS_SPOOKY) || b.is(Tags.Biomes.IS_DEAD)) {
			return 2;
		}
		if (b.is(Tags.Biomes.IS_COLD) || b.is(Tags.Biomes.IS_SPARSE_VEGETATION) || b.is(Tags.Biomes.IS_SAVANNA) || b.is(Tags.Biomes.IS_DRY)) {
			return 1;
		}
		return 0;
	}

	private static int getDistanceDanger(ServerLevel level, BlockPos pos, boolean nether) {
		BlockPos spawn = level.getServer().overworld().getSharedSpawnPos();
		double dx = pos.getX() - spawn.getX();
		double dz = pos.getZ() - spawn.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		float factor = nether ? 0.5f : 1f;
		if (distance < TGConfig.distanceSpawnLevel0 * factor) {
			return 0;
		} else if (distance < TGConfig.distanceSpawnLevel1 * factor) {
			return 1;
		} else if (distance < TGConfig.distanceSpawnLevel2 * factor) {
			return 2;
		}
		return 3;
	}
}
