package techguns.gui.containers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import techguns.TGBlocks;
import techguns.gui.TGMenus;
import techguns.recipes.AmmoBenchRecipe;

/**
 * Ammo bench (Fabric port AmmoBenchScreenHandler): vanilla's stonecutter menu with three input slots, bullet core, casing
 * and powder. Every recipe the inputs make is listed; the player picks one and takes the result, which uses one of each
 * input. No block entity: the inputs drop back to the player when the menu closes.
 */
public class AmmoBenchMenu extends AbstractContainerMenu {
	public static final int RESULT_SLOT = 3;
	private static final int INV_START = 4, HOTBAR_START = 31, INV_END = 40;

	private final ContainerLevelAccess access;
	private final Level level;
	private final DataSlot selectedRecipeIndex = DataSlot.standalone();
	private List<RecipeHolder<AmmoBenchRecipe>> recipes = new ArrayList<>();
	private final ItemStack[] lastInputs = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
	private long lastSoundTime;
	private Runnable slotUpdateListener = () -> {
	};

	public final Container container = new SimpleContainer(3) {
		@Override
		public void setChanged() {
			super.setChanged();
			AmmoBenchMenu.this.slotsChanged(this);
			AmmoBenchMenu.this.slotUpdateListener.run();
		}
	};
	private final ResultContainer resultContainer = new ResultContainer();
	private final Slot resultSlot;

	public AmmoBenchMenu(int id, Inventory inv) {
		this(id, inv, ContainerLevelAccess.NULL);
	}

	public AmmoBenchMenu(int id, Inventory inv, ContainerLevelAccess access) {
		super(TGMenus.AMMO_BENCH.get(), id);
		this.access = access;
		this.level = inv.player.level();
		for (int i = 0; i < 3; i++) {
			int slot = i;
			this.addSlot(new Slot(container, i, 20, 15 + i * 19) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return AmmoBenchRecipe.isInput(level, slot, stack);
				}
			});
		}
		this.resultSlot = this.addSlot(new Slot(resultContainer, 1, 143, 33) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				// the menu's input container: Slot has its own field "container" (here the result container)
				Container inputs = AmmoBenchMenu.this.container;
				stack.onCraftedBy(player.level(), player, stack.getCount());
				resultContainer.awardUsedRecipes(player, List.of(inputs.getItem(0), inputs.getItem(1), inputs.getItem(2)));
				boolean left = true;
				for (int i = 0; i < 3; i++) {
					left &= !inputs.removeItem(i, 1).isEmpty() && !inputs.getItem(i).isEmpty();
				}
				if (left) {
					setupResultSlot();
				}
				access.execute((lvl, pos) -> {
					long t = lvl.getGameTime();
					if (lastSoundTime != t) {
						lvl.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
						lastSoundTime = t;
					}
				});
				super.onTake(player, stack);
			}
		});
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
			}
		}
		for (int c = 0; c < 9; c++) {
			this.addSlot(new Slot(inv, c, 8 + c * 18, 142));
		}
		this.addDataSlot(selectedRecipeIndex);
	}

	public int getSelectedRecipeIndex() {
		return selectedRecipeIndex.get();
	}

	public List<RecipeHolder<AmmoBenchRecipe>> getRecipes() {
		return recipes;
	}

	public int getNumRecipes() {
		return recipes.size();
	}

	/** whether the recipe buttons show */
	public boolean hasInputItems() {
		return !recipes.isEmpty();
	}

	public void registerUpdateListener(Runnable listener) {
		this.slotUpdateListener = listener;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, TGBlocks.AMMO_BENCH.get());
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id >= 0 && id < recipes.size()) {
			selectedRecipeIndex.set(id);
			setupResultSlot();
		}
		return true;
	}

	private AmmoBenchRecipe.Input input() {
		return new AmmoBenchRecipe.Input(container.getItem(0), container.getItem(1), container.getItem(2));
	}

	@Override
	public void slotsChanged(Container inventory) {
		boolean changed = false;
		for (int i = 0; i < 3; i++) {
			ItemStack s = container.getItem(i);
			if (!s.is(lastInputs[i].getItem())) {
				lastInputs[i] = s.copy();
				changed = true;
			}
		}
		if (changed) {
			recipes = new ArrayList<>(AmmoBenchRecipe.find(level, input()));
			selectedRecipeIndex.set(-1);
			resultSlot.set(ItemStack.EMPTY);
		}
	}

	void setupResultSlot() {
		int sel = selectedRecipeIndex.get();
		if (sel >= 0 && sel < recipes.size()) {
			RecipeHolder<AmmoBenchRecipe> r = recipes.get(sel);
			resultContainer.setRecipeUsed(r);
			resultSlot.set(r.value().assemble(input(), level.registryAccess()));
		} else {
			resultSlot.set(ItemStack.EMPTY);
		}
		broadcastChanges();
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != resultContainer && super.canTakeItemForPickAll(stack, slot);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack ret = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			ret = stack.copy();
			if (index == RESULT_SLOT) {
				stack.getItem().onCraftedBy(stack, player.level(), player);
				if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(stack, ret);
			} else if (index < RESULT_SLOT) {
				if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!moveToInput(stack)) {
				if (index < HOTBAR_START) {
					if (!this.moveItemStackTo(stack, HOTBAR_START, INV_END, false)) {
						return ItemStack.EMPTY;
					}
				} else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
					return ItemStack.EMPTY;
				}
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			}
			slot.setChanged();
			if (stack.getCount() == ret.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(player, stack);
			this.broadcastChanges();
		}
		return ret;
	}

	/** shift-click from the inventory: into the first input slot that takes it; false if none does */
	private boolean moveToInput(ItemStack stack) {
		for (int i = 0; i < 3; i++) {
			if (AmmoBenchRecipe.isInput(level, i, stack)) {
				int before = stack.getCount();
				this.moveItemStackTo(stack, i, i + 1, false);
				if (stack.getCount() != before) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		resultContainer.removeItemNoUpdate(1);
		access.execute((lvl, pos) -> this.clearContainer(player, container));
	}
}
