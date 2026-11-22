package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import techguns.gui.TGMenus;
import techguns.items.armors.GenericArmor;
import techguns.recipes.UpgradeBenchRecipe;
import techguns.tileentities.RepairBenchTileEnt;
import techguns.tileentities.UpgradeBenchTileEnt;

/** Upgrade bench menu (1.12 UpgradeBenchContainer): armor + upgrade item -> enchanted armor for xp, anvil like. */
public class UpgradeBenchMenu extends TGBaseMenu<UpgradeBenchTileEnt> {

	public static final int SLOT_Y = 47;
	public static final int SLOT_INPUT_X = 17;
	public static final int SLOT_UPGRADE_X = 53;
	public static final int SLOT_OUTPUT_X = 116;

	public final ResultContainer outputSlot = new ResultContainer();
	protected final SimpleContainer inputSlots = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			UpgradeBenchMenu.this.slotsChanged(this);
		}
	};

	protected final Player player;
	protected int xpNeeded = 0;

	public UpgradeBenchMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (UpgradeBenchTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public UpgradeBenchMenu(int id, Inventory inv, UpgradeBenchTileEnt tile) {
		super(TGMenus.UPGRADE_BENCH.get(), id, inv, tile);
		this.player = inv.player;
		this.addSlot(new Slot(inputSlots, 0, SLOT_INPUT_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof GenericArmor;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addSlot(new Slot(inputSlots, 1, SLOT_UPGRADE_X, SLOT_Y));
		this.addSlot(new Slot(outputSlot, 2, SLOT_OUTPUT_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean mayPickup(Player ply) {
				return this.hasItem() && (ply.getAbilities().instabuild || ply.totalExperience >= xpNeeded);
			}

			@Override
			public void onTake(Player ply, ItemStack stack) {
				inputSlots.setItem(0, ItemStack.EMPTY);
				inputSlots.removeItem(1, 1);
				ply.level().playSound(null, ply.getX(), ply.getY(), ply.getZ(), SoundEvents.ARMOR_EQUIP_DIAMOND.value(), SoundSource.PLAYERS, 1f, 1f);
				if (!ply.getAbilities().instabuild) {
					ply.giveExperiencePoints(-xpNeeded);
				}
				xpNeeded = 0;
				super.onTake(ply, stack);
			}
		});
		this.addPlayerInventorySlots(inv);
		for (int i = 0; i < 4; ++i) {
			this.addSlot(new RepairBenchMenu.ArmorSlot(inv, 39 - i, 17 + i * 18, 18, RepairBenchTileEnt.ARMOR_SLOTS[i],
					RepairBenchMenu.ARMOR_ICONS[i]));
		}
		addData(() -> xpNeeded, v -> xpNeeded = v);
	}

	public int getXpNeeded() {
		return xpNeeded;
	}

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (container == inputSlots) {
			updateOutput();
		}
	}

	protected void updateOutput() {
		ItemStack armor = inputSlots.getItem(0);
		var r = UpgradeBenchRecipe.find(player.level(), armor, inputSlots.getItem(1));
		if (r.isPresent()) {
			ItemStack out = r.get().value().apply(armor);
			this.xpNeeded = out.isEmpty() ? 0 : r.get().value().xpCost(armor);
			outputSlot.setItem(0, out);
		} else {
			this.xpNeeded = 0;
			outputSlot.setItem(0, ItemStack.EMPTY);
		}
		this.broadcastChanges();
	}

	@Override
	public void removed(Player ply) {
		super.removed(ply);
		if (!ply.level().isClientSide) {
			this.clearContainer(ply, inputSlots);
		}
	}

	@Override
	public ItemStack quickMoveStack(Player ply, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack1 = slot.getItem();
		ItemStack stack = stack1.copy();
		int invStart = tileSlots;
		int invEnd = tileSlots + 36;
		if (index == 2) {
			if (!slot.mayPickup(ply) || !this.moveItemStackTo(stack1, invStart, invEnd, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack1, stack);
		} else if (index < invStart || index >= invEnd) {
			if (!this.moveItemStackTo(stack1, invStart, invEnd, false)) {
				return ItemStack.EMPTY;
			}
		} else if (stack1.getItem() instanceof GenericArmor) {
			if (!this.moveItemStackTo(stack1, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (UpgradeBenchRecipe.isUpgradeItem(ply.level(), stack1)) {
			if (!this.moveItemStackTo(stack1, 1, 2, false)) {
				return ItemStack.EMPTY;
			}
		} else {
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
		slot.onTake(ply, stack1);
		return stack;
	}
}
