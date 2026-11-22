package techguns.blocks;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * Ore cluster (1.12 BlockOreCluster): unbreakable, drops nothing, mined by ore drills only.
 */
public class BlockOreCluster extends Block {

	protected final OreClusterType type;

	public BlockOreCluster(OreClusterType type, Properties props) {
		super(props);
		this.type = type;
	}

	public OreClusterType getType() {
		return type;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, ctx, tooltip, flag);
		tooltip.add(Component.translatable("techguns.orecluster.mininglevel").append(": " + type.getMiningLevel()).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("techguns.orecluster.powermult").append(String.format(": x%.1f", type.getPowerMultiplier()))
				.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("techguns.orecluster.amountmult").append(String.format(": x%.1f", type.getAmountMultiplier()))
				.withStyle(ChatFormatting.GRAY));
	}
}
