package techguns.radiation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.Techguns;

/**
 * Radiation (1.12 TGRadiationSystem): the radiation effect adds RAD to players (minus their RAD resistance attribute)
 * and damages mobs, RAD regeneration removes RAD. Players get sick from 500 RAD on and take damage from 1000 RAD.
 * {@link TGConfig#WIP_disableRadiationSystem} stops players from collecting RAD.
 */
public final class TGRadiationSystem {

	public static final int MINOR_POISONING = 500;
	public static final int SEVERE_POISONING = 750;
	public static final int LETHAL_POISONING = 1000;
	public static final int RADLOST_ON_DEATH = 200;
	public static final int INVENTORY_RADIATION_INTERVAL = 60;

	public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, Techguns.MODID);
	public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Techguns.MODID);
	public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, Techguns.MODID);

	public static final DeferredHolder<Attribute, Attribute> RADIATION_RESISTANCE = ATTRIBUTES.register("radiation_resistance",
			() -> new RangedAttribute("attribute.name.techguns.radiationResistance", 0.0d, 0.0d, Float.MAX_VALUE).setSyncable(true));

	public static final DeferredHolder<MobEffect, MobEffect> RADIATION = EFFECTS.register("radiation", RadiationEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> RAD_REGENERATION = EFFECTS.register("radregeneration", RadRegenerationEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> RAD_RESISTANCE = EFFECTS.register("radresistance",
			() -> new SimpleEffect(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x99ac2d).addAttributeModifier(RADIATION_RESISTANCE,
					Techguns.rl("effect.radresistance"), 1.0d, AttributeModifier.Operation.ADD_VALUE));

	public static final DeferredHolder<Potion, Potion> RAD_POTION = POTIONS.register("radpotion",
			() -> new Potion("radpotion", new MobEffectInstance(RADIATION, 300, 4)));
	public static final DeferredHolder<Potion, Potion> RAD_POTION_SEVERE = POTIONS.register("radpotion_severe",
			() -> new Potion("radpotion_severe", new MobEffectInstance(RADIATION, 300, 24)));
	public static final DeferredHolder<Potion, Potion> RAD_REGENERATION_POTION = POTIONS.register("radregenerationpotion",
			() -> new Potion("radregenerationpotion", new MobEffectInstance(RAD_REGENERATION, 400, 9)));
	public static final DeferredHolder<Potion, Potion> RAD_RESISTANCE_POTION = POTIONS.register("radresistancepotion",
			() -> new Potion("radresistancepotion", new MobEffectInstance(RAD_RESISTANCE, 900, 1)));

	public static final TagKey<Item> URANIUM_ORES = TagKey.create(Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/uranium"));
	public static final TagKey<Item> URANIUM_INGOTS = TagKey.create(Registries.ITEM,
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/uranium"));

	/** radiation level and duration of items that radiate in the inventory */
	public record ItemRadiationData(int radamount, int radduration) {
	}

	private static final Map<Supplier<? extends Item>, ItemRadiationData> ITEM_RADIATION = new HashMap<>();
	private static Map<Item, ItemRadiationData> itemRadiation;

	static {
		ITEM_RADIATION.put(TGItems.ENRICHED_URANIUM, new ItemRadiationData(3, 100));
		ITEM_RADIATION.put(TGItems.YELLOWCAKE, new ItemRadiationData(1, 100));
		ITEM_RADIATION.put(TGItems.ANTI_GRAV_CORE, new ItemRadiationData(4, 100));
		ITEM_RADIATION.put(TGItems.PLASMA_GENERATOR, new ItemRadiationData(4, 100));
		ITEM_RADIATION.put(TGItems.TACTICAL_NUKE_WARHEAD, new ItemRadiationData(1, 100));
		ITEM_RADIATION.put(TGItems.ROCKET_NUKE, new ItemRadiationData(1, 100));
	}

	private static final ItemRadiationData TAG_RADIATION = new ItemRadiationData(1, 100);

	private TGRadiationSystem() {
	}

	public static void register(IEventBus modBus) {
		ATTRIBUTES.register(modBus);
		EFFECTS.register(modBus);
		POTIONS.register(modBus);
		modBus.addListener(TGRadiationSystem::addAttributes);
	}

	/** every living entity gets the RAD resistance attribute */
	private static void addAttributes(EntityAttributeModificationEvent event) {
		for (EntityType<? extends LivingEntity> type : event.getTypes()) {
			if (!event.has(type, RADIATION_RESISTANCE)) {
				event.add(type, RADIATION_RESISTANCE);
			}
		}
	}

	public static boolean isEnabled() {
		return !TGConfig.WIP_disableRadiationSystem;
	}

	@Nullable
	public static ItemRadiationData getRadiationData(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		if (itemRadiation == null) {
			Map<Item, ItemRadiationData> m = new HashMap<>();
			ITEM_RADIATION.forEach((k, v) -> m.put(k.get(), v));
			itemRadiation = m;
		}
		ItemRadiationData d = itemRadiation.get(stack.getItem());
		if (d == null && (stack.is(URANIUM_ORES) || stack.is(URANIUM_INGOTS))) {
			d = TAG_RADIATION;
		}
		return d;
	}

	public static int getRadResistance(LivingEntity ent) {
		AttributeInstance a = ent.getAttribute(RADIATION_RESISTANCE);
		return a != null ? (int) a.getValue() : 0;
	}

	/** RAD per second the current radiation effect gives this entity */
	public static int getRadPerSecond(LivingEntity ent) {
		MobEffectInstance rad = ent.getEffect(RADIATION);
		if (rad == null) {
			return 0;
		}
		return Math.clamp(rad.getAmplifier() + 1 - getRadResistance(ent), 0, 1000);
	}

	public static void addRadiation(LivingEntity ent, int duration, int amplifier) {
		ent.addEffect(new MobEffectInstance(RADIATION, duration, amplifier, false, false));
	}

	/**
	 * Radiation effect for all living entities in radius: full strength inside inner_radius, falling linearly to
	 * strength_outer at radius.
	 */
	public static void applyRadToEntities(Level level, double x, double y, double z, double radius, int duration, int strength, double innerRadius,
			int strengthOuter) {
		Vec3 pos = new Vec3(x, y, z);
		AABB bb = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, bb)) {
			double distance = pos.distanceTo(e.position());
			if (distance < radius) {
				int str = strength;
				if (distance > innerRadius) {
					double factor = (distance - innerRadius) / (radius - innerRadius);
					str = (int) Math.round(strength + (strengthOuter - strength) * factor);
				}
				e.addEffect(new MobEffectInstance(RADIATION, duration, str, true, true));
			}
		}
	}

	/** effect without own behaviour */
	public static class SimpleEffect extends MobEffect {
		public SimpleEffect(net.minecraft.world.effect.MobEffectCategory category, int color) {
			super(category, color);
		}
	}

	public static Holder<MobEffect> radiation() {
		return RADIATION;
	}
}
