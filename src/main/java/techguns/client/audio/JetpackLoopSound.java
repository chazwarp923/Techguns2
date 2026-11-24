package techguns.client.audio;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.util.SoundUtil;

/**
 * Jetpack sounds (1.12 TGExtendedPlayerClient#setJumpkeyPressed): when a player's boost starts, jetpack.start plays
 * and jetpack.loop loops on them; when it stops, the loop ends and jetpack.end plays. All at 0.75 volume, for every
 * player the client sees (the boost state of others comes from PacketJetpackState).
 */
public class JetpackLoopSound extends AbstractTickableSoundInstance {
	private static final float VOLUME = 0.75f;

	private final Player player;

	public JetpackLoopSound(Player player) {
		super(TGSounds.JETPACK_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.player = player;
		this.looping = true;
		this.delay = 0;
		this.volume = VOLUME;
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	@Override
	public void tick() {
		if (player.isRemoved() || !TGExtendedPlayer.get(player).isJumpkeyPressed) {
			this.stop();
			return;
		}
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
	public static class Handler {
		private static final Map<Player, JetpackLoopSound> PLAYING = new WeakHashMap<>();

		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) {
				PLAYING.clear();
				return;
			}
			// boost ended: stop the loop, play the end sound (not for players that left)
			PLAYING.entrySet().removeIf(e -> {
				Player p = e.getKey();
				if (!p.isRemoved() && TGExtendedPlayer.get(p).isJumpkeyPressed) {
					return false;
				}
				e.getValue().stop();
				if (!p.isRemoved() && p.level() == mc.level) {
					SoundUtil.playSoundAtEntityPos(p.level(), p, TGSounds.JETPACK_END.get(), VOLUME, 1.0f, false, TGSoundCategory.PLAYER_EFFECT);
				}
				return true;
			});
			for (Player p : mc.level.players()) {
				if (TGExtendedPlayer.get(p).isJumpkeyPressed && !PLAYING.containsKey(p)) {
					JetpackLoopSound s = new JetpackLoopSound(p);
					PLAYING.put(p, s);
					SoundUtil.playSoundAtEntityPos(p.level(), p, TGSounds.JETPACK_START.get(), VOLUME, 1.0f, false, TGSoundCategory.PLAYER_EFFECT);
					mc.getSoundManager().play(s);
				}
			}
		}
	}
}
