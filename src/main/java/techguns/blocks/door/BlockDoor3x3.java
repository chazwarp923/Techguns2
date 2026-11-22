package techguns.blocks.door;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import techguns.TGSounds;
import techguns.tileentities.Door3x3TileEnt;

/**
 * 3x3 tech / hangar door (1.12 BlockTGDoor3x3). The middle block is the master with the block entity and renders
 * the whole door, the 8 other blocks only collide. ZPLANE: the door lies in the z axis (opens along north-south).
 */
public class BlockDoor3x3 extends BaseEntityBlock {

	public static final BooleanProperty MASTER = BooleanProperty.create("master");
	public static final BooleanProperty ZPLANE = BooleanProperty.create("zplane");
	public static final EnumProperty<DoorState> STATE = EnumProperty.create("state", DoorState.class);

	public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

	public static final int BLOCK_UPDATE_DELAY = 5;
	public static final int DOOR_OPEN_TICKS = 20;
	public static final int DOOR_AUTOCLOSE_TICKS = 100;

	protected static final double SIZE = 6;
	protected static final double SIZE2 = 4;
	/** closed, door in the z plane */
	protected static final VoxelShape BB_X = box(SIZE, 0, 0, 16 - SIZE, 16, 16);
	/** closed, door in the x plane */
	protected static final VoxelShape BB_Z = box(0, 0, SIZE, 16, 16, 16 - SIZE);
	protected static final VoxelShape BB_X_OPEN_TOP = box(0, 16 - SIZE2, SIZE, 16, 16, 16 - SIZE);
	protected static final VoxelShape BB_Z_OPEN_TOP = box(SIZE, 16 - SIZE2, 0, 16 - SIZE, 16, 16);
	protected static final VoxelShape BB_X_OPEN_BOTTOM = box(0, 0, SIZE, 16, SIZE2, 16 - SIZE);
	protected static final VoxelShape BB_Z_OPEN_BOTTOM = box(SIZE, 0, 0, 16 - SIZE, SIZE2, 16);
	protected static final VoxelShape BB_Z_OPEN_LEFT = box(SIZE, 0, 0, 16 - SIZE, 16, SIZE2);
	protected static final VoxelShape BB_Z_OPEN_RIGHT = box(SIZE, 0, 16 - SIZE2, 16 - SIZE, 16, 16);
	protected static final VoxelShape BB_X_OPEN_LEFT = box(0, 0, SIZE, SIZE2, 16, 16 - SIZE);
	protected static final VoxelShape BB_X_OPEN_RIGHT = box(16 - SIZE2, 0, SIZE, 16, 16, 16 - SIZE);

	protected final Door3x3Type type;

	public BlockDoor3x3(Properties props, Door3x3Type type) {
		super(props);
		this.type = type;
		this.registerDefaultState(this.stateDefinition.any().setValue(MASTER, false).setValue(ZPLANE, false).setValue(STATE, DoorState.CLOSED));
	}

