package techguns.client.render.tileentities;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import techguns.TGBlockEntities;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.client.gui.AmmoPressScreen;
import techguns.client.gui.MetalPressScreen;
import techguns.client.models.machines.ModelAmmoPress;
import techguns.client.models.machines.ModelMetalPress;
import techguns.gui.TGMenus;
import techguns.tileentities.AmmoPressTileEnt;
import techguns.tileentities.MetalPressTileEnt;

/** Machine block entity renderers, their item renderers and the machine screens. */
public final class TGMachineRenderers {

	public static final RenderMachine<AmmoPressTileEnt> AMMO_PRESS = new RenderMachine<>(new ModelAmmoPress(),
			Techguns.rl("textures/blocks/ammopress.png"));
	public static final RenderMachine<MetalPressTileEnt> METAL_PRESS = new RenderMachine<>(new ModelMetalPress(),
			Techguns.rl("textures/blocks/metalpress.png"));
	public static final RenderMachine<techguns.tileentities.ChemLabTileEnt> CHEM_LAB = new RenderMachine<>(
			new techguns.client.models.machines.ModelChemLab(), Techguns.rl("textures/blocks/chemlab.png"));

	private TGMachineRenderers() {
	}

	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(TGBlockEntities.AMMO_PRESS.get(), ctx -> AMMO_PRESS);
		event.registerBlockEntityRenderer(TGBlockEntities.METAL_PRESS.get(), ctx -> METAL_PRESS);
		event.registerBlockEntityRenderer(TGBlockEntities.BLAST_FURNACE.get(), ctx -> new RenderBlastFurnace());
		event.registerBlockEntityRenderer(TGBlockEntities.GRINDER.get(), ctx -> new RenderGrinder());
		event.registerBlockEntityRenderer(TGBlockEntities.CHARGING_STATION.get(), ctx -> new RenderChargingStation());
		event.registerBlockEntityRenderer(TGBlockEntities.CHEM_LAB.get(), ctx -> CHEM_LAB);
		event.registerBlockEntityRenderer(TGBlockEntities.TURRET.get(), ctx -> new RenderTurret());
		event.registerBlockEntityRenderer(TGBlockEntities.FABRICATOR.get(), ctx -> new RenderFabricator());
		event.registerBlockEntityRenderer(TGBlockEntities.REACTION_CHAMBER.get(), ctx -> new RenderReactionChamber());
		event.registerBlockEntityRenderer(TGBlockEntities.ORE_DRILL.get(), ctx -> new RenderOreDrill());
		event.registerBlockEntityRenderer(TGBlockEntities.DOOR3x3.get(), ctx -> new RenderDoor3x3());
		event.registerEntityRenderer(techguns.TGEntities.TURRET.get(), techguns.client.render.entities.npcs.RenderNPCTurret::new);
	}

	public static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(TGMenus.AMMO_PRESS.get(), AmmoPressScreen::new);
		event.register(TGMenus.METAL_PRESS.get(), MetalPressScreen::new);
		event.register(TGMenus.BLAST_FURNACE.get(), techguns.client.gui.BlastFurnaceScreen::new);
		event.register(TGMenus.GRINDER.get(), techguns.client.gui.GrinderScreen::new);
		event.register(TGMenus.CHARGING_STATION.get(), techguns.client.gui.ChargingStationScreen::new);
		event.register(TGMenus.REPAIR_BENCH.get(), techguns.client.gui.RepairBenchScreen::new);
		event.register(TGMenus.CHEM_LAB.get(), techguns.client.gui.ChemLabScreen::new);
		event.register(TGMenus.CAMO_BENCH.get(), techguns.client.gui.CamoBenchScreen::new);
		event.register(TGMenus.UPGRADE_BENCH.get(), techguns.client.gui.UpgradeBenchScreen::new);
		event.register(TGMenus.TURRET.get(), techguns.client.gui.TurretScreen::new);
		event.register(TGMenus.FABRICATOR.get(), techguns.client.gui.FabricatorScreen::new);
		event.register(TGMenus.REACTION_CHAMBER.get(), techguns.client.gui.ReactionChamberScreen::new);
		event.register(TGMenus.ORE_DRILL.get(), techguns.client.gui.OreDrillScreen::new);
		event.register(TGMenus.EXPLOSIVE_CHARGE.get(), techguns.client.gui.ExplosiveChargeScreen::new);
		event.register(TGMenus.DOOR3x3.get(), techguns.client.gui.Door3x3Screen::new);
	}

	public static void registerItems(RegisterClientExtensionsEvent event) {
		item(event, AMMO_PRESS, TGBlocks.AMMO_PRESS.get());
		item(event, METAL_PRESS, TGBlocks.METAL_PRESS.get());
		item(event, CHEM_LAB, TGBlocks.CHEM_LAB.get());
		event.registerItem(new IClientItemExtensions() {
			private BlockEntityWithoutLevelRenderer bewlr;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (bewlr == null) {
					bewlr = new RenderTurret.ItemRenderer();
				}
				return bewlr;
			}
		}, TGBlocks.TURRET.get().asItem());
	}

	private static void item(RegisterClientExtensionsEvent event, RenderMachine<?> renderer, ItemLike item) {
		event.registerItem(new IClientItemExtensions() {
			private BlockEntityWithoutLevelRenderer bewlr;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (bewlr == null) {
					bewlr = new RenderMachine.ItemRenderer(renderer);
				}
				return bewlr;
			}
		}, item.asItem());
	}
}
