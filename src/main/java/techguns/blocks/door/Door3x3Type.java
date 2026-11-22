package techguns.blocks.door;

/** Kinds of 3x3 doors (1.12 EnumDoorType), one block each now. */
public enum Door3x3Type {
	/** two halves slide sideways */
	METAL,
	/** segments slide up into the frame */
	HANGAR_UP,
	/** segments slide down into the frame */
	HANGAR_DOWN,
	/** METAL with nether textures */
	NETHER;

	public boolean isHangar() {
		return this == HANGAR_UP || this == HANGAR_DOWN;
	}
}