	public Door3x3Type getType() {
		return type;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		throw new UnsupportedOperationException("doors are not data driven");
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(MASTER, ZPLANE, STATE);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(MASTER) ? new Door3x3TileEnt(pos, state) : null;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	/* ---- structure ---- */

	/** the 8 other blocks, relative to the master */
	public static List<BlockPos> slaves(BlockPos master, boolean zplane) {
		Direction side = zplane ? Direction.SOUTH : Direction.EAST;
		List<BlockPos> list = new ArrayList<>(8);
		for (int i = -1; i <= 1; i++) {
			for (int y = -1; y <= 1; y++) {
				if (i != 0 || y != 0) {
					list.add(master.relative(side, i).above(y));
				}
			}
		}
		return list;
	}

	public BlockPos findMaster(BlockGetter level, BlockPos pos, BlockState state) {
		if (state.getValue(MASTER)) {
			return pos;
		}
		for (BlockPos p : slaves(pos, state.getValue(ZPLANE))) {
			BlockState s = level.getBlockState(p);
			if (s.is(this) && s.getValue(MASTER) && s.getValue(ZPLANE) == state.getValue(ZPLANE)) {
				return p;
			}
		}
		return pos;
	}

	/* ---- shapes ---- */

	protected VoxelShape getPlaneShape(BlockState state) {
		return state.getValue(ZPLANE) ? BB_X : BB_Z;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		if (!state.getValue(STATE).isOpen()) {
			return getPlaneShape(state);
		}
		BlockPos master = findMaster(level, pos, state);
		boolean z = state.getValue(ZPLANE);
		boolean frameRow = state.getValue(MASTER) || master.getY() >= pos.getY();
		return switch (type) {
			case HANGAR_UP -> frameRow ? Shapes.empty() : z ? BB_X_OPEN_TOP_FIX : BB_Z_OPEN_TOP_FIX;
			case HANGAR_DOWN -> state.getValue(MASTER) || master.getY() <= pos.getY() ? Shapes.empty() : z ? BB_X_OPEN_BOTTOM_FIX : BB_Z_OPEN_BOTTOM_FIX;
			default -> {
				if (frameRow) {
					int side = z ? Integer.compare(pos.getZ(), master.getZ()) : Integer.compare(pos.getX(), master.getX());
					if (side < 0) {
						yield z ? BB_Z_OPEN_LEFT : BB_X_OPEN_LEFT;
					} else if (side > 0) {
						yield z ? BB_Z_OPEN_RIGHT : BB_X_OPEN_RIGHT;
					}
					yield Shapes.empty();
				}
				yield z ? BB_X_OPEN_TOP_FIX : BB_Z_OPEN_TOP_FIX;
			}
		};
	}

	// the 1.12 "X" / "Z" open boxes were swapped relative to the closed ones; these follow the door plane
	protected static final VoxelShape BB_X_OPEN_TOP_FIX = BB_Z_OPEN_TOP;
	protected static final VoxelShape BB_Z_OPEN_TOP_FIX = BB_X_OPEN_TOP;
	protected static final VoxelShape BB_X_OPEN_BOTTOM_FIX = BB_Z_OPEN_BOTTOM;
	protected static final VoxelShape BB_Z_OPEN_BOTTOM_FIX = BB_X_OPEN_BOTTOM;

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(STATE).isOpen() ? Shapes.empty() : getPlaneShape(state);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return state.getValue(STATE).isOpen();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	/* ---- opening ---- */

	@Nullable
	protected SoundEvent getSoundEvent(DoorState next) {
		return switch (next) {
			case CLOSING -> TGSounds.TECHDOOR_CLOSE.get();
			case OPENING -> TGSounds.TECHDOOR_OPEN.get();
			default -> TGSounds.TECHDOOR_STATE_FINISHED.get();
		};
	}

	public void toggleState(Level level, BlockPos masterPos) {
		toggleState(level, masterPos, true);
	}

	public void toggleState(Level level, BlockPos masterPos, boolean checkNeighbours) {
		if (level.isClientSide) {
			return;
		}
		BlockState masterState = level.getBlockState(masterPos);
		if (!masterState.is(this) || !masterState.getValue(MASTER)) {
			return;
		}
		DoorState next = masterState.getValue(STATE).next();
		for (BlockPos p : slaves(masterPos, masterState.getValue(ZPLANE))) {
			BlockState s = level.getBlockState(p);
			if (s.is(this)) {
				level.setBlock(p, s.setValue(STATE, next), Block.UPDATE_ALL);
			}
		}
		level.setBlock(masterPos, masterState.setValue(STATE, next), Block.UPDATE_ALL);
		SoundEvent sound = getSoundEvent(next);
		if (sound != null) {
			level.playSound(null, masterPos, sound, SoundSource.BLOCKS, 0.9f + level.random.nextFloat() * 0.2f, 0.9f + level.random.nextFloat() * 0.2f);
		}
		if (level.getBlockEntity(masterPos) instanceof Door3x3TileEnt door) {
			door.setLastStateChangeTime(level.getGameTime());
			if (checkNeighbours && type.isHangar()) {
				checkOpenNeighbours(level, masterPos, masterState);
			}
			if (next.isMoving()) {
				level.scheduleTick(masterPos, this, DOOR_OPEN_TICKS - 1);
			} else if (door.isAutoClose() && next == DoorState.OPENED) {
				level.scheduleTick(masterPos, this, BLOCK_UPDATE_DELAY);
			}
		}
	}

	/** hangar doors open whole rows (up to 3 doors to each side) and the stacked door with them */
	protected void checkOpenNeighbours(Level level, BlockPos masterPos, BlockState oldMasterState) {
		Direction side = oldMasterState.getValue(ZPLANE) ? Direction.SOUTH : Direction.EAST;
		toggleRow(level, masterPos, side, oldMasterState);
		BlockPos other = masterPos.above(type == Door3x3Type.HANGAR_UP ? -3 : 3);
		if (checkBlockAndToggleState(level, other, oldMasterState)) {
			toggleRow(level, other, side, oldMasterState);
		}
	}

	private void toggleRow(Level level, BlockPos start, Direction side, BlockState oldMasterState) {
		for (Direction d : new Direction[] { side, side.getOpposite() }) {
			for (int i = 3; i <= 9; i += 3) {
				if (!checkBlockAndToggleState(level, start.relative(d, i), oldMasterState)) {
					break;
				}
			}
		}
	}

	protected boolean checkBlockAndToggleState(Level level, BlockPos pos, BlockState oldMasterState) {
		BlockState s = level.getBlockState(pos);
		if (s.getBlock() instanceof BlockDoor3x3 d && d.type.isHangar() && s.getValue(MASTER) && s.getValue(ZPLANE) == oldMasterState.getValue(ZPLANE)
				&& s.getValue(STATE) == oldMasterState.getValue(STATE)) {
			d.toggleState(level, pos, false);
			return true;
		}
		return false;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos master = findMaster(level, pos, state);
		if (!(level.getBlockEntity(master) instanceof Door3x3TileEnt door)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide) {
			if (!door.isUseableByPlayer(player)) {
				player.displayClientMessage(Component.translatable("techguns.container.security.denied"), true);
			} else if (player.isShiftKeyDown() && isConfigTool(player.getMainHandItem())) {
				if (player instanceof ServerPlayer sp) {
					sp.openMenu(door, buf -> buf.writeBlockPos(master));
				}
			} else if (door.isOpenWithRightClick()) {
				toggleState(level, master);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** wrenches (c:tools/wrench) or an empty hand open the settings */
	protected boolean isConfigTool(ItemStack stack) {
		return stack.isEmpty() || stack.is(WRENCHES);
	}

	/* ---- redstone / sensor ---- */

	public boolean isPowered(Level level, BlockPos masterPos, boolean zplane) {
		if (level.hasNeighborSignal(masterPos)) {
			return true;
		}
		for (BlockPos p : slaves(masterPos, zplane)) {
			if (level.hasNeighborSignal(p)) {
				return true;
			}
		}
		return false;
	}

	public void updateRedstoneState(Level level, BlockPos masterPos) {
		if (!(level.getBlockEntity(masterPos) instanceof Door3x3TileEnt door) || !door.isRedstoneMode() || door.getRedstoneBehaviour() == 0) {
			return;
		}
		BlockState ms = level.getBlockState(masterPos);
		boolean powered = isPowered(level, masterPos, ms.getValue(ZPLANE));
		DoorState s = ms.getValue(STATE);
		boolean wantOpen = door.getRedstoneBehaviour() == 1 ? powered : !powered;
		if ((wantOpen && s == DoorState.CLOSED) || (!wantOpen && s == DoorState.OPENED)) {
			toggleState(level, masterPos);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		if (!level.isClientSide && !(block instanceof BlockDoor3x3)) {
			updateRedstoneState(level, findMaster(level, pos, state));
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (!level.isClientSide && state.getValue(MASTER) && !oldState.is(this)) {
			level.scheduleTick(pos, this, BLOCK_UPDATE_DELAY);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(MASTER) || !(level.getBlockEntity(pos) instanceof Door3x3TileEnt door)) {
			return;
		}
		DoorState s = state.getValue(STATE);
		long sinceChange = level.getGameTime() - door.getLastStateChangeTime();
		if (s.isMoving()) {
			if (sinceChange >= DOOR_OPEN_TICKS) {
				toggleState(level, pos);
			} else {
				level.scheduleTick(pos, this, 1);
			}
		} else if (door.getDoormode() == 0) {
			if (door.isAutoClose() && s == DoorState.OPENED) {
				if (sinceChange > DOOR_AUTOCLOSE_TICKS) {
					toggleState(level, pos);
				} else {
					level.scheduleTick(pos, this, BLOCK_UPDATE_DELAY);
				}
			}
		} else if (door.getDoormode() == 2) {
			boolean open = checkOpenForNearPlayers(level, door, pos);
			if (open && s == DoorState.CLOSED) {
				toggleState(level, pos);
			} else if (!open && s == DoorState.OPENED && sinceChange > DOOR_OPEN_TICKS) {
				toggleState(level, pos);
			}
			level.scheduleTick(pos, this, BLOCK_UPDATE_DELAY);
		}
	}

	protected boolean checkOpenForNearPlayers(Level level, Door3x3TileEnt door, BlockPos masterPos) {
		for (Player p : level.getEntitiesOfClass(Player.class, new AABB(masterPos).inflate(2, 1, 2))) {
			if (!p.isSpectator() && door.isUseableByPlayer(p)) {
				return true;
			}
		}
		return false;
	}

	/* ---- breaking ---- */

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && player.isCreative() && !state.getValue(MASTER)) {
			// creative: take the master away without its drop
			BlockPos master = findMaster(level, pos, state);
			if (!master.equals(pos)) {
				level.setBlock(master, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!level.isClientSide && !state.is(newState.getBlock())) {
			if (state.getValue(MASTER)) {
				for (BlockPos p : slaves(pos, state.getValue(ZPLANE))) {
					BlockState s = level.getBlockState(p);
					if (s.is(this) && !s.getValue(MASTER)) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
					}
				}
			} else {
				BlockPos master = findMaster(level, pos, state);
				if (!master.equals(pos)) {
					// the master drops the door item
					level.destroyBlock(master, true);
				}
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
		return new ItemStack(this);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rot) {
		if (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) {
			return state.setValue(ZPLANE, !state.getValue(ZPLANE));
		}
		return state;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}
}
