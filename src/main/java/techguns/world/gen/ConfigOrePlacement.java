package techguns.world.gen;

import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.TGConfig;
import techguns.Techguns;

/**
 * Placement modifier {@code {"type": "techguns:config_enabled", "ore": "tin"}}: lets a placed feature through only
 * while that ore's {@code worldgen.doOreGen*} config option is on. Checked as chunks generate, so it always uses the
 * current config, and datapack overrides of the ore features keep working.
 */
public class ConfigOrePlacement extends PlacementModifier {

	public enum Ore implements StringRepresentable {
		TIN("tin", () -> TGConfig.doOreGenTin),
		LEAD("lead", () -> TGConfig.doOreGenLead),
		TITANIUM("titanium", () -> TGConfig.doOreGenTitanium),
		URANIUM("uranium", () -> TGConfig.doOreGenUranium);

		public static final Codec<Ore> CODEC = StringRepresentable.fromEnum(Ore::values);
		private final String name;
		private final BooleanSupplier enabled;

		Ore(String name, BooleanSupplier enabled) {
			this.name = name;
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return enabled.getAsBoolean();
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final MapCodec<ConfigOrePlacement> CODEC = Ore.CODEC.fieldOf("ore").xmap(ConfigOrePlacement::new, p -> p.ore);

	public static final DeferredRegister<PlacementModifierType<?>> TYPES = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Techguns.MODID);
	public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<ConfigOrePlacement>> CONFIG_ENABLED = TYPES
			.register("config_enabled", () -> () -> CODEC);

	private final Ore ore;

	public ConfigOrePlacement(Ore ore) {
		this.ore = ore;
	}

	public Ore getOre() {
		return ore;
	}

	public static void register(IEventBus modBus) {
		TYPES.register(modBus);
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
		return ore.isEnabled() ? Stream.of(pos) : Stream.empty();
	}

	@Override
	public PlacementModifierType<?> type() {
		return CONFIG_ENABLED.get();
	}
}
