package techguns.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.Techguns;
import techguns.gui.containers.AmmoPressMenu;
import techguns.gui.containers.MetalPressMenu;

public class TGMenus {

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Techguns.MODID);

	public static final DeferredHolder<MenuType<?>, MenuType<AmmoPressMenu>> AMMO_PRESS = MENUS.register("ammo_press",
			() -> IMenuTypeExtension.create(AmmoPressMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<MetalPressMenu>> METAL_PRESS = MENUS.register("metal_press",
			() -> IMenuTypeExtension.create(MetalPressMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.BlastFurnaceMenu>> BLAST_FURNACE = MENUS.register("blast_furnace",
			() -> IMenuTypeExtension.create(techguns.gui.containers.BlastFurnaceMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.GrinderMenu>> GRINDER = MENUS.register("grinder",
			() -> IMenuTypeExtension.create(techguns.gui.containers.GrinderMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.ChargingStationMenu>> CHARGING_STATION = MENUS.register("charging_station",
			() -> IMenuTypeExtension.create(techguns.gui.containers.ChargingStationMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.RepairBenchMenu>> REPAIR_BENCH = MENUS.register("repair_bench",
			() -> IMenuTypeExtension.create(techguns.gui.containers.RepairBenchMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.ChemLabMenu>> CHEM_LAB = MENUS.register("chem_lab",
			() -> IMenuTypeExtension.create(techguns.gui.containers.ChemLabMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.CamoBenchMenu>> CAMO_BENCH = MENUS.register("camo_bench",
			() -> IMenuTypeExtension.create(techguns.gui.containers.CamoBenchMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.UpgradeBenchMenu>> UPGRADE_BENCH = MENUS.register("armor_bench",
			() -> IMenuTypeExtension.create(techguns.gui.containers.UpgradeBenchMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.TurretMenu>> TURRET = MENUS.register("turret",
			() -> IMenuTypeExtension.create(techguns.gui.containers.TurretMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.FabricatorMenu>> FABRICATOR = MENUS.register("fabricator",
			() -> IMenuTypeExtension.create(techguns.gui.containers.FabricatorMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.ReactionChamberMenu>> REACTION_CHAMBER = MENUS.register("reaction_chamber",
			() -> IMenuTypeExtension.create(techguns.gui.containers.ReactionChamberMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.Door3x3Menu>> DOOR3x3 = MENUS.register("door3x3",
			() -> IMenuTypeExtension.create(techguns.gui.containers.Door3x3Menu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.ExplosiveChargeMenu>> EXPLOSIVE_CHARGE = MENUS.register("explosive_charge",
			() -> IMenuTypeExtension.create(techguns.gui.containers.ExplosiveChargeMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<techguns.gui.containers.OreDrillMenu>> ORE_DRILL = MENUS.register("ore_drill",
			() -> IMenuTypeExtension.create(techguns.gui.containers.OreDrillMenu::new));
}
