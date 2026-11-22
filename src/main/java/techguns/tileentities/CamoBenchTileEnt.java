package techguns.tileentities;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.gui.containers.CamoBenchMenu;
import techguns.items.armors.ICamoChangeable;
import techguns.recipes.CamoBenchRecipe;
import techguns.tileentities.operation.ItemStackHandlerPlus;

/**
 * Camo bench (1.12 CamoBenchTileEnt): switches the camo of the item in the bench and of the worn armor. Blocks cycle
 * through their techguns:camo_bench recipe.
 */
public class CamoBenchTileEnt extends BasicOwnedTileEnt {

	public static final int BUTTON_ID_NEXT = 10;
	public static final int BUTTON_ID_PREV = 11;
	/** + / - for helmet, chest, legs, boots: BUTTON_ID_ARMOR + 2 * i (+1 for back) */
	public static final int BUTTON_ID_ARMOR = 12;

	public CamoBenchTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.CAMO_BENCH.get(), pos, state, 1, false);
	}

	@Override
	protected ItemStackHandlerPlus createInventory(int size) {
		return new ItemStackHandlerPlus(size) {
			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged(slot);
				needUpdate();
			}
		};
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new CamoBenchMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.camobench");
	}

	public ItemStack getItem() {
		return this.inventory.getStackInSlot(0);
	}

	public Optional<CamoBenchRecipe> getBlockRecipe() {
		ItemStack s = getItem();
		if (s.isEmpty() || s.getItem() instanceof ICamoChangeable) {
			return Optional.empty();
		}
		return CamoBenchRecipe.find(level, s).map(h -> h.value());
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id < BUTTON_ID_NEXT) {
			super.buttonClicked(id, ply);
			return;
		}
		if (!this.isUseableByPlayer(ply)) {
			return;
		}
		if (id == BUTTON_ID_NEXT || id == BUTTON_ID_PREV) {
			boolean back = id == BUTTON_ID_PREV;
			ItemStack item = getItem();
			if (item.getItem() instanceof ICamoChangeable camo) {
				camo.switchCamo(item, back);
				this.inventory.setStackInSlot(0, item);
			} else {
				getBlockRecipe().ifPresent(r -> this.inventory.setStackInSlot(0, r.cycle(item, back)));
			}
		} else if (id < BUTTON_ID_ARMOR + 8) {
			int i = (id - BUTTON_ID_ARMOR) / 2;
			boolean back = (id - BUTTON_ID_ARMOR) % 2 == 1;
			ItemStack armor = ply.getItemBySlot(RepairBenchTileEnt.ARMOR_SLOTS[i]);
			if (armor.getItem() instanceof ICamoChangeable camo) {
				camo.switchCamo(armor, back);
			}
		}
	}
}
