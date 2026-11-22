package techguns.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.Door3x3Menu;
import techguns.tileentities.Door3x3TileEnt;

/** 3x3 door settings (1.12 Door3x3Gui): door mode and auto close, plus security and redstone buttons. */
public class Door3x3Screen extends TGMachineScreen<Door3x3Menu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/dungeon_scanner_gui.png");

	private final Door3x3TileEnt door;

	public Door3x3Screen(Door3x3Menu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
		this.door = menu.getTile();
		this.showInventoryText = false;
	}

	@Override
	protected void init() {
		super.init();
		addMenuButton(Door3x3TileEnt.BUTTON_ID_DOORMODE, leftPos + 8, topPos + 16, 32, 16, Component.translatable("techguns.mode"));
		this.addRenderableWidget(new Button(leftPos + 8, topPos + 36, 32, 16, Component.empty(), b -> sendButton(Door3x3TileEnt.BUTTON_ID_AUTOCLOSE),
				n -> n.get()) {
			@Override
			public Component getMessage() {
				return Component.translatable(door.isAutoClose() ? "techguns.yes" : "techguns.no");
			}
		});
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		int color = 4210752;
		g.drawString(this.font, Component.translatable("techguns.container.door3x3.mode." + door.getDoormode()), 44, 20, color, false);
		Component auto = Component.translatable("techguns.container.door3x3.autoclose");
		if (door.getDoormode() != 0) {
			auto = auto.copy().withStyle(ChatFormatting.STRIKETHROUGH);
		}
		g.drawString(this.font, auto, 44, 40, color, false);
		if (door.getDoormode() == 1 && door.getRedstoneBehaviour() == 0) {
			g.drawString(this.font, Component.translatable("techguns.container.door3x3.redstonewarning").withStyle(ChatFormatting.RED), 44, 60, color,
					false);
		}
	}
}
