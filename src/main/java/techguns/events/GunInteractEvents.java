package techguns.events;

import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import techguns.Techguns;
import techguns.api.guns.GunManager;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.api.tginventory.TGSlotType;
import techguns.items.guns.GenericGun;
import techguns.util.InventoryUtil;

/** 1.12 TGEventHandler: off hand use with two handed guns, and the ammo returned by ammo change crafting. */
@EventBusSubscriber(modid = Techguns.MODID)
public class GunInteractEvents {

	/** no off hand actions while a two handed gun is held in either hand */
	private static boolean allowOffhandUse(Player player, InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND || GunManager.canUseOffhand(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (!allowOffhandUse(event.getEntity(), event.getHand())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.PASS);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player ply = event.getEntity();
		if (!allowOffhandUse(ply, event.getHand())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.PASS);
		} else if (event.getHand() == InteractionHand.MAIN_HAND) {
			// sneaking with a gun in the off hand and some other item in the main hand still uses the block
			ItemStack main = event.getItemStack();
			if (ply.isShiftKeyDown() && ply.getOffhandItem().getItem() instanceof GenericGun && !main.isEmpty() && !(main.getItem() instanceof GenericGun)) {
				event.setUseBlock(TriState.TRUE);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!allowOffhandUse(event.getEntity(), event.getHand())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.PASS);
		}
	}

	/**
	 * Ammo change crafting (gun + other ammo): the ammo still loaded in the gun goes back to the player. Fires before the
	 * grid is consumed, so the old gun with its old ammo is still in it.
	 */
	@SubscribeEvent
	public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
		Player ply = event.getEntity();
		if (ply.level().isClientSide || !(event.getCrafting().getItem() instanceof GenericGun)) {
			return;
		}
		ItemStack gun = ItemStack.EMPTY;
		boolean hasAmmo = false;
		var grid = event.getInventory();
		for (int i = 0; i < grid.getContainerSize(); i++) {
			ItemStack stack = grid.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.getItem() instanceof GenericGun && gun.isEmpty()) {
				gun = stack;
			} else if (stack.getItem() instanceof ITGSpecialSlot slot && slot.getSlot(stack) == TGSlotType.AMMOSLOT && !hasAmmo) {
				hasAmmo = true;
			} else {
				return; // not an ammo change recipe
			}
		}
		if (gun.isEmpty() || !hasAmmo) {
			return;
		}
		List<ItemStack> items = ((GenericGun) gun.getItem()).getAmmoOnUnload(gun);
		for (ItemStack i : items) {
			int amount = InventoryUtil.addAmmoToPlayerInventory(ply, i);
			if (amount > 0) {
				ply.level().addFreshEntity(new ItemEntity(ply.level(), ply.getX(), ply.getY(), ply.getZ(), i.copyWithCount(amount)));
			}
		}
	}
}
