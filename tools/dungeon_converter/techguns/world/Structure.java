package techguns.world;
import java.io.*;
import java.util.*;
import techguns.util.MBlock;
public class Structure implements Serializable {
	private static final long serialVersionUID = 1L+1;
	public int sizeX, sizeY, sizeZ;
	public List<MBlock> blocks;
	public HashMap<Integer, List<BlockEntry>> blockEntries;
	public class BlockEntry implements Serializable {
		private static final long serialVersionUID = 1L;
		public int x, y, z, blockIndex;
	}
}
