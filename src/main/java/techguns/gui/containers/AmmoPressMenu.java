package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.AmmoPressTileEnt;

public class AmmoPressMenu extends TGBaseMenu<AmmoPressTileEnt> {

	public static final int SLOT_METAL1_X = 100;
	public static final int SLOT_METAL2_X = 120;
	public static final int SLOT_POWDER_X = 140;
	public static final int SLOTS_INPUT_Y = 17;
	public static final int SLOT_OUTPUT_X = 120;
	public static final int SLOTS_OUTPUT_Y = 60;
	public static final int SLOT_UPGRADE_X = 152;

	public AmmoPressMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (AmmoPressTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public AmmoPressMenu(int id, Inventory inv, AmmoPressTileEnt tile) {
		super(TGMenus.AMMO_PRESS.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, AmmoPressTileEnt.SLOT_METAL1, SLOT_METAL1_X, SLOTS_INPUT_Y).withBackground(TGSlots.INGOTDARKSLOT_TEX));
		this.addSlot(new TGSlots.MachineInput(h, AmmoPressTileEnt.SLOT_METAL2, SLOT_METAL2_X, SLOTS_INPUT_Y).withBackground(TGSlots.INGOTSLOT_TEX));
		this.addSlot(new TGSlots.MachineInput(h, AmmoPressTileEnt.SLOT_POWDER, SLOT_POWDER_X, SLOTS_INPUT_Y).withBackground(TGSlots.POWDERSLOT_TEX));
		this.addSlot(new TGSlots.Output(h, AmmoPressTileEnt.SLOT_OUTPUT, SLOT_OUTPUT_X, SLOTS_OUTPUT_Y));
		this.addSlot(new TGSlots.Upgrade(h, AmmoPressTileEnt.SLOT_UPGRADE, SLOT_UPGRADE_X, SLOTS_OUTPUT_Y));
		this.addPlayerInventorySlots(inv);
		addData(() -> tile.buildPlan, v -> tile.buildPlan = (byte) v);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, AmmoPressTileEnt::getValidSlotForItemInMachine);
	}
}
