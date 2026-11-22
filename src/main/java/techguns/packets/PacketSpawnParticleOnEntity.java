package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.util.EntityCondition;
import techguns.util.TGFXHooks;

/** Server -> client: spawn a Techguns FX attached to an entity. */
public record PacketSpawnParticleOnEntity(String name, int entityID, float offsetX, float offsetY, float offsetZ, boolean attachToHead,
		EntityCondition condition) implements CustomPacketPayload {

	public static final Type<PacketSpawnParticleOnEntity> TYPE = new Type<>(Techguns.rl("spawn_particle_on_entity"));

	public static final StreamCodec<FriendlyByteBuf, PacketSpawnParticleOnEntity> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeUtf(m.name);
		buf.writeVarInt(m.entityID);
		buf.writeFloat(m.offsetX);
		buf.writeFloat(m.offsetY);
		buf.writeFloat(m.offsetZ);
		buf.writeBoolean(m.attachToHead);
		buf.writeByte(m.condition == null ? EntityCondition.NONE.id : m.condition.id);
	}, buf -> new PacketSpawnParticleOnEntity(buf.readUtf(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(),
			EntityCondition.fromByte(buf.readByte())));

	public PacketSpawnParticleOnEntity(String name, Entity ent) {
		this(name, ent, 0.0f, 0.0f, 0.0f, false, EntityCondition.NONE);
	}

	public PacketSpawnParticleOnEntity(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead) {
		this(name, ent, offsetX, offsetY, offsetZ, attachToHead, EntityCondition.NONE);
	}

	public PacketSpawnParticleOnEntity(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead, EntityCondition condition) {
		this(name, ent.getId(), offsetX, offsetY, offsetZ, attachToHead, condition);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketSpawnParticleOnEntity m, IPayloadContext ctx) {
		Entity ent = ctx.player().level().getEntity(m.entityID);
		if (ent != null) {
			TGFXHooks.createFXOnEntityWithOffset(m.name, ent, m.offsetX, m.offsetY, m.offsetZ, m.attachToHead, m.condition);
		} else {
			Techguns.LOGGER.debug("Got packet for FX {} on entity, but entity was null", m.name);
		}
	}
}
