package techguns.blocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;
import techguns.TGBlockEntities;
import techguns.TGEntities;
import techguns.tileentities.TGSpawnerTileEnt;

/** 1.12 BlockTGSpawner: unbreakable dungeon spawner (a hole in the floor, or a soldier spawn marker). */
public class BlockTGSpawner extends BaseEntityBlock {

	public enum Type implements StringRepresentable {
		HOLE, SOLDIER_SPAWN;

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}

	public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);
	public static final MapCodec<BlockTGSpawner> CODEC = simpleCodec(BlockTGSpawner::new);
	private static final VoxelShape SHAPE = box(2, 0, 2, 14, 2, 14);

	public BlockTGSpawner(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, Type.HOLE));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TGSpawnerTileEnt(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, TGBlockEntities.TG_SPAWNER.get(), TGSpawnerTileEnt::serverTick);
	}

	/** placed by hand: default mob for the type (structures configure their spawners themselves) */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof TGSpawnerTileEnt spawner) {
			switch (state.getValue(TYPE)) {
				case HOLE -> spawner.addMobType(TGEntities.ZOMBIE_SOLDIER.get(), 1);
				case SOLDIER_SPAWN -> spawner.addMobType(TGEntities.ARMY_SOLDIER.get(), 1);
			}
		}
	}
}
