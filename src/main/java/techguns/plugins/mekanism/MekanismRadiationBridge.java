package techguns.plugins.mekanism;

import java.util.Map;
import java.util.WeakHashMap;

import mekanism.api.radiation.IRadiationManager;
import mekanism.api.radiation.capability.IRadiationEntity;
import mekanism.api.radiation.capability.IRadiationShielding;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import techguns.Techguns;
import techguns.items.additionalslots.ItemGasMask;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.TGArmorBonus;
import techguns.radiation.TGRadiationSystem;

/**
 * Mekanism (optional): links the two radiation systems. The two doses stay separate (Techguns RAD, Mekanism Sv);
 * protection and cures work on both.
 * <ul>
 * <li><b>Techguns armor against Mekanism radiation.</b> Every Techguns armor piece with RAD resistance, and the gas
 * mask, gets Mekanism's {@code mekanism:radiation_shielding} item capability. A resistance of 1 (a hazmat piece) is
 * that slot's full share of Mekanism's hazmat suit: helmet 25%, chestplate 40%, leggings 20%, boots 15%. Less is
 * scaled down, more is capped. So a full Techguns hazmat or HEV suit shields 100%, full Steam armor 50%. Mekanism
 * also counts the gas mask in its Curios slot (as a helmet share), and caps the total at 100%.</li>
 * <li><b>Mekanism armor against Techguns radiation.</b> Any other mod's armor with that capability (Mekanism's hazmat
 * suit, a MekaSuit piece with the radiation shielding unit) gets Techguns RAD resistance: its shielding divided by
 * its slot's share, so a Mekanism hazmat piece gives 1 like a Techguns hazmat piece.</li>
 * <li><b>Techguns cures lower the Mekanism dose.</b> RAD regeneration (Rad-Away, rad pills, the potion) lowers it by
 * the same share of Mekanism's scale as of ours. Mekanism's dose severity is logarithmic, from its minimum magnitude
 * (1e-5 Sv) to 100 Sv; 1000 RAD (lethal for Techguns) is that whole range. Rad-Away's 300 RAD takes 30% of it, about
 * 2 powers of ten off the dose.</li>
 * <li><b>The RAD resistance effect (rad pills) cuts Mekanism exposure</b> by resistance / 4, since 4 (a full hazmat
 * suit) is 100% in both systems: rad pills III block 75%. Only the effect counts here; Mekanism already applies the
 * armor's shielding.</li>
 * </ul>
 * Only loaded when Mekanism is installed; compiled against Mekanism's API jar.
 */
public final class MekanismRadiationBridge {

	public static final String MEKANISM = "mekanism";

	/** Mekanism's item capability (mekanism.common.capabilities.Capabilities, not in its API); same name and type, same instance */
	public static final ItemCapability<IRadiationShielding, Void> SHIELDING = ItemCapability
			.createVoid(ResourceLocation.fromNamespaceAndPath(MEKANISM, "radiation_shielding"), IRadiationShielding.class);
	/** Mekanism's entity dose capability, likewise */
	public static final EntityCapability<IRadiationEntity, Void> DOSE = EntityCapability
			.createVoid(ResourceLocation.fromNamespaceAndPath(MEKANISM, "radiation"), IRadiationEntity.class);

	/** Techguns RAD that matches Mekanism's whole severity scale */
	public static final double RAD_FOR_FULL_SCALE = TGRadiationSystem.LETHAL_POISONING;
	/** Mekanism's top severity dose (RadiationScale.LOG_MAX) */
	public static final double MAX_DOSE = 100.0;
	/** RAD resistance that blocks all Mekanism exposure: a full hazmat suit */
	public static final double FULL_RESISTANCE = 4.0;

	/** Mekanism dose each player had after the last check, to see how much a tick added */
	private static final Map<Player, Double> lastDose = new WeakHashMap<>();

	private MekanismRadiationBridge() {
	}

	public static void register(IEventBus modBus) {
		modBus.addListener(MekanismRadiationBridge::registerCapabilities);
		NeoForge.EVENT_BUS.addListener(MekanismRadiationBridge::addRadResistance);
		NeoForge.EVENT_BUS.addListener(MekanismRadiationBridge::onPlayerTick);
		TGRadiationSystem.externalRadRegeneration = MekanismRadiationBridge::cure;
	}

