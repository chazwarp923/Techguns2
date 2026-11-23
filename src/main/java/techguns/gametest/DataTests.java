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
import techguns.Techguns;
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
}
