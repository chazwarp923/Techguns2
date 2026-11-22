package techguns.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.MetalPressMenu;
import techguns.tileentities.MetalPressTileEnt;

public class MetalPressScreen extends TGMachineScreen<MetalPressMenu> {

	private final MetalPressTileEnt press;

	public MetalPressScreen(MetalPressMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, Techguns.rl("textures/gui/metal_press_gui.png"));
		this.press = menu.getTile();
	}

	@Override
	protected void init() {
		super.init();
		addMenuButton(MetalPressTileEnt.BUTTON_ID_AUTOSPLIT, leftPos + 20, topPos + 50, 40, 20, Component.translatable("techguns.button.switch"));
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		int color = 4210752;
		g.drawString(font, Component.translatable("techguns.container.metalpress.autosplit").append(":"), 20, 30, color, false);
		g.drawString(font, Component.translatable(press.getAutoSplitMode() == 0 ? "techguns.container.info.off" : "techguns.container.info.on"), 20, 40,
				color, false);
		super.renderLabels(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		if (press.isWorking()) {
			int i1 = press.getProgressScaled(21);
			g.blit(tex, leftPos + 119, topPos + 36, 176, 0, 19, i1 + 1);
		}
	}
}
