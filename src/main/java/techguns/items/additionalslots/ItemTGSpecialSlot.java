package techguns.items.additionalslots;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import techguns.api.tginventory.TGSlotType;
import techguns.items.GenericItem;
import techguns.items.armors.ICamoChangeable;
import techguns.util.InventoryUtil;
import techguns.util.TextUtil;

/**
 * Item worn in a Curios face / back / hands slot (1.12 ItemTGSpecialSlot + ItemTGSpecialSlotAmmo). Durability is
 * used as charge: at max damage the item stops working (it never breaks) and reloads from its ammo item.
 */
public abstract class ItemTGSpecialSlot extends GenericItem implements ICamoChangeable {

	protected final int camoCount;
	@Nullable
	protected final Supplier<? extends Item> ammo;
	@Nullable
	protected final Supplier<? extends Item> ammoEmpty;

	/** dur &lt;= 0: no durability */
	protected ItemTGSpecialSlot(TGSlotType slot, int camoCount, int dur, @Nullable Supplier<? extends Item> ammo,
			@Nullable Supplier<? extends Item> ammoEmpty) {
		super(dur > 0 ? new Item.Properties().durability(dur) : new Item.Properties().stacksTo(1), slot, false);
		this.camoCount = Math.max(1, camoCount);
		this.ammo = ammo;
		this.ammoEmpty = ammoEmpty;
	}

	protected ItemTGSpecialSlot(TGSlotType slot, int camoCount, int dur) {
		this(slot, camoCount, dur, null, null);
	}

	/** charge left (not at max damage) */
	public static boolean hasCharge(ItemStack stack) {
		return !stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage();
	}

	/** uses charge without ever breaking the item */
	public static void useCharge(ItemStack stack, int amount) {
		if (stack.isDamageableItem()) {
			stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + amount));
		}
	}

	/** server: consume one ammo item from the player, refill and give back the empty container */
	protected void tryReloadAndRepair(ItemStack item, Player ply) {
		if (this.ammo == null || ply.level().isClientSide) {
			return;
		}
		if (InventoryUtil.consumeAmmoPlayer(ply, new ItemStack(this.ammo.get()))) {
			item.setDamageValue(0);
			if (this.ammoEmpty != null) {
				int left = InventoryUtil.addAmmoToPlayerInventory(ply, new ItemStack(this.ammoEmpty.get()));
				if (left > 0) {
					ply.level().addFreshEntity(new ItemEntity(ply.level(), ply.getX(), ply.getY(), ply.getZ(), new ItemStack(this.ammoEmpty.get(), left)));
				}
			}
		}
	}

	@Override
	public int getCamoCount() {
		return this.camoCount;
	}

	@Override
	public Component getCurrentCamoName(ItemStack item) {
		String key = this.getDescriptionId() + ".camoname." + getCurrentCamoIndex(item);
		if (Language.getInstance().has(key)) {
			return Component.translatable(key);
		}
		return TextUtil.transTG("item.defaultcamo");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		if (this.ammo != null) {
			tooltip.add(TextUtil.transTG("tooltip.reloads_with").append(": ").append(this.ammo.get().getDescription()).withStyle(ChatFormatting.AQUA));
		}
		if (this.camoCount > 1) {
			tooltip.add(TextUtil.transTG("tooltip.currentcamo").append(": ").append(getCurrentCamoName(stack)).withStyle(ChatFormatting.GRAY));
		}
	}
}
