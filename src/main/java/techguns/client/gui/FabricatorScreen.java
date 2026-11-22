package techguns.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.FabricatorMenu;
import techguns.tileentities.FabricatorTileEntMaster;

/** Fabricator gui (1.12 FabricatorGui). */
public class FabricatorScreen extends TGMachineScreen<FabricatorMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/fabricator_gui.png");

	protected final FabricatorTileEntMaster fab;

	public FabricatorScreen(FabricatorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
		this.fab = menu.getTile();
		this.upgradeSlotX = FabricatorMenu.SLOT_UPGRADE_X - 1;
		this.upgradeSlotY = FabricatorMenu.SLOT_UPGRADE_Y - 1;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		if (fab.isWorking()) {
			int i1 = fab.getProgressScaled(90);
			g.blit(TEXTURE, leftPos + 21, topPos + 53, 0, 167, i1 + 1, 11);
		}
	}

	@Override
	protected void addTooltips(List<Component> tooltip, int mx, int my) {
		super.addTooltips(tooltip, mx, my);
		if (!fab.isFormed() && isInRect(mx, my, 21, 53, 91, 11)) {
			tooltip.add(Component.translatable("techguns.container.oredrill.broken"));
		}
	}
}
