package techguns.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.ReactionChamberMenu;
import techguns.recipes.ReactionChamberRecipe;
import techguns.tileentities.ReactionChamberTileEntMaster;
import techguns.tileentities.operation.ReactionChamberOperation;

/**
 * Reaction chamber gui (1.12 ReactionChamberGui): tank with liquid level marker, intensity bar with the wanted
 * intensity marker, completion and time bars.
 */
public class ReactionChamberScreen extends TGMachineScreen<ReactionChamberMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/reaction_chamber_gui.png");

	private final ReactionChamberTileEntMaster rc;

	public ReactionChamberScreen(ReactionChamberMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
		this.rc = menu.getTile();
		this.hasUpgradeSlot = false;
	}

	@Override
	protected void init() {
		super.init();
		addMenuButton(ReactionChamberTileEntMaster.BUTTON_ID_LIQUIDLEVEL_INC, leftPos + 33, topPos + 58, 10, 10, Component.literal("+"));
		addMenuButton(ReactionChamberTileEntMaster.BUTTON_ID_LIQUIDLEVEL_DEC, leftPos + 43, topPos + 58, 10, 10, Component.literal("-"));
		addMenuButton(ReactionChamberTileEntMaster.BUTTON_ID_INTENSITY_INC, leftPos + 91, topPos + 36, 10, 10, Component.literal("+"));
		addMenuButton(ReactionChamberTileEntMaster.BUTTON_ID_INTENSITY_DEC, leftPos + 101, topPos + 36, 10, 10, Component.literal("-"));
		addMenuButton(ReactionChamberTileEntMaster.BUTTON_ID_DUMPTANK, leftPos + 20, topPos + 7, 8, 8, Component.literal("x"));
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		int k = leftPos;
		int l = topPos;
		ChemLabScreen.drawFluid(g, rc.inputTank.getFluid(), rc.inputTank.getCapacity(), k + ChemLabScreen.INPUT_TANK_X + 1, l + ChemLabScreen.TANK_Y,
				ChemLabScreen.TANK_W, ChemLabScreen.TANK_H);
		g.blit(TEXTURE, k + 18, l + 17, 176, 32, 12, 52);
		int pxHeight = 50 - rc.getLiquidLevel() * 5;
		g.blit(TEXTURE, k + 19, l + 17 + pxHeight, 177, 32, 7, 1);

		int px = (10 - rc.getIntensity()) * 4;
		ReactionChamberOperation op = rc.getCurrentReaction();
		ReactionChamberRecipe rec = op != null ? op.getRecipe(rc.getLevel()) : null;
		if (op != null && rec != null) {
			int complPx = Math.round(Math.min(1f, (float) op.completion / rec.requiredCompletion()) * 100f);
			int durPx = rc.totaltime > 0 ? Math.round(Math.min(1f, (float) rc.progress / rc.totaltime) * 100f) : 0;
			g.blit(TEXTURE, k + 67, l + 61, 0, 167, complPx, 4);
			g.blit(TEXTURE, k + 67, l + 69, 0, 175, durPx, 4);
			g.blit(TEXTURE, k + 114, l + 16 + px, op.requiredIntensity != rc.getIntensity() ? 198 : 190, px, 5, 40 - px);
			int h = 40 - op.requiredIntensity * 4;
			g.blit(TEXTURE, k + 112, l + 15 + h, 178, 22, 5, 3);
		} else {
			g.blit(TEXTURE, k + 114, l + 16 + px, 190, px, 5, 40 - px);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		g.drawString(this.font, rc.getLiquidLevel() * 10 + "%", 34, 50, 4210752, false);
		g.drawString(this.font, String.valueOf(rc.getIntensity()), 121, 18, 4210752, false);
	}

	@Override
	protected void addTooltips(List<Component> tooltip, int mx, int my) {
		super.addTooltips(tooltip, mx, my);
		if (isInRect(mx, my, 18, 16, 11, 51)) {
			ChemLabScreen.tankTooltip(tooltip, rc.inputTank);
		} else if (isInRect(mx, my, 20, 7, 8, 8)) {
			tooltip.add(Component.translatable("techguns.chemlab.dumpinput"));
			tooltip.add(Component.translatable("techguns.chemlab.dumpinput.tooltip"));
		} else if (isInRect(mx, my, 67, 60, 100, 14)) {
			if (!rc.isFormed()) {
				tooltip.add(Component.translatable("techguns.container.oredrill.broken"));
			} else {
				ReactionChamberOperation op = rc.getCurrentReaction();
				ReactionChamberRecipe rec = op != null ? op.getRecipe(rc.getLevel()) : null;
				if (op != null && rec != null) {
					tooltip.add(Component.translatable("techguns.container.reactionchamber.completion").append(": " + op.completion + "/" + rec.requiredCompletion()));
					tooltip.add(Component.translatable("techguns.container.reactionchamber.time").append(": " + rc.progress / 20 + "/" + rc.totaltime / 20 + "s"));
				}
			}
		}
	}
}
