package techguns.client.audio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import techguns.util.EntityCondition;
import techguns.util.MathUtil;

/**
 * 1.12 TGSound: a sound on an entity. Moving sounds follow the entity (and stop when it is gone), gun position sounds sit
 * one block beside the head, and the sound stops as soon as its condition is no longer met (e.g. charging ended).
 */
public class TGSound extends AbstractTickableSoundInstance {

	private final Entity entity;
	private final boolean moving;
	private final boolean gunPosition;
	private final EntityCondition condition;

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
		updatePosition();
	}

	private void updatePosition() {
		this.x = entity.getX();
		this.y = entity.getY();
		this.z = entity.getZ();
		if (gunPosition && entity instanceof LivingEntity living) {
			MathUtil.Vec2 pos = MathUtil.polarOffsetXZ(this.x, this.z, 1.0, living.getYHeadRot() * Math.PI / 180.0);
			this.x = pos.x;
			this.z = pos.y;
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
