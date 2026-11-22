package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;

/** Server -> client: a player swapped the guns in their hands, swap their attack times / fire delays (1.12 PacketSwapWeapon). */
public record PacketSwapWeapon(int playerId) implements CustomPacketPayload {

	public static final Type<PacketSwapWeapon> TYPE = new Type<>(Techguns.rl("swap_weapon"));

	public static final StreamCodec<FriendlyByteBuf, PacketSwapWeapon> CODEC = StreamCodec.of((buf, m) -> buf.writeVarInt(m.playerId),
			buf -> new PacketSwapWeapon(buf.readVarInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketSwapWeapon m, IPayloadContext ctx) {
		Entity ent = ctx.player().level().getEntity(m.playerId);
		if (ent instanceof Player ply) {
			TGExtendedPlayer.get(ply).swapAttackTimes();
		}
	}
}
