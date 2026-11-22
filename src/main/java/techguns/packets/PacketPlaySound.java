package techguns.packets;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.client.audio.TGSoundCategory;
import techguns.util.EntityCondition;
import techguns.util.SoundUtil;

/** Server -> client: play a Techguns sound on an entity (1.12 PacketPlaySound). */
public record PacketPlaySound(ResourceLocation sound, int entityId, float volume, float pitch, boolean repeat, boolean moving, boolean gunPosition,
		boolean playOnOwnPlayer, int category) implements CustomPacketPayload {

	public static final Type<PacketPlaySound> TYPE = new Type<>(Techguns.rl("play_sound"));

	public static final StreamCodec<FriendlyByteBuf, PacketPlaySound> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeResourceLocation(m.sound);
		buf.writeVarInt(m.entityId);
		buf.writeFloat(m.volume);
		buf.writeFloat(m.pitch);
		buf.writeBoolean(m.repeat);
		buf.writeBoolean(m.moving);
		buf.writeBoolean(m.gunPosition);
		buf.writeBoolean(m.playOnOwnPlayer);
		buf.writeVarInt(m.category);
	}, buf -> new PacketPlaySound(buf.readResourceLocation(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean(),
			buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketPlaySound m, IPayloadContext ctx) {
		Entity entity = ctx.player().level().getEntity(m.entityId);
		SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(m.sound);
		TGSoundCategory category = TGSoundCategory.get(m.category);
		if (entity == null || sound == null || category == null) {
			return;
		}
		if (entity != ctx.player() || m.playOnOwnPlayer) {
			// as in 1.12, sounds from the server carry no stop condition (it is only known to the acting client)
			SoundUtil.playOnClient(entity, sound, m.volume, m.pitch, m.repeat, m.moving, m.gunPosition, category, EntityCondition.NONE);
		}
	}
}
