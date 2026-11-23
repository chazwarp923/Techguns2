package techguns;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.entities.projectiles.*;
import techguns.entities.npcs.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.GenericProjectileExplosive;
import techguns.entities.projectiles.GenericProjectileIncendiary;
import techguns.entities.projectiles.StoneBulletProjectile;

public class TGEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Techguns.MODID);

	/** 1.12.2 tracked bullets up to 128 blocks. */
	public static final int BULLET_TRACK_RANGE = 128;

	public static final Supplier<EntityType<GenericProjectile>> GENERIC_PROJECTILE = projectile("genericprojectile", GenericProjectile::new);
	public static final Supplier<EntityType<GenericProjectileIncendiary>> GENERIC_PROJECTILE_INCENDIARY = projectile("genericprojectileincendiary",
			GenericProjectileIncendiary::new);
	public static final Supplier<EntityType<GenericProjectileExplosive>> GENERIC_PROJECTILE_EXPLOSIVE = projectile("genericprojectileexplosive",
			GenericProjectileExplosive::new);
	public static final Supplier<EntityType<StoneBulletProjectile>> STONE_BULLET = projectile("stonebulletprojectile", StoneBulletProjectile::new);
	public static final Supplier<EntityType<AdvancedBulletProjectile>> ADVANCED_BULLET = projectile("advancedbulletprojectile",
			AdvancedBulletProjectile::new);

	public static final Supplier<EntityType<BlasterProjectile>> BLASTER = projectile("blasterprojectile", BlasterProjectile::new);
	public static final Supplier<EntityType<LaserProjectile>> LASER = projectile("laserprojectile", LaserProjectile::new);
	public static final Supplier<EntityType<TeslaProjectile>> TESLA = projectile("teslaprojectile", TeslaProjectile::new);
	public static final Supplier<EntityType<FlamethrowerProjectile>> FLAMETHROWER = projectile("flamethrowerprojectile", FlamethrowerProjectile::new);
	public static final Supplier<EntityType<AlienBlasterProjectile>> ALIEN_BLASTER = projectile("alienblasterprojectile", AlienBlasterProjectile::new);
	public static final Supplier<EntityType<DeatomizerProjectile>> DEATOMIZER = projectile("deatomizerprojectile", DeatomizerProjectile::new);
	public static final Supplier<EntityType<CyberdemonBlasterProjectile>> CYBERDEMON_BLASTER = projectile("cyberdemonblasterprojectile", CyberdemonBlasterProjectile::new);
	public static final Supplier<EntityType<GaussProjectile>> GAUSS = projectile("gaussprojectile", GaussProjectile::new);
	public static final Supplier<EntityType<NDRProjectile>> NDR = projectile("ndrprojectile", NDRProjectile::new);
	public static final Supplier<EntityType<Grenade40mmProjectile>> GRENADE_40MM = projectile("grenade40mmprojectile", Grenade40mmProjectile::new);
	public static final Supplier<EntityType<RocketProjectile>> ROCKET = projectile("rocketprojectile", RocketProjectile::new);
	public static final Supplier<EntityType<RocketProjectileHV>> ROCKET_HV = projectile("rocketprojectilehv", RocketProjectileHV::new);
	public static final Supplier<EntityType<RocketProjectileNuke>> ROCKET_NUKE = projectile("rocketprojectilenuke", RocketProjectileNuke::new);
	public static final Supplier<EntityType<SonicShotgunProjectile>> SONIC_SHOTGUN = projectile("sonicshotgunprojectile", SonicShotgunProjectile::new);
	public static final Supplier<EntityType<BioGunProjectile>> BIOGUN = projectile("biogunprojectile", BioGunProjectile::new);
	public static final Supplier<EntityType<TFGProjectile>> TFG = projectile("tfgprojectile", TFGProjectile::new);
	public static final Supplier<EntityType<techguns.entities.projectiles.GrapplingHookProjectile>> GRAPPLING_HOOK = projectile("grappling_hook_projectile",
			techguns.entities.projectiles.GrapplingHookProjectile::new);
	public static final Supplier<EntityType<techguns.entities.projectiles.MagicRifleProjectile>> MAGIC_RIFLE = projectile("magic_rifle_projectile",
			techguns.entities.projectiles.MagicRifleProjectile::new);
	public static final Supplier<EntityType<PowerHammerProjectile>> POWERHAMMER = projectile("powerhammerprojectile", PowerHammerProjectile::new);
	public static final Supplier<EntityType<ChainsawProjectile>> CHAINSAW = projectile("chainsawprojectile", ChainsawProjectile::new);
	public static final Supplier<EntityType<GuidedMissileProjectile>> GUIDED_MISSILE = projectile("guidedmissileprojectile", GuidedMissileProjectile::new);
	public static final Supplier<EntityType<GuidedMissileProjectileHV>> GUIDED_MISSILE_HV = projectile("guidedmissileprojectilehv", GuidedMissileProjectileHV::new);
	public static final Supplier<EntityType<StielGrenadeProjectile>> GRENADE = projectile("grenadeprojectile", StielGrenadeProjectile::new);
	public static final Supplier<EntityType<FragGrenadeProjectile>> FRAG_GRENADE = projectile("fraggrenadeprojectile", FragGrenadeProjectile::new);

	public static final Supplier<EntityType<techguns.entities.npcs.NPCTurret>> TURRET = ENTITIES.register("turret",
			() -> EntityType.Builder.<techguns.entities.npcs.NPCTurret>of(techguns.entities.npcs.NPCTurret::new, MobCategory.MISC).sized(0.8f, 0.9f)
					.clientTrackingRange(5).updateInterval(3).build("turret"));

	public static final Supplier<EntityType<techguns.entities.special.EntityRadiation>> RADIATION = ENTITIES.register("radiation",
			() -> EntityType.Builder.<techguns.entities.special.EntityRadiation>of(techguns.entities.special.EntityRadiation::new, MobCategory.MISC)
					.sized(0.5f, 0.5f).noSummon().fireImmune().clientTrackingRange(1).updateInterval(100).build("radiation"));

	/* ------------------------------------------------------------------ NPCs */

	/** registered NPC: type, attributes, spawn egg colors */
	public record NpcEntry(Supplier<? extends EntityType<? extends net.minecraft.world.entity.LivingEntity>> type, Supplier<AttributeSupplier.Builder> attributes) {
	}

	public static final List<NpcEntry> NPCS = new ArrayList<>();

	public static final Supplier<EntityType<ZombieSoldier>> ZOMBIE_SOLDIER = npc("zombiesoldier", ZombieSoldier::new, false, ZombieSoldier::createAttributes, 0x757468, 0x38B038);
	public static final Supplier<EntityType<ZombieFarmer>> ZOMBIE_FARMER = npc("zombiefarmer", ZombieFarmer::new, false, ZombieFarmer::createAttributes, 0x757468, 0x38B038);
	public static final Supplier<EntityType<ZombieMiner>> ZOMBIE_MINER = npc("zombieminer", ZombieMiner::new, false, ZombieMiner::createAttributes, 0x757468, 0x38B038);
	public static final Supplier<EntityType<ArmySoldier>> ARMY_SOLDIER = npc("armysoldier", ArmySoldier::new, false, ArmySoldier::createAttributes, 0x74806e, 0x191512);
	public static final Supplier<EntityType<Bandit>> BANDIT = npc("bandit", Bandit::new, false, Bandit::createAttributes, 0x8f9d59, 0x2c3117);
	public static final Supplier<EntityType<Commando>> COMMANDO = npc("commando", Commando::new, false, Commando::createAttributes, 0x191512, 0x74806e);
	public static final Supplier<EntityType<DictatorDave>> DICTATOR_DAVE = npc("dictatordave", DictatorDave::new, false, DictatorDave::createAttributes, 0x8f9d59, 0xA0A000);
	public static final Supplier<EntityType<SkeletonSoldier>> SKELETON_SOLDIER = npc("skeletonsoldier", SkeletonSoldier::new, false, SkeletonSoldier::createAttributes, 0x404040, 0xF0F0F0);
	public static final Supplier<EntityType<PsychoSteve>> PSYCHO_STEVE = npc("psychosteve", PsychoSteve::new, false, PsychoSteve::createAttributes, 0x757468, 0xF0F0F0);
	public static final Supplier<EntityType<StormTrooper>> STORM_TROOPER = npc("stormtrooper", StormTrooper::new, false, StormTrooper::createAttributes, 0xF0F0F0, 0x404040);
	public static final Supplier<EntityType<Outcast>> OUTCAST = npc("outcast", Outcast::new, false, Outcast::createAttributes, 0x101010, 0xFF0000);
	public static final Supplier<EntityType<ZombiePigmanSoldier>> ZOMBIE_PIGMAN_SOLDIER = npc("zombiepigmansoldier", ZombiePigmanSoldier::new, true, ZombiePigmanSoldier::createAttributes, 0xFF1111, 0x770000);
	public static final Supplier<EntityType<ZombiePoliceman>> ZOMBIE_POLICEMAN = npc("zombiepoliceman", ZombiePoliceman::new, false, ZombiePoliceman::createAttributes, 0x303030, 0x0000FF);

	public static final Supplier<EntityType<CyberDemon>> CYBER_DEMON = npc("cyberdemon", CyberDemon::new, true, CyberDemon::createAttributes, 0xFF1111, 0x777777);
	public static final Supplier<EntityType<SuperMutantBasic>> SUPER_MUTANT_BASIC = npc("supermutantbasic", SuperMutantBasic::new, false, SuperMutantBasic::createAttributes, 0xc6a96b, 0x71552e, 1.0f, 2.7f);
	public static final Supplier<EntityType<SuperMutantBasic.Elite>> SUPER_MUTANT_ELITE = npc("supermutantelite", SuperMutantBasic.Elite::new, false, SuperMutantBasic.Elite::createAttributes, 0xc6a96b, 0x61451e, 1.4f, 3.3f);
	public static final Supplier<EntityType<techguns.entities.npcs.AlienBug>> ALIEN_BUG = mob("alienbug", techguns.entities.npcs.AlienBug::new, false,
			techguns.entities.npcs.AlienBug::createAttributes, 0xc6a96b, 0x71552e, 1.1f, 1.2f, 0.65f, 5);
	public static final Supplier<EntityType<techguns.entities.npcs.Ghastling>> GHASTLING = mob("ghastling", techguns.entities.npcs.Ghastling::new, true,
			techguns.entities.npcs.Ghastling::createAttributes, 0xaeaeae, 0xce81ff, 1.0f, 2.1f, 1.5f, 5);
	public static final Supplier<EntityType<techguns.entities.npcs.AttackHelicopter>> ATTACK_HELICOPTER = mob("attackhelicopter",
			techguns.entities.npcs.AttackHelicopter::new, true, techguns.entities.npcs.AttackHelicopter::createAttributes, 0x373d23, 0x8ec0d7, 4.0f, 4.0f, 0.5f, 13);
	public static final Supplier<EntityType<SuperMutantBasic.Heavy>> SUPER_MUTANT_HEAVY = npc("supermutantheavy", SuperMutantBasic.Heavy::new, false, SuperMutantBasic.Heavy::createAttributes, 0xc6a96b, 0x51350e, 1.2f, 3.0f);

	/** placeholder that natural spawning creates, replaced by a Techguns NPC by the spawn manager */
	public static final Supplier<EntityType<TGDummySpawn>> DUMMY_SPAWN = ENTITIES.register("tgdummyspawn",
			() -> EntityType.Builder.<TGDummySpawn>of(TGDummySpawn::new, MobCategory.MONSTER).sized(0.6f, 1.95f).noSummon().noSave().clientTrackingRange(1)
					.build("tgdummyspawn"));

	private static <T extends GenericNPC> Supplier<EntityType<T>> npc(String name, EntityType.EntityFactory<T> factory, boolean fireImmune,
			Supplier<AttributeSupplier.Builder> attributes, int egg1, int egg2) {
		return npc(name, factory, fireImmune, attributes, egg1, egg2, 0.6f, 1.95f);
	}

	private static <T extends GenericNPC> Supplier<EntityType<T>> npc(String name, EntityType.EntityFactory<T> factory, boolean fireImmune,
			Supplier<AttributeSupplier.Builder> attributes, int egg1, int egg2, float width, float height) {
		Supplier<EntityType<T>> type = ENTITIES.register(name, () -> {
			EntityType.Builder<T> b = EntityType.Builder.of(factory, MobCategory.MONSTER).sized(width, height).eyeHeight(height * 0.89f).clientTrackingRange(5);
			if (fireImmune) {
				b.fireImmune();
			}
			return b.build(name);
		});
		NPCS.add(new NpcEntry(type, attributes));
		TGItems.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, egg1, egg2, new Item.Properties()));
		return type;
	}

	/** custom (non GenericNPC) mobs: explicit eye height and client tracking range in chunks */
	private static <T extends net.minecraft.world.entity.Mob> Supplier<EntityType<T>> mob(String name, EntityType.EntityFactory<T> factory, boolean fireImmune,
			Supplier<AttributeSupplier.Builder> attributes, int egg1, int egg2, float width, float height, float eye, int tracking) {
		Supplier<EntityType<T>> type = ENTITIES.register(name, () -> {
			EntityType.Builder<T> b = EntityType.Builder.of(factory, MobCategory.MONSTER).sized(width, height).eyeHeight(eye).clientTrackingRange(tracking);
			if (fireImmune) {
				b.fireImmune();
			}
			return b.build(name);
		});
		NPCS.add(new NpcEntry(type, attributes));
		TGItems.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, egg1, egg2, new Item.Properties()));
		return type;
	}

	public static void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
		event.put(TURRET.get(), techguns.entities.npcs.NPCTurret.createAttributes().build());
		for (NpcEntry e : NPCS) {
			event.put(e.type().get(), e.attributes().get().build());
		}
		event.put(DUMMY_SPAWN.get(), Monster.createMonsterAttributes().build());
	}

	public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
		event.register(DUMMY_SPAWN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
				RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	private static <T extends Entity> Supplier<EntityType<T>> projectile(String name, EntityType.EntityFactory<T> factory) {
		return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC).sized(0.25f, 0.25f)
				.clientTrackingRange(BULLET_TRACK_RANGE / 16).updateInterval(1).noSave().build(name));
	}
}
