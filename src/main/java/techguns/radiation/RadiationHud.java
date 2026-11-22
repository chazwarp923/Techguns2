package techguns.radiation;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;

/** "N RAD" and the current "+[x RAD/s]" in the bottom right corner (1.12 TGGuiEvents). */
public final class RadiationHud {

	private RadiationHud() {
	}

	public static void register(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, Techguns.rl("radiation"), RadiationHud::render);
	}

	private static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		Player ply = mc.player;
		if (ply == null || mc.options.hideGui) {
			return;
		}
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		if (!props.showTGHudElements || props.radlevel <= 0) {
			return;
		}
		Font font = mc.font;
		int w = g.guiWidth();
		int y = g.guiHeight() - 10;
		ChatFormatting color = ChatFormatting.WHITE;
		if (props.radlevel >= TGRadiationSystem.LETHAL_POISONING) {
			color = ChatFormatting.RED;
		} else if (props.radlevel >= TGRadiationSystem.SEVERE_POISONING) {
			color = ChatFormatting.GOLD;
		} else if (props.radlevel >= TGRadiationSystem.MINOR_POISONING) {
			color = ChatFormatting.YELLOW;
		}
		String text = props.radlevel + " RAD";
		g.drawString(font, text, w - font.width(text) - 2, y, color.getColor() | 0xFF000000);
		if (ply.hasEffect(TGRadiationSystem.RADIATION)) {
			String cur = "+[" + TGRadiationSystem.getRadPerSecond(ply) + " RAD/s]";
			g.drawString(font, cur, w - font.width(cur) - 2, y - 10, 0xFFFFFFFF);
		}
	}
}
