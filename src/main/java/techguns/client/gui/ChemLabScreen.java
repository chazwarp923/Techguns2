package techguns.client.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.Techguns;
import techguns.gui.containers.ChemLabMenu;
import techguns.tileentities.ChemLabTileEnt;

/** Chem lab gui (1.12 ChemLabGui): two tanks, dump buttons, drain mode toggle and the 5 stage progress animation. */
public class ChemLabScreen extends TGMachineScreen<ChemLabMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/chem_lab_gui.png");

	public static final int INPUT_TANK_X = 18;
	public static final int TANK_Y = 17;
	public static final int OUTPUT_TANK_X = 157;
	public static final int TANK_W = 10;
	public static final int TANK_H = 50;

	private final ChemLabTileEnt lab;

	public ChemLabScreen(ChemLabMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
		this.lab = menu.getTile();
		this.upgradeSlotX = ChemLabMenu.SLOT_UPGRADE_X - 1;
		this.upgradeSlotY = ChemLabMenu.SLOT_UPGRADE_Y - 1;
	}

	@Override
	protected void init() {
		super.init();
		addMenuButton(ChemLabTileEnt.BUTTON_ID_DUMP_INPUT, leftPos + 20, topPos + 7, 8, 8, Component.literal("x"));
		addMenuButton(ChemLabTileEnt.BUTTON_ID_DUMP_OUTPUT, leftPos + 159, topPos + 7, 8, 8, Component.literal("x"));
		this.addRenderableWidget(new Button(leftPos + 156, topPos + 68, 14, 14, Component.empty(), b -> sendButton(ChemLabTileEnt.BUTTON_ID_TOGGLE_DRAIN),
				narration -> narration.get()) {
			@Override
			protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
				super.renderWidget(g, mouseX, mouseY, partialTick);
				g.blit(TEXTURE, this.getX() + 1, this.getY() + 1, 188, 31 + 12 * lab.getDrainMode(), 12, 12);
			}
		});
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		int k = leftPos;
		int l = topPos;
		drawTank(g, lab.inputTank, k + INPUT_TANK_X + 1, l + TANK_Y);
		drawTank(g, lab.outputTank, k + OUTPUT_TANK_X + 1, l + TANK_Y);
		g.blit(TEXTURE, k + INPUT_TANK_X, l + TANK_Y, 176, 32, TANK_W + 2, TANK_H + 2);
		g.blit(TEXTURE, k + OUTPUT_TANK_X, l + TANK_Y, 176, 32, TANK_W + 2, TANK_H + 2);

		if (lab.isWorking()) {
			float prog = lab.getProgress();
			int i1 = (int) ((prog < 0.2f ? prog * 5 : 1.0f) * 25);
			g.blit(TEXTURE, k + 81, l + 19, 178, 5, 8, i1);
			if (prog >= 0.2f) {
				int i2 = (int) ((prog < 0.4f ? (prog - 0.2f) * 5 : 1.0f) * 9);
				g.blit(TEXTURE, k + 88, l + 39, 186, 25, i2, 1);
			}
			if (prog >= 0.4f) {
				int i3 = (int) ((prog < 0.6f ? (prog - 0.4f) * 5 : 1.0f) * 20);
				g.blit(TEXTURE, k + 97, l + 23 + (20 - i3), 194, 9 + (20 - i3), 14, i3);
			}
			if (prog >= 0.6f) {
				int i4 = (int) ((prog < 0.8f ? (prog - 0.6f) * 5 : 1.0f) * 16);
				g.blit(TEXTURE, k + 108, l + 17, 205, 3, i4, 6);
			}
			if (prog >= 0.8f) {
				int i5 = (int) ((prog - 0.8f) * 5 * 24);
				g.blit(TEXTURE, k + 117, l + 20, 214, 6, 14, i5 + 1);
			}
		}
	}

	private void drawTank(GuiGraphics g, FluidTank tank, int x, int y) {
		drawFluid(g, tank.getFluid(), tank.getCapacity(), x, y, TANK_W, TANK_H);
	}

	/** fluid level in a gui tank: tiles of the still texture from the bottom, clipped to the fill height */
	public static void drawFluid(GuiGraphics g, FluidStack fluid, int capacity, int x, int y, int w, int h) {
		if (fluid.isEmpty() || capacity <= 0) {
			return;
		}
		int px = Math.max(1, Math.min(h, (int) ((long) fluid.getAmount() * h / capacity)));
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
		ResourceLocation still = ext.getStillTexture(fluid);
		if (still == null) {
			return;
		}
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
		int color = ext.getTintColor(fluid);
		float a = ((color >> 24) & 0xFF) / 255f;
		float r = ((color >> 16) & 0xFF) / 255f;
		float gr = ((color >> 8) & 0xFF) / 255f;
		float b = (color & 0xFF) / 255f;
		g.enableScissor(x, y + h - px, x + w, y + h);
		for (int off = 0; off < px; off += 16) {
			g.blit(x, y + h - off - 16, 0, w, 16, sprite, r, gr, b, a == 0 ? 1f : a);
		}
		g.disableScissor();
	}

	@Override
	protected void addTooltips(List<Component> tooltip, int mx, int my) {
		super.addTooltips(tooltip, mx, my);
		if (isInRect(mx, my, INPUT_TANK_X, TANK_Y - 1, TANK_W + 1, TANK_H + 1)) {
			tankTooltip(tooltip, lab.inputTank);
		} else if (isInRect(mx, my, OUTPUT_TANK_X, TANK_Y - 1, TANK_W + 1, TANK_H + 1)) {
			tankTooltip(tooltip, lab.outputTank);
		} else if (isInRect(mx, my, 20, 7, 8, 8)) {
			tooltip.add(Component.translatable("techguns.chemlab.dumpinput"));
			tooltip.add(Component.translatable("techguns.chemlab.dumpinput.tooltip"));
		} else if (isInRect(mx, my, 159, 7, 8, 8)) {
			tooltip.add(Component.translatable("techguns.chemlab.dumpoutput"));
			tooltip.add(Component.translatable("techguns.chemlab.dumpoutput.tooltip"));
		} else if (isInRect(mx, my, 156, 68, 14, 14)) {
			tooltip.add(Component.translatable("techguns.chemlab.toogledrain"));
			tooltip.add(Component.translatable("techguns.chemlab.toogledrain.tooltip." + lab.getDrainMode()));
		}
	}

	public static void tankTooltip(List<Component> tooltip, FluidTank tank) {
		FluidStack f = tank.getFluid();
		tooltip.add(f.isEmpty() ? Component.translatable("techguns.gui.empty") : f.getHoverName());
		tooltip.add(Component.literal(tank.getFluidAmount() + "/" + tank.getCapacity() + "mB"));
	}
}
