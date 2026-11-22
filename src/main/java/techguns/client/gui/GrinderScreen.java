package techguns.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.GrinderMenu;
import techguns.tileentities.GrinderTileEnt;

public class GrinderScreen extends TGMachineScreen<GrinderMenu> {

	private final GrinderTileEnt grinder;

	public GrinderScreen(GrinderMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, Techguns.rl("textures/gui/grinder_gui.png"));
		this.grinder = menu.getTile();
		this.upgradeSlotX = GrinderMenu.SLOT_UPGRADE_X - 1;
		this.upgradeSlotY = GrinderMenu.SLOT_UPGRADE_Y - 1;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		if (grinder.isWorking()) {
			int i1 = grinder.getProgressScaled(21);
			g.blit(tex, leftPos + 31, topPos + 39, 0, 167, i1 + 1, 22);
			int i2 = 20 - i1;
			g.blit(tex, leftPos + 31 + 20 + i2, topPos + 39, i2, 167, i1 + 1, 22);
		}
	}
}
