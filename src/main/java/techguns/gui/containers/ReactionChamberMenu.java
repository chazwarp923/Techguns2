package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.ReactionChamberTileEntMaster;

/** Reaction chamber menu (1.12 ReactionChamberContainer). Tank and reaction state come with the block updates. */
public class ReactionChamberMenu extends TGBaseMenu<ReactionChamberTileEntMaster> {

	public static final int SLOT_INPUT_X = 35;
	public static final int SLOT_FOCUS_X = 93;
	public static final int SLOTS_ROW1_Y = 17;
	public static final int SLOT_OUTPUT_X = 134;
	public static final int SLOT_OUTPUT_Y = 17;

	public ReactionChamberMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (ReactionChamberTileEntMaster) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public ReactionChamberMenu(int id, Inventory inv, ReactionChamberTileEntMaster tile) {
		super(TGMenus.REACTION_CHAMBER.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, ReactionChamberTileEntMaster.SLOT_INPUT, SLOT_INPUT_X, SLOTS_ROW1_Y));
		this.addSlot(new TGSlots.MachineInput(h, ReactionChamberTileEntMaster.SLOT_FOCUS, SLOT_FOCUS_X, SLOTS_ROW1_Y));
		for (int i = 0; i < ReactionChamberTileEntMaster.OUTPUT_SLOTS_COUNT; i++) {
			this.addSlot(new TGSlots.Output(h, ReactionChamberTileEntMaster.SLOT_OUTPUT + i, SLOT_OUTPUT_X + (i % 2) * 18, SLOT_OUTPUT_Y + (i / 2) * 18));
		}
		this.addPlayerInventorySlots(inv);

		addData(tile::getLiquidLevel, tile::setLiquidLevel);
		addData(tile::getIntensity, tile::setIntensity);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, tile::getValidSlotForItemInMachine);
	}
}
