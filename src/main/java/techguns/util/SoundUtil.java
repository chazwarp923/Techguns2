package techguns.util;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import techguns.client.audio.TGSoundCategory;
import techguns.packets.PacketPlaySound;
import techguns.packets.TGPackets;

/**
 * Sound helpers (1.12 SoundUtil). When called on both sides, the server sends a PacketPlaySound to everyone around
 * (the acting player skips it unless playOnOwnPlayer), and the client plays it right away for the acting player.
 * Sounds are TGSounds on the client: moving ones follow the entity, gun position ones sit beside the head, and an
 * EntityCondition can stop them early.
 */
public class SoundUtil {

	/** 1.12 sent sound packets to players within 100 blocks */
	private static final double PACKET_RANGE = 100.0;

	/** client side sound playback, installed during client setup (no-op on a dedicated server) */
	public interface ClientPlayer {
		void play(Entity entity, SoundEvent sound, float volume, float pitch, boolean repeat, boolean moving, boolean gunPosition,
				TGSoundCategory category, EntityCondition condition);
	}

	private static ClientPlayer clientPlayer = null;

	public static void setClientPlayer(ClientPlayer p) {
		clientPlayer = p;
	}

	public static void playOnClient(Entity entity, SoundEvent sound, float volume, float pitch, boolean repeat, boolean moving, boolean gunPosition,
			TGSoundCategory category, EntityCondition condition) {
		if (clientPlayer != null) {
			clientPlayer.play(entity, sound, volume, pitch, repeat, moving, gunPosition, category, condition);
		}
	}

	private static void play(Level level, Entity entity, @Nullable SoundEvent sound, float volume, float pitch, boolean repeat, boolean moving,
			boolean gunPosition, boolean playOnOwnPlayer, TGSoundCategory category, EntityCondition condition) {
		if (sound == null) {
			return;
		}
		if (level.isClientSide) {
			playOnClient(entity, sound, volume, pitch, repeat, moving, gunPosition, category, condition);
		} else {
			ResourceLocation id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
			if (id == null) {
				id = sound.getLocation();
			}
			TGPackets.sendToAllAround(new PacketPlaySound(id, entity.getId(), volume, pitch, repeat, moving, gunPosition, playOnOwnPlayer, category.getId()),
					entity, PACKET_RANGE);
		}
	}

	/** Plays a sound in front of the entity; stops early when the condition fails (on the acting client). */
	public static void playSoundOnEntityGunPosition(Level level, Entity entity, @Nullable SoundEvent sound, float volume, float pitch, boolean repeat,
			boolean moving, boolean playOnOwnPlayer, TGSoundCategory category, EntityCondition condition) {
		play(level, entity, sound, volume, pitch, repeat, moving, true, playOnOwnPlayer, category, condition);
	}

	/** Plays a sound in front of the entity. */
	public static void playSoundOnEntityGunPosition(Level level, Entity entity, @Nullable SoundEvent sound, float volume, float pitch, boolean repeat,
			boolean moving, boolean playOnOwnPlayer, TGSoundCategory category) {
		play(level, entity, sound, volume, pitch, repeat, moving, true, playOnOwnPlayer, category, EntityCondition.NONE);
	}

	public static void playSoundOnEntityGunPosition(Level level, Entity entity, @Nullable SoundEvent sound, float volume, float pitch, boolean repeat,
			boolean moving, TGSoundCategory category) {
		playSoundOnEntityGunPosition(level, entity, sound, volume, pitch, repeat, moving, false, category);
	}

	private static long lastReloadSoundPlayed = 0;

	/** Like playSoundOnEntityGunPosition() but avoids double reload sounds on the client. */
	public static void playReloadSoundOnEntity(Level level, Entity entity, @Nullable SoundEvent sound, float volume, float pitch, boolean repeat,
			boolean moving, TGSoundCategory category) {
		if (level.isClientSide) {
			long now = System.currentTimeMillis();
			if (now - lastReloadSoundPlayed < 500) {
				return;
			}
			lastReloadSoundPlayed = now;
		}
		playSoundOnEntityGunPosition(level, entity, sound, volume, pitch, repeat, moving, false, category);
	}

	/** Plays a sound that follows the entity. */
	public static void playSoundOnEntity(Level level, Entity entity, @Nullable SoundEvent sound, float volume, float pitch, boolean repeat,
			TGSoundCategory category) {
		play(level, entity, sound, volume, pitch, repeat, true, false, false, category, EntityCondition.NONE);
	}

	/** Plays a non-moving sound at the entity's position. */
	public static void playSoundAtEntityPos(Level level, Entity entity, @Nullable SoundEvent sound, float volume, float pitch, boolean repeat,
			TGSoundCategory category) {
		play(level, entity, sound, volume, pitch, repeat, false, false, false, category, EntityCondition.NONE);
	}
}
