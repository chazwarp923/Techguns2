package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.util.TGFXHooks;

/** Server -> client: spawn a Techguns FX at a position. */
public record PacketSpawnParticle(String name, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float scale)
		implements CustomPacketPayload {

	public static final Type<PacketSpawnParticle> TYPE = new Type<>(Techguns.rl("spawn_particle"));

	public static final StreamCodec<FriendlyByteBuf, PacketSpawnParticle> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeUtf(m.name);
		buf.writeDouble(m.posX);
		buf.writeDouble(m.posY);
		buf.writeDouble(m.posZ);
		buf.writeDouble(m.motionX);
		buf.writeDouble(m.motionY);
		buf.writeDouble(m.motionZ);
		buf.writeFloat(m.scale);
	}, buf -> new PacketSpawnParticle(buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
			buf.readDouble(), buf.readFloat()));

	public PacketSpawnParticle(String name, double posX, double posY, double posZ) {
		this(name, posX, posY, posZ, 0, 0, 0, 1.0f);
	}

	public PacketSpawnParticle(String name, double posX, double posY, double posZ, float scale) {
		this(name, posX, posY, posZ, 0, 0, 0, scale);
	}

	public PacketSpawnParticle(String name, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		this(name, posX, posY, posZ, motionX, motionY, motionZ, 1.0f);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketSpawnParticle m, IPayloadContext ctx) {
		TGFXHooks.createFX(m.name, ctx.player().level(), m.posX, m.posY, m.posZ, m.motionX, m.motionY, m.motionZ, m.scale);
	}
}
