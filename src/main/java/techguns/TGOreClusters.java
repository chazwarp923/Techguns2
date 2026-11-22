package techguns;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import techguns.blocks.OreClusterType;
import techguns.tileentities.operation.MachineOperation;

/**
 * What ore drills get out of each ore cluster type (1.12 TGOreClusters). Entries are items, the first item of an item
 * tag (other mods' ores) or the first source fluid of a fluid tag (oil). Entries that resolve to nothing are skipped.
 */
public final class TGOreClusters {

	private TGOreClusters() {
	}

	public record Entry(Supplier<ItemStack> item, Supplier<FluidStack> fluid, int weight) {
		public ItemStack getItem() {
			return item.get();
		}

		public FluidStack getFluid() {
			return fluid.get();
		}

		public boolean isEmpty() {
			return getItem().isEmpty() && getFluid().isEmpty();
		}
	}

	private static final Map<OreClusterType, List<Entry>> ENTRIES = new EnumMap<>(OreClusterType.class);

	private static void add(OreClusterType type, Supplier<? extends Item> item, int weight) {
		ENTRIES.computeIfAbsent(type, t -> new ArrayList<>()).add(new Entry(() -> new ItemStack(item.get()), () -> FluidStack.EMPTY, weight));
	}

	private static void addTag(OreClusterType type, String tag, int weight) {
		TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.parse(tag));
		ENTRIES.computeIfAbsent(type, t -> new ArrayList<>()).add(new Entry(() -> BuiltInRegistries.ITEM.getTag(key)
				.flatMap(set -> set.stream().findFirst()).map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY), () -> FluidStack.EMPTY, weight));
	}

	private static void addFluidTag(OreClusterType type, TagKey<Fluid> key, int weight) {
		ENTRIES.computeIfAbsent(type, t -> new ArrayList<>()).add(new Entry(() -> ItemStack.EMPTY, () -> {
			Optional<Fluid> f = BuiltInRegistries.FLUID.getTag(key)
					.flatMap(set -> set.stream().map(h -> h.value()).filter(fl -> fl.isSource(fl.defaultFluidState())).findFirst());
			return f.map(fl -> new FluidStack(fl, 1000)).orElse(FluidStack.EMPTY);
		}, weight));
	}

	public static final TagKey<Fluid> OIL = TagKey.create(Registries.FLUID, Techguns.rl("oil"));

	static {
		add(OreClusterType.COAL, () -> Items.COAL_ORE, 99);
		add(OreClusterType.COAL, () -> Items.DIAMOND, 1);
		add(OreClusterType.COMMON_METAL, () -> Items.IRON_ORE, 45);
		add(OreClusterType.COMMON_METAL, () -> Items.COPPER_ORE, 30);
		add(OreClusterType.COMMON_METAL, TGBlocks.ORE_TIN::asItem, 25);
		add(OreClusterType.RARE_METAL, TGBlocks.ORE_LEAD::asItem, 50);
		addTag(OreClusterType.RARE_METAL, "c:ores/osmium", 50);
		addTag(OreClusterType.RARE_METAL, "c:ores/aluminum", 50);
		add(OreClusterType.SHINY_METAL, TGBlocks.ORE_TITANIUM::asItem, 20);
		add(OreClusterType.SHINY_METAL, () -> Items.GOLD_ORE, 40);
		addTag(OreClusterType.SHINY_METAL, "c:ores/silver", 40);
		add(OreClusterType.COMMON_GEM, () -> Items.REDSTONE_ORE, 40);
		add(OreClusterType.COMMON_GEM, () -> Items.LAPIS_ORE, 20);
		addTag(OreClusterType.COMMON_GEM, "c:ores/certus_quartz", 40);
		add(OreClusterType.SHINY_GEM, () -> Items.DIAMOND_ORE, 40);
		add(OreClusterType.SHINY_GEM, () -> Items.EMERALD_ORE, 20);
		add(OreClusterType.URANIUM, TGBlocks.ORE_URANIUM::asItem, 50);
		add(OreClusterType.NETHER_CRYSTAL, () -> Items.NETHER_QUARTZ_ORE, 50);
		add(OreClusterType.NETHER_CRYSTAL, () -> Items.GLOWSTONE, 40);
		add(OreClusterType.NETHER_CRYSTAL, () -> Items.BLAZE_ROD, 10);
		addFluidTag(OreClusterType.OIL, OIL, 10);
	}

	/** entries that currently resolve to an item or fluid */
	public static List<Entry> getEntries(OreClusterType type) {
		return ENTRIES.getOrDefault(type, List.of()).stream().filter(e -> !e.isEmpty()).toList();
	}

	/** random ore of the cluster, cobblestone if nothing is available */
	public static MachineOperation getNewOperation(OreClusterType type, RandomSource rnd, double orePerHour, int radius) {
		List<Entry> entries = getEntries(type);
		int total = entries.stream().mapToInt(Entry::weight).sum();
		if (total > 0) {
			int r = rnd.nextInt(total);
			for (Entry e : entries) {
				r -= e.weight();
				if (r < 0) {
					MachineOperation op;
					FluidStack fluid = e.getFluid();
					if (!fluid.isEmpty()) {
						op = new MachineOperation(ItemStack.EMPTY);
						op.getFluidOutputs().add(fluid.copy());
					} else {
						op = new MachineOperation(e.getItem().copy());
					}
					op.setPowerPerTick((int) (8 * orePerHour * (1 + Math.max(radius - 1, 0) * 0.2) * type.getPowerMultiplier() * TGConfig.oreDrillMultiplierPower));
					return op;
				}
			}
		}
		return getCobbleStoneOperation();
	}

	public static MachineOperation getCobbleStoneOperation() {
		MachineOperation op = new MachineOperation(new ItemStack(Items.COBBLESTONE));
		op.setPowerPerTick((int) (24 * TGConfig.oreDrillMultiplierPower));
		return op;
	}

	/** chance of an entry, for JEI */
	public static float getChance(OreClusterType type, Entry entry) {
		int total = getEntries(type).stream().mapToInt(Entry::weight).sum();
		return total > 0 ? (float) entry.weight() / total : 0f;
	}
}
