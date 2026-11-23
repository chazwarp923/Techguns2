package techguns.items;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import techguns.TGItems;
import techguns.api.tginventory.TGSlotType;
import techguns.items.armors.TGArmorBonus;
import techguns.util.InventoryUtil;
import techguns.util.TextUtil;

/**
 * Formerly a single metadata item holding all crafting materials/ammo; now one instance per item.
 * Keeps the legacy behaviour: right click puts ammo into the ammo slots, sneak right click
 * unloads full magazines into bullets + empty magazine.
 */
public class GenericItemShared extends GenericItem {

	/** Set during common setup for full magazines; null for everything else. */
	protected AmmoEntry ammoEntry;

	public GenericItemShared(Properties properties, TGSlotType slotType, boolean useRenderHack) {
		super(properties, slotType, useRenderHack);
	}

	public void setAmmoEntry(ItemLike bullet, ItemLike magazine, int amountBullet) {
		this.ammoEntry = new AmmoEntry(bullet, magazine, amountBullet, 1);
	}

	public AmmoEntry getAmmoEntry() {
		return ammoEntry;
	}

	public record AmmoEntry(ItemLike bullet, ItemLike magazine, int amountBullet, int amountMag) {}

	public float getBonus(TGArmorBonus type, ItemStack stack, boolean consume, Player player) {
		if (stack.is(TGItems.OXYGEN_MASK.get())) {
			if (type == TGArmorBonus.OXYGEN_GEAR) {
				return 1.0f;
			}
		} else if (stack.is(TGItems.WORKING_GLOVES.get())) {
			if (type == TGArmorBonus.BREAKSPEED) {
				return 0.10f;
			}
		}
		return 0f;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		if (slotType == TGSlotType.AMMOSLOT) {
			tooltip.add(TextUtil.transTG("tooltip.rightclickammoslot").withStyle(ChatFormatting.GRAY));
			if (ammoEntry != null) {
				tooltip.add(TextUtil.transTG("tooltip.sneakrightclickunload").withStyle(ChatFormatting.GRAY));
			}
		}
		// the gloves' mining speed is a block_break_speed attribute: Curios lists it ("When worn on hands")
		if (getBonus(TGArmorBonus.OXYGEN_GEAR, stack, false, null) > 0f) {
			tooltip.add(TextUtil.transTG("armorTooltip.oxygengear"));
		}
		String id = BuiltInRegistries.ITEM.getKey(this).getPath();
		if (id.startsWith("oredrill")) {
			int lvl = id.endsWith("carbon") ? 3 : id.endsWith("obsidiansteel") ? 2 : 1;
			tooltip.add(TextUtil.transTG("oredrill.mininglevel").append(" +" + lvl));
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (ammoEntry != null) {
				if (!level.isClientSide) {
					InventoryUtil.addAmmoToPlayerOrDropInWorld(player, new ItemStack(ammoEntry.bullet(), ammoEntry.amountBullet()));
					InventoryUtil.addAmmoToPlayerOrDropInWorld(player, new ItemStack(ammoEntry.magazine(), ammoEntry.amountMag()));
					stack.shrink(1);
				}
				return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
			}
		} else if (slotType == TGSlotType.AMMOSLOT) {
			if (!level.isClientSide) {
				int amount = InventoryUtil.addAmmoToAmmoInventory(player, stack.copy());
				stack.setCount(amount);
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}
		return super.use(level, player, hand);
	}
}
