package techguns.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import techguns.items.ItemAmmoBox;

/**
 * The ammo box contents in its tooltip, drawn like vanilla's bundle tooltip (ClientBundleTooltip). The bundle tooltip
 * blocks the free slot once a bundle holds a stack; an ammo box holds several, so this one blocks it only when full.
 */
public class ClientAmmoBoxTooltip implements ClientTooltipComponent {
	private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/background");
	private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/slot");
	private static final ResourceLocation BLOCKED_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/blocked_slot");
	private static final int SLOT_W = 18, SLOT_H = 20;

	private final ItemAmmoBox.Tooltip tooltip;

	public ClientAmmoBoxTooltip(ItemAmmoBox.Tooltip tooltip) {
		this.tooltip = tooltip;
	}

	private int size() {
		return tooltip.contents().size();
	}

	private int gridSizeX() {
		return Math.max(2, (int) Math.ceil(Math.sqrt(size() + 1.0)));
	}

	private int gridSizeY() {
		return (int) Math.ceil((size() + 1.0) / gridSizeX());
	}

	@Override
	public int getHeight() {
		return gridSizeY() * SLOT_H + 2 + 4;
	}

	@Override
	public int getWidth(Font font) {
		return gridSizeX() * SLOT_W + 2;
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics g) {
		int cols = gridSizeX(), rows = gridSizeY();
		g.blitSprite(BACKGROUND_SPRITE, x, y, cols * SLOT_W + 2, rows * SLOT_H + 2);
		int index = 0;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int sx = x + c * SLOT_W + 1, sy = y + r * SLOT_H + 1;
				if (index >= size()) {
					g.blitSprite(tooltip.full() ? BLOCKED_SLOT_SPRITE : SLOT_SPRITE, sx, sy, 0, SLOT_W, SLOT_H);
				} else {
					ItemStack s = tooltip.contents().getItemUnsafe(index);
					g.blitSprite(SLOT_SPRITE, sx, sy, 0, SLOT_W, SLOT_H);
					g.renderItem(s, sx + 1, sy + 1, index);
					g.renderItemDecorations(font, s, sx + 1, sy + 1);
					if (index == 0) {
						AbstractContainerScreen.renderSlotHighlight(g, sx + 1, sy + 1, 0);
					}
				}
				index++;
			}
		}
	}
}
