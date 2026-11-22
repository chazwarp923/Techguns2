package techguns.world.gen;

import java.util.Comparator;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import techguns.TGConfig;

/**
 * 1.12 TGFluids.OIL_WORLDSPAWN: the first fluid of the FluidListOilWorldspawn config that has a placeable block. Entries
 * are full ids ("immersivepetroleum:crude_oil") or, like 1.12, bare names matched against every mod's fluids.
 */
public final class WorldspawnOil {

	private static boolean resolved;
	private static BlockState oil;

	private WorldspawnOil() {
	}

	/** source block of the worldspawn oil, or null when no installed mod adds one */
	@Nullable
	public static synchronized BlockState block() {
		if (!resolved) {
			resolved = true;
			oil = find();
		}
		return oil;
	}

	private static BlockState find() {
		if (TGConfig.fluidListOilWorldspawn == null) {
			return null;
		}
		for (String name : TGConfig.fluidListOilWorldspawn) {
			if (name.contains(":")) {
				ResourceLocation id = ResourceLocation.tryParse(name);
				BlockState s = id == null ? null : block(BuiltInRegistries.FLUID.get(id));
				if (s != null) {
					return s;
				}
			} else {
				// sorted so every run picks the same mod's fluid
				BlockState s = BuiltInRegistries.FLUID.keySet().stream().filter(id -> id.getPath().equals(name))
						.sorted(Comparator.comparing(ResourceLocation::toString)).map(id -> block(BuiltInRegistries.FLUID.get(id)))
						.filter(b -> b != null).findFirst().orElse(null);
				if (s != null) {
					return s;
				}
			}
		}
		return null;
	}

	@Nullable
	private static BlockState block(Fluid fluid) {
		if (fluid == null || fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) {
			return null;
		}
		BlockState s = fluid.defaultFluidState().createLegacyBlock();
		return s.getBlock() instanceof LiquidBlock ? s : null;
	}
}
