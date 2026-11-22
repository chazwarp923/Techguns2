package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.MetalPressTileEnt;

public class MetalPressMenu extends TGBaseMenu<MetalPressTileEnt> {

	public static final int SLOT_INPUT1_X = 110;
	public static final int SLOT_INPUT2_X = 130;
	public static final int SLOT_INPUTS_Y = 17;
	public static final int SLOT_OUTPUT_X = 120;
	public static final int SLOT_OUTPUTS_Y = 60;
	public static final int SLOT_UPGRADE_X = 152;

	public MetalPressMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (MetalPressTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public MetalPressMenu(int id, Inventory inv, MetalPressTileEnt tile) {
		super(TGMenus.METAL_PRESS.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, MetalPressTileEnt.SLOT_INPUT1, SLOT_INPUT1_X, SLOT_INPUTS_Y));
		this.addSlot(new TGSlots.MachineInput(h, MetalPressTileEnt.SLOT_INPUT2, SLOT_INPUT2_X, SLOT_INPUTS_Y));
		this.addSlot(new TGSlots.Output(h, MetalPressTileEnt.SLOT_OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUTS_Y));
		this.addSlot(new TGSlots.Upgrade(h, MetalPressTileEnt.SLOT_UPGRADE, SLOT_UPGRADE_X, SLOT_OUTPUTS_Y));
		this.addPlayerInventorySlots(inv);
		addData(tile::getAutoSplitMode, v -> tile.setAutoSplitMode((byte) v));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, tile::getValidSlotForItemInMachine);
	}
}
