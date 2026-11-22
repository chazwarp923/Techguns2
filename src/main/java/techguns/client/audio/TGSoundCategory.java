package techguns.client.audio;

import java.util.ArrayList;

import net.minecraft.sounds.SoundSource;

/** Techguns sound categories (each maps to a vanilla SoundSource). Safe on both sides. */
public class TGSoundCategory {
	protected static ArrayList<TGSoundCategory> CATEGORIES = new ArrayList<>();

	public static final TGSoundCategory GUN_FIRE = new TGSoundCategory("gunfire", SoundSource.PLAYERS);
	public static final TGSoundCategory PLAYER_EFFECT = new TGSoundCategory("playereffect", SoundSource.PLAYERS);
	public static final TGSoundCategory RELOAD = new TGSoundCategory("reload", SoundSource.PLAYERS);
	public static final TGSoundCategory EXPLOISON = new TGSoundCategory("explosion", SoundSource.MASTER);
	public static final TGSoundCategory MACHINE = new TGSoundCategory("machine", SoundSource.BLOCKS);
	public static final TGSoundCategory DEATHEFFECT = new TGSoundCategory("deathFX", SoundSource.HOSTILE);
	public static final TGSoundCategory HOSTILE = new TGSoundCategory("hostile", SoundSource.HOSTILE);

	protected final int id;
	protected final String name;
	protected final SoundSource vanillaCategory;

	public TGSoundCategory(String name, SoundSource vanillaCategory) {
		this.id = CATEGORIES.size();
		this.name = name;
		this.vanillaCategory = vanillaCategory;
		CATEGORIES.add(this);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public SoundSource getVanillaCategory() {
		return vanillaCategory;
	}

	public static TGSoundCategory get(int index) {
		return index >= 0 && index < CATEGORIES.size() ? CATEGORIES.get(index) : null;
	}
}
