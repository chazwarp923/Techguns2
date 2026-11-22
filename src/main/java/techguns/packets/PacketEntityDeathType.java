package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.deatheffects.DeathType;

/** Server -> client: an entity died a special death (gore / bio / laser), play the death effect. */
public record PacketEntityDeathType(int entityId, int deathType, float motionX, float motionY, float motionZ) implements CustomPacketPayload {

	public static final Type<PacketEntityDeathType> TYPE = new Type<>(Techguns.rl("entity_death_type"));

	public static final StreamCodec<FriendlyByteBuf, PacketEntityDeathType> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeVarInt(m.entityId);
		buf.writeByte(m.deathType);
		buf.writeFloat(m.motionX);
		buf.writeFloat(m.motionY);
		buf.writeFloat(m.motionZ);
	}, buf -> new PacketEntityDeathType(buf.readVarInt(), buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat()));

	public PacketEntityDeathType(LivingEntity entity, DeathType type) {
		this(entity.getId(), (type == null ? DeathType.DEFAULT : type).getValue(), (float) entity.getDeltaMovement().x,
				(float) entity.getDeltaMovement().y, (float) entity.getDeltaMovement().z);
	}

	public Vec3 motion() {
		return new Vec3(motionX, motionY, motionZ);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketEntityDeathType m, IPayloadContext ctx) {
		techguns.client.deatheffects.DeathEffectClient.onDeathPacket(m);
	}
}
