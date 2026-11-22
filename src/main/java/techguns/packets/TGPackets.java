package techguns.packets;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import techguns.client.ClientPacketHandlers;

/** Network payload registration (mod bus). */
public class TGPackets {

	@SubscribeEvent
	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar r = event.registrar("1");
		r.playToServer(PacketShootGun.TYPE, PacketShootGun.CODEC, PacketShootGun::handle);
		r.playToServer(PacketTGKeybindPress.TYPE, PacketTGKeybindPress.CODEC, PacketTGKeybindPress::handle);
		r.playToClient(PacketRadSync.TYPE, PacketRadSync.CODEC, PacketRadSync::handle);
		r.playToClient(PacketTGToggleSync.TYPE, PacketTGToggleSync.CODEC, PacketTGToggleSync::handle);
		r.playToClient(PacketJetpackState.TYPE, PacketJetpackState.CODEC, PacketJetpackState::handle);
		r.playToClient(PacketGunImpactFX.TYPE, PacketGunImpactFX.CODEC, PacketGunImpactFX::handle);
		r.playToClient(PacketSpawnParticle.TYPE, PacketSpawnParticle.CODEC, PacketSpawnParticle::handle);
		r.playToClient(PacketEntityDeathType.TYPE, PacketEntityDeathType.CODEC, PacketEntityDeathType::handle);
		r.playToClient(PacketSwapWeapon.TYPE, PacketSwapWeapon.CODEC, PacketSwapWeapon::handle);
		r.playToClient(PacketPlaySound.TYPE, PacketPlaySound.CODEC, PacketPlaySound::handle);
		r.playToClient(PacketSpawnParticleOnEntity.TYPE, PacketSpawnParticleOnEntity.CODEC, PacketSpawnParticleOnEntity::handle);
		r.playToClient(GunFiredMessage.TYPE, GunFiredMessage.CODEC, (m, ctx) -> ClientPacketHandlers.handleGunFired(m, ctx));
		r.playToClient(ReloadStartedMessage.TYPE, ReloadStartedMessage.CODEC, (m, ctx) -> ClientPacketHandlers.handleReloadStarted(m, ctx));
	}

	/** Was network.sendToAllAround(msg, TargetPoint(level, x, y, z, range)). */
	public static void sendToAllAround(CustomPacketPayload msg, net.minecraft.world.level.Level level, double x, double y, double z, double range) {
		if (level instanceof ServerLevel sl) {
			PacketDistributor.sendToPlayersNear(sl, null, x, y, z, range, msg);
		}
	}

	/** Was network.sendToAllAround(msg, TargetPoint(entity, range)). */
	public static void sendToAllAround(CustomPacketPayload msg, Entity entity, double range) {
		if (entity.level() instanceof ServerLevel sl) {
			PacketDistributor.sendToPlayersNear(sl, null, entity.getX(), entity.getY(), entity.getZ(), range, msg);
		}
	}
}
