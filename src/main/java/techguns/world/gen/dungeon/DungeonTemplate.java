package techguns.world.gen.dungeon;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import techguns.Techguns;
import techguns.world.gen.GenWorld;
import techguns.world.gen.LegacyBlocks;
import techguns.world.gen.MBlock;

/**
 * A 1.12 dungeon template (world/dungeon/DungeonTemplate): one set of 5x5 segments (corridors, rooms, ramps...) in a
 * style, loaded from the converted JSON in assets/techguns/dungeons (see tools/dungeon_converter).
 */
public class DungeonTemplate {

	private static final Map<String, DungeonTemplate> TEMPLATES = new HashMap<>();

	public final String name;
	public final int sizeXZ;
	public final int sizeY;
	final Map<SegmentType, Segment> segments = new EnumMap<>(SegmentType.class);

	private DungeonTemplate(String name, int sizeXZ, int sizeY) {
		this.name = name;
		this.sizeXZ = sizeXZ;
		this.sizeY = sizeY;
	}

	/** template with chests filled from this loot table (null: plain chests) */
	public static synchronized DungeonTemplate get(String name, String lootTable) {
		String key = name + "|" + lootTable;
		DungeonTemplate t = TEMPLATES.get(key);
		if (t == null) {
			t = load(name, lootTable);
			TEMPLATES.put(key, t);
		}
		return t;
	}

	public Segment segment(SegmentType type) {
		return segments.get(type);
	}

	/** one palette entry, already converted to a 1.21 state */
	record PaletteEntry(BlockState state, String loot) {
	}

	/** 1.12 DungeonSegment + Structure: the blocks of one segment type, per placement layer */
	public static class Segment {
		final int sizeX, sizeY, sizeZ;
		final List<PaletteEntry> palette;
		/** placement layer -> x,y,z,index quadruples */
		final TreeMap<Integer, int[]> layers;

		Segment(int sizeX, int sizeY, int sizeZ, List<PaletteEntry> palette, TreeMap<Integer, int[]> layers) {
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = sizeZ;
			this.palette = palette;
			this.layers = layers;
		}

		/**
		 * 1.12 Structure.placeBlocks: rotated around the centre of the footprint, one counter clockwise step per rotation,
		 * layer by layer
		 */
		public void place(GenWorld w, int x, int y, int z, int rotation) {
			int steps = Math.floorMod(rotation, 4);
			net.minecraft.world.level.block.Rotation rot = MBlock.rotation(steps);
			for (int[] entries : layers.values()) {
				for (int i = 0; i + 3 < entries.length; i += 4) {
					int bx = entries[i], by = entries[i + 1], bz = entries[i + 2];
					PaletteEntry pe = palette.get(entries[i + 3]);
					for (int s = 0; s < steps; s++) { // (i, k) -> (k, size-1-i)
						int t = bx;
						bx = bz;
						bz = sizeX - 1 - t;
					}
					BlockState state = pe.state().rotate(rot);
					if (pe.loot() != null) {
						w.set(x + bx, y + by, z + bz, new MBlock.Chest(state, pe.loot()));
					} else {
						w.set(x + bx, y + by, z + bz, state);
					}
				}
			}
		}
	}

	private static DungeonTemplate load(String name, String lootTable) {
		String path = "/assets/techguns/dungeons/" + name + ".json";
		try (InputStream in = DungeonTemplate.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("missing " + path);
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			DungeonTemplate t = new DungeonTemplate(name, root.get("sizeXZ").getAsInt(), root.get("sizeY").getAsInt());
			for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("segments").entrySet()) {
				SegmentType type = SegmentType.valueOf(e.getKey());
				JsonObject seg = e.getValue().getAsJsonObject();
				JsonArray size = seg.getAsJsonArray("size");
				List<PaletteEntry> palette = new ArrayList<>();
				for (JsonElement p : seg.getAsJsonArray("palette")) {
					palette.add(convert(p.getAsString(), lootTable));
				}
				TreeMap<Integer, int[]> layers = new TreeMap<>();
				for (Map.Entry<String, JsonElement> l : seg.getAsJsonObject("layers").entrySet()) {
					JsonArray a = l.getValue().getAsJsonArray();
					int[] data = new int[a.size()];
					for (int i = 0; i < data.length; i++) {
						data[i] = a.get(i).getAsInt();
					}
					layers.put(Integer.parseInt(l.getKey()), data);
				}
				t.segments.put(type, new Segment(size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(), palette, layers));
			}
			return t;
		} catch (Exception e) {
			Techguns.LOGGER.error("Could not load dungeon template {}", name, e);
			return new DungeonTemplate(name, 5, 5);
		}
	}

	/** "registryname@meta" of 1.12 to a 1.21 state; chests get the template's loot table (1.12 SpecialBlockHandler) */
	private static PaletteEntry convert(String entry, String lootTable) {
		int at = entry.lastIndexOf('@');
		String name = entry.substring(0, at);
		int meta = Integer.parseInt(entry.substring(at + 1));
		BlockState state;
		if (name.equals("chisel:netherbrick")) {
			state = Blocks.NETHER_BRICKS.defaultBlockState(); // 1.12 MBlock replacement table
		} else if (name.startsWith("techguns:")) {
			state = LegacyBlocks.tg(name.substring("techguns:".length()), meta);
		} else {
			state = LegacyBlocks.vanilla(name, meta);
		}
		String loot = lootTable != null && state.getBlock() instanceof ChestBlock ? lootTable : null;
		return new PaletteEntry(state, loot);
	}
}
