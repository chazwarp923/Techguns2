package techguns.plugins.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/** Base of the Techguns machine categories: JEI slot / arrow drawables, a title and an icon block. */
public abstract class MachineCategory<T> implements IRecipeCategory<T> {

	protected static final int TEXT_COLOR = 0xFF808080;

	protected final RecipeType<T> type;
	protected final Component title;
	protected final IDrawable icon;
	protected final IDrawable slot;
	protected final IDrawable outputSlot;
	protected final IDrawableAnimated arrow;
	protected final int width;
	protected final int height;

	protected MachineCategory(IGuiHelper gui, RecipeType<T> type, ItemLike machine, int width, int height, int arrowTicks) {
		this.type = type;
		this.title = Component.translatable(machine.asItem().getDescriptionId());
		this.icon = gui.createDrawableItemLike(machine);
		this.slot = gui.getSlotDrawable();
		this.outputSlot = gui.getOutputSlot();
		this.arrow = gui.createAnimatedRecipeArrow(arrowTicks);
		this.width = width;
		this.height = height;
	}

	@Override
	public RecipeType<T> getRecipeType() {
		return type;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	/** arrow position, null for none */
	protected abstract int[] arrowPos();

	@Override
	public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
		int[] a = arrowPos();
		if (a != null) {
			arrow.draw(g, a[0], a[1]);
		}
	}

	protected static void text(GuiGraphics g, Component text, int x, int y) {
		g.drawString(Minecraft.getInstance().font, text, x, y, TEXT_COLOR, false);
	}

	protected static Component powerAndTime(int powerPerTick, int ticks) {
		return Component.translatable("techguns.jei.power_time", powerPerTick, ticks / 20f);
	}
}
