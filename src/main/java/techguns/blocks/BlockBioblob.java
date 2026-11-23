package techguns.blocks;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import techguns.TGBlocks;
import techguns.TGSounds;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGExplosion;
import techguns.deatheffects.DeathType;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;

/**
 * 1.12 BlockBioblob + BioBlobTileEnt: goo left by bio gun shots. Shrinks every 3 seconds and vanishes; hitting it with
 * more bio shots grows it until it bursts in a poison explosion. FACING is the side it sticks to.
 */
public class BlockBioblob extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 2);
	public static final int SHRINK_TICKS = 60;

	/** 1.12 sizes: inset W and height H in pixels per size */
	private static final int[] W = { 5, 3, 1 };
	private static final int[] H = { 3, 4, 5 };
	private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction d : Direction.values()) {
			VoxelShape[] arr = new VoxelShape[3];
			for (int s = 0; s < 3; s++) {
				int w = W[s], h = H[s];
				arr[s] = switch (d) {
					case DOWN -> box(w, 0, w, 16 - w, h, 16 - w);
					case UP -> box(w, 16 - h, w, 16 - w, 16, 16 - w);
					case NORTH -> box(w, w, 0, 16 - w, 16 - w, h);
					case SOUTH -> box(w, w, 16 - h, 16 - w, 16 - w, 16);
					case WEST -> box(0, w, w, h, 16 - w, 16 - w);
					case EAST -> box(16 - h, w, w, 16, 16 - w, 16 - w);
				};
			}
			SHAPES.put(d, arr);
		}
	}

	public BlockBioblob(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(SIZE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SIZE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES.get(state.getValue(FACING))[state.getValue(SIZE)];
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		if (!old.is(this)) {
			level.scheduleTick(pos, this, SHRINK_TICKS);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int size = state.getValue(SIZE);
		if (size > 0) {
			level.setBlock(pos, state.setValue(SIZE, size - 1), 3);
			level.scheduleTick(pos, this, SHRINK_TICKS);
		} else {
			level.removeBlock(pos, false);
		}
	}

	/** grows the blob by power, bursts above max size (1.12 hitBlob) */
	public static void hitBlob(Level level, BlockPos pos, int power, @Nullable LivingEntity attacker) {
		BlockState state = level.getBlockState(pos);
		if (level.isClientSide || !(state.getBlock() instanceof BlockBioblob)) {
			return;
		}
		int size = state.getValue(SIZE) + power;
		if (size > 2) {
			float radius = 3.0f;
			TGDamageSource dmgSrc = TGDamageSource.causePoisonDamage(level, null, attacker, DeathType.BIO);
			dmgSrc.goreChance = 1.0f;
			dmgSrc.armorPenetration = 0.35f;
			double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
			TGExplosion explosion = new TGExplosion(level, attacker, null, x, y, z, 30, 15, radius, radius * 1.5f, 0.0f);
			explosion.setDmgSrc(dmgSrc);
			level.removeBlock(pos, false);
			explosion.doExplosion(false);
			level.playSound(null, pos, TGSounds.DEATH_BIO.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
			TGPackets.sendToAllAround(new PacketSpawnParticle("bioblobExplosion", x, y, z), level, x, y, z, 50.0);
		} else {
			level.setBlock(pos, state.setValue(SIZE, size), 3);
		}
	}

	/**
	 * bio gun impact: grows a blob that was hit, or sticks a new one onto the hit face (only for shots with block damage,
	 * and only if a player shooter is allowed to place blocks there)
	 */
	public static void onBioShotHit(Level level, BlockPos hitPos, Direction side, int chargeLevel, boolean blockdamage, @Nullable LivingEntity shooter) {
		if (level.isClientSide) {
			return;
		}
		if (level.getBlockState(hitPos).getBlock() instanceof BlockBioblob) {
			hitBlob(level, hitPos, chargeLevel, shooter);
			return;
		}
		BlockPos blobPos = hitPos.relative(side);
		BlockState there = level.getBlockState(blobPos);
		if (there.getBlock() instanceof BlockBioblob) {
			hitBlob(level, blobPos, chargeLevel, shooter);
			return;
		}
		if (!blockdamage || !there.isAir()) {
			return;
		}
		int size = Math.max(0, Math.min(2, chargeLevel - 1));
		BlockState blob = TGBlocks.BIOBLOB.get().defaultBlockState().setValue(FACING, side.getOpposite()).setValue(SIZE, size);
		if (shooter instanceof Entity e) {
			BlockSnapshot snap = BlockSnapshot.create(level.dimension(), level, blobPos);
			level.setBlock(blobPos, blob, 3);
			if (EventHooks.onBlockPlace(e, snap, side)) {
				snap.restore(snap.getFlags());
			}
		} else {
			level.setBlock(blobPos, blob, 3);
		}
	}
}
