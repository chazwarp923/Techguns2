package techguns.blocks.machines.multiblocks;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import techguns.tileentities.FabricatorTileEntMaster;
import techguns.tileentities.MultiBlockMachineTileEntMaster;

/** Schematic for each multiblock controller (1.12 MultiBlockRegister). */
public final class MultiBlockRegister {

	private static final Map<Class<? extends MultiBlockMachineTileEntMaster>, MultiBlockSchematic> REGISTER = new HashMap<>();

	static {
		REGISTER.put(FabricatorTileEntMaster.class, new FabricatorSchematic());
		REGISTER.put(techguns.tileentities.ReactionChamberTileEntMaster.class, new ReactionChamberSchematic());
		REGISTER.put(techguns.tileentities.OreDrillTileEntMaster.class, new OreDrillSchematic());
	}

	private MultiBlockRegister() {
	}

	@Nullable
	public static MultiBlockSchematic get(MultiBlockMachineTileEntMaster master) {
		return REGISTER.get(master.getClass());
	}
}
