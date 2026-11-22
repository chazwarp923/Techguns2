package techguns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.packets.GunFiredMessage;
import techguns.packets.ReloadStartedMessage;

/** Client-only packet handling; only referenced from lambdas so the server never loads it. */
public class ClientPacketHandlers {

	public static void handleGunFired(GunFiredMessage m, IPayloadContext ctx) {
		Entity e = ctx.player().level().getEntity(m.entityId());
		if (e instanceof LivingEntity shooter) {
			if (!m.checkRecoil() || !ShooterValues.isStillRecoiling(shooter, m.offhand(), m.attackType())) {
				ShooterValues.setRecoiltime(shooter, m.offhand(), System.currentTimeMillis() + m.time(), m.time(), m.attackType());
				ShooterValues.setMuzzleFlashTime(shooter, m.offhand(), System.currentTimeMillis() + m.muzzleFlashTime(), m.muzzleFlashTime());
			}
		}
	}

	public static void handleReloadStarted(ReloadStartedMessage m, IPayloadContext ctx) {
		Entity e = ctx.player().level().getEntity(m.entityId());
		if (e instanceof LivingEntity shooter && shooter != Minecraft.getInstance().player) {
			ShooterValues.setReloadtime(shooter, m.offHand(), System.currentTimeMillis() + m.time(), m.time(), m.attackType());
		}
	}
}
