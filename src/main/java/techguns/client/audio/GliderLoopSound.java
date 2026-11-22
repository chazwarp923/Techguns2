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

/** Looping wind sound while a player glides (1.12 ClientProxy#handlePlayerGliding). */
public class GliderLoopSound extends AbstractTickableSoundInstance {

	private final Player player;

	public GliderLoopSound(Player player) {
		super(TGSounds.GLIDER_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.player = player;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.75f;
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	@Override
	public void tick() {
		if (player.isRemoved() || !TGExtendedPlayer.get(player).isGliding) {
			this.stop();
			return;
		}
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
	public static class Handler {
		private static final Map<Player, GliderLoopSound> PLAYING = new WeakHashMap<>();

		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) {
				PLAYING.clear();
				return;
			}
			PLAYING.values().removeIf(GliderLoopSound::isStopped);
			for (Player p : mc.level.players()) {
				if (TGExtendedPlayer.get(p).isGliding && !PLAYING.containsKey(p)) {
					GliderLoopSound s = new GliderLoopSound(p);
					PLAYING.put(p, s);
					mc.getSoundManager().play(s);
				}
			}
		}
	}
}
