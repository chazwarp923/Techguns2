package techguns.gui.containers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.Techguns;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.ChemLabTileEnt;

public class ChemLabMenu extends TGBaseMenu<ChemLabTileEnt> {

	public static final int SLOT_INPUT1_X = 35;
	public static final int SLOT_INPUT2_X = 57;
	public static final int SLOT_OUTPUT_X = 135;
	public static final int SLOTS_ROW1_Y = 17;
	public static final int SLOTS_ROW2_Y = 40;
	public static final int SLOT_UPGRADE_X = SLOT_OUTPUT_X;
	public static final int SLOT_UPGRADE_Y = MetalPressMenu.SLOT_OUTPUTS_Y;

	public ChemLabMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (ChemLabTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public ChemLabMenu(int id, Inventory inv, ChemLabTileEnt tile) {
		super(TGMenus.CHEM_LAB.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, ChemLabTileEnt.SLOT_INPUT1, SLOT_INPUT1_X, SLOTS_ROW1_Y));
		this.addSlot(new TGSlots.MachineInput(h, ChemLabTileEnt.SLOT_INPUT2, SLOT_INPUT2_X, SLOTS_ROW1_Y));
		this.addSlot(new TGSlots.MachineInput(h, ChemLabTileEnt.SLOT_BOTTLE, SLOT_INPUT1_X, SLOTS_ROW2_Y)
				.withBackground(Techguns.rl("gui/emptyslots/emptyslot_bottle")));
		this.addSlot(new TGSlots.Output(h, ChemLabTileEnt.SLOT_OUTPUT, SLOT_OUTPUT_X, SLOTS_ROW1_Y));
		this.addSlot(new TGSlots.Upgrade(h, ChemLabTileEnt.SLOT_UPGRADE, SLOT_UPGRADE_X, SLOT_UPGRADE_Y));
		this.addPlayerInventorySlots(inv);

		// tank contents (fluid components are not synced, the gui only shows fluid type and amount)
		addTank(tile.inputTank);
		addTank(tile.outputTank);
		addData(tile::getDrainMode, v -> tile.setDrainMode(v != 0));
	}

	private void addTank(FluidTank tank) {
		addData(() -> tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid()), v -> {
			Fluid f = v < 0 ? null : BuiltInRegistries.FLUID.byId(v);
			tank.setFluid(f == null ? FluidStack.EMPTY : new FluidStack(f, Math.max(1, tank.getFluidAmount())));
		});
		addData(tank::getFluidAmount, v -> {
			if (!tank.getFluid().isEmpty()) {
				tank.getFluid().setAmount(v);
			}
		});
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, tile::getValidSlotForItemInMachine);
	}
}
