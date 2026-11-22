package techguns.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.Techguns;
import techguns.api.guns.GunManager;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.guns.GenericGun;
import techguns.packets.PacketTGKeybindPress;
import techguns.util.InventoryUtil;

/** Techguns key bindings (1.12 TGKeybinds). */
public class TGKeybinds {

	private static final String CATEGORY = "techguns.key.categories.techguns";

	public static final KeyMapping TOGGLE_NIGHTVISION = key("techguns.key.toggleNightvision", GLFW.GLFW_KEY_N);
	public static final KeyMapping TOGGLE_SAFEMODE = key("techguns.key.toggleSafemode", GLFW.GLFW_KEY_B);
	public static final KeyMapping TOGGLE_STEPASSIST = key("techguns.key.toggleStepassist", GLFW.GLFW_KEY_V);
	public static final KeyMapping FORCE_RELOAD = key("techguns.key.forceReload", GLFW.GLFW_KEY_R);
	public static final KeyMapping TOGGLE_JETPACK = key("techguns.key.toggleJetpack", GLFW.GLFW_KEY_J);

	private static KeyMapping key(String name, int code) {
		return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
	}

	@EventBusSubscriber(modid = Techguns.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class Registration {
		@SubscribeEvent
		public static void register(RegisterKeyMappingsEvent event) {
			event.register(TOGGLE_NIGHTVISION);
			event.register(TOGGLE_SAFEMODE);
			event.register(TOGGLE_STEPASSIST);
			event.register(FORCE_RELOAD);
			event.register(TOGGLE_JETPACK);
		}
	}

	@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
	public static class Input {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Player ply = Minecraft.getInstance().player;
			if (ply == null) {
				return;
			}
			while (TOGGLE_NIGHTVISION.consumeClick()) {
				send(PacketTGKeybindPress.TOGGLE_NIGHTVISION);
			}
			while (TOGGLE_SAFEMODE.consumeClick()) {
				send(PacketTGKeybindPress.TOGGLE_SAFEMODE);
			}
			while (TOGGLE_JETPACK.consumeClick()) {
				send(PacketTGKeybindPress.TOGGLE_JETPACK);
			}
			while (TOGGLE_STEPASSIST.consumeClick()) {
				send(PacketTGKeybindPress.TOGGLE_STEP_ASSIST);
			}
			while (FORCE_RELOAD.consumeClick()) {
				forceReload(ply);
			}
		}
	}

	private static void send(byte id) {
		PacketDistributor.sendToServer(new PacketTGKeybindPress(id));
	}

	private static void forceReload(Player ply) {
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		ItemStack main = ply.getMainHandItem();
		ItemStack off = ply.getOffhandItem();
		boolean canMain = canReloadGun(props, ply, main, InteractionHand.MAIN_HAND);
		boolean canOff = GunManager.canUseOffhand(main, off, ply) && canReloadGun(props, ply, off, InteractionHand.OFF_HAND);
		if (!canMain && !canOff) {
			return;
		}
		InteractionHand hand;
		if (!canOff) {
			hand = InteractionHand.MAIN_HAND;
		} else if (!canMain) {
			hand = InteractionHand.OFF_HAND;
		} else {
			// both reloadable: the one with less ammo left
			double pMain = ((GenericGun) main.getItem()).getPercentAmmoLeft(main);
			double pOff = ((GenericGun) off.getItem()).getPercentAmmoLeft(off);
			hand = pOff < pMain ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		}
		ItemStack gun = ply.getItemInHand(hand);
		PacketDistributor.sendToServer(new PacketTGKeybindPress(PacketTGKeybindPress.FORCE_RELOAD, false, hand == InteractionHand.OFF_HAND));
		((GenericGun) gun.getItem()).tryForcedReload(gun, ply.level(), ply, hand);
	}

	private static boolean canReloadGun(TGExtendedPlayer props, Player ply, ItemStack stack, InteractionHand hand) {
		if (stack.getItem() instanceof GenericGun gun) {
			if (props.getFireDelay(hand) <= 0 && !gun.isFullyLoaded(stack) && ply.getUseItem() != stack) {
				for (ItemStack s : gun.getReloadItem(stack)) {
					if (!InventoryUtil.canConsumeAmmoPlayer(ply, s)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
}
