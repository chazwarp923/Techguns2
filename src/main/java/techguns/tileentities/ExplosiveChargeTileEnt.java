package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import techguns.TGBlockEntities;
import techguns.TGConfig;
import techguns.TGSounds;
import techguns.blocks.machines.BlockExplosiveCharge;
import techguns.gui.containers.ExplosiveChargeMenu;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;

/**
 * Mining charge (1.12 ExplosiveChargeTileEnt / ExplosiveChargeAdvTileEnt). Once armed it counts down and then breaks
 * a square of block lines into the block it is attached to. Blocks are broken in the name of the owner, so
 * protection mods can cancel it.
 */
public class ExplosiveChargeTileEnt extends BasicOwnedTileEnt {

	public static final int BUTTON_ID_DETONATE = 10;
	public static final int BUTTON_ID_RADIUS_PLUS = 11;
	public static final int BUTTON_ID_RADIUS_MINUS = 12;
	public static final int BUTTON_ID_LENGTH_PLUS = 13;
	public static final int BUTTON_ID_LENGTH_MINUS = 14;
	public static final int BUTTON_ID_FUSE_PLUS = 15;
	public static final int BUTTON_ID_FUSE_MINUS = 16;

	protected final boolean advanced;

	protected short maxBlastradius;
	protected short minBlastradius = 3;
	protected short minBlastLength = 2;
	protected short maxBlastLength;
	protected short minFuseTime = 60;
	protected short maxFuseTime = 200;
	protected short blastradius;
	protected short blastlength;
	protected short fusetime = 100;
	protected boolean armed = false;

