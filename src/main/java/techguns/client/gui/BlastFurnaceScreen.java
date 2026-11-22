package techguns.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.BlastFurnaceMenu;
import techguns.tileentities.BlastFurnaceTileEnt;

public class BlastFurnaceScreen extends TGMachineScreen<BlastFurnaceMenu> {

	private final BlastFurnaceTileEnt furnace;

	public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, Techguns.rl("textures/gui/blast_furnace_gui.png"));
		this.furnace = menu.getTile();
		this.upgradeSlotX = BlastFurnaceMenu.SLOT_UPGRADE_X - 1;
		this.upgradeSlotY = BlastFurnaceMenu.SLOT_UPGRADE_Y - 1;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		if (furnace.isWorking()) {
			int i1 = furnace.getProgressScaled(90);
			g.blit(tex, leftPos + 19, topPos + 53, 0, 167, i1 + 1, 11);
		}
	}
}
