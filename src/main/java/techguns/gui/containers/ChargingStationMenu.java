package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.ChargingStationTileEnt;

public class ChargingStationMenu extends TGBaseMenu<ChargingStationTileEnt> {

	public static final int SLOT_UPGRADE_X = MetalPressMenu.SLOT_UPGRADE_X;
	public static final int SLOT_UPGRADE_Y = MetalPressMenu.SLOT_OUTPUTS_Y;

	public ChargingStationMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (ChargingStationTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public ChargingStationMenu(int id, Inventory inv, ChargingStationTileEnt tile) {
		super(TGMenus.CHARGING_STATION.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, ChargingStationTileEnt.SLOT_INPUT, 19, 17));
		this.addSlot(new TGSlots.Output(h, ChargingStationTileEnt.SLOT_OUTPUT, 68, 17));
		this.addSlot(new TGSlots.Upgrade(h, ChargingStationTileEnt.SLOT_UPGRADE, SLOT_UPGRADE_X, SLOT_UPGRADE_Y));
		this.addPlayerInventorySlots(inv);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, tile::getValidSlotForItemInMachine);
	}
}
