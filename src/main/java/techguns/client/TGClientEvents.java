package techguns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import techguns.Techguns;
import techguns.api.guns.IGenericGun;
import techguns.client.render.TGItemRenderer;
import techguns.items.guns.GenericGun;

/** Client game-bus events for gun rendering and zoom. */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class TGClientEvents {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
		if (!event.isCanceled()) {
			TGItemRenderer.currentEntity = event.getEntity();
		}
	}

	@SubscribeEvent
	public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
		TGItemRenderer.currentEntity = null;
	}

	/**
	 * Zoom: toggle zoom via TGClientState.playerZoom, hold-zoom guns while right mouse is held.
	 * Also the speed FOV lock (1.12 handleFovEvent, config client.LockSpeedDependantFov): cancels vanilla's
	 * speed-dependent FOV change and uses a fixed multiplier while sprinting instead. Low priority, as in 1.12, so
	 * other mods' changes are less likely to undo it.
	 */
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onComputeFov(ComputeFovModifierEvent event) {
		Player ply = event.getPlayer();
		float f = SpeedFovLock.factor(ply);
		ItemStack stack = ply.getItemInHand(InteractionHand.MAIN_HAND);
		if (stack.getItem() instanceof GenericGun gun && gun.canZoom()) {
			Minecraft mc = Minecraft.getInstance();
			if (gun.isHoldZoom()) {
				TGClientState.playerZoom = mc.options.keyUse.isDown() && !ShooterValues.getPlayerIsReloading(ply, false) ? gun.getZoomMult() : 1.0f;
			}
			f *= TGClientState.playerZoom;
		} else {
			TGClientState.playerZoom = 1.0f;
		}
		if (f != 1.0f) {
			event.setNewFovModifier(event.getNewFovModifier() * f);
		}
	}


	@SubscribeEvent
	public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
		if (Minecraft.getInstance().level != null) {
			techguns.client.particle.TGParticleManager.INSTANCE.tickParticles();
			if (!Minecraft.getInstance().isPaused()) {
				techguns.client.particle.TGEntityParticles.tickAll();
			}
		}
	}

	@SubscribeEvent
	public static void onRenderLevelStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
		if (event.getStage() == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			techguns.client.particle.TGParticleManager.INSTANCE.renderParticles(event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
		}
	}

	@SubscribeEvent
	public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
		techguns.client.particle.TGParticleManager.INSTANCE.clear();
		techguns.client.particle.TGEntityParticles.clearAll();
	}

	public static boolean isHoldingGun(Player ply) {
		return ply.getMainHandItem().getItem() instanceof IGenericGun || ply.getOffhandItem().getItem() instanceof IGenericGun;
	}
}
