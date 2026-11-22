package techguns.blocks;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import techguns.TGConfig;

/**
 * Ore cluster types (1.12 EnumOreClusterType). Mining level, ore amount and power multipliers come from the server
 * config.
 */
public enum OreClusterType {
	COAL(() -> TGConfig.mininglevel_coal, () -> TGConfig.oremult_coal, () -> TGConfig.powermult_coal),
	COMMON_METAL(() -> TGConfig.mininglevel_common_metal, () -> TGConfig.oremult_common_metal, () -> TGConfig.powermult_common_metal),
	RARE_METAL(() -> TGConfig.mininglevel_rare_metal, () -> TGConfig.oremult_rare_metal, () -> TGConfig.powermult_rare_metal),
	SHINY_METAL(() -> TGConfig.mininglevel_shiny_metal, () -> TGConfig.oremult_shiny_metal, () -> TGConfig.powermult_shiny_metal),
	URANIUM(() -> TGConfig.mininglevel_uranium, () -> TGConfig.oremult_uranium, () -> TGConfig.powermult_uranium),
	COMMON_GEM(() -> TGConfig.mininglevel_common_gem, () -> TGConfig.oremult_common_gem, () -> TGConfig.powermult_common_gem),
	SHINY_GEM(() -> TGConfig.mininglevel_shiny_gem, () -> TGConfig.oremult_shiny_gem, () -> TGConfig.powermult_shiny_gem),
	NETHER_CRYSTAL(() -> TGConfig.mininglevel_nether_crystal, () -> TGConfig.oremult_nether_crystal, () -> TGConfig.powermult_nether_crystal),
	OIL(() -> TGConfig.mininglevel_oil, () -> TGConfig.oremult_oil, () -> TGConfig.powermult_oil);

	private final IntSupplier miningLevel;
	private final DoubleSupplier amountMultiplier;
	private final DoubleSupplier powerMultiplier;

	OreClusterType(IntSupplier miningLevel, DoubleSupplier amountMultiplier, DoubleSupplier powerMultiplier) {
		this.miningLevel = miningLevel;
		this.amountMultiplier = amountMultiplier;
		this.powerMultiplier = powerMultiplier;
	}

	public String getName() {
		return name().toLowerCase();
	}

	public int getMiningLevel() {
		return miningLevel.getAsInt();
	}

	public double getAmountMultiplier() {
		return amountMultiplier.getAsDouble();
	}

	public double getPowerMultiplier() {
		return powerMultiplier.getAsDouble();
	}
}
