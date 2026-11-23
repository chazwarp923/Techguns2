package techguns.client.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeHolder;
import techguns.gui.containers.AmmoBenchMenu;
import techguns.recipes.AmmoBenchRecipe;

/**
 * Ammo bench screen (Fabric port AmmoBenchScreen): vanilla's stonecutter screen and texture, with the input slot painted
 * over and three input slots drawn in its place.
 */
public class AmmoBenchScreen extends AbstractContainerScreen<AmmoBenchMenu> {
	private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller");
	private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller_disabled");
	private static final ResourceLocation RECIPE_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_selected");
	private static final ResourceLocation RECIPE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_highlighted");
	private static final ResourceLocation RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe");
	private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");
	private static final int RECIPES_X = 52, RECIPES_Y = 14, COLUMNS = 4, SHOWN = 12;

	private float scrollOffs;
	private boolean scrolling;
	private int startIndex;
	private boolean displayRecipes;

	public AmmoBenchScreen(AmmoBenchMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		menu.registerUpdateListener(this::containerChanged);
		this.titleLabelY--;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		this.renderTooltip(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos, y = this.topPos;
		g.blit(BG_LOCATION, x, y, 0, 0, this.imageWidth, this.imageHeight);
		// paint over the stonecutter's input slot, then draw the three input slots with its slot graphic
		g.blit(BG_LOCATION, x + 19, y + 32, 19, 14, 18, 18);
		for (int i = 0; i < 3; i++) {
			g.blit(BG_LOCATION, x + 19, y + 14 + i * 19, 19, 32, 18, 18);
		}
		int k = (int) (41.0F * this.scrollOffs);
		g.blitSprite(isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE, x + 119, y + 15 + k, 12, 15);
		if (!displayRecipes) {
			return;
		}
		List<RecipeHolder<AmmoBenchRecipe>> recipes = menu.getRecipes();
		for (int i = startIndex; i < startIndex + SHOWN && i < menu.getNumRecipes(); i++) {
			int bx = buttonX(i), by = buttonY(i);
			ResourceLocation sprite = i == menu.getSelectedRecipeIndex() ? RECIPE_SELECTED_SPRITE
					: mouseX >= bx && mouseY >= by && mouseX < bx + 16 && mouseY < by + 18 ? RECIPE_HIGHLIGHTED_SPRITE : RECIPE_SPRITE;
			g.blitSprite(sprite, bx, by - 1, 16, 18);
			g.renderItem(recipes.get(i).value().result(), bx, by);
			g.renderItemDecorations(this.font, recipes.get(i).value().result(), bx, by);
		}
	}

	private int buttonX(int index) {
		return this.leftPos + RECIPES_X + (index - startIndex) % COLUMNS * 16;
	}

	private int buttonY(int index) {
		return this.topPos + RECIPES_Y + (index - startIndex) / COLUMNS * 18 + 2;
	}

	@Override
	protected void renderTooltip(GuiGraphics g, int x, int y) {
		super.renderTooltip(g, x, y);
		if (displayRecipes) {
			List<RecipeHolder<AmmoBenchRecipe>> recipes = menu.getRecipes();
			for (int i = startIndex; i < startIndex + SHOWN && i < menu.getNumRecipes(); i++) {
				int bx = buttonX(i), by = buttonY(i);
				if (x >= bx && x < bx + 16 && y >= by && y < by + 18) {
					g.renderTooltip(this.font, recipes.get(i).value().result(), x, y);
				}
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		this.scrolling = false;
		if (displayRecipes) {
			for (int i = startIndex; i < startIndex + SHOWN; i++) {
				double dx = mouseX - buttonX(i), dy = mouseY - (buttonY(i) - 2);
				if (dx >= 0 && dy >= 0 && dx < 16 && dy < 18 && menu.clickMenuButton(this.minecraft.player, i)) {
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
					this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
					return true;
				}
			}
			int sx = this.leftPos + 119, sy = this.topPos + 9;
			if (mouseX >= sx && mouseX < sx + 12 && mouseY >= sy && mouseY < sy + 54) {
				this.scrolling = true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (scrolling && isScrollBarActive()) {
			int top = this.topPos + 14, bottom = top + 54;
			this.scrollOffs = Mth.clamp(((float) mouseY - top - 7.5F) / ((bottom - top) - 15.0F), 0.0F, 1.0F);
			this.startIndex = (int) (this.scrollOffs * getOffscreenRows() + 0.5) * COLUMNS;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (isScrollBarActive()) {
			int rows = getOffscreenRows();
			this.scrollOffs = Mth.clamp(this.scrollOffs - (float) scrollY / rows, 0.0F, 1.0F);
			this.startIndex = (int) (this.scrollOffs * rows + 0.5) * COLUMNS;
		}
		return true;
	}

	private boolean isScrollBarActive() {
		return displayRecipes && menu.getNumRecipes() > SHOWN;
	}

	private int getOffscreenRows() {
		return (menu.getNumRecipes() + COLUMNS - 1) / COLUMNS - 3;
	}

	private void containerChanged() {
		this.displayRecipes = menu.hasInputItems();
		if (!displayRecipes) {
			this.scrollOffs = 0.0F;
			this.startIndex = 0;
		}
	}
}
