package techguns.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import mekanism.api.radiation.IRadiationManager;
import mekanism.api.radiation.capability.IRadiationEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import techguns.TGArmors;
import techguns.TGItems;
import techguns.items.armors.GenericArmor;
import techguns.plugins.mekanism.MekanismRadiationBridge;
import techguns.radiation.TGRadiationSystem;

/**
 * The Mekanism radiation bridge, checked through Mekanism itself. Only called when Mekanism is loaded: this class
 * uses Mekanism's API, which isn't there otherwise.
 */
final class MekanismRadiationChecks {

	private static final EntityCapability<IRadiationEntity, Void> RADIATION = EntityCapability
			.createVoid(ResourceLocation.fromNamespaceAndPath("mekanism", "radiation"), IRadiationEntity.class);

	private MekanismRadiationChecks() {
	}

	static void run(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();

		// Techguns armor -> Mekanism shielding
		check(problems, "hazmat chestplate", shielding(stack(TGArmors.HAZMAT_CHESTPLATE.get())), 0.4);
		check(problems, "steam helmet", shielding(stack(TGArmors.STEAM_HELMET.get())), 0.5 * 0.25);
		check(problems, "power armor mk2 boots (resistance 1.25, capped)", shielding(stack(TGArmors.T4_POWER_BOOTS.get())), 0.15);
		check(problems, "gas mask", shielding(new ItemStack(TGItems.GAS_MASK.get())), 0.25);
		ItemStack broken = stack(TGArmors.HAZMAT_HELMET.get());
		broken.setDamageValue(broken.getMaxDamage() - 1);
		if (GenericArmor.isBroken(broken)) {
			check(problems, "broken hazmat helmet", shielding(broken), 0.0);
		} else {
			problems.add("couldn't break the hazmat helmet for the test");
		}

		// Mekanism armor -> Techguns RAD resistance, through the item's attribute modifiers
		for (String id : List.of("hazmat_mask", "hazmat_gown", "hazmat_pants", "hazmat_boots")) {
			var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mekanism", id));
			double res = new ItemStack(item).getAttributeModifiers().modifiers().stream()
					.filter(e -> e.attribute().equals(TGRadiationSystem.RADIATION_RESISTANCE)).mapToDouble(e -> e.modifier().amount()).sum();
			check(problems, "mekanism:" + id + " RAD resistance", res, 1.0);
		}

		// end to end: Mekanism radiates a player in full Techguns hazmat and one without
		ServerPlayer suited = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tg_mek_suited"));
		ServerPlayer bare = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tg_mek_bare"));
		suited.setItemSlot(EquipmentSlot.HEAD, stack(TGArmors.HAZMAT_HELMET.get()));
		suited.setItemSlot(EquipmentSlot.CHEST, stack(TGArmors.HAZMAT_CHESTPLATE.get()));
		suited.setItemSlot(EquipmentSlot.LEGS, stack(TGArmors.HAZMAT_LEGGINGS.get()));
		suited.setItemSlot(EquipmentSlot.FEET, stack(TGArmors.HAZMAT_BOOTS.get()));
		for (EquipmentSlot s : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			bare.setItemSlot(s, ItemStack.EMPTY);
		}
		IRadiationEntity suitedRad = suited.getCapability(RADIATION), bareRad = bare.getCapability(RADIATION);
		if (suitedRad == null || bareRad == null) {
			problems.add("players have no Mekanism radiation capability");
		} else if (!IRadiationManager.INSTANCE.isRadiationEnabled()) {
			problems.add("Mekanism radiation is disabled");
		} else {
			double s0 = suitedRad.getRadiation(), b0 = bareRad.getRadiation();
			IRadiationManager.INSTANCE.radiate(suited, 1.0);
			IRadiationManager.INSTANCE.radiate(bare, 1.0);
			double sGain = suitedRad.getRadiation() - s0, bGain = bareRad.getRadiation() - b0;
			if (bGain < 0.5) {
				problems.add("an unprotected player only got " + bGain + " Sv, the end-to-end check doesn't work");
			}
			if (sGain > 1.0e-6) {
				problems.add("full Techguns hazmat let " + sGain + " Sv through");
			}
		}

		// Rad-Away (RAD regeneration XV for 20 s, 300 RAD) also takes 30% of Mekanism's log scale off the dose
		ServerPlayer cured = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tg_mek_cured"));
		IRadiationEntity curedRad = cured.getCapability(RADIATION);
		if (curedRad != null) {
			var props = techguns.capabilities.TGExtendedPlayer.get(cured);
			props.radlevel = 400;
			curedRad.set(10.0);
			for (int i = 0; i < 20; i++) {
				TGRadiationSystem.RAD_REGENERATION.value().applyEffectTick(cured, 14);
			}
			double decades = Math.log10(100.0 / IRadiationManager.INSTANCE.minRadiationMagnitude()) * 0.3;
			check(problems, "Techguns RAD after Rad-Away", props.radlevel, 100);
			check(problems, "Mekanism dose after Rad-Away (10 Sv before)", curedRad.getRadiation(), 10.0 * Math.pow(10.0, -decades));
		}

		// the RAD resistance effect of rad pills (III) blocks 75% of new Mekanism exposure
		ServerPlayer pills = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tg_mek_pills"));
		for (EquipmentSlot s : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			pills.setItemSlot(s, ItemStack.EMPTY);
		}
		IRadiationEntity pillsRad = pills.getCapability(RADIATION);
		if (pillsRad != null) {
			pills.addEffect(new net.minecraft.world.effect.MobEffectInstance(TGRadiationSystem.RAD_RESISTANCE, 3600, 2));
			MekanismRadiationBridge.applyResistanceEffect(pills);
			double before = pillsRad.getRadiation();
			IRadiationManager.INSTANCE.radiate(pills, 1.0);
			MekanismRadiationBridge.applyResistanceEffect(pills);
			check(problems, "Mekanism dose gained on rad pills III", pillsRad.getRadiation() - before, 0.25);
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
	}

	private static ItemStack stack(net.minecraft.world.item.Item item) {
		return new ItemStack(item);
	}

	private static double shielding(ItemStack stack) {
		var cap = stack.getCapability(MekanismRadiationBridge.SHIELDING);
		return cap == null ? -1.0 : cap.getRadiationShielding();
	}

	private static void check(List<String> problems, String what, double actual, double expected) {
		if (Math.abs(actual - expected) > 1.0e-6) {
			problems.add(what + ": " + actual + " instead of " + expected + (actual == -1.0 ? " (no capability)" : ""));
		}
	}
}
