package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import techguns.Techguns;

/** Server -> client: an entity fired a gun (recoil animation + muzzle flash). */
public record GunFiredMessage(int entityId, int time, int muzzleFlashTime, byte attackType, boolean checkRecoil, boolean offhand)
		implements CustomPacketPayload {

	public static final Type<GunFiredMessage> TYPE = new Type<>(Techguns.rl("gun_fired"));

	public static final StreamCodec<FriendlyByteBuf, GunFiredMessage> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeInt(m.entityId);
		buf.writeInt(m.time);
		buf.writeInt(m.muzzleFlashTime);
		buf.writeByte(m.attackType);
		buf.writeBoolean(m.checkRecoil);
		buf.writeBoolean(m.offhand);
	}, buf -> new GunFiredMessage(buf.readInt(), buf.readInt(), buf.readInt(), buf.readByte(), buf.readBoolean(), buf.readBoolean()));

	public GunFiredMessage(LivingEntity shooter, int firetime, int muzzleflashtime, int attackType, boolean checkRecoil, boolean offhand) {
		this(shooter.getId(), firetime, muzzleflashtime, (byte) attackType, checkRecoil, offhand);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
