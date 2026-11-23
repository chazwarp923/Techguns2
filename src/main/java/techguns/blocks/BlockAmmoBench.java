package techguns.blocks;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import techguns.gui.containers.AmmoBenchMenu;

/** Ammo bench (Fabric port AmmoBenchBlock): a workbench that opens the ammo bench menu. Faces the player who placed it. */
public class BlockAmmoBench extends HorizontalDirectionalBlock {
	public static final MapCodec<BlockAmmoBench> CODEC = simpleCodec(BlockAmmoBench::new);
	private static final Component TITLE = Component.translatable("techguns.container.ammobench");

	/** the Fabric port's shape: the body, the table top and the back board; the model faces north unrotated */
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			SHAPES.put(d, Shapes.or(box(d, 1, 0, 1, 15, 8, 15), box(d, 0, 8, 0, 16, 10, 16), box(d, 1, 10, 14, 15, 16, 16)));
		}
	}

	/** a box of the north-facing model, turned to face d (the blockstate's y rotation) */
	private static VoxelShape box(Direction d, double x0, double y0, double z0, double x1, double y1, double z1) {
		return switch (d) {
			case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
	}

	public BlockAmmoBench(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		player.openMenu(state.getMenuProvider(level, pos));
		return InteractionResult.CONSUME;
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider((id, inv, player) -> new AmmoBenchMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE);
	}
}
