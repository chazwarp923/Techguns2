package techguns.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.Techguns;
import techguns.api.guns.GunManager;
import techguns.api.guns.IGenericGun;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.guns.GenericGun;
import techguns.packets.PacketShootGun;

/**
 * Client fire input (was TGEventHandler#onMouseEvent + TGTickHandler#localClientPlayerTick).
 * Left mouse fires the main hand gun, right mouse an off hand gun; the shot is simulated
 * locally (sound, recoil, ammo) and sent to the server which spawns the projectiles.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class ClientGunInput {

	public static boolean keyFirePressedMainhand = false;
	public static boolean keyFirePressedOffhand = false;

	private static boolean inGameHasFocus(Minecraft mc) {
		return mc.screen == null && mc.isWindowActive() && mc.mouseHandler.isMouseGrabbed();
	}

	@SubscribeEvent
	public static void onMouseButton(InputEvent.MouseButton.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer ply = mc.player;
		if (ply == null || mc.screen != null) {
			return;
		}
		boolean pressed = event.getAction() != GLFW.GLFW_RELEASE;
		int button = event.getButton();

		ItemStack main = ply.getMainHandItem();
		ItemStack off = ply.getOffhandItem();

		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && main.getItem() instanceof IGenericGun gun) {
			if (gun.isShootWithLeftClick()) {
				keyFirePressedMainhand = pressed;
				event.setCanceled(true);
			} else if (pressed && ShooterValues.getReloadtime(ply, false) - System.currentTimeMillis() > 0) {
				// can't mine/attack while reloading
				event.setCanceled(true);
			}
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !ply.isShiftKeyDown() && off.getItem() instanceof IGenericGun gun
				&& GunManager.canUseOffhand(ply)) {
			if (gun.isShootWithLeftClick()) {
				keyFirePressedOffhand = pressed;
				event.setCanceled(true);
			} else if (pressed && ShooterValues.getReloadtime(ply, true) - System.currentTimeMillis() > 0) {
				event.setCanceled(true);
			}
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && main.getItem() instanceof techguns.items.guns.GenericGunCharge charge
				&& charge.getLockOnTicks() > 0) {
			// lock-on weapons: pressing or releasing right mouse resets the lock
			TGExtendedPlayer props = TGExtendedPlayer.get(ply);
			props.lockOnEntity = null;
			props.lockOnTicks = -1;
		}
	}

	@SubscribeEvent
	public static void localClientPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		Minecraft mc = Minecraft.getInstance();
		if (!player.level().isClientSide || player != mc.player) {
			return;
		}
		if (!inGameHasFocus(mc) || player.isSpectator()) {
			keyFirePressedMainhand = false;
			keyFirePressedOffhand = false;
			return;
		}
		TGExtendedPlayer props = TGExtendedPlayer.get(player);

		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() instanceof IGenericGun gun && gun.isShootWithLeftClick()) {
			if (keyFirePressedMainhand) {
				if (props.getFireDelay(InteractionHand.MAIN_HAND) <= 0) {
					Entity target = null;
					if (gun instanceof GenericGun g && g.getLockOnTicks() > 0 && props.lockOnEntity != null && props.lockOnTicks > g.getLockOnTicks()) {
						target = props.lockOnEntity;
					}
					fire(gun, stack, player, InteractionHand.MAIN_HAND, target);
				}
				if (gun.isSemiAuto()) {
					keyFirePressedMainhand = false;
				}
			}
		} else {
			keyFirePressedMainhand = false;
		}

		ItemStack stackOff = player.getOffhandItem();
		if (stackOff.getItem() instanceof IGenericGun gun && gun.isShootWithLeftClick()) {
			if (keyFirePressedOffhand) {
				if (props.getFireDelay(InteractionHand.OFF_HAND) <= 0) {
					fire(gun, stackOff, player, InteractionHand.OFF_HAND, null);
				}
				if (gun.isSemiAuto()) {
					keyFirePressedOffhand = false;
				}
			}
		} else {
			keyFirePressedOffhand = false;
		}

		if (stack.getItem() instanceof GenericGun g && g.getLockOnTicks() > 0 && g.getAmmoLeft(stack) <= 0 && props.lockOnEntity != null) {
			props.lockOnEntity = null;
			props.lockOnTicks = -1;
		}
	}

	private static void fire(IGenericGun gun, ItemStack stack, Player player, InteractionHand hand, Entity target) {
		boolean zooming = gun.isZooming();
		PacketDistributor.sendToServer(new PacketShootGun(zooming, hand, target));
		gun.shootGunPrimary(stack, player.level(), player, zooming, hand, target);
	}
}
