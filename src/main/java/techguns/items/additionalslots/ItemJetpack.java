package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import techguns.TGItems;
import techguns.api.tginventory.TGSlotType;
import techguns.capabilities.TGExtendedPlayer;

/**
 * Back slot: hold jump to fly up (jetpack toggle), glides like the glider. The local client reads the jump key and
 * tells the server (PacketTGKeybindPress JETPACK_BOOST_START/STOP), which shares it with players that see the flames.
 */
public class ItemJetpack extends ItemTGSpecialSlot {

	public ItemJetpack(int camoCount, int dur) {
		super(TGSlotType.BACKSLOT, camoCount, dur, TGItems.FUEL_TANK, TGItems.FUEL_TANK_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack item, Player ply) {
		if (!hasCharge(item)) {
			tryReloadAndRepair(item, ply);
		}
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		if (ply.level().isClientSide) {
			techguns.client.JetpackInput.tickLocalPlayer(ply, props, hasCharge(item));
		}
		if (hasCharge(item)) {
			if (props.isJumpkeyPressed) {
				applyBoostToPlayer(ply, item);
			}
			ItemGlider.glide(ply);
		}
	}

	protected void applyBoostToPlayer(Player ply, ItemStack item) {
		Vec3 m = ply.getDeltaMovement();
		ply.setDeltaMovement(m.x, Math.min(m.y + 0.15f, 0.4f), m.z);
		ply.resetFallDistance();
		useCharge(item, 1);
	}
}
