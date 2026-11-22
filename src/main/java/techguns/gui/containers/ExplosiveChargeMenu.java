package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.tileentities.ExplosiveChargeTileEnt;

/** Mining charge settings (1.12 ExplosiveChargeContainer): no slots, just the blast settings. */
public class ExplosiveChargeMenu extends TGBaseMenu<ExplosiveChargeTileEnt> {

	public ExplosiveChargeMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (ExplosiveChargeTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public ExplosiveChargeMenu(int id, Inventory inv, ExplosiveChargeTileEnt tile) {
		super(TGMenus.EXPLOSIVE_CHARGE.get(), id, inv, tile);
		addData(tile::getBlastradius, tile::setBlastradius);
		addData(tile::getBlastlength, tile::setBlastlength);
		addData(tile::getFusetime, tile::setFusetime);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return !tile.isArmed() && tile.isUseableByPlayer(player);
	}
}
