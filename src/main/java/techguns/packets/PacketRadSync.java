package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;

/** Server -> client: the player's RAD level. */
public record PacketRadSync(int radlevel) implements CustomPacketPayload {

	public static final Type<PacketRadSync> TYPE = new Type<>(Techguns.rl("rad_sync"));

	public static final StreamCodec<FriendlyByteBuf, PacketRadSync> CODEC = StreamCodec.of((buf, m) -> buf.writeVarInt(m.radlevel),
			buf -> new PacketRadSync(buf.readVarInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketRadSync m, IPayloadContext ctx) {
		ctx.enqueueWork(() -> {
			if (ctx.player() != null) {
				TGExtendedPlayer.get(ctx.player()).radlevel = m.radlevel;
			}
		});
	}
}
