package techguns.plugins.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.recipes.BlastFurnaceRecipe;
import techguns.recipes.ChargingStationRecipe;
import techguns.recipes.ChemLabRecipe;
import techguns.recipes.CamoBenchRecipe;
import techguns.recipes.UpgradeBenchRecipe;
import techguns.tileentities.ChargingStationTileEnt;
import techguns.recipes.GrinderRecipe;
import techguns.recipes.MetalPressRecipe;
import techguns.recipes.TGRecipes;
import techguns.tileentities.AmmoPressTileEnt;
import techguns.tileentities.GrinderTileEnt;
import techguns.tileentities.MetalPressTileEnt;
import techguns.tileentities.operation.AmmoPressBuildPlans;

/** The JEI categories of the Techguns machines. */
public final class TGJeiCategories {

	public static final RecipeType<RecipeHolder<MetalPressRecipe>> METAL_PRESS = RecipeType.createFromVanilla(TGRecipes.METAL_PRESS.get());
	public static final RecipeType<RecipeHolder<BlastFurnaceRecipe>> BLAST_FURNACE = RecipeType.createFromVanilla(TGRecipes.BLAST_FURNACE.get());
	public static final RecipeType<RecipeHolder<ChargingStationRecipe>> CHARGING_STATION = RecipeType
			.createFromVanilla(TGRecipes.CHARGING_STATION.get());
	public static final RecipeType<RecipeHolder<ChemLabRecipe>> CHEM_LAB = RecipeType.createFromVanilla(TGRecipes.CHEM_LAB.get());
	public static final RecipeType<RecipeHolder<CamoBenchRecipe>> CAMO_BENCH = RecipeType.createFromVanilla(TGRecipes.CAMO_BENCH.get());
	public static final RecipeType<RecipeHolder<UpgradeBenchRecipe>> UPGRADE_BENCH = RecipeType.createFromVanilla(TGRecipes.UPGRADE_BENCH.get());
	public static final RecipeType<RecipeHolder<techguns.recipes.FabricatorRecipe>> FABRICATOR = RecipeType.createFromVanilla(TGRecipes.FABRICATOR.get());
	public static final RecipeType<OreDrillRecipe> ORE_DRILL = RecipeType.create(techguns.Techguns.MODID, "ore_drill", OreDrillRecipe.class);
	public static final RecipeType<RecipeHolder<techguns.recipes.ReactionChamberRecipe>> REACTION_CHAMBER = RecipeType.createFromVanilla(TGRecipes.REACTION_CHAMBER.get());
	public static final RecipeType<AmmoPressDisplay> AMMO_PRESS = RecipeType.create(Techguns.MODID, "ammo_press", AmmoPressDisplay.class);
	public static final RecipeType<GrinderDisplay> GRINDER = RecipeType.create(Techguns.MODID, "grinder", GrinderDisplay.class);

	private TGJeiCategories() {
	}

	/** one build plan of the ammo press */
	public record AmmoPressDisplay(int plan) {
	}

	/** grinder recipe or the repair materials of a piece of armor */
	public record GrinderDisplay(List<ItemStack> input, List<GrinderRecipe.ChanceOutput> outputs, boolean armor) {
		public static GrinderDisplay of(GrinderRecipe r) {
			return new GrinderDisplay(List.of(r.input().getItems()), r.results(), false);
		}
	}

	/* ---- metal press ---- */

	public static class MetalPress extends MachineCategory<RecipeHolder<MetalPressRecipe>> {
		public MetalPress(IGuiHelper gui) {
			super(gui, METAL_PRESS, TGBlocks.METAL_PRESS.get(), 100, 30, 100);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 44, 1 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<MetalPressRecipe> holder, IFocusGroup focuses) {
			MetalPressRecipe r = holder.value();
			b.addInputSlot(1, 1).setStandardSlotBackground().addIngredients(r.input1());
			b.addInputSlot(21, 1).setStandardSlotBackground().addIngredients(r.input2());
			b.addOutputSlot(79, 1).setOutputSlotBackground().addItemStack(r.result());
			if (r.allowSwap()) {
				b.setShapeless(88, 22);
			}
		}

