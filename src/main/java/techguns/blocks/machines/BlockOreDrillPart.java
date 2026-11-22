package techguns.blocks.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import techguns.blocks.BlockOreCluster;

/**
 * Ore drill parts (1.12 BlockOreDrill machine types frame, scaffold, rod and engine). Formed parts keep their model
 * except the rods (the controller renders the drill) and the hidden scaffolds in front of the drill head.
 */
public class BlockOreDrillPart extends BlockMultiBlockSlave {

	public BlockOreDrillPart(Properties props) {
		super(props);
	}

	/** scaffold: faces between formed scaffolds are not drawn; the inner scaffolds of the last layer are hidden */
	public static class Scaffold extends BlockOreDrillPart {
		public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");

		public Scaffold(Properties props) {
			super(props);
			this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false).setValue(HIDDEN, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(FORMED, HIDDEN);
		}

		@Override
		protected boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
			if (adjacent.is(this) && state.getValue(FORMED) && adjacent.getValue(FORMED) && !adjacent.getValue(HIDDEN)) {
				return true;
			}
			return super.skipRendering(state, adjacent, dir);
		}
	}

	/**
	 * rod: only one rod line per ore cluster, and the rod line has to start at a cluster (1.12 ItemBlockOreDrill)
	 */
	public static class Rod extends BlockOreDrillPart {
		public Rod(Properties props) {
			super(props);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext ctx) {
			Level level = ctx.getLevel();
			BlockPos pos = ctx.getClickedPos();
			String error = null;
			BlockPos cluster = findNeighbour(level, pos, s -> s.getBlock() instanceof BlockOreCluster);
			if (cluster != null) {
				if (clusterHasRod(level, cluster)) {
					error = "techguns.msg.error.alreadyrod";
				}
			} else if (findNeighbour(level, pos, s -> s.is(this)) == null) {
				error = "techguns.msg.error.rodplacement";
			}
			if (error != null) {
				Player player = ctx.getPlayer();
				if (player != null && level.isClientSide) {
					player.displayClientMessage(Component.translatable(error), true);
				}
				return null;
			}
			return super.getStateForPlacement(ctx);
		}

		/** does a rod touch the connected cluster blocks of the same type */
		private boolean clusterHasRod(Level level, BlockPos start) {
			BlockState cluster = level.getBlockState(start);
			java.util.Set<BlockPos> visited = new java.util.HashSet<>();
			java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
			queue.add(start);
			visited.add(start);
			while (!queue.isEmpty() && visited.size() < 4096) {
				BlockPos p = queue.poll();
				for (Direction d : Direction.values()) {
					BlockPos n = p.relative(d);
					if (visited.contains(n) || !level.isLoaded(n)) {
						continue;
					}
					BlockState s = level.getBlockState(n);
					if (s.is(this)) {
						return true;
					}
					if (s == cluster) {
						visited.add(n);
						queue.add(n);
					}
				}
			}
			return false;
		}
	}

	public static BlockPos findNeighbour(Level level, BlockPos pos, java.util.function.Predicate<BlockState> test) {
		for (Direction d : Direction.values()) {
			if (test.test(level.getBlockState(pos.relative(d)))) {
				return pos.relative(d);
			}
		}
		return null;
	}

	/** controller: two controllers next to each other would fight over the drill */
	public static class Controller extends BlockMultiBlockController<techguns.tileentities.OreDrillTileEntMaster> {
		public Controller(Properties props) {
			super(props, techguns.TGBlockEntities.ORE_DRILL);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext ctx) {
			if (findNeighbour(ctx.getLevel(), ctx.getClickedPos(), s -> s.is(this)) != null) {
				Player player = ctx.getPlayer();
				if (player != null && ctx.getLevel().isClientSide) {
					player.displayClientMessage(Component.translatable("techguns.msg.error.oredrill.controllernext"), true);
				}
				return null;
			}
			return super.getStateForPlacement(ctx);
		}
	}
}
