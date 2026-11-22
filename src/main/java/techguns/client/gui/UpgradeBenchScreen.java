package techguns.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.UpgradeBenchMenu;

/** Upgrade bench gui (1.12 UpgradeBenchGui): rainbow animation while an upgrade is possible, xp and xp cost. */
public class UpgradeBenchScreen extends TGMachineScreen<UpgradeBenchMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/upgrade_bench_gui.png");

	public UpgradeBenchScreen(UpgradeBenchMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		if (!menu.outputSlot.getItem(0).isEmpty() && minecraft != null && minecraft.level != null) {
			long time = minecraft.level.getGameTime();
			int i = 3 - (int) (time % 4);
			float hue = (time % 360) / 360f;
			int x = i >= 2 ? 0 : 89;
			int y = i % 2 == 1 ? 0 : 31;
			int c = Mth.hsvToRgb(hue, 1f, 1f);
			g.setColor(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, 1f);
			g.blit(TEXTURE, leftPos + 37, topPos + 40, x, 167 + y, 89, 31);
			g.setColor(1f, 1f, 1f, 1f);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		var ply = minecraft.player;
		int xp = menu.getXpNeeded();
		int color = (ply.getAbilities().instabuild || xp <= ply.totalExperience) ? 8453920 : 16736352; // anvil colors
		drawXPText(g, Component.translatable("techguns.gui.xp").append(": " + ply.totalExperience), 95, 17, 8453920);
		drawXPText(g, Component.translatable("techguns.gui.xpcost").append(": " + xp), 95, 27, color);
	}

	protected void drawXPText(GuiGraphics g, Component s, int x, int y, int color) {
		int shadow = -16777216 | (color & 16579836) >> 2 | color & -16777216;
		g.drawString(font, s, x, y + 1, shadow, false);
		g.drawString(font, s, x + 1, y, shadow, false);
		g.drawString(font, s, x + 1, y + 1, shadow, false);
		g.drawString(font, s, x, y, color | 0xFF000000, false);
	}
}
