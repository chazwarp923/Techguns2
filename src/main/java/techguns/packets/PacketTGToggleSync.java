package techguns.packets;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.util.TextUtil;

/**
 * Server -> client: the player's toggle states (jetpack, night vision, safe mode, step assist), optionally with the
 * "[Techguns]: changed [X] to [On]" message of the key that changed one (1.12 PacketShowKeybindConfirmMessage).
 */
public record PacketTGToggleSync(boolean jetpack, boolean nightVision, boolean safemode, boolean stepAssist, byte messageId, boolean state)
		implements CustomPacketPayload {

	public static final Type<PacketTGToggleSync> TYPE = new Type<>(Techguns.rl("toggle_sync"));

	public static final StreamCodec<FriendlyByteBuf, PacketTGToggleSync> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeBoolean(m.jetpack);
		buf.writeBoolean(m.nightVision);
		buf.writeBoolean(m.safemode);
		buf.writeBoolean(m.stepAssist);
		buf.writeByte(m.messageId);
		buf.writeBoolean(m.state);
	}, buf -> new PacketTGToggleSync(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readByte(), buf.readBoolean()));

	public static PacketTGToggleSync of(TGExtendedPlayer props, int messageId, boolean state) {
		return new PacketTGToggleSync(props.enableJetpack, props.enableNightVision, props.enableSafemode, props.enableStepAssist, (byte) messageId,
				state);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static String langKey(int id) {
		return switch (id) {
			case PacketTGKeybindPress.TOGGLE_NIGHTVISION -> "armorTooltip.nightvision";
			case PacketTGKeybindPress.TOGGLE_JETPACK -> "msg.enablejetpack";
			case PacketTGKeybindPress.TOGGLE_SAFEMODE -> "msg.safemode";
			case PacketTGKeybindPress.TOGGLE_STEP_ASSIST -> "armorTooltip.stepassist";
			default -> null;
		};
	}

	public static void handle(PacketTGToggleSync m, IPayloadContext ctx) {
		ctx.enqueueWork(() -> {
			Player ply = ctx.player();
			TGExtendedPlayer props = TGExtendedPlayer.get(ply);
			props.enableJetpack = m.jetpack;
			props.enableNightVision = m.nightVision;
			props.enableSafemode = m.safemode;
			props.enableStepAssist = m.stepAssist;
			String key = langKey(m.messageId);
			if (key != null) {
				Component msg = Component.literal("[Techguns]: ").withStyle(ChatFormatting.YELLOW)
						.append(TextUtil.transTG("msg.keybindtogglechange").withStyle(ChatFormatting.WHITE))
						.append(Component.literal(" [").append(TextUtil.transTG(key)).append("] ").withStyle(ChatFormatting.YELLOW))
						.append(TextUtil.transTG("msg.keybindtogglechange2").withStyle(ChatFormatting.WHITE)).append(" ")
						.append(Component.literal("[").append(TextUtil.transTG("container.info." + (m.state ? "on" : "off"))).append("]")
								.withStyle(m.state ? ChatFormatting.GREEN : ChatFormatting.DARK_RED));
				ply.displayClientMessage(msg, true);
			}
		});
	}
}
