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
}
