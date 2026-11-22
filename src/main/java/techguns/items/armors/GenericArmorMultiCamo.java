package techguns.items.armors;

import java.util.List;
import java.util.Random;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import techguns.TGDataComponents;
import techguns.util.TextUtil;

/** Armor with several textures; sneak + right click cycles the camo. */
public class GenericArmorMultiCamo extends GenericArmor implements ICamoChangeable {

	protected static final Random rnd = new Random();
	protected String camoNameSuffix = "";

	public GenericArmorMultiCamo(Holder<ArmorMaterial> vanillaMaterial, TGArmorMaterial material, String[] textureNames, EquipmentSlot slot) {
		super(vanillaMaterial, material, textureNames, slot);
	}

	public GenericArmorMultiCamo setCamoNameSuffix(String s) {
		this.camoNameSuffix = s + ".";
		return this;
	}

	@Override
	public int getCamoCount() {
		return textureNames.length;
	}

	@Override
	public int getCurrentCamoIndex(ItemStack item) {
		return item.getOrDefault(TGDataComponents.CAMO.get(), 0);
	}

	@Override
	public Component getCurrentCamoName(ItemStack item) {
		int camoID = this.getCurrentCamoIndex(item);
		if (camoID >= 0) {
			return camoName(textureNames[0], camoNameSuffix, camoID);
		}
		return TextUtil.transTG("item.invalidcamo");
	}

	@Override
	protected void addCamoInformation(ItemStack item, List<Component> list) {
		list.add(TextUtil.transTG("tooltip.currentcamo").append(": ").append(getCurrentCamoName(item)));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			ItemStack item = player.getItemInHand(hand);
			this.switchCamo(item);
			if (level.isClientSide) {
				player.displayClientMessage(TextUtil.transTG("message.camoswitch").append(" ").append(getCurrentCamoName(item)), false);
			}
			return InteractionResultHolder.sidedSuccess(item, level.isClientSide);
		}
		return super.use(level, player, hand);
	}

	public static int getRandomCamoIndexFor(GenericArmorMultiCamo type) {
		int count = type.getCamoCount();
		return count <= 0 ? 0 : rnd.nextInt(count);
	}

	public static ItemStack getNewWithCamo(Item item, int camo) {
		ItemStack armor = new ItemStack(item);
		armor.set(TGDataComponents.CAMO.get(), camo);
		return armor;
	}
}
