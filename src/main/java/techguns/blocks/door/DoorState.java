package techguns.blocks.door;

import net.minecraft.util.StringRepresentable;

/** 1.12 EnumDoorState */
public enum DoorState implements StringRepresentable {
	CLOSED, OPENED, OPENING, CLOSING;

	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}

	public boolean isOpen() {
		return this == OPENED || this == OPENING;
	}

	public boolean isMoving() {
		return this == OPENING || this == CLOSING;
	}

	public DoorState next() {
		return switch (this) {
			case CLOSED -> OPENING;
			case OPENED -> CLOSING;
			case OPENING -> OPENED;
			case CLOSING -> CLOSED;
		};
	}
}
