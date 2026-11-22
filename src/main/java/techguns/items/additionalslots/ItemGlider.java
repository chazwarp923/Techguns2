package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import techguns.api.tginventory.TGSlotType;
import techguns.capabilities.TGExtendedPlayer;

/** Back slot: sneak in the air to glide. */
public class ItemGlider extends ItemTGSpecialSlot {

	public ItemGlider(int camoCount, int dur) {
		super(TGSlotType.BACKSLOT, camoCount, dur);
	}

	@Override
	public void onPlayerTick(ItemStack item, Player ply) {
		glide(ply);
	}

	/** sets TGExtendedPlayer.isGliding; motion is applied on both sides, the client one moves the local player */
	public static void glide(Player ply) {
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		boolean gliding = false;
		if (!ply.onGround() && ply.isShiftKeyDown() && !ply.getAbilities().flying) {
			Vec3 m = ply.getDeltaMovement();
			if (m.y < -0.1f) {
				gliding = true;
				// "totally stolen from modular powersuits glider code"
				Vec3 look = ply.getLookAngle();
				double motion = Math.min(0.08, -0.1 - m.y);
				ply.setDeltaMovement(m.x + look.x * motion, m.y + motion, m.z + look.z * motion);
			}
			ply.resetFallDistance();
		}
		props.isGliding = gliding;
	}
}