	/** Mekanism dose after curing this much Techguns RAD: the same share of Mekanism's log scale */
	public static double curedDose(double dose, double rad) {
		double min = IRadiationManager.INSTANCE.minRadiationMagnitude();
		double decades = Math.log10(MAX_DOSE / min) * rad / RAD_FOR_FULL_SCALE;
		return Math.max(IRadiationManager.INSTANCE.baselineRadiation(), dose * Math.pow(10.0, -decades));
	}

	/** RAD regeneration also lowers the Mekanism dose */
	public static void cure(Player ply, int rad) {
		IRadiationEntity dose = ply.getCapability(DOSE);
		if (dose != null && rad > 0) {
			double cured = curedDose(dose.getRadiation(), rad);
			dose.set(cured);
			lastDose.put(ply, cured);
		}
	}

	private static void onPlayerTick(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer ply) {
			applyResistanceEffect(ply);
		}
	}

	/** Share of Mekanism exposure the RAD resistance effect blocks, 0..1 */
	public static double blockedByEffect(Player ply) {
		MobEffectInstance eff = ply.getEffect(TGRadiationSystem.RAD_RESISTANCE);
		return eff == null ? 0.0 : Math.min(1.0, (eff.getAmplifier() + 1) / FULL_RESISTANCE);
	}

	/**
	 * Takes back the blocked share of the Mekanism dose gained since the last check. Public for the game tests, which
	 * can't tick fake players.
	 */
	public static void applyResistanceEffect(Player ply) {
		IRadiationEntity dose = ply.getCapability(DOSE);
		if (dose == null) {
			return;
		}
		double now = dose.getRadiation();
		Double last = lastDose.get(ply);
		double blocked = blockedByEffect(ply);
		if (last != null && now > last && blocked > 0.0) {
			now = now - (now - last) * blocked;
			dose.set(now);
		}
		lastDose.put(ply, now);
	}

	/** Mekanism's hazmat suit split (ItemHazmatSuitArmor.getShieldingByArmor) */
	public static double slotShare(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> 0.25;
			case CHEST -> 0.4;
			case LEGS -> 0.2;
			case FEET -> 0.15;
			default -> 0.0;
		};
	}

	/** Mekanism shielding of a Techguns piece, 0..1 of all-round protection */
	public static double shielding(ItemStack stack) {
		if (stack.getItem() instanceof GenericArmor armor) {
			if (GenericArmor.isBroken(stack)) {
				return 0.0;
			}
			return Math.min(1.0, armor.getBonus(TGArmorBonus.RAD_RESISTANCE)) * slotShare(armor.getEquipmentSlot());
		}
		if (stack.getItem() instanceof ItemGasMask mask) {
			return Math.min(1.0, mask.getRadresist()) * slotShare(EquipmentSlot.HEAD);
		}
		return 0.0;
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		int count = 0;
		for (Item item : BuiltInRegistries.ITEM) {
			if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Techguns.MODID)) {
				continue;
			}
			boolean shields = item instanceof GenericArmor armor && armor.getBonus(TGArmorBonus.RAD_RESISTANCE) > 0
					|| item instanceof ItemGasMask mask && mask.getRadresist() > 0;
			if (shields) {
				event.registerItem(SHIELDING, (stack, ctx) -> () -> shielding(stack), item);
				count++;
			}
		}
		Techguns.LOGGER.debug("Mekanism radiation shielding for {} Techguns items", count);
	}

	/** Techguns RAD resistance of another mod's armor piece from its Mekanism shielding, or 0 */
	public static double radResistanceOf(ItemStack stack) {
		if (!(stack.getItem() instanceof ArmorItem armor) || BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Techguns.MODID)) {
			return 0.0; // Techguns armor has its own resistance (and the capability from us)
		}
		double share = slotShare(armor.getEquipmentSlot());
		if (share <= 0.0) {
			return 0.0;
		}
		IRadiationShielding cap = stack.getCapability(SHIELDING);
		return cap == null ? 0.0 : Math.max(0.0, cap.getRadiationShielding()) / share;
	}

	private static void addRadResistance(ItemAttributeModifierEvent event) {
		ItemStack stack = event.getItemStack();
		double res = radResistanceOf(stack);
		if (res > 0.0) {
			EquipmentSlot slot = ((ArmorItem) stack.getItem()).getEquipmentSlot();
			event.addModifier(TGRadiationSystem.RADIATION_RESISTANCE,
					new AttributeModifier(Techguns.rl("mekanism_shielding." + slot.getName()), res, AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.bySlot(slot));
		}
	}
}
