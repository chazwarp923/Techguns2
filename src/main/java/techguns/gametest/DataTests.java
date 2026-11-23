package techguns.gametest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGuns;
import techguns.Techguns;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GunStat;
import techguns.items.guns.GunStats;
import techguns.recipes.TGRecipes;

/** Game tests for the mod's data: recipes, loot tables and translations load and are complete. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class DataTests {

	/** every machine recipe type has recipes, and every recipe has a result */
	@GameTest(template = "empty")
	public static void machineRecipesLoad(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		Map<RecipeType<?>, Integer> counts = new HashMap<>();
		List<String> problems = new ArrayList<>();
		for (RecipeHolder<?> r : server.getRecipeManager().getRecipes()) {
			counts.merge(r.value().getType(), 1, Integer::sum);
			if (r.id().getNamespace().equals(Techguns.MODID) && r.value().getResultItem(server.registryAccess()).isEmpty()
					&& !r.value().isSpecial()) {
				problems.add(r.id() + " has no result");
			}
		}
		for (var type : TGRecipes.TYPES.getEntries()) {
			if (counts.getOrDefault(type.get(), 0) == 0) {
				problems.add("no recipes of type " + type.getId());
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** every techguns loot table loads, and the ones the structures use exist */
	@GameTest(template = "empty")
	public static void lootTablesLoad(GameTestHelper helper) {
		var registries = helper.getLevel().getServer().reloadableRegistries();
		List<String> problems = new ArrayList<>();
		int n = 0;
		for (ResourceLocation id : registries.getKeys(Registries.LOOT_TABLE)) {
			if (!id.getNamespace().equals(Techguns.MODID)) {
				continue;
			}
			n++;
			if (registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id)) == LootTable.EMPTY) {
				problems.add(id + " did not load");
			}
		}
		for (String used : new String[] { "chests/castle", "chests/militarybase_barracks", "chests/militarybase_bunker" }) {
			if (!registries.getKeys(Registries.LOOT_TABLE).contains(Techguns.rl(used))) {
				problems.add("missing loot table " + used);
			}
		}
		helper.assertTrue(n > 0, "no techguns loot tables");
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** every item and block of the mod has an English name */
	@GameTest(template = "empty")
	public static void everythingIsTranslated(GameTestHelper helper) throws Exception {
		JsonObject lang;
		try (InputStream in = DataTests.class.getResourceAsStream("/assets/techguns/lang/en_us.json")) {
			helper.assertTrue(in != null, "no en_us.json");
			lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
		List<String> missing = new ArrayList<>();
		for (var item : BuiltInRegistries.ITEM) {
			if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Techguns.MODID) && !lang.has(item.getDescriptionId())) {
				missing.add(item.getDescriptionId());
			}
		}
		for (var block : BuiltInRegistries.BLOCK) {
			if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Techguns.MODID) && !lang.has(block.getDescriptionId())) {
				missing.add(block.getDescriptionId());
			}
		}
		helper.assertTrue(missing.isEmpty(), missing.size() + " untranslated: " + String.join(", ", missing.subList(0, Math.min(20, missing.size()))));
		helper.succeed();
	}

	/**
	 * Every progression advancement file loaded (a broken one is skipped with only a log line), leads back to the root,
	 * and has its title and description in every language. Item and entity ids are checked by hand: 1.21.1 loads a
	 * criterion with an unknown item id without any error, and the advancement can then never be earned.
	 */
	@GameTest(template = "empty")
	public static void advancementsLoad(GameTestHelper helper) throws Exception {
		MinecraftServer server = helper.getLevel().getServer();
		var files = net.neoforged.fml.ModList.get().getModFileById(Techguns.MODID).getFile().findResource("data", "techguns", "advancement", "main");
		List<String> ids = new ArrayList<>();
		try (var list = java.nio.file.Files.list(files)) {
			list.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".json")).forEach(n -> ids.add(n.substring(0, n.length() - 5)));
		}
		helper.assertTrue(ids.size() >= 20, "only " + ids.size() + " advancement files");
		Map<String, JsonObject> langs = new HashMap<>();
		for (String l : List.of("en_us", "de_de", "ru_ru")) {
			try (InputStream in = DataTests.class.getResourceAsStream("/assets/techguns/lang/" + l + ".json")) {
				langs.put(l, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			}
		}
		List<String> problems = new ArrayList<>();
		ResourceLocation root = Techguns.rl("main/root");
		for (String id : ids) {
			try (var r = java.nio.file.Files.newBufferedReader(files.resolve(id + ".json"))) {
				checkIds(JsonParser.parseReader(r), null, id, problems);
			}
			var holder = server.getAdvancements().get(Techguns.rl("main/" + id));
			if (holder == null) {
				problems.add(id + " did not load");
				continue;
			}
			var adv = holder.value();
			// parent chain reaches the root
			ResourceLocation at = holder.id();
			for (int depth = 0; !at.equals(root); depth++) {
				var cur = server.getAdvancements().get(at);
				if (cur == null || cur.value().parent().isEmpty() || depth > 30) {
					problems.add(id + " doesn't lead to the root (stuck at " + at + ")");
					break;
				}
				at = cur.value().parent().get();
			}
			if (adv.display().isEmpty()) {
				problems.add(id + " has no display");
				continue;
			}
			for (var c : List.of(adv.display().get().getTitle(), adv.display().get().getDescription())) {
				if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) {
					for (var e : langs.entrySet()) {
						if (!e.getValue().has(tc.getKey())) {
							problems.add(e.getKey() + " misses " + tc.getKey());
						}
					}
				} else {
					problems.add(id + " has an untranslated title or description");
				}
			}
		}
		// the tab only shows once its root is earned, so the root is granted on joining
		var rootAdv = server.getAdvancements().get(root);
		if (rootAdv == null || !rootAdv.value().criteria().values().stream()
				.allMatch(c -> c.trigger() == net.minecraft.advancements.CriteriaTriggers.TICK)) {
			problems.add("root is not granted on joining (minecraft:tick), so the tab stays hidden");
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** item ids under "items" and icon "id", entity ids under "entity": "type" must be registered */
	private static void checkIds(com.google.gson.JsonElement e, String key, String file, List<String> problems) {
		if (e.isJsonObject()) {
			for (var en : e.getAsJsonObject().entrySet()) {
				if (en.getKey().equals("type") && "entity".equals(key) && en.getValue().isJsonPrimitive()) {
					ResourceLocation rl = ResourceLocation.parse(en.getValue().getAsString());
					if (!BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
						problems.add(file + ": unknown entity " + rl);
					}
				} else {
					checkIds(en.getValue(), en.getKey(), file, problems);
				}
			}
		} else if (e.isJsonArray()) {
			e.getAsJsonArray().forEach(x -> checkIds(x, key, file, problems));
		} else if (e.isJsonPrimitive() && ("items".equals(key) || "id".equals(key))) {
			String v = e.getAsString();
			if (!v.startsWith("#") && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(v))) {
				problems.add(file + ": unknown item " + v);
			}
		}
	}

	/**
	 * Compat recipes (data/techguns/recipe/compat/&lt;modid&gt;/): each is gated on its mod, and loads exactly when its
	 * conditions say so. Without the mods this checks the gating; run with -Pcompat_mods=true (Create, Mekanism and
	 * Immersive Engineering) a recipe that should load but doesn't is one that failed to parse. For the single-input
	 * smelting, blasting, pressing and washing recipes it also checks that no other recipe already takes the same input.
	 */
	@GameTest(template = "empty")
	public static void compatRecipes(GameTestHelper helper) throws Exception {
		MinecraftServer server = helper.getLevel().getServer();
		var dir = net.neoforged.fml.ModList.get().getModFileById(Techguns.MODID).getFile().findResource("data", "techguns", "recipe", "compat");
		List<java.nio.file.Path> files = new ArrayList<>();
		try (var walk = java.nio.file.Files.walk(dir)) {
			walk.filter(f -> f.toString().endsWith(".json")).forEach(files::add);
		}
		helper.assertTrue(files.size() >= 15, "only " + files.size() + " compat recipes");
		List<String> problems = new ArrayList<>();
		int loadedCount = 0;
		for (var f : files) {
			String rel = dir.relativize(f).toString().replace(java.io.File.separatorChar, '/');
			String mod = rel.substring(0, rel.indexOf('/'));
			ResourceLocation id = Techguns.rl("compat/" + rel.substring(0, rel.length() - 5));
			JsonObject json;
			try (var r = java.nio.file.Files.newBufferedReader(f)) {
				json = JsonParser.parseReader(r).getAsJsonObject();
			}
			var conds = json.getAsJsonArray("neoforge:conditions");
			if (conds == null || conds.isEmpty() || !conds.get(0).getAsJsonObject().get("type").getAsString().equals("neoforge:mod_loaded")
					|| !conds.get(0).getAsJsonObject().get("modid").getAsString().equals(mod)) {
				problems.add(rel + " is not gated on " + mod + " first");
				continue;
			}
			boolean expected = true;
			for (var c : conds) {
				expected &= modCondition(c.getAsJsonObject(), rel, problems);
			}
			var holder = server.getRecipeManager().byKey(id);
			if (expected && holder.isEmpty()) {
				problems.add(rel + " should load but didn't (parse error?)");
			} else if (!expected && holder.isPresent()) {
				problems.add(rel + " loaded although its conditions are false");
			}
			if (holder.isEmpty()) {
				continue;
			}
			loadedCount++;
			var recipe = holder.get().value();
			String type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString();
			if (List.of("minecraft:smelting", "minecraft:blasting", "create:pressing", "create:splashing").contains(type)
					&& recipe.getIngredients().size() == 1 && recipe.getIngredients().get(0).getItems().length > 0) {
				var probe = recipe.getIngredients().get(0).getItems()[0];
				for (var other : server.getRecipeManager().getRecipes()) {
					if (other.id().equals(id) || other.value().getType() != recipe.getType() || other.value().getIngredients().size() != 1) {
						continue;
					}
					if (other.value().getIngredients().get(0).test(probe)) {
						problems.add(rel + " duplicates " + other.id() + " for " + BuiltInRegistries.ITEM.getKey(probe.getItem()));
					}
				}
			}
		}
		Techguns.LOGGER.info("compat recipes: {} files, {} loaded", files.size(), loadedCount);
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** evaluates neoforge:mod_loaded and neoforge:not, the only conditions compat recipes use */
	private static boolean modCondition(JsonObject c, String file, List<String> problems) {
		return switch (c.get("type").getAsString()) {
			case "neoforge:mod_loaded" -> net.neoforged.fml.ModList.get().isLoaded(c.get("modid").getAsString());
			case "neoforge:not" -> !modCondition(c.getAsJsonObject("value"), file, problems);
			default -> {
				problems.add(file + ": unexpected condition " + c.get("type"));
				yield false;
			}
		};
	}

	/**
	 * Techguns' own gun_stats data map lists every gun with exactly its code values, so packs have them all in one
	 * place and the file can't drift from TGuns. On a mismatch the expected file is written to
	 * run-gametest/gun_stats.expected.json (copy it to data/techguns/data_maps/item/gun_stats.json).
	 */
	@GameTest(template = "empty")
	public static void gunStatsMatchCode(GameTestHelper helper) {
		var items = helper.getLevel().registryAccess().registryOrThrow(Registries.ITEM);
		var loaded = items.getDataMap(GunStats.DATA_MAP);
		List<String> problems = new ArrayList<>();
		for (GenericGun gun : GenericGun.guns) {
			var key = items.getResourceKey(gun).orElseThrow();
			Map<GunStat, Double> entry = loaded.get(key);
			if (entry == null) {
				problems.add(key.location() + " has no entry");
				continue;
			}
			var code = GunStats.codeValues(gun);
			if (!entry.keySet().equals(code.keySet())) {
				problems.add(key.location() + " lists " + entry.keySet() + " instead of " + code.keySet());
				continue;
			}
			code.forEach((stat, v) -> {
				if ((float) entry.get(stat).doubleValue() != (float) v.doubleValue()) {
					problems.add(key.location() + " " + stat.getSerializedName() + " " + entry.get(stat) + " instead of " + v);
				}
				// and the loaded value is what the gun has now
				if ((float) stat.get(gun) != (float) v.doubleValue()) {
					problems.add(key.location() + " " + stat.getSerializedName() + " is " + stat.get(gun) + " after loading, not " + v);
				}
			});
		}
		if (!problems.isEmpty()) {
			java.nio.file.Path out = java.nio.file.Path.of("gun_stats.expected.json").toAbsolutePath();
			try {
				java.nio.file.Files.writeString(out,
						new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(GunStats.codeValuesJson()) + "\n");
			} catch (java.io.IOException e) {
				problems.add("couldn't write " + out + ": " + e);
			}
			problems.add(0, "expected file written to " + out);
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems.subList(0, Math.min(problems.size(), 12))));
		helper.succeed();
	}

	/**
	 * A data map entry overrides just the stats it names: whole number stats are rounded, values below a stat's
	 * minimum raised, stats the gun doesn't have ignored. Applying again restores the code values first.
	 */
	@GameTest(template = "empty")
	public static void gunStatsOverride(GameTestHelper helper) {
		var items = helper.getLevel().registryAccess().registryOrThrow(Registries.ITEM);
		GenericGun pistol = TGuns.PISTOL.get(), shotgun = TGuns.COMBATSHOTGUN.get(), ak = TGuns.AK47.get();
		List<String> problems = new ArrayList<>();
		try {
			Map<GunStat, Double> pistolStats = new java.util.EnumMap<>(GunStat.class);
			pistolStats.put(GunStat.DAMAGE, 99.0);
			pistolStats.put(GunStat.FIRE_DELAY, 2.6);
			pistolStats.put(GunStat.BULLET_SPEED, -1.0);
			pistolStats.put(GunStat.PELLETS, 5.0); // not a shotgun
			GunStats.apply(items, Map.of(items.getResourceKey(pistol).orElseThrow(), pistolStats,
					items.getResourceKey(shotgun).orElseThrow(), Map.of(GunStat.PELLETS, 3.0)));
			expect(problems, "pistol damage", GunStat.DAMAGE.get(pistol), 99);
			expect(problems, "pistol fire delay (2.6 rounded)", pistol.getMinFiretime(), 3);
			expect(problems, "pistol bullet speed (-1, raised to the minimum)", GunStat.BULLET_SPEED.get(pistol), 0.01f);
			expect(problems, "pistol reload time (not overridden)", pistol.getReloadtime(), GunStats.codeValues(pistol).get(GunStat.RELOAD_TIME));
			expect(problems, "combat shotgun pellets", GunStat.PELLETS.get(shotgun), 3);
			expect(problems, "combat shotgun damage (not overridden)", GunStat.DAMAGE.get(shotgun), GunStats.codeValues(shotgun).get(GunStat.DAMAGE));
			expect(problems, "ak47 damage (no entry)", GunStat.DAMAGE.get(ak), GunStats.codeValues(ak).get(GunStat.DAMAGE));
			if (GunStat.PELLETS.appliesTo(pistol)) {
				problems.add("pellets applies to the pistol");
			}
			// an entry that's gone: the gun goes back to its code values
			GunStats.apply(items, Map.of());
			expect(problems, "pistol damage once the entry is gone", GunStat.DAMAGE.get(pistol), 8.0f);
			expect(problems, "pistol fire delay once the entry is gone", pistol.getMinFiretime(), 4);
			expect(problems, "combat shotgun pellets once the entry is gone", GunStat.PELLETS.get(shotgun), 8);
		} finally {
			GunStats.apply(items);
		}
		expect(problems, "pistol damage after re-applying", GunStat.DAMAGE.get(pistol), GunStats.codeValues(pistol).get(GunStat.DAMAGE));
		expect(problems, "combat shotgun pellets after re-applying", GunStat.PELLETS.get(shotgun), GunStats.codeValues(shotgun).get(GunStat.PELLETS));
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	private static void expect(List<String> problems, String what, double actual, double expected) {
		if ((float) actual != (float) expected) {
			problems.add(what + ": " + actual + " instead of " + expected);
		}
	}
}
