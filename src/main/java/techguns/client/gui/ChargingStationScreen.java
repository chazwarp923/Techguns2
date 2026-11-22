package techguns.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.ChargingStationMenu;
import techguns.tileentities.ChargingStationTileEnt;

public class ChargingStationScreen extends TGMachineScreen<ChargingStationMenu> {

	private final ChargingStationTileEnt station;

	public ChargingStationScreen(ChargingStationMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, Techguns.rl("textures/gui/charging_station_gui.png"));
		this.station = menu.getTile();
		this.upgradeSlotX = ChargingStationMenu.SLOT_UPGRADE_X - 1;
		this.upgradeSlotY = ChargingStationMenu.SLOT_UPGRADE_Y - 1;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		if (station.isWorking()) {
			int i1 = station.getProgressScaled(26);
			g.blit(tex, leftPos + 39, topPos + 19, 0, 167, i1 + 1, 10);
		}
	}
}
