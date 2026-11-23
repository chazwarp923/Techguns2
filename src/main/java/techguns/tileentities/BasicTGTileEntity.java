package techguns.tileentities;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base of all Techguns block entities (1.12 BasicTGTileEntity). Everything the client needs (rotation, owner,
 * machine progress, ...) is written by {@link #writeClientData} and sent with the chunk / block update packets.
 */
public abstract class BasicTGTileEntity extends BlockEntity implements net.minecraft.world.MenuProvider {

	/** Has this block entity a rotation (0-3, 90 degree steps) saved? */
	protected final boolean hasRotation;
	public byte rotation = 0;

	public BasicTGTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean hasRotation) {
		super(type, pos, state);
		this.hasRotation = hasRotation;
	}

	@Override
	public abstract Component getDisplayName();

	/** the gui of this block entity, null if it has none */
	@Nullable
	@Override
	public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
		return null;
	}

	protected int getUseRangeSquared() {
		return 64;
	}

	public boolean isUseableByPlayer(Player player) {
		if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
			return false;
		}
		return player.distanceToSqr(worldPosition.getX() + 0.5d, worldPosition.getY() + 0.5d, worldPosition.getZ() + 0.5d) <= getUseRangeSquared();
	}

	/** all data the client needs too */
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		if (this.hasRotation) {
			tags.putByte("rotation", this.rotation);
		}
	}

	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		if (this.hasRotation) {
			byte old = this.rotation;
			this.rotation = (byte) (tags.getByte("rotation") % 4);
			if (old != this.rotation && this.level != null && this.level.isClientSide) {
				// JSON machine models are rotated by the model data: re-mesh the block
				this.requestModelDataUpdate();
				this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 8);
			}
		}
	}

	/** rotation of block models that turn with the block entity (see techguns.client.models.RotatedBlockModel) */
	public static final net.neoforged.neoforge.client.model.data.ModelProperty<Integer> ROTATION = new net.neoforged.neoforge.client.model.data.ModelProperty<>();

	@Override
	public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
		return this.hasRotation ? net.neoforged.neoforge.client.model.data.ModelData.builder().with(ROTATION, (int) this.rotation).build()
				: net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		this.writeClientData(tag, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.readClientData(tag, registries);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		this.writeClientData(tag, registries);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		this.readClientData(tag, registries);
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
		this.readClientData(pkt.getTag(), registries);
	}

	public boolean hasRotation() {
		return hasRotation;
	}

	public void rotateTile() {
		if (this.level != null && !this.level.isClientSide && this.hasRotation) {
			this.rotation = (byte) ((this.rotation + 1) % 4);
			this.needUpdate();
		}
	}

	/** server side: save and send the client data to everyone tracking the chunk */
	public void needUpdate() {
		this.setChanged();
		if (this.level != null && !this.level.isClientSide) {
			BlockState state = this.getBlockState();
			this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}
}
