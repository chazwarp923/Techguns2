package techguns.world.dungeon;
import java.io.*;
import techguns.world.Structure;
public class DungeonSegment implements Serializable {
	private static final long serialVersionUID = 1L+1;
	public TemplateSegment.SegmentType type;
	public Structure structure;
	public transient String templateName;
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		templateName = in.readUTF();
	}
}
