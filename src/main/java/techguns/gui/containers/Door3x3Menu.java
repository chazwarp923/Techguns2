package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.tileentities.Door3x3TileEnt;

/** 3x3 door settings (1.12 Door3x3Container): no slots. */
public class Door3x3Menu extends TGBaseMenu<Door3x3TileEnt> {

	public Door3x3Menu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (Door3x3TileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public Door3x3Menu(int id, Inventory inv, Door3x3TileEnt tile) {
		super(TGMenus.DOOR3x3.get(), id, inv, tile);
		// direct field access on the client side, the setters would schedule ticks
		addData(tile::getDoormode, v -> tile.syncDoormode((byte) v));
		addData(() -> tile.isAutoClose() ? 1 : 0, v -> tile.syncAutoClose(v != 0));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return tile.isUseableByPlayer(player);
	}
}
