package techguns.world.dungeon;
import java.io.*;
import java.util.*;
public class DungeonTemplate implements Serializable {
	private static final long serialVersionUID = 1L+1;
	public int sizeXZ;
	public int sizeY;
	public HashMap<TemplateSegment.SegmentType, DungeonSegment> segments;
	public String name;
}
