package techguns;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.blocks.BlockSandbags;
import techguns.blocks.BlockTGLadder;
import techguns.blocks.BlockTGLamp;
import techguns.blocks.BlockTGLantern;

/**
 * Block registry. The 1.12.2 metadata blocks (basicore, metalpanel, nethermetal, concrete, lamp0, ...)
 * are flattened into one block per former variant. Explosion resistance values follow the 1.12.2
 * semantics (setHardness(h) gave an explosion resistance of h).
 */
public class TGBlocks {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Techguns.MODID);

	/* Ores (copper ore removed, vanilla provides it) */
	public static final DeferredBlock<Block> ORE_TIN = ore("ore_tin", 4.0f, 0);
	public static final DeferredBlock<Block> ORE_LEAD = ore("ore_lead", 6.0f, 0);
	public static final DeferredBlock<Block> ORE_TITANIUM = ore("ore_titanium", 8.0f, 0);
	public static final DeferredBlock<Block> ORE_URANIUM = ore("ore_uranium", 7.0f, 4);
	/* Deepslate variants (new, 1.12 had no deepslate): same drops, a bit harder like vanilla's deepslate ores */
	public static final DeferredBlock<Block> DEEPSLATE_ORE_TIN = deepslateOre("deepslate_ore_tin", 4.0f, 0);
	public static final DeferredBlock<Block> DEEPSLATE_ORE_LEAD = deepslateOre("deepslate_ore_lead", 6.0f, 0);
	public static final DeferredBlock<Block> DEEPSLATE_ORE_TITANIUM = deepslateOre("deepslate_ore_titanium", 8.0f, 0);
	public static final DeferredBlock<Block> DEEPSLATE_ORE_URANIUM = deepslateOre("deepslate_ore_uranium", 7.0f, 4);

	/* Metal panels (camo changeable group "metalpanel") */
	public static final DeferredBlock<Block> CONTAINER_RED = metal("container_red");
	public static final DeferredBlock<Block> CONTAINER_GREEN = metal("container_green");
	public static final DeferredBlock<Block> CONTAINER_BLUE = metal("container_blue");
	public static final DeferredBlock<Block> CONTAINER_ORANGE = metal("container_orange");
	public static final DeferredBlock<Block> PANEL_LARGE_BORDER = metal("panel_large_border");
	public static final DeferredBlock<Block> STEELFRAME_BLUE = metal("steelframe_blue");
	public static final DeferredBlock<Block> STEELFRAME_DARK = metal("steelframe_dark");
	public static final DeferredBlock<Block> STEELFRAME_SCAFFOLD = metal("steelframe_scaffold");

	/* Nether metal */
	public static final DeferredBlock<Block> NETHERMETAL_PANEL = nether("nethermetal_panel", 0);
	public static final DeferredBlock<Block> NETHERMETAL_GRATE1 = nether("nethermetal_grate1", 0);
	public static final DeferredBlock<Block> NETHERMETAL_GRATE2 = nether("nethermetal_grate2", 0);
	public static final DeferredBlock<Block> NETHERMETAL_GREY_DARK = nether("nethermetal_grey_dark", 0);
	public static final DeferredBlock<Block> NETHERMETAL_GREY = nether("nethermetal_grey", 0);
	public static final DeferredBlock<Block> NETHERMETAL_GREY_TILES = nether("nethermetal_grey_tiles", 0);
	public static final DeferredBlock<Block> NETHERMETAL_BORDER_RED = nether("nethermetal_border_red", 0);
	public static final DeferredBlock<Block> NETHERMETAL_PLATE_BLACK = nether("nethermetal_plate_black", 0);
	public static final DeferredBlock<Block> NETHERMETAL_PLATE_RED = nether("nethermetal_plate_red", 0);
	public static final DeferredBlock<Block> NETHERMETAL_BORDER_LAVA = nether("nethermetal_border_lava", 15);

	/* Reinforced concrete */
	public static final DeferredBlock<Block> CONCRETE_BROWN = concrete("concrete_brown");
	public static final DeferredBlock<Block> CONCRETE_BROWN_LIGHT = concrete("concrete_brown_light");
	public static final DeferredBlock<Block> CONCRETE_GREY = concrete("concrete_grey");
	public static final DeferredBlock<Block> CONCRETE_GREY_DARK = concrete("concrete_grey_dark");
	public static final DeferredBlock<Block> CONCRETE_BROWN_PIPES = concrete("concrete_brown_pipes");
	public static final DeferredBlock<Block> CONCRETE_BROWN_LIGHT_SCAFF = concrete("concrete_brown_light_scaff");

	/* Neon lights */
	public static final DeferredBlock<Block> NEONTUBES2 = neon("neontubes2");
	public static final DeferredBlock<Block> NEONTUBES2_ROTATED = neon("neontubes2_rotated");
	public static final DeferredBlock<Block> NEONTUBES4 = neon("neontubes4");
	public static final DeferredBlock<Block> NEONTUBES4_ROTATED = neon("neontubes4_rotated");
	public static final DeferredBlock<Block> NEONSQUARE_WHITE = neon("neonsquare_white");

	/* Lamps */
	public static final DeferredBlock<BlockTGLamp> LAMP_YELLOW = register("lamp_yellow", BlockTGLamp::new, lampProps());
	public static final DeferredBlock<BlockTGLamp> LAMP_WHITE = register("lamp_white", BlockTGLamp::new, lampProps());
	public static final DeferredBlock<BlockTGLantern> LANTERN_YELLOW = register("lantern_yellow", BlockTGLantern::new, lampProps());
	public static final DeferredBlock<BlockTGLantern> LANTERN_WHITE = register("lantern_white", BlockTGLantern::new, lampProps());

	/* Ladders */
	public static final DeferredBlock<BlockTGLadder> LADDER_METAL = ladder("ladder_metal");
	public static final DeferredBlock<BlockTGLadder> LADDER_SHINY = ladder("ladder_shiny");
	public static final DeferredBlock<BlockTGLadder> LADDER_RUSTY = ladder("ladder_rusty");
	public static final DeferredBlock<BlockTGLadder> LADDER_CARBON = ladder("ladder_carbon");

	/* Stairs */
	public static final DeferredBlock<StairBlock> STAIRS_METAL = stairs("stairs_metal", PANEL_LARGE_BORDER, metalProps());
	public static final DeferredBlock<StairBlock> STAIRS_METAL_2 = stairs("stairs_metal_2", STEELFRAME_DARK, metalProps());
	public static final DeferredBlock<StairBlock> STAIRS_CONCRETE = stairs("stairs_concrete", CONCRETE_BROWN_LIGHT,
			concreteProps().strength(6.0f, 6.0f));
	public static final DeferredBlock<StairBlock> STAIRS_CONCRETE_2 = stairs("stairs_concrete_2", CONCRETE_GREY_DARK,
			concreteProps().strength(6.0f, 6.0f));

	public static final DeferredBlock<BlockSandbags> SANDBAGS = register("sandbags", BlockSandbags::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).sound(SoundType.WOOL).strength(3.0f, 9.0f).noOcclusion());

	/* Machines (1.12 basicmachine / simplemachine metadata blocks) */
	public static final DeferredBlock<techguns.blocks.machines.BlockMachine<techguns.tileentities.AmmoPressTileEnt>> AMMO_PRESS = register("ammo_press",
			p -> new techguns.blocks.machines.BlockMachine<>(p, TGBlockEntities.AMMO_PRESS, net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED),
			machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockMachine<techguns.tileentities.MetalPressTileEnt>> METAL_PRESS = register("metal_press",
			p -> new techguns.blocks.machines.BlockMachine<>(p, TGBlockEntities.METAL_PRESS, net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED),
			machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockMachineFacing<techguns.tileentities.BlastFurnaceTileEnt>> BLAST_FURNACE = register("blast_furnace",
			p -> new techguns.blocks.machines.BlockMachineFacing<>(p, TGBlockEntities.BLAST_FURNACE), machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockMachineFacing<techguns.tileentities.GrinderTileEnt>> GRINDER = register("grinder",
			p -> new techguns.blocks.machines.BlockMachineFacing<>(p, TGBlockEntities.GRINDER), machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockMachineFacing<techguns.tileentities.ChargingStationTileEnt>> CHARGING_STATION = register("charging_station",
			p -> new techguns.blocks.machines.BlockMachineFacing<>(p, TGBlockEntities.CHARGING_STATION), machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockMachineFacing<techguns.tileentities.RepairBenchTileEnt>> REPAIR_BENCH = register("repair_bench",
			p -> new techguns.blocks.machines.BlockMachineFacing<>(p, TGBlockEntities.REPAIR_BENCH), machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockMachine<techguns.tileentities.ChemLabTileEnt>> CHEM_LAB = register("chem_lab",
			p -> new techguns.blocks.machines.BlockMachine<>(p, TGBlockEntities.CHEM_LAB, net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED),
			machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockMachineFacing<techguns.tileentities.CamoBenchTileEnt>> CAMO_BENCH = register("camo_bench",
			p -> new techguns.blocks.machines.BlockMachineFacing<>(p, TGBlockEntities.CAMO_BENCH), machineProps());
	/** 1.12 id armor_bench */
	public static final DeferredBlock<techguns.blocks.machines.BlockMachineFacing<techguns.tileentities.UpgradeBenchTileEnt>> UPGRADE_BENCH = register("armor_bench",
			p -> new techguns.blocks.machines.BlockMachineFacing<>(p, TGBlockEntities.UPGRADE_BENCH) {
				private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 13, 16);

				@Override
				protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
						net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
					return SHAPE;
				}
			}, machineProps());

	public static final DeferredBlock<techguns.blocks.machines.BlockTurret> TURRET = register("turret", techguns.blocks.machines.BlockTurret::new,
			machineProps());

	/* ---- multiblocks (1.12 multiblockmachine) ---- */
	public static final DeferredBlock<techguns.blocks.machines.BlockMultiBlockSlave> FABRICATOR_HOUSING = register("fabricator_housing",
			techguns.blocks.machines.BlockMultiBlockSlave::new, machineProps().noOcclusion());
	public static final DeferredBlock<techguns.blocks.machines.BlockMultiBlockSlave> FABRICATOR_GLASS = register("fabricator_glass",
			techguns.blocks.machines.BlockMultiBlockSlave::new,
			machineProps().noOcclusion().sound(net.minecraft.world.level.block.SoundType.GLASS).isViewBlocking((s, l, pos) -> false));
	public static final DeferredBlock<techguns.blocks.machines.BlockMultiBlockController<techguns.tileentities.FabricatorTileEntMaster>> FABRICATOR_CONTROLLER = register(
			"fabricator_controller", p -> new techguns.blocks.machines.BlockMultiBlockController<>(p, TGBlockEntities.FABRICATOR), machineProps().noOcclusion());
	public static final DeferredBlock<techguns.blocks.machines.BlockMultiBlockSlave> REACTIONCHAMBER_HOUSING = register("reactionchamber_housing",
			techguns.blocks.machines.BlockMultiBlockSlave::new, machineProps().noOcclusion());
	public static final DeferredBlock<techguns.blocks.machines.BlockMultiBlockSlave> REACTIONCHAMBER_GLASS = register("reactionchamber_glass",
			techguns.blocks.machines.BlockMultiBlockSlave::new,
			machineProps().noOcclusion().sound(net.minecraft.world.level.block.SoundType.GLASS).isViewBlocking((s, l, pos) -> false));
	public static final DeferredBlock<techguns.blocks.machines.BlockMultiBlockController<techguns.tileentities.ReactionChamberTileEntMaster>> REACTIONCHAMBER_CONTROLLER = register(
			"reactionchamber_controller", p -> new techguns.blocks.machines.BlockMultiBlockController<>(p, TGBlockEntities.REACTION_CHAMBER),
			machineProps().noOcclusion());

	public static final DeferredBlock<techguns.blocks.machines.BlockOreDrillPart> OREDRILL_FRAME = register("oredrill_frame",
			techguns.blocks.machines.BlockOreDrillPart::new, machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockOreDrillPart.Scaffold> OREDRILL_SCAFFOLD = register("oredrill_scaffold",
			techguns.blocks.machines.BlockOreDrillPart.Scaffold::new, machineProps().noOcclusion().isViewBlocking((s, l, pos) -> false));
	public static final DeferredBlock<techguns.blocks.machines.BlockOreDrillPart.Rod> OREDRILL_ROD = register("oredrill_rod",
			techguns.blocks.machines.BlockOreDrillPart.Rod::new, machineProps().noOcclusion());
	public static final DeferredBlock<techguns.blocks.machines.BlockOreDrillPart> OREDRILL_ENGINE = register("oredrill_engine",
			techguns.blocks.machines.BlockOreDrillPart::new, machineProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockOreDrillPart.Controller> OREDRILL_CONTROLLER = register("oredrill_controller",
			techguns.blocks.machines.BlockOreDrillPart.Controller::new, machineProps());

	/* ---- ore clusters, mined by ore drills ---- */
	public static final java.util.Map<techguns.blocks.OreClusterType, DeferredBlock<techguns.blocks.BlockOreCluster>> ORE_CLUSTERS = oreClusters();

	private static java.util.Map<techguns.blocks.OreClusterType, DeferredBlock<techguns.blocks.BlockOreCluster>> oreClusters() {
		java.util.Map<techguns.blocks.OreClusterType, DeferredBlock<techguns.blocks.BlockOreCluster>> map = new java.util.EnumMap<>(techguns.blocks.OreClusterType.class);
		for (techguns.blocks.OreClusterType t : techguns.blocks.OreClusterType.values()) {
			map.put(t, register("ore_cluster_" + t.getName(), p -> new techguns.blocks.BlockOreCluster(t, p), BlockBehaviour.Properties.of()
					.mapColor(t == techguns.blocks.OreClusterType.NETHER_CRYSTAL ? MapColor.NETHER : MapColor.STONE).strength(-1.0f, 3600000.0f)
					.noLootTable().sound(SoundType.STONE).isValidSpawn((s, l, pos, e) -> false)));
		}
		return map;
	}

	/* ---- doors ---- */
	public static final DeferredBlock<techguns.blocks.door.BlockDoor3x3> DOOR3x3_METAL = door3x3("door3x3_metal", techguns.blocks.door.Door3x3Type.METAL);
	public static final DeferredBlock<techguns.blocks.door.BlockDoor3x3> DOOR3x3_HANGAR = door3x3("door3x3_hangar", techguns.blocks.door.Door3x3Type.HANGAR_UP);
	public static final DeferredBlock<techguns.blocks.door.BlockDoor3x3> DOOR3x3_HANGAR_DOWN = door3x3("door3x3_hangar_down",
			techguns.blocks.door.Door3x3Type.HANGAR_DOWN);
	public static final DeferredBlock<techguns.blocks.door.BlockDoor3x3> DOOR3x3_NETHER = door3x3("door3x3_nether", techguns.blocks.door.Door3x3Type.NETHER);
	public static final DeferredBlock<techguns.blocks.door.BlockBunkerDoor> BUNKER_DOOR = bunkerDoor();

	private static DeferredBlock<techguns.blocks.door.BlockDoor3x3> door3x3(String name, techguns.blocks.door.Door3x3Type type) {
		DeferredBlock<techguns.blocks.door.BlockDoor3x3> b = registerNoItem(name, p -> new techguns.blocks.door.BlockDoor3x3(p, type),
				BlockBehaviour.Properties.of().mapColor(type == techguns.blocks.door.Door3x3Type.NETHER ? MapColor.NETHER : MapColor.METAL)
						.sound(SoundType.METAL).strength(5.0f, 10.0f).requiresCorrectToolForDrops().noOcclusion()
						.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
		TGItems.register(name, () -> new techguns.items.ItemDoor3x3(b.get(), new Item.Properties()));
		return b;
	}

	private static DeferredBlock<techguns.blocks.door.BlockBunkerDoor> bunkerDoor() {
		DeferredBlock<techguns.blocks.door.BlockBunkerDoor> b = registerNoItem("bunker_door", techguns.blocks.door.BlockBunkerDoor::new,
				BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(8.0f, 8.0f).requiresCorrectToolForDrops()
						.noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
		TGItems.register("bunker_door", () -> new net.minecraft.world.item.DoubleHighBlockItem(b.get(), new Item.Properties()));
		return b;
	}

	/* ---- mining charges ---- */
	public static final DeferredBlock<techguns.blocks.machines.BlockExplosiveCharge> EXPLOSIVE_CHARGE = register("explosive_charge",
			p -> new techguns.blocks.machines.BlockExplosiveCharge(p, false), chargeProps());
	public static final DeferredBlock<techguns.blocks.machines.BlockExplosiveCharge> EXPLOSIVE_CHARGE_ADVANCED = register("explosive_charge_advanced",
			p -> new techguns.blocks.machines.BlockExplosiveCharge(p, true), chargeProps());

	private static BlockBehaviour.Properties chargeProps() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.METAL).strength(1.0f, 1.0f).noOcclusion()
				.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
	}

	/* ---- camo netting (camo bench groups "camonet" / "camonet_top") ---- */
	public static final DeferredBlock<techguns.blocks.BlockCamoNet> CAMONET = camonet("camonet", false);
	public static final DeferredBlock<techguns.blocks.BlockCamoNet> CAMONET_DESERT = camonet("camonet_desert", false);
	public static final DeferredBlock<techguns.blocks.BlockCamoNet> CAMONET_SNOW = camonet("camonet_snow", false);
	public static final DeferredBlock<techguns.blocks.BlockCamoNet> CAMONET_TOP = camonet("camonet_top", true);
	public static final DeferredBlock<techguns.blocks.BlockCamoNet> CAMONET_TOP_DESERT = camonet("camonet_top_desert", true);
	public static final DeferredBlock<techguns.blocks.BlockCamoNet> CAMONET_TOP_SNOW = camonet("camonet_top_snow", true);

	/* ---- military crates, drop their loot when broken ---- */
	public static final java.util.List<String> MILITARY_CRATE_TYPES = java.util.List.of("ammo", "gun", "armor", "medical", "explosive", "generic_oak",
			"generic_jungle", "generic_birch", "generic_spruce");
	public static final java.util.Map<String, DeferredBlock<techguns.blocks.BlockMilitaryCrate>> MILITARY_CRATES = militaryCrates();

	/* ---- bug nest blocks ---- */
	public static final DeferredBlock<Block> BUGNEST_SAND = register("bugnest_sand", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.SAND).sound(SoundType.SAND).strength(3.0f));
	public static final DeferredBlock<Block> BUGNEST_EGGS = register("bugnest_eggs", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).sound(SoundType.SLIME_BLOCK).strength(4.0f));
	public static final DeferredBlock<techguns.blocks.BlockSlimyLadder> SLIMY_LADDER = register("slimy_ladder", techguns.blocks.BlockSlimyLadder::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).sound(SoundType.SLIME_BLOCK).strength(0.4f).noOcclusion());

	private static DeferredBlock<techguns.blocks.BlockCamoNet> camonet(String name, boolean top) {
		return register(name, p -> new techguns.blocks.BlockCamoNet(top, p), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
				.sound(SoundType.WOOL).strength(2.0f).noOcclusion().ignitedByLava());
	}

	private static java.util.Map<String, DeferredBlock<techguns.blocks.BlockMilitaryCrate>> militaryCrates() {
		java.util.Map<String, DeferredBlock<techguns.blocks.BlockMilitaryCrate>> map = new java.util.LinkedHashMap<>();
		for (String t : MILITARY_CRATE_TYPES) {
			map.put(t, register("military_crate_" + t, techguns.blocks.BlockMilitaryCrate::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
					.instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(4.0f).noOcclusion().ignitedByLava()));
		}
		return map;
	}

	/* ---- bio gun goo and dungeon spawners ---- */
	public static final DeferredBlock<techguns.blocks.BlockBioblob> BIOBLOB = registerNoItem("bioblob", techguns.blocks.BlockBioblob::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).sound(SoundType.SLIME_BLOCK).strength(0.5f).lightLevel(s -> 7).noOcclusion()
					.noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
	public static final DeferredBlock<techguns.blocks.BlockTGSpawner> TG_SPAWNER = tgSpawner();

	private static DeferredBlock<techguns.blocks.BlockTGSpawner> tgSpawner() {
		DeferredBlock<techguns.blocks.BlockTGSpawner> b = register("tg_spawner", techguns.blocks.BlockTGSpawner::new, BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion().noCollission().isValidSpawn((s, l, p, e) -> false));
		TGItems.register("tg_spawner_soldier_spawn", () -> new BlockItem(b.get(), new Item.Properties().component(
				net.minecraft.core.component.DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY
						.with(techguns.blocks.BlockTGSpawner.TYPE, techguns.blocks.BlockTGSpawner.Type.SOLDIER_SPAWN))));
		return b;
	}

	/* ---- helpers ---- */

	private static BlockBehaviour.Properties metalProps() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(8.0f, 8.0f)
				.requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties machineProps() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(4.0f, 4.0f).requiresCorrectToolForDrops()
				.noOcclusion();
	}

	private static BlockBehaviour.Properties concreteProps() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE)
				.strength(8.0f, 8.0f).requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties lampProps() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).sound(SoundType.GLASS).strength(0.25f).lightLevel(s -> 15)
				.noOcclusion();
	}

	private static DeferredBlock<Block> ore(String name, float hardness, int light) {
		return register(name, Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
				.sound(SoundType.STONE).strength(hardness, hardness).requiresCorrectToolForDrops().lightLevel(s -> light));
	}

	/** vanilla's deepslate ores take 1.5 more hardness than the stone ones and have the deepslate look and sound */
	private static DeferredBlock<Block> deepslateOre(String name, float hardness, int light) {
		return register(name, Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
				.sound(SoundType.DEEPSLATE).strength(hardness + 1.5f, hardness).requiresCorrectToolForDrops().lightLevel(s -> light));
	}

	private static DeferredBlock<Block> metal(String name) {
		return register(name, Block::new, metalProps());
	}

	private static DeferredBlock<Block> nether(String name, int light) {
		return register(name, Block::new, metalProps().lightLevel(s -> light));
	}

	private static DeferredBlock<Block> concrete(String name) {
		return register(name, Block::new, concreteProps());
	}

	private static DeferredBlock<Block> neon(String name) {
		return register(name, Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).sound(SoundType.GLASS)
				.strength(4.0f, 4.0f).lightLevel(s -> 15));
	}

	private static DeferredBlock<BlockTGLadder> ladder(String name) {
		return register(name, BlockTGLadder::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
				.strength(6.0f, 6.0f).noOcclusion());
	}

	private static DeferredBlock<StairBlock> stairs(String name, Supplier<? extends Block> base, BlockBehaviour.Properties props) {
		return register(name, p -> new StairBlock(base.get().defaultBlockState(), p), props);
	}

	public static <T extends Block> DeferredBlock<T> register(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties props) {
		DeferredBlock<T> block = BLOCKS.register(name, () -> factory.apply(props));
		TGItems.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
		return block;
	}

	/** Registers a block without a block item. */
	public static <T extends Block> DeferredBlock<T> registerNoItem(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties props) {
		return BLOCKS.register(name, () -> factory.apply(props));
	}
}
