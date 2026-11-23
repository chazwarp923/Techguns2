package techguns.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;
import techguns.blocks.machines.BlockMultiBlockSlave;
import techguns.blocks.machines.multiblocks.MultiBlockSchematic;
import techguns.tileentities.MultiBlockMachineTileEntMaster;
import techguns.tileentities.MultiBlockSlaveTileEnt;

/**
 * The Patchouli manual and the Ponder scenes are client side, but what they point at can be checked here: the book's
 * items, recipes, blocks, entities, images and links, and that every Ponder schematic is a multiblock that really forms.
 */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class GuideTests {

	private static final String BOOK = "assets/techguns/patchouli_books/techguns_manual/en_us";
	private static final Pattern LINK = Pattern.compile("\\$\\(l:([a-z0-9_/]+)(?:#([a-z0-9_]+))?\\)");
	private static final String[] PONDER_SCHEMATICS = { "fabricator", "reaction_chamber", "ore_drill_small", "ore_drill" };

	private static Path modFile(String path) {
		return ModList.get().getModFileById(Techguns.MODID).getFile().findResource(path.split("/"));
	}

	private static JsonObject json(Path p) throws IOException {
		try (Reader r = Files.newBufferedReader(p)) {
			return JsonParser.parseReader(r).getAsJsonObject();
		}
	}

	private static String id(Path root, Path file) {
		String rel = root.relativize(file).toString().replace('\\', '/');
		return rel.substring(0, rel.length() - ".json".length());
	}

	/** every entry's category, icons, items, recipes, multiblock blocks, entities, images and links resolve */
	@GameTest(template = "empty")
	public static void manualReferencesResolve(GameTestHelper helper) throws IOException {
		ServerLevel level = helper.getLevel();
		List<String> problems = new ArrayList<>();

		JsonObject book = json(modFile("data/techguns/patchouli_books/techguns_manual/book.json"));
		helper.assertTrue(book.get("use_resource_pack").getAsBoolean(), "1.20+ Patchouli needs use_resource_pack");
		String model = book.get("model").getAsString();
		if (!Files.exists(modFile("assets/techguns/models/item/" + ResourceLocation.parse(model).getPath() + ".json"))) {
			problems.add("book model " + model);
		}

		Path root = modFile(BOOK);
		Set<String> categories = new HashSet<>();
		try (Stream<Path> s = Files.list(root.resolve("categories"))) {
			s.forEach(p -> categories.add(id(root.resolve("categories"), p)));
		}
		Path entriesRoot = root.resolve("entries");
		Map<String, JsonObject> entries = new HashMap<>();
		try (Stream<Path> s = Files.walk(entriesRoot)) {
			for (Path p : s.filter(Files::isRegularFile).toList()) {
				entries.put(id(entriesRoot, p), json(p));
			}
		}
		helper.assertTrue(entries.size() >= 39, "only " + entries.size() + " entries");

		List<String> texts = new ArrayList<>();
		for (Path p : categories.stream().map(c -> root.resolve("categories/" + c + ".json")).toList()) {
			JsonObject c = json(p);
			checkItem(c.get("icon").getAsString(), "category " + p.getFileName(), problems);
			texts.add(c.get("description").getAsString());
			if (c.has("parent") && !c.get("parent").getAsString().contains(":")) {
				problems.add("category " + p.getFileName() + ": parent must be domain:name");
			}
		}
		for (var e : entries.entrySet()) {
			String where = "entry " + e.getKey();
			JsonObject entry = e.getValue();
			// Patchouli 1.20+ only takes fully qualified category ids (domain:name)
			ResourceLocation category = ResourceLocation.tryParse(entry.get("category").getAsString());
			if (!entry.get("category").getAsString().contains(":") || category == null || !category.getNamespace().equals(Techguns.MODID)
					|| !categories.contains(category.getPath())) {
				problems.add(where + ": category " + entry.get("category"));
			}
			checkItem(entry.get("icon").getAsString(), where, problems);
			for (JsonElement pe : entry.getAsJsonArray("pages")) {
				JsonObject page = pe.getAsJsonObject();
				String type = page.get("type").getAsString();
				if (page.has("text")) {
					texts.add(page.get("text").getAsString());
				}
				if (page.has("item")) {
					checkItem(page.get("item").getAsString(), where, problems);
				}
				for (String key : new String[] { "recipe", "recipe2" }) {
					if (page.has(key)) {
						ResourceLocation rl = ResourceLocation.parse(page.get(key).getAsString());
						var recipe = level.getRecipeManager().byKey(rl);
						if (recipe.isEmpty()) {
							problems.add(where + ": recipe " + rl);
						} else if (type.equals("crafting") && recipe.get().value().getType() != net.minecraft.world.item.crafting.RecipeType.CRAFTING) {
							// Patchouli's crafting page only looks up crafting recipes
							problems.add(where + ": recipe " + rl + " is not a crafting recipe");
						}
					}
				}
				if (page.has("images")) {
					for (JsonElement img : page.getAsJsonArray("images")) {
						ResourceLocation rl = ResourceLocation.parse(img.getAsString());
						if (!Files.exists(modFile("assets/" + rl.getNamespace() + "/" + rl.getPath()))) {
							problems.add(where + ": image " + rl);
						}
					}
				}
				if (type.equals("multiblock")) {
					checkMultiblock(page.getAsJsonObject("multiblock"), where, problems);
				}
				if (type.equals("entity")) {
					checkEntity(level, page.get("entity").getAsString(), where, problems);
				}
			}
		}

		// $(l:entry#anchor) links point at an entry, and at an anchor that the entry has
		for (String t : texts) {
			Matcher m = LINK.matcher(t);
			while (m.find()) {
				JsonObject target = entries.get(m.group(1));
				if (target == null) {
					problems.add("link to missing entry " + m.group(1));
				} else if (m.group(2) != null && !hasAnchor(target, m.group(2))) {
					problems.add("link to missing anchor " + m.group(1) + "#" + m.group(2));
				}
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	private static boolean hasAnchor(JsonObject entry, String anchor) {
		for (JsonElement p : entry.getAsJsonArray("pages")) {
			JsonObject page = p.getAsJsonObject();
			if (page.has("anchor") && page.get("anchor").getAsString().equals(anchor)) {
				return true;
			}
		}
		return false;
	}

	private static void checkItem(String item, String where, List<String> problems) {
		ResourceLocation rl = ResourceLocation.tryParse(item);
		if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
			problems.add(where + ": item " + item);
		}
	}

	private static void checkMultiblock(JsonObject mb, String where, List<String> problems) {
		JsonObject mapping = mb.getAsJsonObject("mapping");
		for (var m : mapping.entrySet()) {
			ResourceLocation rl = ResourceLocation.tryParse(m.getValue().getAsString());
			if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) {
				problems.add(where + ": multiblock block " + m.getValue());
			}
		}
		// Patchouli's DenseMultiblock: same row count per layer, same row length, exactly one '0' (the center),
		// and every other character mapped (' ' is air, '_' is anything)
		int rows = -1, len = -1, centers = 0;
		for (JsonElement layer : mb.getAsJsonArray("pattern")) {
			JsonArray arr = (JsonArray) layer;
			if (rows == -1) {
				rows = arr.size();
			} else if (arr.size() != rows) {
				problems.add(where + ": multiblock layers have different row counts");
			}
			for (JsonElement row : arr) {
				String r = row.getAsString();
				if (len == -1) {
					len = r.length();
				} else if (r.length() != len) {
					problems.add(where + ": multiblock rows have different lengths");
				}
				for (char c : r.toCharArray()) {
					if (c == '0') {
						centers++;
					} else if (c != ' ' && c != '_' && !mapping.has(String.valueOf(c))) {
						problems.add(where + ": pattern char '" + c + "' has no mapping");
					}
				}
			}
		}
		if (centers != 1) {
			problems.add(where + ": multiblock needs exactly one '0' center, has " + centers);
		}
	}

	/** like Patchouli's EntityUtil: id + SNBT with ' for " */
	private static void checkEntity(ServerLevel level, String s, String where, List<String> problems) {
		int nbtStart = s.indexOf('{');
		String id = nbtStart > 0 ? s.substring(0, nbtStart) : s;
		var type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(id));
		if (type.isEmpty()) {
			problems.add(where + ": entity " + id);
			return;
		}
		Entity e = type.get().create(level);
		if (nbtStart > 0) {
			try {
				CompoundTag tag = TagParser.parseTag(s.substring(nbtStart).replaceAll("([^\\\\])'", "$1\"").replaceAll("\\\\'", "'"));
				e.load(tag);
			} catch (Exception ex) {
				problems.add(where + ": entity nbt " + ex.getMessage());
				return;
			}
		}
		// the book shows armor sets on armor stands: all 4 pieces have to load
		if (e instanceof LivingEntity living && s.contains("ArmorItems")) {
			for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
				if (living.getItemBySlot(slot).isEmpty()) {
					problems.add(where + ": no " + slot.getName() + " armor on the " + id);
				}
			}
		}
	}

	/** with Patchouli installed the manual can be crafted, and it is the Techguns book */
	@GameTest(template = "empty")
	public static void manualRecipe(GameTestHelper helper) {
		if (!ModList.get().isLoaded("patchouli")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		for (String name : new String[] { "techguns_manual", "techguns_manual_alt" }) {
			var recipe = level.getRecipeManager().byKey(Techguns.rl(name));
			helper.assertTrue(recipe.isPresent(), "recipe " + name + " not loaded");
			ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
			helper.assertTrue(BuiltInRegistries.ITEM.getKey(result.getItem()).equals(ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book")),
					name + " makes " + result);
			var bookType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("patchouli", "book"));
			helper.assertTrue(bookType != null && Techguns.rl("techguns_manual").equals(result.get(bookType)), name + " is not the Techguns manual");
		}
		helper.succeed();
	}

	/** every Ponder scene schematic is a valid multiblock: placed in the world, its controller forms it */
	@GameTest(template = "empty_large")
	public static void ponderSchematicsForm(GameTestHelper helper) throws IOException {
		ServerLevel level = helper.getLevel();
		var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "tg_test"));
		List<String> problems = new ArrayList<>();
		BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
		for (String name : PONDER_SCHEMATICS) {
			CompoundTag tag;
			try (InputStream in = Files.newInputStream(modFile("assets/techguns/ponder/" + name + ".nbt"))) {
				tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
			}
			StructureTemplate template = new StructureTemplate();
			template.load(level.holderLookup(Registries.BLOCK), tag);
			var size = template.getSize();
			// clear the area, then place the scene
			BlockPos.betweenClosed(origin, origin.offset(6, 7, 6)).forEach(p -> level.removeBlock(p, false));
			template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), 2);

			BlockPos controller = null;
			int parts = 0;
			for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
				BlockEntity be = level.getBlockEntity(p);
				if (be instanceof MultiBlockMachineTileEntMaster) {
					controller = p.immutable();
				} else if (level.getBlockState(p).getBlock() instanceof BlockMultiBlockSlave) {
					parts++;
				}
			}
			if (controller == null) {
				problems.add(name + ": no controller");
				continue;
			}
			var master = (MultiBlockMachineTileEntMaster) level.getBlockEntity(controller);
			MultiBlockSchematic schematic = master.getSchematic();
			if (!schematic.checkForm(level, player, controller, Direction.NORTH)) {
				problems.add(name + ": does not form");
				continue;
			}
			schematic.form(level, master, Direction.NORTH);
			if (!master.isFormed()) {
				problems.add(name + ": controller not formed");
			}
			int linked = 0;
			for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
				if (level.getBlockEntity(p) instanceof MultiBlockSlaveTileEnt slave && controller.equals(slave.getMasterPos())) {
					linked++;
				}
			}
			if (linked != parts) {
				problems.add(name + ": " + linked + " of " + parts + " parts linked");
			}
			schematic.unform(level, master);
			if (master.isFormed()) {
				problems.add(name + ": does not unform");
			}
		}
		BlockPos.betweenClosed(origin, origin.offset(6, 7, 6)).forEach(p -> level.removeBlock(p, false));
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}
}
