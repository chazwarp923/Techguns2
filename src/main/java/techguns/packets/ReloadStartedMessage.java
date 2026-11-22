package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import techguns.Techguns;

/** Server -> client: an entity started reloading. */
public record ReloadStartedMessage(int entityId, int time, byte attackType, boolean offHand) implements CustomPacketPayload {

	public static final Type<ReloadStartedMessage> TYPE = new Type<>(Techguns.rl("reload_started"));

	public static final StreamCodec<FriendlyByteBuf, ReloadStartedMessage> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeInt(m.entityId);
		buf.writeInt(m.time);
		buf.writeByte(m.attackType);
		buf.writeBoolean(m.offHand);
	}, buf -> new ReloadStartedMessage(buf.readInt(), buf.readInt(), buf.readByte(), buf.readBoolean()));

	public ReloadStartedMessage(LivingEntity shooter, InteractionHand hand, int firetime, int attackType) {
		this(shooter.getId(), firetime, (byte) attackType, hand == InteractionHand.OFF_HAND);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
