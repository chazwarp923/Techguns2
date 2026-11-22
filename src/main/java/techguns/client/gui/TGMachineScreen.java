package techguns.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.gui.containers.TGBaseMenu;
import techguns.tileentities.BasicInventoryTileEnt;
import techguns.tileentities.BasicOwnedTileEnt;
import techguns.tileentities.BasicPoweredTileEnt;
import techguns.tileentities.BasicRedstoneTileEnt;

/**
 * Screen of the Techguns block entities (1.12 TGBaseGui / OwnedTileEntGui / RedstoneTileEntGui / PoweredTileEntGui):
 * security and redstone buttons left of the gui, power bar, upgrade slot frame.
 */
public abstract class TGMachineScreen<M extends TGBaseMenu<?>> extends AbstractContainerScreen<M> {

	/** the icons for security / redstone / power live on the ammo press texture */
	public static final ResourceLocation WIDGETS = Techguns.rl("textures/gui/ammo_press_gui.png");
	public static final String POWER_UNIT = "FE";

	protected final ResourceLocation tex;
	protected final BasicInventoryTileEnt tile;
	protected int heightSecurityButton = 40;
	protected boolean hasUpgradeSlot = true;
	protected int upgradeSlotX = 151;
	protected int upgradeSlotY = 59;
	protected boolean showInventoryText = true;

