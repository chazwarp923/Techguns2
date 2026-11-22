package techguns.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import techguns.Techguns;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.guns.GenericGun;
import techguns.packets.PacketSwapWeapon;
import techguns.packets.TGPackets;

/** Common (both sides) player tick logic. */
@EventBusSubscriber(modid = Techguns.MODID)
public class TGTickHandler {

	@SubscribeEvent
	public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
		TGExtendedPlayer props = TGExtendedPlayer.get(event.getEntity());
		if (props.fireDelayMainhand > 0) {
			props.fireDelayMainhand--;
		}
		if (props.loopSoundDelayMainhand > 0) {
			props.loopSoundDelayMainhand--;
		}
		if (props.fireDelayOffhand > 0) {
			props.fireDelayOffhand--;
		}
		if (props.loopSoundDelayOffhand > 0) {
			props.loopSoundDelayOffhand--;
		}
		if (!event.getEntity().level().isClientSide && props.swingSoundDelay > 0) {
			props.swingSoundDelay--;
		}
		// armor bonuses: see ArmorEvents

		Player ply = event.getEntity();
		if (!ply.level().isClientSide) {
			detectWeaponSwap(ply, props);
		}
	}

	/**
	 * 1.12 weapon swap detection (server only): when the same gun stacks moved between the hands (swap hands key), swap
	 * the fire delays here and tell the clients around to swap attack times as well.
	 */
	private static void detectWeaponSwap(Player ply, TGExtendedPlayer props) {
		ItemStack gunMH = ply.getMainHandItem().getItem() instanceof GenericGun ? ply.getMainHandItem() : ItemStack.EMPTY;
		ItemStack gunOH = ply.getOffhandItem().getItem() instanceof GenericGun ? ply.getOffhandItem() : ItemStack.EMPTY;

		if (!gunOH.isEmpty() && props.gunMainHand == gunOH || !gunMH.isEmpty() && props.gunOffHand == gunMH) {
			TGPackets.sendToAllAround(new PacketSwapWeapon(ply.getId()), ply, 50.0);
			int i = props.fireDelayMainhand;
			props.fireDelayMainhand = props.fireDelayOffhand;
			props.fireDelayOffhand = i;
		}
		props.gunMainHand = gunMH;
		props.gunOffHand = gunOH;
	}
}
