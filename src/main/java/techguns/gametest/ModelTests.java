package techguns.gametest;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;

/**
 * Every registered Techguns block has a blockstate file and every item has an item model, and every model, parent
 * and texture they reach exists in the mod's assets. Catches the purple-and-black missing model/texture in game,
 * which the server never loads. Vanilla and other namespaces are assumed present. Also: no blockstate file without a
 * registered block (1.12 leftovers).
 */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class ModelTests {

	private static Path asset(String path) {
		return ModList.get().getModFileById(Techguns.MODID).getFile().findResource(("assets/techguns/" + path).split("/"));
	}

	private static JsonObject json(String path) throws IOException {
		try (Reader r = Files.newBufferedReader(asset(path))) {
			return JsonParser.parseReader(r).getAsJsonObject();
		}
	}

	/** Checks a model reference (as written in a blockstate or "parent") and everything it reaches. */
	private static void checkModel(String ref, String from, Set<String> seen, List<String> problems) throws IOException {
		ResourceLocation rl = ResourceLocation.parse(ref);
		if (!rl.getNamespace().equals(Techguns.MODID) || rl.getPath().startsWith("builtin/") || !seen.add(rl.getPath())) {
			return;
		}
		String file = "models/" + rl.getPath() + ".json";
		if (!Files.exists(asset(file))) {
			problems.add(from + " -> missing model " + ref);
			return;
		}
		JsonObject model = json(file);
		if (model.has("loader")) {
			return; // custom loaders (obj) reference their own files
		}
		if (model.has("parent")) {
			checkModel(model.get("parent").getAsString(), file, seen, problems);
		}
		if (model.has("textures")) {
			for (var e : model.getAsJsonObject("textures").entrySet()) {
				String tex = e.getValue().getAsString();
				if (tex.startsWith("#")) {
					continue;
				}
				ResourceLocation t = ResourceLocation.parse(tex);
				if (t.getNamespace().equals(Techguns.MODID) && !Files.exists(asset("textures/" + t.getPath() + ".png"))) {
					problems.add(file + " -> missing texture " + tex);
				}
			}
		}
	}

	private static void collectModels(JsonElement e, List<String> out) {
		if (e.isJsonObject()) {
			for (var en : e.getAsJsonObject().entrySet()) {
				if (en.getKey().equals("model") && en.getValue().isJsonPrimitive()) {
					out.add(en.getValue().getAsString());
				} else {
					collectModels(en.getValue(), out);
				}
			}
		} else if (e.isJsonArray()) {
			e.getAsJsonArray().forEach(x -> collectModels(x, out));
		}
	}

	@GameTest(template = "empty")
	public static void allModelsAndTexturesResolve(GameTestHelper helper) throws IOException {
		List<String> problems = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (var e : BuiltInRegistries.BLOCK.entrySet()) {
			ResourceLocation id = e.getKey().location();
			if (!id.getNamespace().equals(Techguns.MODID) || e.getValue().defaultBlockState().getRenderShape() == RenderShape.INVISIBLE) {
				continue;
			}
			String file = "blockstates/" + id.getPath() + ".json";
			if (!Files.exists(asset(file))) {
				problems.add("block " + id.getPath() + " has no blockstate");
				continue;
			}
			JsonObject bs = json(file);
			if (bs.has("forge_marker")) {
				problems.add(file + " is a 1.12 forge blockstate");
				continue;
			}
			List<String> models = new ArrayList<>();
			collectModels(bs, models);
			for (String m : models) {
				checkModel(m, file, seen, problems);
			}
		}
		for (var e : BuiltInRegistries.ITEM.entrySet()) {
			ResourceLocation id = e.getKey().location();
			if (!id.getNamespace().equals(Techguns.MODID)) {
				continue;
			}
			if (!Files.exists(asset("models/item/" + id.getPath() + ".json"))) {
				problems.add("item " + id.getPath() + " has no item model" + (e.getValue() instanceof BlockItem ? " (block item)" : ""));
				continue;
			}
			checkModel("techguns:item/" + id.getPath(), "item " + id.getPath(), seen, problems);
		}
		// no leftover blockstate files: each one belongs to a registered block
		try (var files = Files.list(asset("blockstates"))) {
			for (Path f : files.toList()) {
				String fileName = f.getFileName().toString();
				String name = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : null;
				if (name == null) {
					problems.add("blockstates/" + fileName + " is not a .json file");
				} else if (!BuiltInRegistries.BLOCK.containsKey(Techguns.rl(name))) {
					problems.add("blockstates/" + name + ".json has no block");
				}
			}
		}
		problems = new ArrayList<>(new TreeSet<>(problems));
		helper.assertTrue(problems.isEmpty(), problems.size() + " model problems: " + String.join("; ", problems));
		helper.succeed();
	}
}