		@Override
		public void draw(RecipeHolder<MetalPressRecipe> recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(recipe, slots, g, mouseX, mouseY);
			text(g, powerAndTime(MetalPressTileEnt.POWER_PER_TICK, 100), 0, 22);
		}
	}

	/* ---- blast furnace ---- */

	public static class BlastFurnace extends MachineCategory<RecipeHolder<BlastFurnaceRecipe>> {
		public BlastFurnace(IGuiHelper gui) {
			super(gui, BLAST_FURNACE, TGBlocks.BLAST_FURNACE.get(), 100, 30, 200);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 44, 1 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<BlastFurnaceRecipe> holder, IFocusGroup focuses) {
			BlastFurnaceRecipe r = holder.value();
			b.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(List.of(r.input1().getItems()));
			b.addInputSlot(21, 1).setStandardSlotBackground().addItemStacks(List.of(r.input2().getItems()));
			b.addOutputSlot(79, 1).setOutputSlotBackground().addItemStack(r.result());
		}

		@Override
		public void draw(RecipeHolder<BlastFurnaceRecipe> recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(recipe, slots, g, mouseX, mouseY);
			text(g, powerAndTime(recipe.value().powerPerTick(), recipe.value().time()), 0, 22);
		}
	}

	/* ---- charging station ---- */

	public static class ChargingStation extends MachineCategory<RecipeHolder<ChargingStationRecipe>> {
		public ChargingStation(IGuiHelper gui) {
			super(gui, CHARGING_STATION, TGBlocks.CHARGING_STATION.get(), 100, 30, 60);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 24, 1 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<ChargingStationRecipe> holder, IFocusGroup focuses) {
			ChargingStationRecipe r = holder.value();
			b.addInputSlot(1, 1).setStandardSlotBackground().addIngredients(r.input().ingredient());
			b.addOutputSlot(59, 1).setOutputSlotBackground().addItemStack(r.result());
		}

		@Override
		public void draw(RecipeHolder<ChargingStationRecipe> recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(recipe, slots, g, mouseX, mouseY);
			int amount = recipe.value().chargeAmount();
			text(g, powerAndTime(ChargingStationTileEnt.CHARGERATE, Math.max(1, amount / ChargingStationTileEnt.CHARGERATE)), 0, 22);
		}
	}

	/* ---- chem lab ---- */

	public static class ChemLab extends MachineCategory<RecipeHolder<ChemLabRecipe>> {
		public ChemLab(IGuiHelper gui) {
			super(gui, CHEM_LAB, TGBlocks.CHEM_LAB.get(), 140, 62, ChemLabRecipe.TIME);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 64, 11 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<ChemLabRecipe> holder, IFocusGroup focuses) {
			ChemLabRecipe r = holder.value();
			var fin = b.addInputSlot(1, 1).setFluidRenderer(1000, false, 10, 50);
			r.fluidInput().ifPresent(f -> {
				// tags also hold the flowing variants, those render empty and make the slot blink while cycling
				java.util.Arrays.stream(f.getFluids()).map(fs -> fs.getFluid()).filter(fl -> fl.isSource(fl.defaultFluidState())).distinct()
						.forEach(fl -> fin.addFluidStack(fl, f.amount()));
			});
			// always all three item slots, so recipe transfer lines up with the menu slots
			var in1 = b.addInputSlot(19, 1).setStandardSlotBackground();
			var in2 = b.addInputSlot(41, 1).setStandardSlotBackground();
			var bottle = b.addInputSlot(19, 24).setStandardSlotBackground();
			r.input1().ifPresent(i -> in1.addItemStacks(List.of(i.getItems())));
			r.input2().ifPresent(i -> in2.addItemStacks(List.of(i.getItems())));
			r.bottle().ifPresent(i -> bottle.addItemStacks(List.of(i.getItems())));
			if (!r.result().isEmpty()) {
				b.addOutputSlot(97, 11).setOutputSlotBackground().addItemStack(r.result());
			}
			if (!r.fluidResult().isEmpty()) {
				b.addOutputSlot(127, 1).setFluidRenderer(1000, false, 10, 50).addFluidStack(r.fluidResult().getFluid(), r.fluidResult().getAmount());
			}
			if (r.allowSwap()) {
				b.setShapeless(64, 32);
			}
		}

