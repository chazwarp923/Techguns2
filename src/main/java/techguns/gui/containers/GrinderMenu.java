package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.GrinderTileEnt;

public class GrinderMenu extends TGBaseMenu<GrinderTileEnt> {

	public static final int SLOT_UPGRADE_X = 152;
	public static final int SLOT_UPGRADE_Y = 60;

	public GrinderMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (GrinderTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public GrinderMenu(int id, Inventory inv, GrinderTileEnt tile) {
		super(TGMenus.GRINDER.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, GrinderTileEnt.SLOT_INPUT, 18, 17));
		this.addSlot(new TGSlots.Upgrade(h, GrinderTileEnt.SLOT_UPGRADE, SLOT_UPGRADE_X, SLOT_UPGRADE_Y));
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				this.addSlot(new TGSlots.Output(h, GrinderTileEnt.SLOT_OUTPUT0 + i * 3 + j, 80 + j * 18, 17 + i * 18));
			}
		}
		this.addPlayerInventorySlots(inv);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, tile::getValidSlotForItemInMachine);
	}
}
