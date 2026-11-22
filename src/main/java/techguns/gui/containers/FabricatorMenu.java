package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.FabricatorTileEntMaster;

/** Fabricator menu (1.12 FabricatorContainer). */
public class FabricatorMenu extends TGBaseMenu<FabricatorTileEntMaster> {

	public static final ResourceLocation WIRES_ICON = Techguns.rl("gui/emptyslots/emptyslot_wires");
	public static final ResourceLocation POWDER_ICON = Techguns.rl("gui/emptyslots/emptyslot_powder");
	public static final ResourceLocation PLATE_ICON = Techguns.rl("gui/emptyslots/emptyslot_plate");

	public static final int SLOT_INPUT1_X = 19;
	public static final int SLOT_WIRES_X = 47;
	public static final int SLOT_POWDER_X = 68;
	public static final int SLOT_PLATE_X = 89;
	public static final int SLOTS_ROW1_Y = 17;
	public static final int SLOT_OUTPUT_X = 116;
	public static final int SLOT_OUTPUT_Y = 50;
	public static final int SLOT_UPGRADE_X = 150;
	public static final int SLOT_UPGRADE_Y = 50;

	public FabricatorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (FabricatorTileEntMaster) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public FabricatorMenu(int id, Inventory inv, FabricatorTileEntMaster tile) {
		super(TGMenus.FABRICATOR.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, FabricatorTileEntMaster.SLOT_INPUT1, SLOT_INPUT1_X, SLOTS_ROW1_Y));
		this.addSlot(new TGSlots.MachineInput(h, FabricatorTileEntMaster.SLOT_WIRES, SLOT_WIRES_X, SLOTS_ROW1_Y).withBackground(WIRES_ICON));
		this.addSlot(new TGSlots.MachineInput(h, FabricatorTileEntMaster.SLOT_POWDER, SLOT_POWDER_X, SLOTS_ROW1_Y).withBackground(POWDER_ICON));
		this.addSlot(new TGSlots.MachineInput(h, FabricatorTileEntMaster.SLOT_PLATE, SLOT_PLATE_X, SLOTS_ROW1_Y).withBackground(PLATE_ICON));
		this.addSlot(new TGSlots.Output(h, FabricatorTileEntMaster.SLOT_OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUT_Y));
		this.addSlot(new TGSlots.Upgrade(h, FabricatorTileEntMaster.SLOT_UPGRADE, SLOT_UPGRADE_X, SLOT_UPGRADE_Y));
		this.addPlayerInventorySlots(inv);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, tile::getValidSlotForItemInMachine);
	}
}
