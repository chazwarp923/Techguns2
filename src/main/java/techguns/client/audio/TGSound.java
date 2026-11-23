package techguns.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import techguns.util.EntityCondition;

/**
 * 1.12 TGSound: a sound on an entity. Moving sounds follow the entity (and stop when it is gone), and the sound stops
 * as soon as its condition is no longer met (e.g. charging ended).
 * <p>
 * Gun position sounds: 1.12 put them one block to the side of the head (cos/sin of the yaw is the player's left in
 * Minecraft's coordinates), which 1.21's 3D audio pans fully into the left ear. Now the local player's own gun sounds
 * play centred on the listener, and other entities' gun sounds come from the middle of the entity.
 */
public class TGSound extends AbstractTickableSoundInstance {

	private final Entity entity;
	private final boolean moving;
	private final boolean gunPosition;
	private final EntityCondition condition;
	/** the local player's own gun sound: played at the listener */
	private final boolean ownGun;

	public TGSound(SoundEvent sound, Entity entity, float volume, float pitch, boolean repeat, boolean moving, boolean gunPosition,
			TGSoundCategory category, EntityCondition condition) {
		super(sound, category.getVanillaCategory(), SoundInstance.createUnseededRandom());
		this.entity = entity;
		this.volume = volume;
		this.pitch = pitch;
		this.looping = repeat;
		this.delay = 0;
		this.moving = moving;
		this.gunPosition = gunPosition;
		this.condition = condition == null ? EntityCondition.NONE : condition;
		this.ownGun = gunPosition && entity == Minecraft.getInstance().player;
		if (ownGun) {
			this.relative = true;
		}
		updatePosition();
	}

	private void updatePosition() {
		if (ownGun) {
			// relative to the listener: no direction
			this.x = 0;
			this.y = 0;
			this.z = 0;
			return;
		}
		this.x = entity.getX();
		this.y = entity.getY();
		this.z = entity.getZ();
		if (gunPosition) {
			this.y += entity.getBbHeight() * 0.5f;
		}
	}

	@Override
	public void tick() {
		if (!condition.evaluate(entity)) {
			this.stop();
			return;
		}
		if (moving) {
			if (entity.isRemoved()) {
				this.stop();
				return;
			}
			updatePosition();
		}
	}
}
