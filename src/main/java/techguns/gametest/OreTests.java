package techguns.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGBlocks;
import techguns.TGItems;
import techguns.Techguns;
import techguns.recipes.TGRecipes;
import techguns.world.gen.ConfigOrePlacement;

/** The deepslate ore variants behave like the stone ores: tags, tool level, drops, recipes and worldgen. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class OreTests {

	private static Map<String, Block[]> ores() {
		return Map.of(
				"tin", new Block[] { TGBlocks.ORE_TIN.get(), TGBlocks.DEEPSLATE_ORE_TIN.get() },
				"lead", new Block[] { TGBlocks.ORE_LEAD.get(), TGBlocks.DEEPSLATE_ORE_LEAD.get() },
				"titanium", new Block[] { TGBlocks.ORE_TITANIUM.get(), TGBlocks.DEEPSLATE_ORE_TITANIUM.get() },
				"uranium", new Block[] { TGBlocks.ORE_URANIUM.get(), TGBlocks.DEEPSLATE_ORE_URANIUM.get() });
	}

	@GameTest(template = "empty")
	public static void deepslateOresMatchStoneOres(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> problems = new ArrayList<>();
		List<TagKey<Block>> toolTags = List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL, BlockTags.NEEDS_IRON_TOOL,
				BlockTags.NEEDS_DIAMOND_TOOL);
		for (var e : ores().entrySet()) {
			String metal = e.getKey();
			Block stone = e.getValue()[0], deep = e.getValue()[1];
			BlockState ds = deep.defaultBlockState();
			TagKey<Block> oreTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/" + metal));
			if (!ds.is(oreTag) || !ds.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores_in_ground/deepslate")))) {
				problems.add(metal + ": deepslate ore tags");
			}
			if (!new ItemStack(deep).is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/" + metal)))) {
				problems.add(metal + ": deepslate ore item tag");
			}
			for (TagKey<Block> t : toolTags) {
				if (stone.defaultBlockState().is(t) != ds.is(t)) {
					problems.add(metal + ": tool tag " + t.location() + " differs");
				}
			}
			if (ds.getLightEmission() != stone.defaultBlockState().getLightEmission()) {
				problems.add(metal + ": light");
			}
			// drops itself
			BlockPos pos = helper.absolutePos(BlockPos.ZERO);
			level.setBlock(pos, ds, 3);
			List<ItemStack> drops = Block.getDrops(ds, level, pos, null, null, new ItemStack(Items.DIAMOND_PICKAXE));
			level.removeBlock(pos, false);
			if (drops.size() != 1 || !drops.get(0).is(deep.asItem())) {
				problems.add(metal + ": drops " + drops);
			}
			// the ore feature places the deepslate variant into deepslate
			var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
					.get(ResourceKey.create(Registries.CONFIGURED_FEATURE, Techguns.rl("ore_" + metal)));
			if (!(feature instanceof ConfiguredFeature<?, ?> cf && cf.config() instanceof OreConfiguration oc
					&& oc.targetStates.stream().anyMatch(t -> t.state.is(deep) && t.target.test(Blocks.DEEPSLATE.defaultBlockState(), level.getRandom()))
					&& oc.targetStates.stream().anyMatch(t -> t.state.is(stone) && t.target.test(Blocks.STONE.defaultBlockState(), level.getRandom())))) {
				problems.add(metal + ": ore feature targets");
			}
		}
		// smelting and the reaction chamber take both variants
		var rm = level.getRecipeManager();
		for (var ore : List.of(TGBlocks.DEEPSLATE_ORE_TIN.get(), TGBlocks.DEEPSLATE_ORE_LEAD.get())) {
			for (var type : List.of(RecipeType.SMELTING, RecipeType.BLASTING)) {
				if (rm.getRecipeFor(type, new SingleRecipeInput(new ItemStack(ore)), level).isEmpty()) {
					problems.add(BuiltInRegistries.BLOCK.getKey(ore) + " has no " + type);
				}
			}
		}
		boolean rcTakesDeepslate = rm.getAllRecipesFor(TGRecipes.REACTION_CHAMBER.get()).stream()
				.anyMatch(r -> r.value().input().test(new ItemStack(TGBlocks.DEEPSLATE_ORE_TITANIUM.get()))
						&& r.value().results().stream().anyMatch(s -> s.is(TGItems.ORE_TITANIUM.get())));
		if (!rcTakesDeepslate) {
			problems.add("reaction chamber does not turn deepslate titanium ore into raw titanium");
		}
		if (!new ItemStack(TGItems.ORE_TITANIUM.get()).is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials/titanium")))) {
			problems.add("raw titanium is not c:raw_materials/titanium");
		}
		// copper is vanilla's: no Techguns copper ingot or ore
		if (BuiltInRegistries.ITEM.containsKey(Techguns.rl("ingotcopper")) || BuiltInRegistries.BLOCK.containsKey(Techguns.rl("ore_copper"))) {
			problems.add("Techguns copper ingot/ore registered");
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** Each ore's placed feature checks its worldgen.doOreGen* option, and placement stops while it is off. */
	@GameTest(template = "empty")
	public static void oreGenConfigIsApplied(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> problems = new ArrayList<>();
		var placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
		for (ConfigOrePlacement.Ore ore : ConfigOrePlacement.Ore.values()) {
			PlacedFeature pf = placed.get(Techguns.rl("ore_" + ore.getSerializedName()));
			if (pf == null) {
				problems.add(ore + ": no placed feature");
				continue;
			}
			var check = pf.placement().stream().filter(m -> m instanceof ConfigOrePlacement c && c.getOre() == ore).findFirst();
			if (check.isEmpty()) {
				problems.add(ore + ": placed feature has no config check");
				continue;
			}
			BlockPos pos = helper.absolutePos(BlockPos.ZERO);
			boolean saved = getFlag(ore);
			try {
				setFlag(ore, true);
				long on = check.get().getPositions(null, level.getRandom(), pos).count();
				setFlag(ore, false);
				long off = check.get().getPositions(null, level.getRandom(), pos).count();
				if (on != 1 || off != 0) {
					problems.add(ore + ": positions on=" + on + " off=" + off);
				}
			} finally {
				setFlag(ore, saved);
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	private static boolean getFlag(ConfigOrePlacement.Ore ore) {
		return switch (ore) {
			case TIN -> techguns.TGConfig.doOreGenTin;
			case LEAD -> techguns.TGConfig.doOreGenLead;
			case TITANIUM -> techguns.TGConfig.doOreGenTitanium;
			case URANIUM -> techguns.TGConfig.doOreGenUranium;
		};
	}

	private static void setFlag(ConfigOrePlacement.Ore ore, boolean value) {
		switch (ore) {
			case TIN -> techguns.TGConfig.doOreGenTin = value;
			case LEAD -> techguns.TGConfig.doOreGenLead = value;
			case TITANIUM -> techguns.TGConfig.doOreGenTitanium = value;
			case URANIUM -> techguns.TGConfig.doOreGenUranium = value;
		}
	}
}
