package techguns.tileentities;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;

/**
 * Part of a formed multiblock (1.12 MultiBlockMachineTileEntSlave): knows its master (controller) position and its
 * part type. Housing parts forward item and energy capabilities to the master.
 */
public class MultiBlockSlaveTileEnt extends BlockEntity {

	@Nullable
	protected BlockPos masterPos;
	/** 0 - unformed, other values depend on the multiblock */
	protected byte type = 0;

	public MultiBlockSlaveTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.MULTIBLOCK_SLAVE.get(), pos, state);
	}

	public boolean hasMaster() {
		return masterPos != null;
	}

	@Nullable
	public BlockPos getMasterPos() {
		return masterPos;
	}

	public int getSlaveType() {
		return type;
	}

	@Nullable
	public MultiBlockMachineTileEntMaster getMaster() {
		if (masterPos != null && level != null && level.isLoaded(masterPos)
				&& level.getBlockEntity(masterPos) instanceof MultiBlockMachineTileEntMaster m) {
			return m;
		}
		return null;
	}

	/** capability requests go to the master when it wants this part to forward them */
	@Nullable
	public BlockEntity getCapabilityTarget(net.neoforged.neoforge.capabilities.BlockCapability<?, ?> cap) {
		MultiBlockMachineTileEntMaster m = getMaster();
		return m != null && m.forwardsCapability(type, cap) ? m : null;
	}

	public void form(BlockPos masterPos, byte type) {
		this.masterPos = masterPos.immutable();
		this.type = type;
		this.sync();
	}

	public void unform() {
		this.masterPos = null;
		this.type = 0;
		this.sync();
	}

	protected void sync() {
		this.setChanged();
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
			level.invalidateCapabilities(worldPosition);
			// many cable / pipe mods only look for capabilities again on a neighbor update
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		}
	}

	/** block broken: break the whole multiblock */
	public void onBlockBreak() {
		if (level != null && !level.isClientSide) {
			MultiBlockMachineTileEntMaster m = getMaster();
			if (m != null) {
				m.onMultiBlockBreak();
			}
		}
	}

	protected void writeData(CompoundTag tag) {
		if (masterPos != null) {
			tag.put("master", NbtUtils.writeBlockPos(masterPos));
			tag.putByte("type", type);
		}
	}

	protected void readData(CompoundTag tag) {
		this.masterPos = NbtUtils.readBlockPos(tag, "master").orElse(null);
		this.type = masterPos != null ? tag.getByte("type") : 0;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		writeData(tag);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		readData(tag);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		writeData(tag);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