		@Override
		public void draw(RecipeHolder<ChemLabRecipe> recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(recipe, slots, g, mouseX, mouseY);
			text(g, powerAndTime(recipe.value().powerPerTick(), ChemLabRecipe.TIME), 0, 54);
		}
	}

	/* ---- fabricator ---- */

	public static class Fabricator extends MachineCategory<RecipeHolder<techguns.recipes.FabricatorRecipe>> {
		public Fabricator(IGuiHelper gui) {
			super(gui, FABRICATOR, TGBlocks.FABRICATOR_CONTROLLER.get(), 132, 30, techguns.recipes.FabricatorRecipe.TIME);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 78, 1 };
		}

		private static void slot(IRecipeLayoutBuilder b, int x, java.util.Optional<net.neoforged.neoforge.common.crafting.SizedIngredient> ing) {
			var s = b.addInputSlot(x, 1).setStandardSlotBackground();
			ing.ifPresent(i -> s.addItemStacks(java.util.Arrays.asList(i.getItems())));
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<techguns.recipes.FabricatorRecipe> holder, IFocusGroup focuses) {
			var r = holder.value();
			slot(b, 1, java.util.Optional.of(r.input()));
			slot(b, 20, r.wires());
			slot(b, 39, r.powder());
			slot(b, 58, r.plate());
			b.addOutputSlot(113, 1).setOutputSlotBackground().addItemStack(r.result());
		}

