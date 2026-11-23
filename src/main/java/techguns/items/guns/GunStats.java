package techguns.items.guns;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import techguns.Techguns;

/**
 * Data-driven gun stats: the {@code techguns:gun_stats} item data map
 * ({@code data/<namespace>/data_maps/item/gun_stats.json}), stat name to value, e.g.
 *
 * <pre>
 * {"values": {"techguns:pistol": {"damage": 9, "fire_delay": 3}}}
 * </pre>
 *
 * A gun's entry sets only the stats it names; the rest keep the values from {@link techguns.TGuns}. A datapack's
 * entry for a gun replaces Techguns' own entry for it, so a pack lists every stat it wants to differ from the code
 * values. Techguns ships every gun's values in its own file, so they're all in one place to copy from.
 * <p>
 * The values are written into the gun items when data maps load: on the server after a (re)load, on clients after
 * the server sends them (the map is synced, so client-side prediction and tooltips match). Each time, all guns first
 * go back to their code values, so a removed entry or stat really goes away. NPCs and turrets copy the npc_* stats
 * when they take up their gun, so those only change for NPCs equipped after a reload.
 */
public final class GunStats {

	public static final Codec<Map<GunStat, Double>> CODEC = Codec.unboundedMap(GunStat.CODEC, Codec.DOUBLE);

	public static final DataMapType<Item, Map<GunStat, Double>> DATA_MAP = DataMapType.builder(Techguns.rl("gun_stats"), Registries.ITEM, CODEC)
			.synced(CODEC, true).build();

	/** each gun's stats as the code set them, taken before the first data map is applied */
	private static final Map<GenericGun, EnumMap<GunStat, Double>> CODE_VALUES = new IdentityHashMap<>();

	private GunStats() {
	}

	public static void register(IEventBus modBus) {
		modBus.addListener(GunStats::registerDataMap);
		// the code values, before any data map is loaded
		modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() -> GenericGun.guns.forEach(GunStats::codeValues)));
		NeoForge.EVENT_BUS.addListener(GunStats::onDataMapsUpdated);
	}

	private static void registerDataMap(RegisterDataMapTypesEvent event) {
		event.register(DATA_MAP);
	}

	private static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
		event.ifRegistry(Registries.ITEM, GunStats::apply);
	}

	/** the code values of every stat this gun has (taken in common setup, or on first use if that's earlier) */
	public static synchronized EnumMap<GunStat, Double> codeValues(GenericGun gun) {
		return CODE_VALUES.computeIfAbsent(gun, g -> {
			EnumMap<GunStat, Double> m = new EnumMap<>(GunStat.class);
			for (GunStat s : GunStat.values()) {
				if (s.appliesTo(g)) {
					m.put(s, s.get(g));
				}
			}
			return m;
		});
	}

	/** Resets every gun to its code values, then applies the data map. */
	public static void apply(Registry<Item> items) {
		apply(items, items.getDataMap(DATA_MAP));
	}

	/** Resets every gun to its code values, then applies these entries. Public for the game tests. */
	public static synchronized void apply(Registry<Item> items, Map<ResourceKey<Item>, Map<GunStat, Double>> entries) {
		for (GenericGun gun : GenericGun.guns) {
			codeValues(gun).forEach((s, v) -> s.set(gun, v));
		}
		int changed = 0;
		for (Map.Entry<ResourceKey<Item>, Map<GunStat, Double>> e : entries.entrySet()) {
			Item item = items.get(e.getKey());
			if (!(item instanceof GenericGun gun)) {
				Techguns.LOGGER.warn("gun_stats: {} isn't a Techguns gun, ignored", e.getKey().location());
				continue;
			}
			for (Map.Entry<GunStat, Double> stat : e.getValue().entrySet()) {
				GunStat s = stat.getKey();
				if (!s.appliesTo(gun)) {
					Techguns.LOGGER.warn("gun_stats: {} has no {} stat, ignored", e.getKey().location(), s.getSerializedName());
					continue;
				}
				double set = s.set(gun, stat.getValue());
				if (set != stat.getValue()) {
					Techguns.LOGGER.warn("gun_stats: {} {} {} is out of range or not a whole number, using {}", e.getKey().location(),
							s.getSerializedName(), stat.getValue(), set);
				}
				if (set != codeValues(gun).get(s)) {
					changed++;
				}
			}
		}
		Techguns.LOGGER.debug("gun_stats: applied, {} stats differ from the code values", changed);
	}

	/** the data map file with every gun's code values, as Techguns ships it */
	public static JsonObject codeValuesJson() {
		Map<String, GenericGun> sorted = new java.util.TreeMap<>();
		for (GenericGun gun : GenericGun.guns) {
			sorted.put(BuiltInRegistries.ITEM.getKey(gun).toString(), gun);
		}
		JsonObject values = new JsonObject();
		sorted.forEach((id, gun) -> {
			JsonObject stats = new JsonObject();
			codeValues(gun).forEach((s, v) -> {
				if (s.isInteger()) {
					stats.addProperty(s.getSerializedName(), (int) Math.round(v));
				} else {
					// via float, so 0.025f is written as 0.025 and not 0.02500000037252903
					stats.addProperty(s.getSerializedName(), s == GunStat.GRAVITY ? v : (Number) (float) v.doubleValue());
				}
			});
			values.add(id, stats);
		});
		JsonObject root = new JsonObject();
		root.add("values", values);
		return root;
	}

	/** stat values of this gun as currently applied, for tests */
	public static Map<GunStat, Double> current(GenericGun gun) {
		Map<GunStat, Double> m = new LinkedHashMap<>();
		for (GunStat s : GunStat.values()) {
			if (s.appliesTo(gun)) {
				m.put(s, s.get(gun));
			}
		}
		return m;
	}
}
