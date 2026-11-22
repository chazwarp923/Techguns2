package techguns.client.gui;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.gui.containers.OreDrillMenu;
import techguns.tileentities.OreDrillTileEntMaster;

/** Ore drill gui (1.12 OreDrillGui): tanks, fuel buffer, progress and a hint when the drill head is missing. */
public class OreDrillScreen extends TGMachineScreen<OreDrillMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/ore_drill_gui.png");

	public static final int INPUT_TANK_X = 15;
	public static final int TANK_Y = 17;
	public static final int OUTPUT_TANK_X = 157;
	public static final int TANK_W = 10;
	public static final int TANK_H = 50;

	private final OreDrillTileEntMaster drill;

	public OreDrillScreen(OreDrillMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
		this.drill = menu.getTile();
		this.hasUpgradeSlot = false;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		int k = leftPos;
		int l = topPos;
		ChemLabScreen.drawFluid(g, drill.inputTank.getFluid(), drill.inputTank.getCapacity(), k + INPUT_TANK_X + 1, l + TANK_Y, TANK_W, TANK_H);
		ChemLabScreen.drawFluid(g, drill.outputTank.getFluid(), drill.outputTank.getCapacity(), k + OUTPUT_TANK_X + 1, l + TANK_Y, TANK_W, TANK_H);
		g.blit(TEXTURE, k + INPUT_TANK_X, l + TANK_Y, 176, 40, TANK_W + 2, TANK_H + 2);
		g.blit(TEXTURE, k + OUTPUT_TANK_X, l + TANK_Y, 176, 40, TANK_W + 2, TANK_H + 2);
		if (drill.isWorking()) {
			int i1 = drill.getProgressScaled(36);
			g.blit(TEXTURE, k + 73, l + 17, 177, 1, 25, i1 + 1);
		}
		if (drill.getCurrentFuelBufferMax() > 0) {
			int s = 12 - drill.getFuelBufferScaled(12);
			g.blit(TEXTURE, k + 35, l + 36 + s, 188, 39 + s, 6, 12 - s);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		Component msg = null;
		if (!drill.isFormed()) {
			msg = Component.translatable("techguns.container.oredrill.broken");
		} else if (drill.getDrillItemMiningLevel() < 0) {
			msg = Component.translatable("techguns.container.oredrill.drillrequired." + OreDrillTileEntMaster.getDrillSize(drill.getDrillRadius()));
		}
		if (msg != null) {
			g.drawString(this.font, msg, (this.imageWidth - this.font.width(msg)) / 2 + 14, 24, 0xFFFF2020, false);
		}
	}

	@Override
	protected void addTooltips(List<Component> tooltip, int mx, int my) {
		super.addTooltips(tooltip, mx, my);
		if (isInRect(mx, my, 74, 16, 27, 39)) {
			if (drill.totaltime > 0) {
				tooltip.add(Component.literal(String.format("%.2f%%", drill.progress * 100.0f / drill.totaltime)));
				tooltip.add(Component.translatable("techguns.gui.oredrill.orehour").append(String.format(": %.2f", 1200.0 / drill.totaltime * 60.0)));
			}
			if (drill.getPowerPerTick() > 0) {
				tooltip.add(Component.literal(POWER_UNIT + "/t: " + drill.getPowerPerTick()));
			}
			tooltip.add(Component.empty());
			tooltip.add(Component.translatable("techguns.oredrill.drillsize").append(": " + drill.getTotalDrillLength()).withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.translatable("techguns.oredrill.drillradius").append(": " + drill.getDrillRadius()).withStyle(ChatFormatting.GRAY));
		} else if (isInRect(mx, my, INPUT_TANK_X, TANK_Y - 1, TANK_W + 1, TANK_H + 1)) {
			ChemLabScreen.tankTooltip(tooltip, drill.inputTank);
		} else if (isInRect(mx, my, OUTPUT_TANK_X, TANK_Y - 1, TANK_W + 1, TANK_H + 1)) {
			ChemLabScreen.tankTooltip(tooltip, drill.outputTank);
		} else if (isInRect(mx, my, 34, 36, 8, 14)) {
			if (drill.getCurrentFuelBufferMax() > 0) {
				int pwrtick = drill.getPowerPerTick();
				float factor = TGConfig.oreDrillMultiplierFuel;
				int ticksLeft = factor <= 0 || pwrtick <= 0 ? -1 : (int) (drill.getFuelBuffer() / (pwrtick / factor));
				tooltip.add(Component.translatable("techguns.oredrill.currentfuel").append(" " + drill.getFuelBuffer() + "/" + drill.getCurrentFuelBufferMax()));
				tooltip.add(Component.translatable("techguns.oredrill.remaining").append(": " + ticksLeft + " ")
						.append(Component.translatable("techguns.oredrill.ticks")));
			} else {
				tooltip.add(Component.translatable("techguns.oredrill.nofuel"));
			}
		}
	}
}
