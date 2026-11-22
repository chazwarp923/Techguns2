package techguns.tileentities;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** block entity with fluid tanks (1.12 ITileEntityFluidTanks): fluid capability and bucket interaction */
public interface IFluidTankTile {

	IFluidHandler getFluidHandler();

	/** right click with a fluid container, true if something was filled or drained */
	boolean onFluidContainerInteract(Player player, InteractionHand hand);
}