	public ExplosiveChargeTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.EXPLOSIVE_CHARGE.get(), pos, state, 0, false);
		this.advanced = state.getBlock() instanceof BlockExplosiveCharge b && b.isAdvanced();
		if (advanced) {
			this.maxBlastradius = 7;
			this.maxBlastLength = 10;
			this.blastradius = 5;
			this.blastlength = 10;
		} else {
			this.maxBlastradius = 3;
			this.maxBlastLength = 5;
			this.blastradius = 3;
			this.blastlength = 3;
		}
	}

	public boolean isAdvanced() {
		return advanced;
	}

	public SoundEvent getSoundPlant() {
		return advanced ? TGSounds.C4_PLANT.get() : TGSounds.TNT_PLANT.get();
	}

	public SoundEvent getTickSound() {
		return advanced ? TGSounds.C4_TICK.get() : TGSounds.TNT_TICK.get();
	}

	public SoundEvent getInitSound() {
		return advanced ? TGSounds.C4_INIT.get() : TGSounds.TNT_INIT.get();
	}

	protected boolean canBreakBlockHardness(float hardness) {
		return advanced ? hardness >= 0 && hardness <= TGConfig.explosiveChargeAdvancedMaxBlockHardness
				: hardness >= 0 && hardness < TGConfig.explosiveChargeMaxBlockHardness;
	}

	protected int getBlastRadiusForCuboid() {
		return this.blastradius / 2;
	}

	protected Direction getOrientation() {
		BlockState state = this.getBlockState();
		return state.hasProperty(BlockExplosiveCharge.FACING) ? state.getValue(BlockExplosiveCharge.FACING) : Direction.DOWN;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ExplosiveChargeTileEnt te) {
		if (!te.armed) {
			return;
		}
		te.fusetime--;
		if (te.fusetime > 0 && te.fusetime % 20 == 0) {
			level.playSound(null, pos, te.getTickSound(), SoundSource.BLOCKS, 4.0f, 1.0f);
		}
		if (te.fusetime <= 0) {
			te.explode();
		}
	}

	protected void explode() {
		if (!(this.level instanceof ServerLevel sl)) {
			return;
		}
		BlockPos pos = this.getBlockPos();
		sl.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f,
				(1.0f + (sl.random.nextFloat() - sl.random.nextFloat()) * 0.2f) * 0.7f);
		Direction facing = this.getOrientation();
		Direction side1;
		Direction side2;
		if (facing.getAxis() == Direction.Axis.X) {
			side1 = Direction.NORTH;
			side2 = Direction.UP;
		} else if (facing.getAxis() == Direction.Axis.Z) {
			side1 = Direction.EAST;
			side2 = Direction.UP;
		} else {
			side1 = Direction.NORTH;
			side2 = Direction.EAST;
		}
		ServerPlayer owner = this.owner != null ? (ServerPlayer) sl.getPlayerByUUID(this.owner) : null;
		sl.removeBlock(pos, false);
		int rad = this.getBlastRadiusForCuboid();
		for (BlockPos p : BlockPos.betweenClosed(pos.relative(side1, rad).relative(side2, rad), pos.relative(side1.getOpposite(), rad).relative(side2.getOpposite(), rad))) {
			explodeLine(sl, owner, p.immutable(), facing);
		}
	}

	protected void explodeLine(ServerLevel sl, ServerPlayer owner, BlockPos start, Direction direction) {
		BlockPos.MutableBlockPos pos = start.mutable();
		for (int i = 0; i < this.blastlength; i++) {
			pos.move(direction);
			if (explodeBlock(sl, owner, pos.immutable())) {
				return;
			}
		}
	}

	/** @return true when the explosion is blocked */
	protected boolean explodeBlock(ServerLevel sl, ServerPlayer owner, BlockPos p) {
		BlockState bs = sl.getBlockState(p);
		if (owner == null || !this.canBreakBlockHardness(bs.getDestroySpeed(sl, p))) {
			return true;
		}
		if (!bs.isAir()) {
			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(sl, p, bs, owner);
			if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
				return true;
			}
			Block.dropResources(bs, sl, p, sl.getBlockEntity(p), owner, net.minecraft.world.item.ItemStack.EMPTY);
			sl.removeBlock(p, false);
		}
		if (sl.random.nextFloat() > 0.5f) {
			TGPackets.sendToAllAround(new PacketSpawnParticle("MiningChargeBlockExplosion", p.getX() + 0.5d, p.getY() + 0.5d, p.getZ() + 0.5d), sl,
					p.getX() + 0.5d, p.getY() + 0.5d, p.getZ() + 0.5d, 50.0d);
		}
		return false;
	}

	public short getBlastradius() {
		return blastradius;
	}

	public short getBlastlength() {
		return blastlength;
	}

	public short getFusetime() {
		return fusetime;
	}

	public void setBlastradius(int v) {
		this.blastradius = (short) v;
	}

	public void setBlastlength(int v) {
		this.blastlength = (short) v;
	}

	public void setFusetime(int v) {
		this.fusetime = (short) v;
	}

	public boolean isArmed() {
		return armed;
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (this.armed) {
			return;
		}
		switch (id) {
			case BUTTON_ID_DETONATE -> {
				if (this.isUseableByPlayer(ply) && this.level != null && !this.level.isClientSide) {
					this.armed = true;
					this.level.playSound(null, this.getBlockPos(), this.getInitSound(), SoundSource.BLOCKS, 4.0f, 1.0f);
					BlockState state = this.getBlockState();
					if (state.hasProperty(BlockExplosiveCharge.ARMED)) {
						this.level.setBlock(this.getBlockPos(), state.setValue(BlockExplosiveCharge.ARMED, true), Block.UPDATE_ALL);
					}
					this.needUpdate();
					ply.closeContainer();
				}
			}
			case BUTTON_ID_RADIUS_PLUS -> change(blastradius < maxBlastradius, () -> blastradius++);
			case BUTTON_ID_RADIUS_MINUS -> change(blastradius > minBlastradius, () -> blastradius--);
			case BUTTON_ID_LENGTH_PLUS -> change(blastlength < maxBlastLength, () -> blastlength++);
			case BUTTON_ID_LENGTH_MINUS -> change(blastlength > minBlastLength, () -> blastlength--);
			case BUTTON_ID_FUSE_PLUS -> change(fusetime < maxFuseTime, () -> fusetime += 20);
			case BUTTON_ID_FUSE_MINUS -> change(fusetime > minFuseTime, () -> fusetime -= 20);
			default -> super.buttonClicked(id, ply);
		}
	}

	private void change(boolean allowed, Runnable r) {
		if (allowed) {
			r.run();
			this.needUpdate();
		}
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return armed ? null : new ExplosiveChargeMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		if (tags.contains("blastradius")) {
			this.blastradius = tags.getShort("blastradius");
			this.blastlength = tags.getShort("blastlength");
			this.fusetime = tags.getShort("fusetime");
		}
		this.armed = tags.getBoolean("armed");
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putShort("blastradius", blastradius);
		tags.putShort("blastlength", blastlength);
		tags.putShort("fusetime", fusetime);
		tags.putBoolean("armed", armed);
	}
}
