package techguns;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components; replace the 1.12.2 stack NBT tags ("ammo", "camo", "ammovariant", ...). */
public class TGDataComponents {
	public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
			Techguns.MODID);

	/** Rounds currently loaded in a gun. */
	public static final Supplier<DataComponentType<Integer>> AMMO = register("ammo",
			b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
	/** Camo / texture variant index of guns and armor. */
	public static final Supplier<DataComponentType<Integer>> CAMO = register("camo",
			b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
	/** Ammo variant key currently loaded ("default", "incendiary", ...). */
	public static final Supplier<DataComponentType<String>> AMMO_VARIANT = register("ammo_variant",
			b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

	/** Installed mining head level of melee mining tools (0 = default head). */
	public static final Supplier<DataComponentType<Integer>> MINING_HEAD = register("mining_head",
			b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Stored power of powered armor (steam / RF). */
	public static final Supplier<DataComponentType<Integer>> ARMOR_POWER = register("armor_power",
			b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

	private static <T> Supplier<DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
		return COMPONENTS.registerComponentType(name, builder);
	}
}
