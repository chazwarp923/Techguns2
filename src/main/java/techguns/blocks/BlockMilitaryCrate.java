package techguns.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Military supply crate (1.12 BlockMilitaryCrate). Broken by a player without silk touch it drops the contents loot
 * table of its type (blocks/military_crate_* loot tables) instead of itself.
 */
public class BlockMilitaryCrate extends Block {

	protected static final VoxelShape SHAPE = box(0.5, 0, 0.5, 15.5, 16, 15.5);

	public BlockMilitaryCrate(Properties props) {
		super(props);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
