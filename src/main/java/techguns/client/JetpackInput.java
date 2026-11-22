package techguns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.capabilities.TGExtendedPlayer;
import techguns.packets.PacketTGKeybindPress;

/** Client side of the jetpack: the local player's jump key starts / stops the boost (1.12 ItemJetpack client part). */
public final class JetpackInput {

	private JetpackInput() {
	}

	public static void tickLocalPlayer(Player ply, TGExtendedPlayer props, boolean hasFuel) {
		Minecraft mc = Minecraft.getInstance();
		if (ply != mc.player) {
			return;
		}
		boolean pressed = hasFuel && props.enableJetpack && mc.options.keyJump.isDown();
		if (pressed != props.isJumpkeyPressed) {
			props.isJumpkeyPressed = pressed;
			PacketDistributor.sendToServer(
					new PacketTGKeybindPress(pressed ? PacketTGKeybindPress.JETPACK_BOOST_START : PacketTGKeybindPress.JETPACK_BOOST_STOP, false, false));
		}
	}
}
