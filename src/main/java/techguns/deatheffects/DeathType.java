package techguns.deatheffects;

/** Was EntityDeathUtils.DeathType in 1.12.2. */
public enum DeathType {
	DEFAULT(0), GORE(1), BIO(2), LASER(3), DISMEMBER(4);

	private final int value;

	DeathType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static DeathType byValue(int v) {
		for (DeathType t : values()) {
			if (t.value == v) {
				return t;
			}
		}
		return DEFAULT;
	}
}
