package techguns.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.blocks.door.BlockDoor3x3;
import techguns.gui.containers.Door3x3Menu;

/**
 * Master block of a 3x3 door (1.12 Door3x3TileEntity): door mode (manual / redstone / player sensor), auto close and
 * the time of the last state change for the animation. The change time is taken from the game time whenever the
 * door state changes, on both sides.
 */
public class Door3x3TileEnt extends BasicRedstoneTileEnt {

	public static final int BUTTON_ID_DOORMODE = BUTTON_ID_REDSTONE + 1;
	public static final int BUTTON_ID_AUTOCLOSE = BUTTON_ID_REDSTONE + 2;

	protected long lastStateChangeTime = 0L;
	/** 0 manual, 1 redstone, 2 player sensor */
	protected byte doormode = 0;
	protected boolean autoClose = true;

	public Door3x3TileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.DOOR3x3.get(), pos, state, 0, false);
	}

	@Override
	public void setBlockState(BlockState state) {
		BlockState old = this.getBlockState();
		super.setBlockState(state);
		if (this.level != null && old.hasProperty(BlockDoor3x3.STATE) && state.hasProperty(BlockDoor3x3.STATE)
				&& old.getValue(BlockDoor3x3.STATE) != state.getValue(BlockDoor3x3.STATE)) {
			this.lastStateChangeTime = this.level.getGameTime();
		}
	}

	public long getLastStateChangeTime() {
		return lastStateChangeTime;
	}

	public void setLastStateChangeTime(long time) {
		this.lastStateChangeTime = time;
	}

	public boolean isOpenWithRightClick() {
		return doormode == 0;
	}

	public boolean isRedstoneMode() {
		return doormode == 1;
	}

	public boolean isPlayerDetector() {
		return doormode == 2;
	}

	public boolean isAutoClose() {
		return autoClose;
	}

	public void setAutoClose(boolean autoClose) {
		boolean schedule = !this.autoClose && autoClose;
		this.autoClose = autoClose;
		if (schedule) {
			scheduleDoorTick();
		}
	}

	public byte getDoormode() {
		return doormode;
	}

	public void setDoormode(byte doormode) {
		this.doormode = doormode;
		if (doormode == 0 || doormode == 2) {
			scheduleDoorTick();
		}
	}

	/** menu data sync, no tick scheduling */
	public void syncDoormode(byte doormode) {
		this.doormode = doormode;
	}

	public void syncAutoClose(boolean autoClose) {
		this.autoClose = autoClose;
	}

	protected void scheduleDoorTick() {
		if (this.level != null && !this.level.isClientSide) {
			this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), BlockDoor3x3.BLOCK_UPDATE_DELAY);
		}
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id == BUTTON_ID_AUTOCLOSE) {
			if (this.isUseableByPlayer(ply)) {
				this.setAutoClose(!this.autoClose);
				this.needUpdate();
			}
		} else if (id == BUTTON_ID_DOORMODE) {
			if (this.isUseableByPlayer(ply)) {
				this.setDoormode((byte) ((this.doormode + 1) % 3));
				this.needUpdate();
			}
		} else {
			super.buttonClicked(id, ply);
		}
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new Door3x3Menu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.door3x3");
	}

	@Override
	protected int getUseRangeSquared() {
		return 100;
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.autoClose = !tags.contains("autoClose") || tags.getBoolean("autoClose");
		this.doormode = tags.getByte("doormode");
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		tags.putBoolean("autoClose", this.autoClose);
		tags.putByte("doormode", this.doormode);
	}
}
