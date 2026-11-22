package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.gui.containers.UpgradeBenchMenu;

/**
 * Upgrade bench (1.12 UpgradeBenchTileEnt): puts enchantments on Techguns armor with upgrade items and xp. Like an
 * anvil the inputs live in the menu, the block only stores owner and security.
 */
public class UpgradeBenchTileEnt extends BasicOwnedTileEnt {

	public UpgradeBenchTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.UPGRADE_BENCH.get(), pos, state, 0, false);
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new UpgradeBenchMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.armorbench");
	}
}
