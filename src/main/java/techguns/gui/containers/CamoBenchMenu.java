package techguns.gui.containers;

import com.mojang.datafixers.util.Pair;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import techguns.gui.TGMenus;
import techguns.tileentities.CamoBenchTileEnt;
import techguns.tileentities.RepairBenchTileEnt;

public class CamoBenchMenu extends TGBaseMenu<CamoBenchTileEnt> {

	public static final int SLOT_INPUT_X = 17;
	public static final int SLOT_INPUT_Y = 18;

	private final int armorStart;

	public CamoBenchMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (CamoBenchTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public CamoBenchMenu(int id, Inventory inv, CamoBenchTileEnt tile) {
		super(TGMenus.CAMO_BENCH.get(), id, inv, tile);
		this.addSlot(new SlotItemHandler(tile.getInventory(), 0, SLOT_INPUT_X, SLOT_INPUT_Y));
		this.addPlayerInventorySlots(inv);
		this.armorStart = this.slots.size();
		for (int i = 0; i < 4; ++i) {
			this.addSlot(new RepairBenchMenu.ArmorSlot(inv, 39 - i, 99 + i * 18, 18, RepairBenchTileEnt.ARMOR_SLOTS[i],
					RepairBenchMenu.ARMOR_ICONS[i]));
		}
		this.addSlot(new Slot(inv, 40, -17, 94) {
			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
			}
		});
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack1 = slot.getItem();
		ItemStack stack = stack1.copy();
		int invStart = tileSlots;
		int invEnd = tileSlots + 36;
		if (index < invStart || index >= invEnd) {
			// bench slot: armor back on, everything else to the inventory
			boolean moved = false;
			EquipmentSlot eq = player.getEquipmentSlotForItem(stack1);
			if (index < invStart && eq.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
				int t = 3 - eq.getIndex();
				moved = this.moveItemStackTo(stack1, armorStart + t, armorStart + t + 1, false);
			}
			if (!moved && !this.moveItemStackTo(stack1, invStart, invEnd, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack1, 0, 1, false)) {
			return ItemStack.EMPTY;
		}
		if (stack1.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack1.getCount() == stack.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack1);
		return stack;
	}
}
