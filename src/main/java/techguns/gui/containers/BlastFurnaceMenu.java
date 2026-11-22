package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.BlastFurnaceTileEnt;

public class BlastFurnaceMenu extends TGBaseMenu<BlastFurnaceTileEnt> {

	public static final int SLOT_UPGRADE_X = 150;
	public static final int SLOT_UPGRADE_Y = 50;

	public BlastFurnaceMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (BlastFurnaceTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public BlastFurnaceMenu(int id, Inventory inv, BlastFurnaceTileEnt tile) {
		super(TGMenus.BLAST_FURNACE.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, BlastFurnaceTileEnt.SLOT_INPUT1, 19, 17).withBackground(TGSlots.INGOTSLOT_TEX));
		this.addSlot(new TGSlots.MachineInput(h, BlastFurnaceTileEnt.SLOT_INPUT2, 47, 17).withBackground(TGSlots.POWDERSLOT_TEX));
		this.addSlot(new TGSlots.Output(h, BlastFurnaceTileEnt.SLOT_OUTPUT, 116, 50));
		this.addSlot(new TGSlots.Upgrade(h, BlastFurnaceTileEnt.SLOT_UPGRADE, SLOT_UPGRADE_X, SLOT_UPGRADE_Y));
		this.addPlayerInventorySlots(inv);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, tile::getValidSlotForItemInMachine);
	}
}
