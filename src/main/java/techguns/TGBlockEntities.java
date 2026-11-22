package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.tileentities.AmmoPressTileEnt;
import techguns.tileentities.BasicInventoryTileEnt;
import techguns.tileentities.BasicPoweredTileEnt;
import techguns.tileentities.MetalPressTileEnt;

@EventBusSubscriber(modid = Techguns.MODID, bus = EventBusSubscriber.Bus.MOD)
public class TGBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Techguns.MODID);

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.TGSpawnerTileEnt>> TG_SPAWNER = BLOCK_ENTITIES.register("tg_spawner",
			() -> BlockEntityType.Builder.of(techguns.tileentities.TGSpawnerTileEnt::new, TGBlocks.TG_SPAWNER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoPressTileEnt>> AMMO_PRESS = BLOCK_ENTITIES.register("ammo_press",
			() -> BlockEntityType.Builder.of(AmmoPressTileEnt::new, TGBlocks.AMMO_PRESS.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MetalPressTileEnt>> METAL_PRESS = BLOCK_ENTITIES.register("metal_press",
			() -> BlockEntityType.Builder.of(MetalPressTileEnt::new, TGBlocks.METAL_PRESS.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.BlastFurnaceTileEnt>> BLAST_FURNACE = BLOCK_ENTITIES.register("blast_furnace",
			() -> BlockEntityType.Builder.of(techguns.tileentities.BlastFurnaceTileEnt::new, TGBlocks.BLAST_FURNACE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.GrinderTileEnt>> GRINDER = BLOCK_ENTITIES.register("grinder",
			() -> BlockEntityType.Builder.of(techguns.tileentities.GrinderTileEnt::new, TGBlocks.GRINDER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.ChargingStationTileEnt>> CHARGING_STATION = BLOCK_ENTITIES.register("charging_station",
			() -> BlockEntityType.Builder.of(techguns.tileentities.ChargingStationTileEnt::new, TGBlocks.CHARGING_STATION.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.RepairBenchTileEnt>> REPAIR_BENCH = BLOCK_ENTITIES.register("repair_bench",
			() -> BlockEntityType.Builder.of(techguns.tileentities.RepairBenchTileEnt::new, TGBlocks.REPAIR_BENCH.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.ChemLabTileEnt>> CHEM_LAB = BLOCK_ENTITIES.register("chem_lab",
			() -> BlockEntityType.Builder.of(techguns.tileentities.ChemLabTileEnt::new, TGBlocks.CHEM_LAB.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.CamoBenchTileEnt>> CAMO_BENCH = BLOCK_ENTITIES.register("camo_bench",
			() -> BlockEntityType.Builder.of(techguns.tileentities.CamoBenchTileEnt::new, TGBlocks.CAMO_BENCH.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.TurretTileEnt>> TURRET = BLOCK_ENTITIES.register("turret",
			() -> BlockEntityType.Builder.of(techguns.tileentities.TurretTileEnt::new, TGBlocks.TURRET.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.UpgradeBenchTileEnt>> UPGRADE_BENCH = BLOCK_ENTITIES.register("armor_bench",
			() -> BlockEntityType.Builder.of(techguns.tileentities.UpgradeBenchTileEnt::new, TGBlocks.UPGRADE_BENCH.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.MultiBlockSlaveTileEnt>> MULTIBLOCK_SLAVE = BLOCK_ENTITIES.register("multiblock_slave",
			() -> BlockEntityType.Builder.of(techguns.tileentities.MultiBlockSlaveTileEnt::new, TGBlocks.FABRICATOR_HOUSING.get(), TGBlocks.FABRICATOR_GLASS.get(),
					TGBlocks.REACTIONCHAMBER_HOUSING.get(), TGBlocks.REACTIONCHAMBER_GLASS.get(), TGBlocks.OREDRILL_FRAME.get(), TGBlocks.OREDRILL_SCAFFOLD.get(),
					TGBlocks.OREDRILL_ROD.get(), TGBlocks.OREDRILL_ENGINE.get())
					.build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.FabricatorTileEntMaster>> FABRICATOR = BLOCK_ENTITIES.register("fabricator",
			() -> BlockEntityType.Builder.of(techguns.tileentities.FabricatorTileEntMaster::new, TGBlocks.FABRICATOR_CONTROLLER.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.ReactionChamberTileEntMaster>> REACTION_CHAMBER = BLOCK_ENTITIES.register("reaction_chamber",
			() -> BlockEntityType.Builder.of(techguns.tileentities.ReactionChamberTileEntMaster::new, TGBlocks.REACTIONCHAMBER_CONTROLLER.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.Door3x3TileEnt>> DOOR3x3 = BLOCK_ENTITIES.register("door3x3",
			() -> BlockEntityType.Builder.of(techguns.tileentities.Door3x3TileEnt::new, TGBlocks.DOOR3x3_METAL.get(), TGBlocks.DOOR3x3_HANGAR.get(),
					TGBlocks.DOOR3x3_HANGAR_DOWN.get(), TGBlocks.DOOR3x3_NETHER.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.ExplosiveChargeTileEnt>> EXPLOSIVE_CHARGE = BLOCK_ENTITIES.register("explosive_charge",
			() -> BlockEntityType.Builder.of(techguns.tileentities.ExplosiveChargeTileEnt::new, TGBlocks.EXPLOSIVE_CHARGE.get(), TGBlocks.EXPLOSIVE_CHARGE_ADVANCED.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<techguns.tileentities.OreDrillTileEntMaster>> ORE_DRILL = BLOCK_ENTITIES.register("ore_drill",
			() -> BlockEntityType.Builder.of(techguns.tileentities.OreDrillTileEntMaster::new, TGBlocks.OREDRILL_CONTROLLER.get()).build(null));

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		for (DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>> holder : BLOCK_ENTITIES.getEntries()) {
			register(event, holder.get());
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> void register(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
				(be, side) -> target(be, Capabilities.ItemHandler.BLOCK) instanceof BasicInventoryTileEnt inv ? inv.getInventory() : null);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type,
				(be, side) -> target(be, Capabilities.EnergyStorage.BLOCK) instanceof BasicPoweredTileEnt pw ? pw.getEnergyStorage() : null);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
				(be, side) -> target(be, Capabilities.FluidHandler.BLOCK) instanceof techguns.tileentities.IFluidTankTile ft ? ft.getFluidHandler() : null);
	}

	/** multiblock parts hand capability requests to their controller */
	private static net.minecraft.world.level.block.entity.BlockEntity target(net.minecraft.world.level.block.entity.BlockEntity be,
			net.neoforged.neoforge.capabilities.BlockCapability<?, ?> cap) {
		return be instanceof techguns.tileentities.MultiBlockSlaveTileEnt slave ? slave.getCapabilityTarget(cap) : be;
	}
}