		@Override
		public void draw(RecipeHolder<techguns.recipes.FabricatorRecipe> recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(recipe, slots, g, mouseX, mouseY);
			text(g, powerAndTime(techguns.tileentities.FabricatorTileEntMaster.POWER_PER_TICK, techguns.recipes.FabricatorRecipe.TIME), 0, 22);
		}
	}

	/* ---- reaction chamber ---- */

	public static class ReactionChamber extends MachineCategory<RecipeHolder<techguns.recipes.ReactionChamberRecipe>> {
		public ReactionChamber(IGuiHelper gui) {
			super(gui, REACTION_CHAMBER, TGBlocks.REACTIONCHAMBER_CONTROLLER.get(), 160, 92, techguns.tileentities.operation.ReactionChamberOperation.RECIPE_TICKRATE);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 62, 1 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<techguns.recipes.ReactionChamberRecipe> holder, IFocusGroup focuses) {
			var r = holder.value();
			// item slots first, in menu order, for recipe transfer
			b.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(List.of(r.input().getItems()));
			b.addInputSlot(20, 1).setStandardSlotBackground().addItemStack(new ItemStack(r.focus()));
			var fin = b.addInputSlot(40, 1).setStandardSlotBackground().setFluidRenderer(r.getFluidAmount(), false, 16, 16);
			java.util.Arrays.stream(r.fluid().getStacks()).map(fs -> fs.getFluid()).filter(fl -> fl.isSource(fl.defaultFluidState())).distinct()
					.forEach(fl -> fin.addFluidStack(fl, r.getFluidAmount()));
			for (int i = 0; i < r.results().size() && i < 4; i++) {
				b.addOutputSlot(90 + i * 18, 1).setOutputSlotBackground().addItemStack(r.results().get(i));
			}
		}

		private static net.minecraft.network.chat.MutableComponent line(String key, Object value) {
			return Component.translatable("techguns.container.reactionchamber." + key).append(": " + value);
		}

		@Override
		public void draw(RecipeHolder<techguns.recipes.ReactionChamberRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(holder, slots, g, mouseX, mouseY);
			var r = holder.value();
			int rt = techguns.tileentities.operation.ReactionChamberOperation.RECIPE_TICKRATE;
			int y = 22;
			text(g, line("intensity", r.intensity()), 0, y);
			text(g, r.isStable() ? Component.translatable("techguns.container.reactionchamber.stable")
					: line("intensitymargin", r.intensityMargin()).append(", ").append(line("chance", Math.round(r.instability() * 100) + "%")), 0, y += 10);
			text(g, line("completion", r.requiredCompletion() + "/" + r.ticks() + " ")
					.append(Component.translatable("techguns.container.reactionchamber.recipeticks")), 0, y += 10);
			text(g, line("time", r.ticks() * rt / 20 + "s"), 0, y += 10);
			text(g, line("consumption", r.liquidConsumption() + "/" + r.getFluidAmount() + "mB"), 0, y += 10);
			text(g, Component.translatable("techguns.container.reactionchamber.risk").append(": ").append(r.risk().getDisplayName()), 0, y += 10);
			text(g, Component.literal(r.power() + " FE / ").append(Component.translatable("techguns.container.reactionchamber.recipetick")), 0, y += 10);
		}
	}

	/* ---- ore drill ---- */

	/** what an ore cluster gives (1.12 OreDrillJeiRecipe) */
	public record OreDrillRecipe(techguns.blocks.OreClusterType type, techguns.TGOreClusters.Entry entry) {
		public static List<OreDrillRecipe> all() {
			List<OreDrillRecipe> list = new java.util.ArrayList<>();
			for (techguns.blocks.OreClusterType t : techguns.blocks.OreClusterType.values()) {
				techguns.TGOreClusters.getEntries(t).forEach(e -> list.add(new OreDrillRecipe(t, e)));
			}
			return list;
		}
	}

	public static class OreDrill extends MachineCategory<OreDrillRecipe> {
		public OreDrill(IGuiHelper gui) {
			super(gui, ORE_DRILL, TGBlocks.OREDRILL_CONTROLLER.get(), 140, 52, 100);
		}

		@Override
		public Component getTitle() {
			return Component.translatable("techguns.container.oredrill");
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 26, 1 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, OreDrillRecipe r, IFocusGroup focuses) {
			b.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(new ItemStack(TGBlocks.ORE_CLUSTERS.get(r.type()).get()));
			var out = b.addOutputSlot(60, 1).setOutputSlotBackground();
			var fluid = r.entry().getFluid();
			if (!fluid.isEmpty()) {
				out.setFluidRenderer(1000, false, 16, 16).addFluidStack(fluid.getFluid(), fluid.getAmount());
			} else {
				out.addItemStack(r.entry().getItem());
			}
		}

		@Override
		public void draw(OreDrillRecipe r, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(r, slots, g, mouseX, mouseY);
			var t = r.type();
			text(g, Component.translatable("techguns.oredrill.jei.chance").append(String.format(": %.2f%%", techguns.TGOreClusters.getChance(t, r.entry()) * 100f)), 82, 5);
			text(g, Component.translatable("techguns.orecluster.mininglevel").append(": " + t.getMiningLevel()), 0, 22);
			double orePerHour = 4.5d * t.getAmountMultiplier() * techguns.TGConfig.oreDrillMultiplierOres;
			int powerTick = (int) (8 * orePerHour * t.getPowerMultiplier() * techguns.TGConfig.oreDrillMultiplierPower);
			text(g, Component.translatable("techguns.oredrill.powerperore").append(": "), 0, 32);
			text(g, Component.literal((int) (powerTick * 20 * 60 * 60 / orePerHour) + " FE"), 0, 42);
		}
	}

	/* ---- camo bench ---- */

	public static class CamoBench extends MachineCategory<RecipeHolder<CamoBenchRecipe>> {
		public CamoBench(IGuiHelper gui) {
			super(gui, CAMO_BENCH, TGBlocks.CAMO_BENCH.get(), 144, 48, 100);
		}

		@Override
		protected int[] arrowPos() {
			return null;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<CamoBenchRecipe> holder, IFocusGroup focuses) {
			var items = holder.value().items();
			for (int i = 0; i < items.size() && i < 16; i++) {
				b.addInputSlot(1 + (i % 8) * 18, 1 + (i / 8) * 18).setStandardSlotBackground().addItemStack(new ItemStack(items.get(i)));
			}
		}

		@Override
		public void draw(RecipeHolder<CamoBenchRecipe> recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			text(g, Component.translatable("techguns.jei.camo_bench"), 0, 39);
		}
	}

	/* ---- upgrade bench ---- */

	private static List<ItemStack> techgunsArmor;

	private static List<ItemStack> techgunsArmor() {
		if (techgunsArmor == null) {
			techgunsArmor = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(i -> i instanceof techguns.items.armors.GenericArmor)
					.map(ItemStack::new).toList();
		}
		return techgunsArmor;
	}

	public static class UpgradeBench extends MachineCategory<RecipeHolder<UpgradeBenchRecipe>> {
		public UpgradeBench(IGuiHelper gui) {
			super(gui, UPGRADE_BENCH, TGBlocks.UPGRADE_BENCH.get(), 110, 30, 100);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 46, 1 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<UpgradeBenchRecipe> holder, IFocusGroup focuses) {
			UpgradeBenchRecipe r = holder.value();
			List<ItemStack> armor = techgunsArmor().stream().filter(r::isValidArmor).toList();
			b.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(armor);
			b.addInputSlot(23, 1).setStandardSlotBackground().addIngredients(r.upgrade());
			b.addOutputSlot(83, 1).setOutputSlotBackground().addItemStacks(armor.stream().map(r::apply).toList());
		}

		@Override
		public void draw(RecipeHolder<UpgradeBenchRecipe> recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(recipe, slots, g, mouseX, mouseY);
			text(g, Component.translatable("techguns.jei.upgrade_bench.xp", techguns.TGConfig.upgrade_xp_cost), 0, 22);
		}
	}

	/* ---- ammo press ---- */

	public static class AmmoPress extends MachineCategory<AmmoPressDisplay> {
		public AmmoPress(IGuiHelper gui) {
			super(gui, AMMO_PRESS, TGBlocks.AMMO_PRESS.get(), 110, 30, 100);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 62, 1 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, AmmoPressDisplay recipe, IFocusGroup focuses) {
			b.addInputSlot(1, 1).setStandardSlotBackground().addIngredients(Ingredient.of(AmmoPressBuildPlans.METAL1));
			b.addInputSlot(21, 1).setStandardSlotBackground()
					.addItemStacks(List.of(Ingredient.of(AmmoPressBuildPlans.METAL2).getItems()).stream().map(s -> s.copyWithCount(2)).toList());
			b.addInputSlot(41, 1).setStandardSlotBackground().addIngredients(Ingredient.of(AmmoPressBuildPlans.POWDER));
			b.addOutputSlot(93, 1).setOutputSlotBackground().addItemStack(AmmoPressBuildPlans.getResult(recipe.plan()));
		}

		@Override
		public void draw(AmmoPressDisplay recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(recipe, slots, g, mouseX, mouseY);
			text(g, powerAndTime(AmmoPressTileEnt.POWER_PER_TICK, 100), 0, 22);
		}
	}

	/* ---- grinder ---- */

	public static class Grinder extends MachineCategory<GrinderDisplay> {
		public Grinder(IGuiHelper gui) {
			super(gui, GRINDER, TGBlocks.GRINDER.get(), 116, 76, GrinderTileEnt.TIME);
		}

		@Override
		protected int[] arrowPos() {
			return new int[] { 24, 19 };
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder b, GrinderDisplay recipe, IFocusGroup focuses) {
			b.addInputSlot(1, 19).setStandardSlotBackground().addItemStacks(recipe.input());
			for (int i = 0; i < 9; i++) {
				var s = b.addOutputSlot(61 + (i % 3) * 18, 1 + (i / 3) * 18).setStandardSlotBackground();
				if (i >= recipe.outputs().size()) {
					continue;
				}
				GrinderRecipe.ChanceOutput o = recipe.outputs().get(i);
				s.addItemStack(o.item());
				if (o.chance() < 1f) {
					String pct = String.format("%.1f", o.chance() * 100f);
					s.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("techguns.jei.chance", pct)));
				}
			}
		}

		@Override
		public void draw(GrinderDisplay recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
			super.draw(recipe, slots, g, mouseX, mouseY);
			text(g, powerAndTime(GrinderTileEnt.POWER_PER_TICK, GrinderTileEnt.TIME), 0, 57);
			if (recipe.armor()) {
				text(g, Component.translatable("techguns.jei.grinder.armor"), 0, 67);
			}
		}
	}
}
