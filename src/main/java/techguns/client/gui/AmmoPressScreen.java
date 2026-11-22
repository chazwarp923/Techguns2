package techguns.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.AmmoPressMenu;
import techguns.tileentities.AmmoPressTileEnt;
import techguns.tileentities.operation.AmmoPressBuildPlans;

public class AmmoPressScreen extends TGMachineScreen<AmmoPressMenu> {

	private final AmmoPressTileEnt press;

	public AmmoPressScreen(AmmoPressMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, Techguns.rl("textures/gui/ammo_press_gui.png"));
		this.press = menu.getTile();
	}

	@Override
	protected void init() {
		super.init();
		addMenuButton(AmmoPressTileEnt.BUTTON_ID_NEXT, leftPos + 40, topPos + 50, 20, 20, Component.literal(">"));
		addMenuButton(AmmoPressTileEnt.BUTTON_ID_PREV, leftPos + 20, topPos + 50, 20, 20, Component.literal("<"));
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		int color = 4210752;
		g.drawString(font, Component.translatable("techguns.gui.ammopress.build").append(":"), 20, 30, color, false);
		g.drawString(font, AmmoPressBuildPlans.getResult(press.buildPlan).getHoverName(), 20, 40, color, false);
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
