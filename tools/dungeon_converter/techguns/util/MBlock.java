package techguns.util;
import java.io.*;
public class MBlock implements Serializable {
	private static final long serialVersionUID = 1L;
	public int meta;
	public transient String name;
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		name = in.readUTF();
	}
}
