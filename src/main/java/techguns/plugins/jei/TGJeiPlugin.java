package techguns.plugins.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.client.gui.AmmoPressScreen;
import techguns.client.gui.BlastFurnaceScreen;
import techguns.client.gui.GrinderScreen;
import techguns.client.gui.MetalPressScreen;
import techguns.gui.TGMenus;
import techguns.gui.containers.AmmoPressMenu;
import techguns.gui.containers.BlastFurnaceMenu;
import techguns.gui.containers.GrinderMenu;
import techguns.gui.containers.MetalPressMenu;
import techguns.items.armors.GenericArmor;
import techguns.recipes.GrinderRecipe;
import techguns.recipes.TGRecipes;
import techguns.tileentities.operation.AmmoPressBuildPlans;

/**
 * JEI integration: a category per machine, the machine blocks as catalysts, click areas on the progress bars and
 * recipe transfer into the input slots. Every newly ported machine adds its category here.
 */
@JeiPlugin
public class TGJeiPlugin implements IModPlugin {

	private static final int PLAYER_INV_SLOTS = 36;

	@Override
	public ResourceLocation getPluginUid() {
		return Techguns.rl("jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration reg) {
		IGuiHelper gui = reg.getJeiHelpers().getGuiHelper();
		reg.addRecipeCategories(new TGJeiCategories.AmmoPress(gui), new TGJeiCategories.MetalPress(gui), new TGJeiCategories.BlastFurnace(gui),
				new TGJeiCategories.Grinder(gui), new TGJeiCategories.ChargingStation(gui), new TGJeiCategories.Fabricator(gui), new TGJeiCategories.ReactionChamber(gui), new TGJeiCategories.OreDrill(gui), new TGJeiCategories.ChemLab(gui), new TGJeiCategories.CamoBench(gui),
				new TGJeiCategories.UpgradeBench(gui));
	}

	@Override
	public void registerRecipes(IRecipeRegistration reg) {
		reg.addIngredientInfo(java.util.List.of(new net.minecraft.world.item.ItemStack(TGBlocks.TG_SPAWNER.get()),
				new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(techguns.Techguns.rl("tg_spawner_soldier_spawn")))),
				mezz.jei.api.constants.VanillaTypes.ITEM_STACK, net.minecraft.network.chat.Component.translatable("techguns.jei.tg_spawner.info"));
		reg.addIngredientInfo(new net.minecraft.world.item.ItemStack(techguns.TGuns.BIOGUN.get()), mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
				net.minecraft.network.chat.Component.translatable("techguns.jei.bioblob.info"));
		reg.addIngredientInfo(new net.minecraft.world.item.ItemStack(TGBlocks.TURRET.get()), mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
				net.minecraft.network.chat.Component.translatable("techguns.jei.turret.info"));
		var level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		RecipeManager rm = level.getRecipeManager();
		reg.addRecipes(TGJeiCategories.METAL_PRESS, rm.getAllRecipesFor(TGRecipes.METAL_PRESS.get()));
		reg.addRecipes(TGJeiCategories.BLAST_FURNACE, rm.getAllRecipesFor(TGRecipes.BLAST_FURNACE.get()));
		// fuel / oil recipes only show when a mod adds fluids to the techguns:fuel / techguns:oil tags
		reg.addRecipes(TGJeiCategories.CHEM_LAB, rm.getAllRecipesFor(TGRecipes.CHEM_LAB.get()).stream()
				.filter(r -> r.value().fluidInput()
						.map(f -> java.util.Arrays.stream(f.getFluids()).anyMatch(fs -> fs.getFluid().isSource(fs.getFluid().defaultFluidState())))
						.orElse(true))
				.toList());
		reg.addRecipes(TGJeiCategories.CAMO_BENCH, rm.getAllRecipesFor(TGRecipes.CAMO_BENCH.get()));
		reg.addRecipes(TGJeiCategories.UPGRADE_BENCH, rm.getAllRecipesFor(TGRecipes.UPGRADE_BENCH.get()));
		reg.addRecipes(TGJeiCategories.FABRICATOR, rm.getAllRecipesFor(TGRecipes.FABRICATOR.get()));
		reg.addIngredientInfo(java.util.List.of(new net.minecraft.world.item.ItemStack(TGBlocks.FABRICATOR_CONTROLLER.get()), new net.minecraft.world.item.ItemStack(TGBlocks.FABRICATOR_HOUSING.get()), new net.minecraft.world.item.ItemStack(TGBlocks.FABRICATOR_GLASS.get())),
				mezz.jei.api.constants.VanillaTypes.ITEM_STACK, net.minecraft.network.chat.Component.translatable("techguns.jei.multiblock.fabricator"));
		reg.addRecipes(TGJeiCategories.REACTION_CHAMBER, rm.getAllRecipesFor(TGRecipes.REACTION_CHAMBER.get()));
		reg.addRecipes(TGJeiCategories.ORE_DRILL, TGJeiCategories.OreDrillRecipe.all());
		reg.addIngredientInfo(java.util.List.of(new net.minecraft.world.item.ItemStack(TGBlocks.OREDRILL_CONTROLLER.get()), new net.minecraft.world.item.ItemStack(TGBlocks.OREDRILL_ENGINE.get()),
				new net.minecraft.world.item.ItemStack(TGBlocks.OREDRILL_ROD.get()), new net.minecraft.world.item.ItemStack(TGBlocks.OREDRILL_FRAME.get()), new net.minecraft.world.item.ItemStack(TGBlocks.OREDRILL_SCAFFOLD.get())),
				mezz.jei.api.constants.VanillaTypes.ITEM_STACK, net.minecraft.network.chat.Component.translatable("techguns.jei.multiblock.oredrill"));
		reg.addIngredientInfo(java.util.List.of(new net.minecraft.world.item.ItemStack(TGBlocks.EXPLOSIVE_CHARGE.get()),
				new net.minecraft.world.item.ItemStack(TGBlocks.EXPLOSIVE_CHARGE_ADVANCED.get())), mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
				net.minecraft.network.chat.Component.translatable("techguns.jei.explosivecharge.info"));
		reg.addIngredientInfo(TGBlocks.MILITARY_CRATES.values().stream().map(b -> new net.minecraft.world.item.ItemStack(b.get())).toList(),
				mezz.jei.api.constants.VanillaTypes.ITEM_STACK, net.minecraft.network.chat.Component.translatable("techguns.jei.militarycrate.info"));
		reg.addIngredientInfo(java.util.List.of(new net.minecraft.world.item.ItemStack(TGBlocks.DOOR3x3_METAL.get()),
				new net.minecraft.world.item.ItemStack(TGBlocks.DOOR3x3_HANGAR.get()), new net.minecraft.world.item.ItemStack(TGBlocks.DOOR3x3_HANGAR_DOWN.get()),
				new net.minecraft.world.item.ItemStack(TGBlocks.DOOR3x3_NETHER.get())), mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
				net.minecraft.network.chat.Component.translatable("techguns.jei.door3x3.info"));
		reg.addIngredientInfo(java.util.List.of(new net.minecraft.world.item.ItemStack(techguns.TGItems.RAD_AWAY.get()),
				new net.minecraft.world.item.ItemStack(techguns.TGItems.RAD_PILLS.get())), mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
				net.minecraft.network.chat.Component.translatable("techguns.jei.radiation.info"));
		java.util.List<mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe> brewing = new java.util.ArrayList<>();
		int brewId = 0;
		for (techguns.radiation.RadBrewingRecipe r : techguns.radiation.RadiationEvents.brewingRecipes()) {
			java.util.List<net.minecraft.world.item.ItemStack> ingredients = java.util.List.of(r.ingredient().getItems());
			if (ingredients.isEmpty()) {
				continue;
			}
			for (net.minecraft.world.item.Item container : new net.minecraft.world.item.Item[] { net.minecraft.world.item.Items.POTION,
					net.minecraft.world.item.Items.SPLASH_POTION, net.minecraft.world.item.Items.LINGERING_POTION }) {
				brewing.add(reg.getVanillaRecipeFactory().createBrewingRecipe(ingredients,
						net.minecraft.world.item.alchemy.PotionContents.createItemStack(container, r.input()),
						net.minecraft.world.item.alchemy.PotionContents.createItemStack(container, r.output()), techguns.Techguns.rl("brewing/rad_" + brewId++)));
			}
		}
		reg.addRecipes(mezz.jei.api.constants.RecipeTypes.BREWING, brewing);
		reg.addIngredientInfo(TGBlocks.ORE_CLUSTERS.values().stream().map(b -> new net.minecraft.world.item.ItemStack(b.get())).toList(),
				mezz.jei.api.constants.VanillaTypes.ITEM_STACK, net.minecraft.network.chat.Component.translatable("techguns.jei.orecluster.info"));
		reg.addIngredientInfo(java.util.List.of(new net.minecraft.world.item.ItemStack(TGBlocks.REACTIONCHAMBER_CONTROLLER.get()), new net.minecraft.world.item.ItemStack(TGBlocks.REACTIONCHAMBER_HOUSING.get()), new net.minecraft.world.item.ItemStack(TGBlocks.REACTIONCHAMBER_GLASS.get())),
				mezz.jei.api.constants.VanillaTypes.ITEM_STACK, net.minecraft.network.chat.Component.translatable("techguns.jei.multiblock.reactionchamber"));
		reg.addRecipes(TGJeiCategories.CHARGING_STATION, rm.getAllRecipesFor(TGRecipes.CHARGING_STATION.get()));

		List<TGJeiCategories.AmmoPressDisplay> plans = new ArrayList<>();
		for (int i = 0; i < AmmoPressBuildPlans.PLAN_COUNT; i++) {
			plans.add(new TGJeiCategories.AmmoPressDisplay(i));
		}
		reg.addRecipes(TGJeiCategories.AMMO_PRESS, plans);

		List<TGJeiCategories.GrinderDisplay> grinder = new ArrayList<>();
		rm.getAllRecipesFor(TGRecipes.GRINDER.get()).forEach(h -> grinder.add(TGJeiCategories.GrinderDisplay.of(h.value())));
		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof GenericArmor) {
				ItemStack armor = new ItemStack(item);
				List<ItemStack> mats = GrinderRecipe.getArmorOutputs(armor);
				if (!mats.isEmpty()) {
					grinder.add(new TGJeiCategories.GrinderDisplay(List.of(armor),
							mats.stream().map(m -> new GrinderRecipe.ChanceOutput(m, 1f)).toList(), true));
				}
			}
		}
		reg.addRecipes(TGJeiCategories.GRINDER, grinder);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
		reg.addRecipeCatalyst(TGBlocks.AMMO_PRESS.get(), TGJeiCategories.AMMO_PRESS);
		reg.addRecipeCatalyst(TGBlocks.METAL_PRESS.get(), TGJeiCategories.METAL_PRESS);
		reg.addRecipeCatalyst(TGBlocks.BLAST_FURNACE.get(), TGJeiCategories.BLAST_FURNACE);
		reg.addRecipeCatalyst(TGBlocks.GRINDER.get(), TGJeiCategories.GRINDER);
		reg.addRecipeCatalyst(TGBlocks.CHEM_LAB.get(), TGJeiCategories.CHEM_LAB);
		reg.addRecipeCatalyst(TGBlocks.CAMO_BENCH.get(), TGJeiCategories.CAMO_BENCH);
		reg.addRecipeCatalyst(TGBlocks.UPGRADE_BENCH.get(), TGJeiCategories.UPGRADE_BENCH);
		reg.addRecipeCatalyst(TGBlocks.FABRICATOR_CONTROLLER.get(), TGJeiCategories.FABRICATOR);
		reg.addRecipeCatalyst(TGBlocks.REACTIONCHAMBER_CONTROLLER.get(), TGJeiCategories.REACTION_CHAMBER);
		reg.addRecipeCatalyst(TGBlocks.OREDRILL_CONTROLLER.get(), TGJeiCategories.ORE_DRILL);
		reg.addRecipeCatalyst(TGBlocks.CHARGING_STATION.get(), TGJeiCategories.CHARGING_STATION);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration reg) {
		reg.addRecipeClickArea(AmmoPressScreen.class, 119, 36, 19, 22, TGJeiCategories.AMMO_PRESS);
		reg.addRecipeClickArea(MetalPressScreen.class, 119, 36, 19, 22, TGJeiCategories.METAL_PRESS);
		reg.addRecipeClickArea(BlastFurnaceScreen.class, 19, 53, 90, 11, TGJeiCategories.BLAST_FURNACE);
		reg.addRecipeClickArea(GrinderScreen.class, 31, 39, 42, 22, TGJeiCategories.GRINDER);
		reg.addRecipeClickArea(techguns.client.gui.ChemLabScreen.class, 81, 17, 50, 30, TGJeiCategories.CHEM_LAB);
		reg.addRecipeClickArea(techguns.client.gui.UpgradeBenchScreen.class, 72, 47, 40, 16, TGJeiCategories.UPGRADE_BENCH);
		reg.addRecipeClickArea(techguns.client.gui.FabricatorScreen.class, 21, 53, 91, 11, TGJeiCategories.FABRICATOR);
		reg.addRecipeClickArea(techguns.client.gui.OreDrillScreen.class, 74, 16, 27, 39, TGJeiCategories.ORE_DRILL);
		reg.addRecipeClickArea(techguns.client.gui.ReactionChamberScreen.class, 55, 17, 34, 16, TGJeiCategories.REACTION_CHAMBER);
		reg.addRecipeClickArea(techguns.client.gui.ChargingStationScreen.class, 39, 19, 27, 10, TGJeiCategories.CHARGING_STATION);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
		// menu slots: machine slots first (inputs at the start), then the player inventory
		reg.addRecipeTransferHandler(AmmoPressMenu.class, TGMenus.AMMO_PRESS.get(), TGJeiCategories.AMMO_PRESS, 0, 3, 5, PLAYER_INV_SLOTS);
		reg.addRecipeTransferHandler(MetalPressMenu.class, TGMenus.METAL_PRESS.get(), TGJeiCategories.METAL_PRESS, 0, 2, 4, PLAYER_INV_SLOTS);
		reg.addRecipeTransferHandler(BlastFurnaceMenu.class, TGMenus.BLAST_FURNACE.get(), TGJeiCategories.BLAST_FURNACE, 0, 2, 4, PLAYER_INV_SLOTS);
		reg.addRecipeTransferHandler(GrinderMenu.class, TGMenus.GRINDER.get(), TGJeiCategories.GRINDER, 0, 1, 11, PLAYER_INV_SLOTS);
		reg.addRecipeTransferHandler(techguns.gui.containers.ChemLabMenu.class, TGMenus.CHEM_LAB.get(), TGJeiCategories.CHEM_LAB, 0, 3, 5, PLAYER_INV_SLOTS);
		reg.addRecipeTransferHandler(techguns.gui.containers.FabricatorMenu.class, TGMenus.FABRICATOR.get(), TGJeiCategories.FABRICATOR, 0, 4, 6, 36);
		reg.addRecipeTransferHandler(techguns.gui.containers.ReactionChamberMenu.class, TGMenus.REACTION_CHAMBER.get(), TGJeiCategories.REACTION_CHAMBER, 0, 2, 6, 36);
		reg.addRecipeTransferHandler(techguns.gui.containers.ChargingStationMenu.class, TGMenus.CHARGING_STATION.get(), TGJeiCategories.CHARGING_STATION, 0, 1, 3,
				PLAYER_INV_SLOTS);
	}
}
