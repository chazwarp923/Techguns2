package techguns.tileentities;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import techguns.factions.TGTeams;

/**
 * Block entity with an owner (1.12 BasicOwnedTileEnt). Security: 0 anyone, 1 allies + team, 2 team, 3 owner only.
 * Levels 1 and 2 need FTB Teams (see {@link TGTeams}), without it the button toggles between 0 and 3 like in 1.12.
 */
public abstract class BasicOwnedTileEnt extends BasicInventoryTileEnt {

	public static final int BUTTON_ID_SECURITY = 1;
	public static final int BUTTON_ID_REDSTONE = BUTTON_ID_SECURITY + 1;

	protected byte security = 0;
	@Nullable
	protected UUID owner;

	public BasicOwnedTileEnt(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize, boolean hasRotation) {
		super(type, pos, state, inventorySize, hasRotation);
	}

	public void setOwner(Player ply) {
		this.owner = ply.getUUID();
		this.security = 0;
	}

	public boolean isOwnedByPlayer(Player ply) {
		return this.owner != null && this.owner.equals(ply.getUUID());
	}

	@Nullable
	public UUID getOwner() {
		return this.owner;
	}

	public byte getSecurity() {
		return this.security;
	}

	public void setSecurity(byte security) {
		this.security = security;
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.security = tags.getByte("security");
		this.owner = tags.hasUUID("owner") ? tags.getUUID("owner") : null;
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		if (this.owner != null) {
			tags.putByte("security", this.security);
			tags.putUUID("owner", this.owner);
		}
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id == BUTTON_ID_SECURITY) {
			if (this.isOwnedByPlayer(ply)) {
				if (TGTeams.isTeamModLoaded()) {
					this.security = (byte) ((this.security + 1) % 4);
				} else {
					this.security = (byte) (this.security == 0 ? 3 : 0);
				}
				this.needUpdate();
			}
		} else {
			super.buttonClicked(id, ply);
		}
	}

	@Override
	public boolean isUseableByPlayer(Player player) {
		if (!super.isUseableByPlayer(player)) {
			return false;
		}
		if (this.owner == null || this.security == 0 || this.isOwnedByPlayer(player)) {
			return true;
		}
		// team data only exists on the server, the client menu follows what the server decided
		if (this.level != null && this.level.isClientSide) {
			return true;
		}
		return TGTeams.canAccess(this.owner, player.getUUID(), this.security);
	}
}
