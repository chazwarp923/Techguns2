package techguns.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import techguns.Techguns;

/**
 * The compat recipes (data/techguns/recipe/compat/) run in the other mods' real machines: furnaces for Create's
 * crushed ores, Create's press, mixer and fan washing, Mekanism's combiner, and Immersive Engineering's own recipe
 * lookups (its machines are multiblocks). Each test does nothing without its mod, and skips a recipe whose conditions
 * left it out (DataTests.compatRecipes checks that gating). Expected results are written out here, not read from the
 * recipe, so a wrong result in a recipe file fails too. Only the other mods' block ids and NeoForge capabilities are
 * used, plus a little reflection (and Mekanism's API, through MekanismMachineChecks), so the mods aren't needed to
 * compile.
 */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class CompatMachineTests {

	/** Create's crushed tin and lead in a furnace and a blast furnace */
	@GameTest(template = "empty_large", timeoutTicks = 400)
	public static void createCrushedOreFurnaces(GameTestHelper helper) {
		if (!loaded("create")) {
			helper.succeed();
			return;
		}
		List<Runnable> checks = new ArrayList<>();
		int x = 0;
		for (String metal : List.of("tin", "lead")) {
			for (String kind : List.of("smelting", "blasting")) {
				String recipe = "create/" + kind + "/ingot_" + metal + "_from_crushed";
				if (!recipeLoaded(helper, recipe)) {
					continue;
				}
				BlockPos pos = new BlockPos(x, 1, 1);
				x += 2;
				helper.setBlock(pos, kind.equals("smelting") ? Blocks.FURNACE : Blocks.BLAST_FURNACE);
				var furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(pos);
				furnace.setItem(0, stack(helper, "create:crushed_raw_" + metal, 1));
				furnace.setItem(1, new ItemStack(Items.LAVA_BUCKET));
				Item result = item(helper, "techguns:ingot" + metal);
				checks.add(() -> expect(helper, recipe, furnace.getItem(2), result, 1));
			}
		}
		succeedWhenAll(helper, checks);
	}

	/** Create's mechanical press over a depot, one per compat pressing recipe, all driven by one creative motor */
	@GameTest(template = "empty_large", timeoutTicks = 400)
	public static void createPressing(GameTestHelper helper) {
		if (!loaded("create")) {
			helper.succeed();
			return;
		}
		String[][] cases = { { "bronze", "bronze" }, { "lead", "lead" }, { "obsidian_steel", "obsidiansteel" },
				{ "steel", "steel" }, { "tin", "tin" }, { "titanium", "titanium" } };
		motor(helper, new BlockPos(0, 3, 1), Direction.EAST);
		List<Runnable> checks = new ArrayList<>();
		for (int i = 0; i < cases.length; i++) {
			String recipe = "create/pressing/plate_" + cases[i][0];
			BlockPos press = new BlockPos(1 + i, 3, 1);
			// every press keeps the shaft going to the next one
			helper.setBlock(press, with(block(helper, "create:mechanical_press"), BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
			if (!recipeLoaded(helper, recipe)) {
				continue;
			}
			BlockPos depot = press.below(2);
			helper.setBlock(depot, block(helper, "create:depot"));
			insert(helper, helper.absolutePos(depot), stack(helper, "techguns:ingot" + cases[i][1], 1), recipe);
			Item result = item(helper, "techguns:plate" + cases[i][1]);
			checks.add(() -> expect(helper, recipe, find(helper, depot, result), result, 1));
		}
		succeedWhenAll(helper, checks);
	}

	/**
	 * Create's mechanical mixer over a basin on a lit blaze burner (both recipes need heat). First, no other recipe
	 * the mixer may use ties with a compat mixing recipe: Create sorts matches only by ingredient count, so a tie is
	 * settled by recipe load order (steel + obsidian once made raw obsidian steel from the shapeless crafting recipe,
	 * in one mod setup and not another). Create leaves out recipes whose id ends in "_manual_only".
	 */
	@GameTest(template = "empty_large", timeoutTicks = 400)
	public static void createMixing(GameTestHelper helper) {
		if (!loaded("create")) {
			helper.succeed();
			return;
		}
		List<String> ties = new ArrayList<>();
		var recipes = helper.getLevel().getServer().getRecipeManager().getRecipes();
		for (var mix : recipes) {
			if (!mix.id().getNamespace().equals(Techguns.MODID) || !mix.id().getPath().startsWith("compat/create/mixing/")) {
				continue;
			}
			List<ItemStack> inputs = mix.value().getIngredients().stream().map(CompatMachineTests::sample).toList();
			for (var other : recipes) {
				String type = BuiltInRegistries.RECIPE_TYPE.getKey(other.value().getType()).toString();
				boolean automatable = other.value() instanceof net.minecraft.world.item.crafting.ShapelessRecipe || type.equals("create:mixing");
				if (other != mix && automatable && !other.id().getPath().endsWith("_manual_only")
						&& other.value().getIngredients().size() == inputs.size() && takes(other.value().getIngredients(), inputs, 0, new boolean[inputs.size()])) {
					ties.add(mix.id() + " ties with " + other.id());
				}
			}
		}
		helper.assertTrue(ties.isEmpty(), String.join("; ", ties));
		List<Runnable> checks = new ArrayList<>();
		checks.add(mixer(helper, new BlockPos(1, 1, 1), "create/mixing/bronze_ingot",
				List.of(new ItemStack(Items.COPPER_INGOT, 3), stack(helper, "techguns:ingottin", 1)), "techguns:ingotbronze", 4));
		checks.add(mixer(helper, new BlockPos(4, 1, 4), "create/mixing/obsidian_steel_ingot",
				List.of(stack(helper, "techguns:ingotsteel", 1), new ItemStack(Items.OBSIDIAN)), "techguns:ingotobsidiansteel", 1));
		succeedWhenAll(helper, checks);
	}

	/** the Techguns item an ingredient takes, else its first item */
	private static ItemStack sample(net.minecraft.world.item.crafting.Ingredient ing) {
		var items = ing.getItems();
		for (ItemStack s : items) {
			if (BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(Techguns.MODID)) {
				return s;
			}
		}
		return items.length > 0 ? items[0] : ItemStack.EMPTY;
	}

	/** whether the ingredients from {@code i} on can each take a different one of the unused inputs */
	private static boolean takes(List<net.minecraft.world.item.crafting.Ingredient> ings, List<ItemStack> inputs, int i, boolean[] used) {
		if (i == ings.size()) {
			return true;
		}
		for (int j = 0; j < inputs.size(); j++) {
			if (!used[j] && ings.get(i).test(inputs.get(j))) {
				used[j] = true;
				if (takes(ings, inputs, i + 1, used)) {
					return true;
				}
				used[j] = false;
			}
		}
		return false;
	}

	private static Runnable mixer(GameTestHelper helper, BlockPos burner, String recipe, List<ItemStack> inputs, String result, int count) {
		BlockPos basin = burner.above();
		// the mixer has no shaft: it's a cogwheel, turned by a cog beside it that the motor drives from above
		BlockPos cog = basin.above(2).east();
		motor(helper, cog.above(), Direction.DOWN);
		helper.setBlock(cog, block(helper, "create:cogwheel").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
		// the block's default state is an unlit "none"; placing one by hand gives "smouldering", which fuel then lights
		BlockState burnerState = block(helper, "create:blaze_burner").defaultBlockState();
		var heat = burnerState.getBlock().getStateDefinition().getProperty("blaze");
		helper.setBlock(burner, set(burnerState, heat, "smouldering"));
		helper.setBlock(basin, block(helper, "create:basin"));
		helper.setBlock(basin.above(2), block(helper, "create:mechanical_mixer"));
		// regular fuel lights the burner to "kindled", which is what "heated" recipes need
		BlockPos abs = helper.absolutePos(burner);
		Object lit = invokeStatic(helper, "com.simibubi.create.content.processing.burner.BlazeBurnerBlock", "tryInsert",
				new Class<?>[] { BlockState.class, Level.class, BlockPos.class, ItemStack.class, boolean.class, boolean.class, boolean.class },
				helper.getLevel().getBlockState(abs), helper.getLevel(), abs, new ItemStack(Items.COAL_BLOCK), false, false, false);
		helper.assertTrue(lit instanceof net.minecraft.world.InteractionResultHolder<?> r && r.getResult().consumesAction(),
				recipe + ": the blaze burner didn't take coal (" + lit + ")");
		for (ItemStack in : inputs) {
			insert(helper, helper.absolutePos(basin), in, recipe);
		}
		Item res = item(helper, result);
		return () -> expect(helper, recipe + " (burner " + helper.getBlockState(burner) + ", mixer speed " + speed(helper, basin.above(2)) + ")",
				find(helper, basin, res), res, count);
	}

	/** Create's fan washing: a fan blowing through water onto a depot */
	@GameTest(template = "empty_large", timeoutTicks = 600)
	public static void createWashing(GameTestHelper helper) {
		if (!loaded("create")) {
			helper.succeed();
			return;
		}
		List<Runnable> checks = new ArrayList<>();
		int z = 1;
		for (String[] c : new String[][] { { "lead", "techguns:nuggetlead", "9" }, { "uranium", "techguns:yellowcake", "1" } }) {
			String recipe = "create/splashing/crushed_raw_" + c[0];
			if (!recipeLoaded(helper, recipe)) {
				continue;
			}
			for (int x = 0; x < 5; x++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
			motor(helper, new BlockPos(0, 1, z), Direction.EAST);
			helper.setBlock(new BlockPos(1, 1, z), with(block(helper, "create:encased_fan"), BlockStateProperties.FACING, Direction.EAST));
			helper.setBlock(new BlockPos(2, 1, z - 1), Blocks.STONE);
			helper.setBlock(new BlockPos(2, 1, z + 1), Blocks.STONE);
			helper.setBlock(new BlockPos(2, 1, z), Blocks.WATER);
			BlockPos depot = new BlockPos(3, 1, z);
			helper.setBlock(depot, block(helper, "create:depot"));
			insert(helper, helper.absolutePos(depot), stack(helper, "create:crushed_raw_" + c[0], 1), recipe);
			Item res = item(helper, c[1]);
			int count = Integer.parseInt(c[2]);
			checks.add(() -> expect(helper, recipe, find(helper, depot, res), res, count));
			z += 3;
		}
		succeedWhenAll(helper, checks);
	}

	/** Mekanism's combiner, its slots and energy buffer filled directly */
	@GameTest(template = "empty_large", timeoutTicks = 600)
	public static void mekanismCombining(GameTestHelper helper) {
		String recipe = "mekanism/combining/obsidian_steel_ingot";
		if (!loaded("mekanism") || !recipeLoaded(helper, recipe)) {
			helper.succeed();
			return;
		}
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, block(helper, "mekanism:combiner"));
		for (ItemStack in : List.of(stack(helper, "techguns:ingotsteel", 1), new ItemStack(Items.OBSIDIAN))) {
			helper.assertTrue(MekanismMachineChecks.insert(helper.getBlockEntity(pos), in).isEmpty(), recipe + ": the combiner didn't take " + in);
		}
		helper.onEachTick(() -> helper.assertTrue(MekanismMachineChecks.fillEnergy(helper.getBlockEntity(pos)) > 0, recipe + ": the combiner has no energy buffer"));
		Item res = item(helper, "techguns:ingotobsidiansteel");
		succeedWhenAll(helper, List.of(() -> expect(helper, recipe, find(helper, pos, res), res, 1)));
	}

	/**
	 * Immersive Engineering: the metal press, alloy smelter and arc furnace are multiblocks, so this asks IE's own
	 * recipe lookups, the ones those machines call, what they make from Techguns' ingots.
	 */
	@GameTest(template = "empty")
	public static void immersiveEngineeringRecipes(GameTestHelper helper) {
		if (!loaded("immersiveengineering")) {
			helper.succeed();
			return;
		}
		String api = "blusunrize.immersiveengineering.api.crafting.";
		Level level = helper.getLevel();
		ItemStack mold = stack(helper, "immersiveengineering:mold_plate", 1);
		ItemStack steel = stack(helper, "techguns:ingotsteel", 1);
		Item obsidianSteel = item(helper, "techguns:ingotobsidiansteel");
		List<String> problems = new ArrayList<>();

		for (String metal : List.of("obsidian_steel", "titanium")) {
			String name = metal.replace("_", "");
			Object holder = invokeStatic(helper, api + "MetalPressRecipe", "findRecipe",
					new Class<?>[] { ItemStack.class, ItemStack.class, Level.class }, mold, stack(helper, "techguns:ingot" + name, 1), level);
			check(helper, problems, "immersiveengineering/metalpress/plate_" + metal, holder, item(helper, "techguns:plate" + name));
		}
		Object alloy = invokeStatic(helper, api + "AlloyRecipe", "findRecipe",
				new Class<?>[] { Level.class, ItemStack.class, ItemStack.class, classOf(helper, api + "AlloyRecipe") },
				level, steel, new ItemStack(Items.OBSIDIAN), null);
		check(helper, problems, "immersiveengineering/alloysmelter/obsidian_steel", alloy, obsidianSteel);
		// the smelter takes its two inputs either way round
		Object alloySwapped = invokeStatic(helper, api + "AlloyRecipe", "findRecipe",
				new Class<?>[] { Level.class, ItemStack.class, ItemStack.class, classOf(helper, api + "AlloyRecipe") },
				level, new ItemStack(Items.OBSIDIAN), steel, null);
		check(helper, problems, "immersiveengineering/alloysmelter/obsidian_steel (inputs swapped)", alloySwapped, obsidianSteel);
		Object arc = invokeStatic(helper, api + "ArcFurnaceRecipe", "findRecipe",
				new Class<?>[] { Level.class, ItemStack.class, NonNullList.class },
				level, steel, NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.OBSIDIAN)));
		check(helper, problems, "immersiveengineering/arcfurnace/obsidian_steel", arc, obsidianSteel);

		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	private static void check(GameTestHelper helper, List<String> problems, String recipe, Object found, Item expected) {
		if (found instanceof RecipeHolder<?> h) {
			found = h.value();
		}
		if (found == null) {
			problems.add(recipe + ": IE found no recipe");
			return;
		}
		ItemStack out = ((Recipe<?>) found).getResultItem(helper.getLevel().registryAccess());
		if (!out.is(expected)) {
			problems.add(recipe + ": IE makes " + out + ", not " + expected);
		}
	}

	/* ---------------------------------------------------------------- helpers */

	private static boolean loaded(String mod) {
		return ModList.get().isLoaded(mod);
	}

	private static boolean recipeLoaded(GameTestHelper helper, String path) {
		return helper.getLevel().getServer().getRecipeManager().byKey(Techguns.rl("compat/" + path)).isPresent();
	}

	/** passes once every check passes; at the timeout the failure lists every check that still doesn't */
	private static void succeedWhenAll(GameTestHelper helper, List<Runnable> checks) {
		helper.succeedWhen(() -> {
			List<String> failed = new ArrayList<>();
			for (Runnable c : checks) {
				try {
					c.run();
				} catch (net.minecraft.gametest.framework.GameTestAssertException e) {
					failed.add(e.getMessage());
				}
			}
			helper.assertTrue(failed.isEmpty(), String.join("; ", failed));
		});
	}

	private static void expect(GameTestHelper helper, String recipe, ItemStack got, Item expected, int count) {
		helper.assertTrue(got.is(expected) && got.getCount() >= count,
				recipe + ": expected " + count + " " + BuiltInRegistries.ITEM.getKey(expected) + ", the machine has " + got);
	}

	private static Item item(GameTestHelper helper, String id) {
		var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
		helper.assertTrue(item.isPresent(), "no item " + id);
		return item.get();
	}

	private static ItemStack stack(GameTestHelper helper, String id, int count) {
		return new ItemStack(item(helper, id), count);
	}

	private static Block block(GameTestHelper helper, String id) {
		var block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id));
		helper.assertTrue(block.isPresent(), "no block " + id);
		return block.get();
	}

	private static BlockState with(Block block, net.minecraft.world.level.block.state.properties.Property<Direction> prop, Direction dir) {
		return block.defaultBlockState().setValue(prop, dir);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static BlockState set(BlockState state, net.minecraft.world.level.block.state.properties.Property prop, String value) {
		return state.setValue(prop, (Comparable) prop.getValue(value).orElseThrow());
	}

	private static Object speed(GameTestHelper helper, BlockPos pos) {
		var be = helper.getBlockEntity(pos);
		try {
			return be.getClass().getMethod("getSpeed").invoke(be);
		} catch (ReflectiveOperationException e) {
			return e.toString();
		}
	}

	/** a Create creative motor turning at full speed, its shaft pointing {@code dir} */
	private static void motor(GameTestHelper helper, BlockPos pos, Direction dir) {
		helper.setBlock(pos, with(block(helper, "create:creative_motor"), BlockStateProperties.FACING, dir));
		var be = helper.getBlockEntity(pos);
		try {
			Object speed = be.getClass().getField("generatedSpeed").get(be);
			speed.getClass().getMethod("setValue", int.class).invoke(speed, 256);
		} catch (ReflectiveOperationException e) {
			helper.fail("couldn't set the creative motor's speed: " + e);
		}
	}

	/** the block's item handlers: unsided first, then each side (machines like Mekanism's give each side its slots) */
	private static List<IItemHandler> items(GameTestHelper helper, BlockPos abs) {
		List<IItemHandler> all = new ArrayList<>();
		for (int i = -1; i < 6; i++) {
			var h = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, i < 0 ? null : Direction.from3DDataValue(i));
			if (h != null) {
				all.add(h);
			}
		}
		return all;
	}

	/** puts the whole stack into whichever slot of the block takes it, from any side */
	private static void insert(GameTestHelper helper, BlockPos abs, ItemStack stack, String what) {
		var handlers = items(helper, abs);
		helper.assertTrue(!handlers.isEmpty(), what + ": the block has no item handler");
		ItemStack rest = stack.copy();
		for (IItemHandler h : handlers) {
			for (int i = 0; i < h.getSlots() && !rest.isEmpty(); i++) {
				rest = h.insertItem(i, rest, false);
			}
		}
		if (!rest.isEmpty()) {
			List<String> slots = new ArrayList<>();
			for (IItemHandler h : handlers) {
				for (int i = 0; i < h.getSlots(); i++) {
					slots.add(i + ": " + h.getStackInSlot(i) + (h.isItemValid(i, stack) ? ", valid" : ""));
				}
			}
			helper.fail(what + ": the machine didn't take " + stack + " (handlers: " + slots + ")");
		}
	}

	/** the biggest stack of {@code item} in the block's inventory, or else what it holds */
	private static ItemStack find(GameTestHelper helper, BlockPos rel, Item item) {
		ItemStack found = ItemStack.EMPTY;
		List<ItemStack> held = new ArrayList<>();
		for (IItemHandler h : items(helper, helper.absolutePos(rel))) {
			for (int i = 0; i < h.getSlots(); i++) {
				ItemStack s = h.getStackInSlot(i);
				if (s.is(item) && s.getCount() > found.getCount()) {
					found = s;
				} else if (!s.isEmpty() && held.stream().noneMatch(x -> ItemStack.isSameItemSameComponents(x, s))) {
					held.add(s);
				}
			}
		}
		return !found.isEmpty() || held.isEmpty() ? found : held.get(0);
	}

	private static Class<?> classOf(GameTestHelper helper, String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			helper.fail("no class " + name);
			return null;
		}
	}

	private static Object invokeStatic(GameTestHelper helper, String cls, String method, Class<?>[] types, Object... args) {
		try {
			return classOf(helper, cls).getMethod(method, types).invoke(null, args);
		} catch (ReflectiveOperationException e) {
			helper.fail(cls + "." + method + " failed: " + (e.getCause() != null ? e.getCause() : e));
			return null;
		}
	}
}
