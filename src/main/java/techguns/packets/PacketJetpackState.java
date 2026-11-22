package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;

/** Server -> client: another player started / stopped the jetpack boost (for the flame effect). */
public record PacketJetpackState(int entityId, boolean boosting) implements CustomPacketPayload {

	public static final Type<PacketJetpackState> TYPE = new Type<>(Techguns.rl("jetpack_state"));

	public static final StreamCodec<FriendlyByteBuf, PacketJetpackState> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeVarInt(m.entityId);
		buf.writeBoolean(m.boosting);
	}, buf -> new PacketJetpackState(buf.readVarInt(), buf.readBoolean()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketJetpackState m, IPayloadContext ctx) {
		ctx.enqueueWork(() -> {
			Entity e = ctx.player().level().getEntity(m.entityId);
			if (e instanceof Player p && p != ctx.player()) {
				TGExtendedPlayer.get(p).isJumpkeyPressed = m.boosting;
			}
		});
	}
}
