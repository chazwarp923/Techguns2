package techguns.client.test;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Draws a page of items in a grid, for the client test's screenshots. A failing item is reported and skipped. */
public class ItemGridScreen extends Screen {
	public static final int COLS = 16;
	public static final int ROWS = 9;
	public static final int CELL = 24;

	private final List<ItemStack> items;
	private final Consumer<String> onError;
	private final String title;

	public ItemGridScreen(String title, List<ItemStack> items, Consumer<String> onError) {
		super(Component.literal(title));
		this.title = title;
		this.items = items;
		this.onError = onError;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, this.width, this.height, 0xFF404858);
		int x0 = (this.width - COLS * CELL) / 2;
		int y0 = 20;
		g.drawString(this.font, this.title, x0, 6, 0xFFFFFF);
		for (int i = 0; i < this.items.size(); i++) {
			ItemStack stack = this.items.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			int x = x0 + (i % COLS) * CELL + 4;
			int y = y0 + (i / COLS) * CELL + 4;
			try {
				g.renderItem(stack, x, y);
				g.renderItemDecorations(this.font, stack, x, y);
			} catch (Throwable t) {
				this.onError.accept(stack.getItem() + ": " + t);
				this.items.set(i, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