	public TGMachineScreen(M menu, Inventory inv, Component title, ResourceLocation tex) {
		super(menu, inv, title);
		this.tex = tex;
		this.tile = menu.getTile();
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	public static boolean isInRect(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX <= (x + width) && mouseY >= y && mouseY <= (y + height);
	}

	protected void sendButton(int id) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
		}
	}

	/** a vanilla button that sends a container button click */
	protected Button addMenuButton(int id, int x, int y, int w, int h, Component text) {
		return this.addRenderableWidget(Button.builder(text, b -> sendButton(id)).bounds(x, y, w, h).build());
	}

	/** a 20x20 button showing a 16x16 icon from WIDGETS */
	protected class IconButton extends Button {
		private final IntSupplier v;
		private final int u;

		public IconButton(int id, int x, int y, int u, IntSupplier v) {
			super(x, y, 20, 20, Component.empty(), b -> sendButton(id), DEFAULT_NARRATION);
			this.u = u;
			this.v = v;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(g, mouseX, mouseY, partialTick);
			g.blit(WIDGETS, this.getX() + 2, this.getY() + 2, u, v.getAsInt(), 16, 16);
		}
	}

	@Override
	protected void init() {
		super.init();
		if (tile instanceof BasicOwnedTileEnt owned) {
			this.addRenderableWidget(new IconButton(BasicOwnedTileEnt.BUTTON_ID_SECURITY, leftPos - 22, topPos + heightSecurityButton, 234,
					() -> 58 + 16 * owned.getSecurity()));
		}
		if (tile instanceof BasicRedstoneTileEnt rs) {
			this.addRenderableWidget(new IconButton(BasicOwnedTileEnt.BUTTON_ID_REDSTONE, leftPos - 22, topPos + 10, 234,
					() -> 10 + 16 * rs.getRedstoneBehaviour()));
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		this.renderTooltip(g, mouseX, mouseY);
		int mx = mouseX - leftPos;
		int my = mouseY - topPos;
		List<Component> tooltip = new ArrayList<>();
		this.addTooltips(tooltip, mx, my);
		if (!tooltip.isEmpty()) {
			g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
		}
	}

	/** mx/my relative to the gui */
	protected void addTooltips(List<Component> tooltip, int mx, int my) {
		if (tile instanceof BasicOwnedTileEnt owned && isInRect(mx, my, -22, heightSecurityButton, 20, 20)) {
			byte type = owned.getSecurity();
			ChatFormatting color = switch (type) {
				case 1 -> ChatFormatting.YELLOW;
				case 2 -> ChatFormatting.GOLD;
				case 3 -> ChatFormatting.RED;
				default -> ChatFormatting.GREEN;
			};
			Component own = Component.translatable("techguns.container.security.tooltip.unowned");
			UUID owner = owned.getOwner();
			if (owner != null) {
				own = Component.translatable("techguns.container.security.tooltip.owner").append(": " + resolvePlayerName(owner));
			}
			tooltip.add(Component.translatable("techguns.container.security.owner").append(": ").append(own));
			tooltip.add(Component.translatable("techguns.container.security.tooltip").append(": ")
					.append(Component.translatable("techguns.container.security.tooltip." + type).withStyle(color)));
			tooltip.add(Component.translatable("techguns.container.security.tooltip.description." + type));
			tooltip.add(Component.translatable("techguns.container.security.tooltip.descr2"));
		}
		if (tile instanceof BasicRedstoneTileEnt rs && (isInRect(mx, my, 6, 5, 10, 5) || isInRect(mx, my, -22, 10, 20, 20))) {
			tooltip.add(Component.translatable("techguns.container.redstone"));
			tooltip.add(Component.translatable("techguns.container.redstone.mode").append(": ").append(Component.translatable(rs.getRedstoneModeText())));
			tooltip.add(Component.translatable("techguns.container.redstone.signal").append(": ").append(Component.translatable(rs.getSignalStateText())));
		}
		if (tile instanceof BasicPoweredTileEnt pw && isInRect(mx, my, 7, 17, 5, 49)) {
			if (TGConfig.machinesNeedNoPower) {
				tooltip.add(Component.translatable("techguns.container.power"));
			} else {
				tooltip.add(Component.translatable("techguns.container.power").append(": " + pw.getEnergyStorage().getEnergyStored() + "/"
						+ pw.getEnergyStorage().getMaxEnergyStored() + " " + POWER_UNIT));
			}
		}
	}

	protected static String resolvePlayerName(UUID id) {
		var conn = Minecraft.getInstance().getConnection();
		if (conn != null) {
			PlayerInfo info = conn.getPlayerInfo(id);
			if (info != null) {
				return info.getProfile().getName();
			}
		}
		return id.toString();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		int color = 4210752;
		if (this.showInventoryText) {
			g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, color, false);
		}
		g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, color, false);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int k = this.leftPos;
		int l = this.topPos;
		g.blit(tex, k, l, 0, 0, this.imageWidth, this.imageHeight);
		if (tile instanceof BasicOwnedTileEnt) {
			g.blit(WIDGETS, k - 22 - 5, l + heightSecurityButton - 5, 195, 0, 22 + 5, 30);
		}
		if (tile instanceof BasicRedstoneTileEnt rs) {
			g.blit(WIDGETS, k - 22 - 5, l + 10 - 5, 195, 0, 22 + 5, 30);
			if (rs.isRedstoneEnabled()) {
				g.blit(WIDGETS, k + 6, l + 5, 240, 0, 5, 5);
				g.blit(WIDGETS, k + 6 + 5, l + 5, 245, 5, 5, 5);
			} else {
				g.blit(WIDGETS, k + 6, l + 5, 240, 5, 5, 5);
				g.blit(WIDGETS, k + 6 + 5, l + 5, 245, 0, 5, 5);
			}
		}
		if (tile instanceof BasicPoweredTileEnt pw) {
			int max = Math.max(1, pw.getEnergyStorage().getMaxEnergyStored());
			int power = (int) ((long) pw.getEnergyStorage().getEnergyStored() * 48 / max);
			int diff = TGConfig.machinesNeedNoPower ? 0 : 48 - power;
			g.blit(WIDGETS, k + 8, l + 17 + diff, 251, 1 + diff, 4, 48 - diff);
			if (hasUpgradeSlot) {
				g.blit(WIDGETS, k + upgradeSlotX, l + upgradeSlotY, 119, 59, 20, 20);
			}
		}
	}
}
