package techguns.plugins.jade;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import techguns.factions.TGTeams;
import techguns.tileentities.BasicMachineTileEnt;
import techguns.tileentities.BasicOwnedTileEnt;
import techguns.tileentities.MultiBlockSlaveTileEnt;

/**
 * What the Jade overlay shows for a Techguns machine, written on the server: owner name, security level and the
 * progress of the current operation. Any part of a formed multiblock shows its controller. No Jade classes here,
 * so this side is testable without Jade; {@link TGJadePlugin} sends and draws it.
 * <p>
 * Energy, items and fluids are not here: Jade shows those itself from the NeoForge capabilities the machines expose.
 */
public final class MachineInfo {

	public static final String OWNER = "tg_owner";
	public static final String SECURITY = "tg_security";
	public static final String PROGRESS = "tg_progress";
	public static final String TOTAL = "tg_total";

	private MachineInfo() {
	}

	/** the block entity whose data a looked-at block shows: the controller for a multiblock part */
	@Nullable
	public static BlockEntity target(@Nullable BlockEntity be) {
		if (be instanceof MultiBlockSlaveTileEnt slave) {
			return slave.hasMaster() ? slave.getMaster() : null;
		}
		return be;
	}

	public static void write(CompoundTag tag, @Nullable BlockEntity looked, @Nullable MinecraftServer server) {
		BlockEntity be = target(looked);
		if (be instanceof BasicOwnedTileEnt owned && owned.getOwner() != null) {
			UUID owner = owned.getOwner();
			String name = server == null ? null : TGTeams.lookupName(server, owner);
			tag.putString(OWNER, name != null ? name : owner.toString().substring(0, 8));
			tag.putByte(SECURITY, owned.getSecurity());
		}
		if (be instanceof BasicMachineTileEnt machine && machine.totaltime > 0) {
			tag.putInt(PROGRESS, machine.progress);
			tag.putInt(TOTAL, machine.totaltime);
		}
	}

	/** progress 0..1, or -1 when the machine isn't working */
	public static float progress(CompoundTag tag) {
		int total = tag.getInt(TOTAL);
		return total > 0 ? Math.min(1.0f, tag.getInt(PROGRESS) / (float) total) : -1.0f;
	}
}
