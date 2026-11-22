package techguns.items.armors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.ShieldStats;
import techguns.util.TextUtil;

/** Techguns shield: vanilla blocking, damage reduction against Techguns damage from {@link ShieldStats}. */
public class GenericShield extends ShieldItem implements ICamoChangeable {

	protected final int camocount;
	protected final ShieldStats stats;

	protected Supplier<? extends Item> repairMat = null;
	protected int repairMatCount = 0;

	public GenericShield(int durability, int camocount, ShieldStats stats) {
		super(new Item.Properties().durability(durability));
		this.camocount = Math.max(camocount, 1);
		this.stats = stats;
		ShieldStats.SHIELD_STATS.put(this, stats);
	}

	public GenericShield setRepairMat(Supplier<? extends Item> mat, int amount) {
		this.repairMat = mat;
		this.repairMatCount = amount;
		return this;
	}

	public List<ItemStack> getRepairMats(ItemStack item) {
		List<ItemStack> mats = new ArrayList<>();
		if (item.getDamageValue() > 0 && this.repairMat != null) {
			float dmgpercent = item.getDamageValue() / (float) (item.getMaxDamage() - 1);
			int count = (int) Math.ceil(this.repairMatCount * dmgpercent);
			if (count > 0) {
				mats.add(new ItemStack(this.repairMat.get(), count));
			}
		}
		return mats;
	}

	public boolean canRepairOnRepairBench(ItemStack item) {
		return this.repairMatCount > 0;
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
		return this.repairMat != null && repair.is(this.repairMat.get());
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		// ShieldItem would switch to the banner color variant names
		return this.getDescriptionId();
	}

	@Override
	public int getCamoCount() {
		return this.camocount;
	}

	@Override
	public Component getCurrentCamoName(ItemStack item) {
		return Component.translatable(this.getDescriptionId() + ".camoname." + getCurrentCamoIndex(item));
	}

	private static String pct(double d) {
		return GenericArmor.formatAV((float) d) + "%";
	}

	@Override
	public void appendHoverText(ItemStack item, TooltipContext context, List<Component> list, TooltipFlag flag) {
		if (GenericArmor.isShiftDown()) {
			list.add(TextUtil.transTG("armorTooltip.durability").append(": " + (item.getMaxDamage() - item.getDamageValue()) + "/" + item.getMaxDamage()));
			list.add(TextUtil.transTG("armorTooltip.resistances").append(":").withStyle(ChatFormatting.BLUE));
			Object[][] rows = { { ChatFormatting.DARK_GRAY, "AR", DamageType.PHYSICAL }, { ChatFormatting.GRAY, "PR", DamageType.PROJECTILE },
					{ ChatFormatting.DARK_RED, "EX", DamageType.EXPLOSION }, { ChatFormatting.DARK_AQUA, "E", DamageType.ENERGY },
					{ ChatFormatting.RED, "F", DamageType.FIRE }, { ChatFormatting.AQUA, "I", DamageType.ICE },
					{ ChatFormatting.YELLOW, "L", DamageType.LIGHTNING }, { ChatFormatting.DARK_GREEN, "P", DamageType.POISON },
					{ ChatFormatting.DARK_GRAY, "D", DamageType.DARK }, { ChatFormatting.GREEN, "RAD", DamageType.RADIATION } };
			for (Object[] r : rows) {
				list.add(Component.literal(" " + r[1] + ": " + pct(stats.getReductionPercentAgainstType((DamageType) r[2]))).withStyle((ChatFormatting) r[0]));
			}
			if (camocount > 1) {
				list.add(TextUtil.transTG("tooltip.currentcamo").append(": ").append(getCurrentCamoName(item)));
			}
		} else {
			list.add(TextUtil.transTG("gun.tooltip.shift1").append(" ").append(TextUtil.transTG("gun.tooltip.shift2").withStyle(ChatFormatting.GREEN))
					.append(" ").append(TextUtil.transTG("gun.tooltip.shift3").withStyle(ChatFormatting.GRAY)));
		}
	}
}
