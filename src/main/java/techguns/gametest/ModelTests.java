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
 * and texture they reach exists in the mod's assets, with each texture in the block atlas
 * (assets/minecraft/atlases/blocks.json, or vanilla's block/ and item/ folders). Catches the purple-and-black missing model/texture in game,
 * which the server never loads. Vanilla and other namespaces are assumed present. Also: no blockstate file without a
 * registered block (1.12 leftovers).
 */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class ModelTests {

	private static Path asset(String path) {
		return modFile("assets/techguns/" + path);
	}

	private static Path modFile(String path) {
		return ModList.get().getModFileById(Techguns.MODID).getFile().findResource(path.split("/"));
	}

	/** Block atlas sprites a model may use: our directory prefixes and single sprites, plus vanilla's own folders. */
	private record Atlas(List<String> prefixes, Set<String> singles) {
		boolean contains(ResourceLocation tex) {
			if (tex.getNamespace().equals("minecraft")) {
				return tex.getPath().startsWith("block/") || tex.getPath().startsWith("item/");
			}
			return singles.contains(tex.toString()) || prefixes.stream().anyMatch(tex.getPath()::startsWith);
		}
	}

	private static Atlas blockAtlas() throws IOException {
		List<String> prefixes = new ArrayList<>(List.of("block/", "item/")); // vanilla's sources apply to every namespace
		Set<String> singles = new HashSet<>();
		try (Reader r = Files.newBufferedReader(modFile("assets/minecraft/atlases/blocks.json"))) {
			for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("sources")) {
				JsonObject src = e.getAsJsonObject();
				switch (src.get("type").getAsString()) {
					case "directory", "minecraft:directory" -> prefixes.add(src.get("prefix").getAsString());
					case "single", "minecraft:single" -> singles.add(ResourceLocation.parse(src.get("resource").getAsString()).toString());
					default -> {
					}
				}
			}
		}
		return new Atlas(prefixes, singles);
	}

	private static JsonObject json(String path) throws IOException {
		try (Reader r = Files.newBufferedReader(asset(path))) {
			return JsonParser.parseReader(r).getAsJsonObject();
		}
	}

	/** Checks a model reference (as written in a blockstate or "parent") and everything it reaches. */
	private static Atlas atlas;

	private static void checkModel(String ref, String from, Set<String> seen, List<String> problems) throws IOException {
		checkModel(ref, from, Set.of(), seen, problems);
	}

	/** inherited: texture variables the models using this one as their parent define */
	private static void checkModel(String ref, String from, Set<String> inherited, Set<String> seen, List<String> problems) throws IOException {
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
			if (model.get("loader").getAsString().equals("neoforge:composite")) {
				// the JSON machine models: each child is a model of its own, with the root's textures too
				checkTextures(model, file, problems);
				for (var child : model.getAsJsonObject("children").entrySet()) {
					JsonObject c = child.getValue().getAsJsonObject();
					checkTextures(c, file + " (" + child.getKey() + ")", problems);
					Set<String> defined = new java.util.HashSet<>(model.getAsJsonObject("textures").keySet());
					if (c.has("textures")) {
						defined.addAll(c.getAsJsonObject("textures").keySet());
					}
					checkFaceRefs(c, defined, file + " (" + child.getKey() + ")", problems);
				}
			}
			return; // other custom loaders (obj) reference their own files
		}
		Set<String> defined = new java.util.HashSet<>(inherited);
		if (model.has("textures")) {
			defined.addAll(model.getAsJsonObject("textures").keySet());
		}
		if (model.has("parent")) {
			checkModel(model.get("parent").getAsString(), file, defined, seen, problems);
		}
		checkTextures(model, file, problems);
		checkFaceRefs(model, defined, file, problems);
	}

	/**
	 * Every face of the model's own elements uses a texture variable that's defined: by the model, a model using it as
	 * parent, or the composite root. A template only reached as a parent is checked with its children's variables.
	 */
	private static void checkFaceRefs(JsonObject model, Set<String> defined, String file, List<String> problems) {
		if (!model.has("elements")) {
			return;
		}
		for (JsonElement el : model.getAsJsonArray("elements")) {
			for (var face : el.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
				String tex = face.getValue().getAsJsonObject().get("texture").getAsString();
				if (tex.startsWith("#") && !defined.contains(tex.substring(1))) {
					problems.add(file + " -> face texture " + tex + " isn't defined");
				}
			}
		}
	}

	private static void checkTextures(JsonObject model, String file, List<String> problems) {
		if (model.has("textures")) {
			for (var e : model.getAsJsonObject("textures").entrySet()) {
				String tex = e.getValue().getAsString();
				if (tex.startsWith("#")) {
					continue;
				}
				ResourceLocation t = ResourceLocation.parse(tex);
				if (t.getNamespace().equals(Techguns.MODID) && !Files.exists(asset("textures/" + t.getPath() + ".png"))) {
					problems.add(file + " -> missing texture " + tex);
				} else if (!atlas.contains(t)) {
					// the file can exist and still render as missing: models only see block atlas sprites
					problems.add(file + " -> texture not in the block atlas " + tex);
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
		atlas = blockAtlas();
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

	/**
	 * Blocks that face a way turn their model with it: in a "variants" blockstate, the y rotation of each facing,
	 * minus the facing's own angle, is the same for all four (with the other properties the same). The charging
	 * station had no rotations, so it looked north whichever way it was placed.
	 */
	@GameTest(template = "empty")
	public static void facingBlockstatesRotate(GameTestHelper helper) throws IOException {
		List<String> problems = new ArrayList<>();
		for (var e : BuiltInRegistries.BLOCK.entrySet()) {
			ResourceLocation id = e.getKey().location();
			var state = e.getValue().defaultBlockState();
			if (!id.getNamespace().equals(Techguns.MODID) || state.getRenderShape() == RenderShape.INVISIBLE
					|| !state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
				continue;
			}
			String file = "blockstates/" + id.getPath() + ".json";
			if (!Files.exists(asset(file)) || !json(file).has("variants")) {
				continue;
			}
			// other properties -> the rotation offset its facings agree on
			java.util.Map<String, Integer> offsets = new java.util.HashMap<>();
			for (var v : json(file).getAsJsonObject("variants").entrySet()) {
				String facing = null;
				List<String> rest = new ArrayList<>();
				for (String kv : v.getKey().split(",")) {
					if (kv.startsWith("facing=")) {
						facing = kv.substring(7);
					} else {
						rest.add(kv);
					}
				}
				if (facing == null || !v.getValue().isJsonObject()) {
					continue;
				}
				var dir = net.minecraft.core.Direction.byName(facing);
				int y = v.getValue().getAsJsonObject().has("y") ? v.getValue().getAsJsonObject().get("y").getAsInt() : 0;
				// blockstate y turns clockwise from above, as Direction.toYRot does; north is 180
				int offset = Math.floorMod(y - ((int) dir.toYRot() - 180), 360);
				Integer before = offsets.putIfAbsent(String.join(",", rest), offset);
				if (before != null && before != offset) {
					problems.add(file + ": " + v.getKey() + " is turned " + y + ", which doesn't match the other facings");
				}
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/**
	 * Every OBJ block / item model face's vertex normals point the way the face does. NeoForge's OBJ loader picks each
	 * quad's direction, and so its shade, from these normals: smoothed (averaged) normals made the reaction chamber's
	 * flat panels split into a light and a dark triangle.
	 */
	@GameTest(template = "empty")
	public static void objNormalsMatchFaces(GameTestHelper helper) throws IOException {
		List<String> problems = new ArrayList<>();
		Path root = asset("models");
		List<Path> objs;
		try (var walk = Files.walk(root)) {
			objs = walk.filter(f -> f.toString().endsWith(".obj")).sorted().toList();
		}
		for (Path obj : objs) {
			List<float[]> v = new ArrayList<>(), vn = new ArrayList<>();
			int bad = 0;
			for (String line : Files.readAllLines(obj)) {
				String[] p = line.trim().split("\\s+");
				if (p[0].equals("v")) {
					v.add(new float[] { Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]) });
				} else if (p[0].equals("vn")) {
					vn.add(new float[] { Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]) });
				} else if (p[0].equals("f") && p.length >= 4) {
					float[] a = v.get(Integer.parseInt(p[1].split("/")[0]) - 1), b = v.get(Integer.parseInt(p[2].split("/")[0]) - 1),
							c = v.get(Integer.parseInt(p[3].split("/")[0]) - 1);
					float[] e1 = { b[0] - a[0], b[1] - a[1], b[2] - a[2] }, e2 = { c[0] - a[0], c[1] - a[1], c[2] - a[2] };
					float[] g = { e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0] };
					double lg = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
					for (int i = 1; i < p.length && lg > 0; i++) {
						String[] idx = p[i].split("/");
						if (idx.length < 3 || idx[2].isEmpty()) {
							continue; // no normal: the loader uses the face's own
						}
						float[] n = vn.get(Integer.parseInt(idx[2]) - 1);
						double ln = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
						if ((g[0] * n[0] + g[1] * n[1] + g[2] * n[2]) / (lg * ln) < 0.98) {
							bad++;
							break;
						}
					}
				}
			}
			if (bad > 0) {
				problems.add(root.relativize(obj) + ": " + bad + " faces with normals off the face");
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}
}
