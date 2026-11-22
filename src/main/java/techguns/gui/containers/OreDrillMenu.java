package techguns.gui.containers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.OreDrillTileEntMaster;

/** Ore drill menu (1.12 OreDrillContainer). */
public class OreDrillMenu extends TGBaseMenu<OreDrillTileEntMaster> {

	public static final int SLOT_DRILL_X = 30;
	public static final int SLOTS_ROW1_Y = 17;
	public static final int SLOTS_FURNACE_Y = 51;
	public static final int SLOTS_OUTPUT_X = 102;

	public OreDrillMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (OreDrillTileEntMaster) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public OreDrillMenu(int id, Inventory inv, OreDrillTileEntMaster tile) {
		super(TGMenus.ORE_DRILL.get(), id, inv, tile);
		var h = tile.getInventory();
		this.addSlot(new TGSlots.MachineInput(h, OreDrillTileEntMaster.SLOT_DRILL, SLOT_DRILL_X, SLOTS_ROW1_Y));
		this.addSlot(new TGSlots.MachineInput(h, OreDrillTileEntMaster.SLOT_FURNACE, SLOT_DRILL_X, SLOTS_FURNACE_Y));
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				this.addSlot(new TGSlots.Output(h, OreDrillTileEntMaster.SLOT_OUTPUT1 + i * 3 + j, SLOTS_OUTPUT_X + 18 * i, SLOTS_ROW1_Y + 18 * j));
			}
		}
		this.addPlayerInventorySlots(inv);

		// data slots are shorts: split the fuel buffer values
		addData(() -> tile.getFuelBuffer() & 0xFFFF, v -> tile.setFuelBuffer((tile.getFuelBuffer() & 0xFFFF0000) | (v & 0xFFFF)));
		addData(() -> tile.getFuelBuffer() >>> 16, v -> tile.setFuelBuffer((tile.getFuelBuffer() & 0xFFFF) | ((v & 0xFFFF) << 16)));
		addData(() -> tile.getCurrentFuelBufferMax() & 0xFFFF,
				v -> tile.setCurrentFuelBufferMax((tile.getCurrentFuelBufferMax() & 0xFFFF0000) | (v & 0xFFFF)));
		addData(() -> tile.getCurrentFuelBufferMax() >>> 16,
				v -> tile.setCurrentFuelBufferMax((tile.getCurrentFuelBufferMax() & 0xFFFF) | ((v & 0xFFFF) << 16)));
		addTank(tile.inputTank);
		addTank(tile.outputTank);
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
