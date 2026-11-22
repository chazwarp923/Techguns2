import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import techguns.util.MBlock;
import techguns.world.Structure;
import techguns.world.dungeon.*;

/** dumps 1.12 Techguns dungeon templates (.ser) to JSON: palette as "name@meta", block entries per placement layer */
public class Convert {
	public static void main(String[] args) throws Exception {
		File in = new File(args[0]), out = new File(args[1]);
		for (File f : Objects.requireNonNull(in.listFiles((d, n) -> n.endsWith(".ser")))) {
			DungeonTemplate t;
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
				t = (DungeonTemplate) ois.readObject();
			}
			StringBuilder sb = new StringBuilder();
			sb.append("{\n  \"sizeXZ\": ").append(t.sizeXZ).append(",\n  \"sizeY\": ").append(t.sizeY).append(",\n  \"segments\": {");
			boolean firstSeg = true;
			List<TemplateSegment.SegmentType> types = new ArrayList<>(t.segments.keySet());
			Collections.sort(types);
			for (TemplateSegment.SegmentType type : types) {
				Structure s = t.segments.get(type).structure;
				sb.append(firstSeg ? "\n" : ",\n").append("    \"").append(type.name()).append("\": {\n");
				firstSeg = false;
				sb.append("      \"size\": [").append(s.sizeX).append(", ").append(s.sizeY).append(", ").append(s.sizeZ).append("],\n");
				sb.append("      \"palette\": [");
				for (int i = 0; i < s.blocks.size(); i++) {
					MBlock b = s.blocks.get(i);
					sb.append(i == 0 ? "" : ", ").append('"').append(b.name).append('@').append(b.meta).append('"');
				}
				sb.append("],\n      \"layers\": {");
				List<Integer> layers = new ArrayList<>(s.blockEntries.keySet());
				Collections.sort(layers);
				boolean firstLayer = true;
				for (int layer : layers) {
					sb.append(firstLayer ? "\n" : ",\n").append("        \"").append(layer).append("\": [");
					firstLayer = false;
					List<Structure.BlockEntry> entries = s.blockEntries.get(layer);
					for (int i = 0; i < entries.size(); i++) {
						Structure.BlockEntry e = entries.get(i);
						sb.append(i == 0 ? "" : ",").append(e.x).append(',').append(e.y).append(',').append(e.z).append(',').append(e.blockIndex);
					}
					sb.append("]");
				}
				sb.append("\n      }\n    }");
			}
			sb.append("\n  }\n}\n");
			String name = f.getName().substring(0, f.getName().length() - 4);
			try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(out, name + ".json")), StandardCharsets.UTF_8)) {
				w.write(sb.toString());
			}
			System.out.println(name + ": sizeXZ " + t.sizeXZ + " sizeY " + t.sizeY + " segments " + t.segments.size());
		}
	}
}
