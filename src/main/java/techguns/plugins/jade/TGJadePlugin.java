package techguns.plugins.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import techguns.Techguns;
import techguns.tileentities.BasicOwnedTileEnt;
import techguns.tileentities.MultiBlockSlaveTileEnt;

/**
 * Jade (optional): looking at a Techguns machine shows the progress of its current operation, its owner and its
 * security level. Jade finds this class by its annotation and only loads it when Jade is installed. Energy, items
 * and fluids come from Jade's own capability providers.
 */
@WailaPlugin
public class TGJadePlugin implements IWailaPlugin {

	public static final ResourceLocation MACHINE = Techguns.rl("machine");

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(MachineProvider.INSTANCE, BasicOwnedTileEnt.class);
		registration.registerBlockDataProvider(MachineProvider.INSTANCE, MultiBlockSlaveTileEnt.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(MachineProvider.INSTANCE, Block.class);
	}

	public enum MachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public ResourceLocation getUid() {
			return MACHINE;
		}

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			MachineInfo.write(data, accessor.getBlockEntity(), accessor.getLevel().getServer());
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();
			float progress = MachineInfo.progress(data);
			if (progress >= 0) {
				IElementHelper h = IElementHelper.get();
				Component text = Component.translatable("techguns.jade.progress", Math.round(progress * 100));
				tooltip.add(h.progress(progress, text, h.progressStyle().color(0xFF3FA535, 0xFF2A7A24), BoxStyle.getNestedBox(), true));
			}
			if (data.contains(MachineInfo.OWNER)) {
				tooltip.add(Component.translatable("techguns.container.security.tooltip.owner").append(": ")
						.append(Component.literal(data.getString(MachineInfo.OWNER)).withStyle(ChatFormatting.WHITE)));
				byte security = data.getByte(MachineInfo.SECURITY);
				ChatFormatting color = switch (security) {
					case 1 -> ChatFormatting.YELLOW;
					case 2 -> ChatFormatting.GOLD;
					case 3 -> ChatFormatting.RED;
					default -> ChatFormatting.GREEN;
				};
				tooltip.add(Component.translatable("techguns.container.security.tooltip").append(": ")
						.append(Component.translatable("techguns.container.security.tooltip." + security).withStyle(color)));
			}
		}
	}
}
