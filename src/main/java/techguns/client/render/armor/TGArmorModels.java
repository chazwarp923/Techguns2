package techguns.client.render.armor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import techguns.TGArmors;
import techguns.client.models.armor.ModelArmorCoat;
import techguns.client.models.armor.ModelBeret;
import techguns.client.models.armor.ModelBipedTG;
import techguns.client.models.armor.ModelExoSuit;
import techguns.client.models.armor.ModelSteamArmor;
import techguns.client.models.armor.ModelT3PowerArmor;
import techguns.client.models.armor.ModelT4PowerArmorMk2;
import techguns.items.armors.GenericArmor;

/** Custom 3d armor models by key (1.12 ClientProxy#registerArmorModel). */
public class TGArmorModels {

	private static final Map<ResourceLocation, Supplier<ModelBipedTG>> FACTORIES = new HashMap<>();
	private static final Map<ResourceLocation, TGArmorModel> MODELS = new HashMap<>();

	static {
		FACTORIES.put(TGArmors.ARMORMODEL_STEAM_ARMOR_0, () -> new ModelSteamArmor(0));
		FACTORIES.put(TGArmors.ARMORMODEL_STEAM_ARMOR_1, () -> new ModelSteamArmor(1));
		FACTORIES.put(TGArmors.ARMORMODEL_POWER_ARMOR_0, () -> new ModelT3PowerArmor(0));
		FACTORIES.put(TGArmors.ARMORMODEL_POWER_ARMOR_1, () -> new ModelT3PowerArmor(1));
		FACTORIES.put(TGArmors.ARMORMODEL_EXO_SUIT_0, () -> new ModelExoSuit(0, 1.0f));
		FACTORIES.put(TGArmors.ARMORMODEL_EXO_SUIT_1, () -> new ModelExoSuit(1, 0.5f));
		FACTORIES.put(TGArmors.ARMORMODEL_EXO_SUIT_2, () -> new ModelExoSuit(0, 0.75f));
		FACTORIES.put(TGArmors.ARMORMODEL_BERET_0, ModelBeret::new);
		FACTORIES.put(TGArmors.ARMORMODEL_COAT_0, () -> new ModelArmorCoat(0, 1.0f));
		FACTORIES.put(TGArmors.ARMORMODEL_COAT_1, () -> new ModelArmorCoat(1, 0.51f));
		FACTORIES.put(TGArmors.ARMORMODEL_COAT_2, () -> new ModelArmorCoat(2, 0.49f));
		FACTORIES.put(TGArmors.ARMORMODEL_COAT_3, () -> new ModelArmorCoat(3, 0.75f));
		FACTORIES.put(TGArmors.ARMORMODEL_STEAM_ARMOR_2, () -> new ModelSteamArmor(0, 0.01f));
		FACTORIES.put(TGArmors.ARMORMODEL_POWER_ARMOR_2, () -> new ModelT3PowerArmor(0, 0.01f));
		FACTORIES.put(TGArmors.ARMORMODEL_POWER_ARMOR_MK2_0, () -> new ModelT4PowerArmorMk2(0));
		FACTORIES.put(TGArmors.ARMORMODEL_POWER_ARMOR_MK2_1, () -> new ModelT4PowerArmorMk2(1));
		FACTORIES.put(TGArmors.ARMORMODEL_POWER_ARMOR_MK2_2, () -> new ModelT4PowerArmorMk2(0, 0.01f));
	}

	@Nullable
	public static TGArmorModel get(ResourceLocation key) {
		return MODELS.computeIfAbsent(key, k -> {
			Supplier<ModelBipedTG> f = FACTORIES.get(k);
			return f != null ? new TGArmorModel(f.get()) : null;
		});
	}

	/** 1.12 ItemRenderHack: these pieces show their 3d model as inventory icon */
	public static final String[] ITEM_3D = { "steam_helmet", "steam_chestplate", "steam_leggings", "steam_boots", "t3_power_helmet",
			"t3_power_chestplate", "t3_power_leggings", "t3_power_boots", "t4_power_helmet", "t4_power_chestplate", "t4_power_leggings",
			"t4_power_boots" };

	public static boolean hasItem3d(String name) {
		return java.util.Arrays.asList(ITEM_3D).contains(name);
	}

	public static void initItemRenderers() {
		reg3d(TGArmors.STEAM_HELMET.get(), new ModelSteamArmor(0), "steam_armor", EquipmentSlot.HEAD);
		reg3d(TGArmors.STEAM_CHESTPLATE.get(), new ModelSteamArmor(0), "steam_armor", EquipmentSlot.CHEST);
		reg3d(TGArmors.STEAM_LEGGINGS.get(), new ModelSteamArmor(1), "steam_armor", EquipmentSlot.LEGS);
		reg3d(TGArmors.STEAM_BOOTS.get(), new ModelSteamArmor(0), "steam_armor", EquipmentSlot.FEET);
		reg3d(TGArmors.T3_POWER_HELMET.get(), new ModelT3PowerArmor(0), "powerarmor", EquipmentSlot.HEAD);
		reg3d(TGArmors.T3_POWER_CHESTPLATE.get(), new ModelT3PowerArmor(0), "powerarmor", EquipmentSlot.CHEST);
		reg3d(TGArmors.T3_POWER_LEGGINGS.get(), new ModelT3PowerArmor(1), "powerarmor", EquipmentSlot.LEGS);
		reg3d(TGArmors.T3_POWER_BOOTS.get(), new ModelT3PowerArmor(0), "powerarmor", EquipmentSlot.FEET);
		reg3d(TGArmors.T4_POWER_HELMET.get(), new ModelT4PowerArmorMk2(0), "powerarmor_mk2_darkgrey", EquipmentSlot.HEAD);
		reg3d(TGArmors.T4_POWER_CHESTPLATE.get(), new ModelT4PowerArmorMk2(0), "powerarmor_mk2_darkgrey", EquipmentSlot.CHEST);
		reg3d(TGArmors.T4_POWER_LEGGINGS.get(), new ModelT4PowerArmorMk2(1), "powerarmor_mk2_darkgrey", EquipmentSlot.LEGS);
		reg3d(TGArmors.T4_POWER_BOOTS.get(), new ModelT4PowerArmorMk2(0), "powerarmor_mk2_darkgrey", EquipmentSlot.FEET);
	}

	private static void reg3d(net.minecraft.world.item.Item item, ModelBipedTG model, String tex, EquipmentSlot slot) {
		techguns.client.render.TGItemRenderer.register(item,
				new techguns.client.render.item.RenderArmorItem(model, techguns.Techguns.rl("textures/models/armor/" + tex + ".png"), slot));
	}

	/** custom armor model + 3d inventory renderer */
	public static final IClientItemExtensions ARMOR_EXTENSION_3D = new IClientItemExtensions() {
		@Override
		public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
			return techguns.client.render.TGItemRenderer.get();
		}

		@Override
		public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
			return ARMOR_EXTENSION.getGenericArmorModel(entity, stack, slot, original);
		}
	};

	/** client extension for armor with a custom model */
	public static final IClientItemExtensions ARMOR_EXTENSION = new IClientItemExtensions() {
		@Override
		public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
			if (stack.getItem() instanceof GenericArmor ga && ga.getArmorModelKey() != null) {
				TGArmorModel m = get(ga.getArmorModelKey());
				if (m != null) {
					return m.prepare(original);
				}
			}
			return original;
		}
	};
}
