package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import techguns.blocks.machines.BlockMultiBlockController;
import techguns.blocks.machines.multiblocks.MultiBlockRegister;
import techguns.blocks.machines.multiblocks.MultiBlockSchematic;

/**
 * Controller of a multiblock machine (1.12 MultiBlockMachineTileEntMaster). Formed state and the multiblock direction
 * live in the controller blockstate, the machine only works while formed.
 */
public abstract class MultiBlockMachineTileEntMaster extends BasicMachineTileEnt {

	public MultiBlockMachineTileEntMaster(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize, int maximumPower) {
		super(type, pos, state, inventorySize, false, maximumPower);
	}

	public boolean isFormed() {
		BlockState s = getBlockState();
		return s.hasProperty(BlockMultiBlockController.FORMED) && s.getValue(BlockMultiBlockController.FORMED);
	}

	public Direction getMultiblockDirection() {
		BlockState s = getBlockState();
		return s.hasProperty(BlockMultiBlockController.FACING) ? s.getValue(BlockMultiBlockController.FACING) : Direction.SOUTH;
	}

	@Override
	protected boolean enabled() {
		return this.isFormed();
	}

	public MultiBlockSchematic getSchematic() {
		return MultiBlockRegister.get(this);
	}

	@Override
	public void onBlockBreak() {
		super.onBlockBreak();
		this.onMultiBlockBreak();
	}

	/** does a formed part of this type hand the capability to this controller */
	public boolean forwardsCapability(int slaveType, net.neoforged.neoforge.capabilities.BlockCapability<?, ?> cap) {
		return false;
	}

	/** collision / outline shape of a formed part */
	public net.minecraft.world.phys.shapes.VoxelShape getSlaveShape(int slaveType, BlockPos slavePos) {
		return net.minecraft.world.phys.shapes.Shapes.block();
	}

	/** a part (or this controller) was broken */
	public void onMultiBlockBreak() {
		if (level != null && !level.isClientSide && this.isFormed()) {
			MultiBlockSchematic schematic = getSchematic();
			if (schematic != null) {
				schematic.unform(level, this);
			}
		}
	}

	/** set the controller blockstate formed / unformed (only while the controller block is still there) */
	public void setFormed(boolean formed, Direction direction) {
		if (level == null || level.isClientSide) {
			return;
		}
		BlockState s = level.getBlockState(worldPosition);
		if (s.getBlock() instanceof BlockMultiBlockController<?>) {
			level.setBlock(worldPosition, s.setValue(BlockMultiBlockController.FORMED, formed).setValue(BlockMultiBlockController.FACING, direction), 3);
		}
		this.contentsChanged = true;
		this.needUpdate();
	}
}
