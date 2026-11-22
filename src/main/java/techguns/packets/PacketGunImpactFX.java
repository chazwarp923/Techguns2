package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.util.TGFXHooks;

/** Server -> client: bullet impact sound and particles. */
public record PacketGunImpactFX(short soundType, double x, double y, double z, float pitch, float yaw, boolean incendiary, boolean blue)
		implements CustomPacketPayload {

	public static final Type<PacketGunImpactFX> TYPE = new Type<>(Techguns.rl("gun_impact_fx"));

	public static final StreamCodec<FriendlyByteBuf, PacketGunImpactFX> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeShort(m.soundType);
		buf.writeDouble(m.x);
		buf.writeDouble(m.y);
		buf.writeDouble(m.z);
		buf.writeFloat(m.pitch);
		buf.writeFloat(m.yaw);
		buf.writeBoolean(m.incendiary);
		buf.writeBoolean(m.blue);
	}, buf -> new PacketGunImpactFX(buf.readShort(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(),
			buf.readBoolean(), buf.readBoolean()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketGunImpactFX m, IPayloadContext ctx) {
		Level level = ctx.player().level();
		double x = m.x, y = m.y, z = m.z;
		if (m.incendiary) {
			TGFXHooks.createFX("Impact_IncendiaryBullet", level, x, y, z, 0.0D, 0.0D, 0.0D, m.pitch, m.yaw);
		}
		SoundEvent sound;
		String fx;
		switch (m.soundType) {
			case 0 -> { sound = TGSounds.BULLET_IMPACT_STONE.get(); fx = "Impact_BulletRock"; }
			case 1 -> { sound = TGSounds.BULLET_IMPACT_WOOD.get(); fx = "Impact_BulletWood"; }
			case 2 -> { sound = TGSounds.BULLET_IMPACT_GLASS.get(); fx = "Impact_BulletGlass"; }
			case 3 -> { sound = TGSounds.BULLET_IMPACT_METAL.get(); fx = "Impact_BulletMetal"; }
			case 4 -> { sound = TGSounds.BULLET_IMPACT_DIRT.get(); fx = "Impact_BulletDirt"; }
			case 5 -> { sound = TGSounds.BULLET_IMPACT_DIRT.get(); fx = null; }
			default -> { sound = TGSounds.BULLET_IMPACT_DIRT.get(); fx = "Impact_BulletDefault"; }
		}
		level.playLocalSound(x, y, z, sound, SoundSource.AMBIENT, 1.0f, 1.0f, true);
		if (fx != null) {
			if (m.blue) fx = fx + "_Blue";
			TGFXHooks.createFX(fx, level, x, y, z, 0.0D, 0.0D, 0.0D, m.pitch, m.yaw);
		}
	}
}
