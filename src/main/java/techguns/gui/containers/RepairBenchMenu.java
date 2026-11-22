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
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.items.SlotItemHandler;
import techguns.gui.TGMenus;
import techguns.tileentities.RepairBenchTileEnt;

public class RepairBenchMenu extends TGBaseMenu<RepairBenchTileEnt> {

	public static final ResourceLocation[] ARMOR_ICONS = { InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
			InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS };

	/** first armor slot (helmet), then chest, legs, boots, offhand */
	private final int armorStart;

	public RepairBenchMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (RepairBenchTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public RepairBenchMenu(int id, Inventory inv, RepairBenchTileEnt tile) {
		super(TGMenus.REPAIR_BENCH.get(), id, inv, tile);
		var h = tile.getInventory();
		for (int i = 0; i < RepairBenchTileEnt.MAT_SLOTS; ++i) {
			this.addSlot(new SlotItemHandler(h, i, 8 + i * 18, 57));
		}
		this.addSlot(new SlotItemHandler(h, RepairBenchTileEnt.SLOT_REPAIR, 8, 18));
		this.addPlayerInventorySlots(inv);
		this.armorStart = this.slots.size();
		for (int i = 0; i < 4; ++i) {
			this.addSlot(new ArmorSlot(inv, 39 - i, 89 + i * 20, 18, RepairBenchTileEnt.ARMOR_SLOTS[i], ARMOR_ICONS[i]));
		}
		this.addSlot(new Slot(inv, 40, 64, 18) {
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
			// bench or armor slots -> player inventory
			if (!this.moveItemStackTo(stack1, invStart, invEnd, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			EquipmentSlot eq = player.getEquipmentSlotForItem(stack1);
			boolean moved = false;
			if (eq.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
				int t = 3 - eq.getIndex(); // FEET=0 .. HEAD=3, the slots go helmet first
				moved = this.moveItemStackTo(stack1, armorStart + t, armorStart + t + 1, false);
			}
			if (!moved && RepairBenchTileEnt.isRepairable(stack1)) {
				moved = this.moveItemStackTo(stack1, RepairBenchTileEnt.SLOT_REPAIR, RepairBenchTileEnt.SLOT_REPAIR + 1, false);
			}
			if (!moved && !this.moveItemStackTo(stack1, 0, RepairBenchTileEnt.MAT_SLOTS, false)) {
				return ItemStack.EMPTY;
			}
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

	/** player armor slot (1.12 SlotArmor, the vanilla ArmorSlot is not public) */
	public static class ArmorSlot extends Slot {
		private final EquipmentSlot type;
		private final ResourceLocation icon;
		private final Player owner;

		public ArmorSlot(Inventory inv, int index, int x, int y, EquipmentSlot type, ResourceLocation icon) {
			super(inv, index, x, y);
			this.type = type;
			this.icon = icon;
			this.owner = inv.player;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.canEquip(type, owner);
		}

		@Override
		public boolean mayPickup(Player player) {
			ItemStack s = this.getItem();
			return (s.isEmpty() || player.isCreative() || !EnchantmentHelper.has(s, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
					&& super.mayPickup(player);
		}

		@Override
		public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
			return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
		}
	}
}
