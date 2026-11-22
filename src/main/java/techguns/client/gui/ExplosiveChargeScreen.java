package techguns.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.ExplosiveChargeMenu;
import techguns.tileentities.ExplosiveChargeTileEnt;

/** Mining charge gui (1.12 ExplosiveChargeGui): radius / length / delay and the detonate button. */
public class ExplosiveChargeScreen extends AbstractContainerScreen<ExplosiveChargeMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/tnt_charge_gui.png");
	public static final ResourceLocation TEXTURE_ADVANCED = Techguns.rl("textures/gui/exp_charge_gui.png");

	private final ExplosiveChargeTileEnt charge;

	public ExplosiveChargeScreen(ExplosiveChargeMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.charge = menu.getTile();
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	private void send(int id) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
		}
	}

	private void button(int id, int x, int y, int w, int h, Component text) {
		this.addRenderableWidget(Button.builder(text, b -> send(id)).bounds(leftPos + x, topPos + y, w, h).build());
	}

	@Override
	protected void init() {
		super.init();
		button(ExplosiveChargeTileEnt.BUTTON_ID_DETONATE, 57, 73, 60, 20, Component.translatable("techguns.container.explosivecharge.detonate"));
		int left = 120;
		Component plus = Component.literal("+");
		Component minus = Component.literal("-");
		button(ExplosiveChargeTileEnt.BUTTON_ID_RADIUS_PLUS, left, 40, 10, 10, plus);
		button(ExplosiveChargeTileEnt.BUTTON_ID_RADIUS_MINUS, left + 10, 40, 10, 10, minus);
		button(ExplosiveChargeTileEnt.BUTTON_ID_LENGTH_PLUS, left, 50, 10, 10, plus);
		button(ExplosiveChargeTileEnt.BUTTON_ID_LENGTH_MINUS, left + 10, 50, 10, 10, minus);
		button(ExplosiveChargeTileEnt.BUTTON_ID_FUSE_PLUS, left, 60, 10, 10, plus);
		button(ExplosiveChargeTileEnt.BUTTON_ID_FUSE_MINUS, left + 10, 60, 10, 10, minus);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		g.blit(charge.isAdvanced() ? TEXTURE_ADVANCED : TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		int color = 4210752;
		g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, color, false);
		g.drawString(this.font, Component.translatable("techguns.container.explosivecharge.radius", charge.getBlastradius()), 37, 40, color, false);
		g.drawString(this.font, Component.translatable("techguns.container.explosivecharge.length", charge.getBlastlength()), 37, 50, color, false);
		g.drawString(this.font, Component.translatable("techguns.container.explosivecharge.delay", charge.getFusetime() / 20), 37, 60, color, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		this.renderTooltip(g, mouseX, mouseY);
	}
}
