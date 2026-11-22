package techguns.client.gui;

import java.util.List;
import java.util.function.IntSupplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import techguns.Techguns;
import techguns.gui.containers.TurretMenu;
import techguns.tileentities.TurretTileEnt;

/** Turret base gui (1.12 TurretGui): repair / health bar, target animals and pvp buttons. */
public class TurretScreen extends TGMachineScreen<TurretMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/turret_base_gui.png");

	protected final TurretTileEnt turret;

	public TurretScreen(TurretMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
		this.turret = menu.getTile();
		this.hasUpgradeSlot = false;
	}

	/** 20x20 button with a 16x16 icon from the turret gui texture */
	protected class TurretButton extends Button {
		private final int u;
		private final IntSupplier mode;

		public TurretButton(int id, int x, int y, int u, IntSupplier mode) {
			super(x, y, 20, 20, Component.empty(), b -> sendButton(id), narration -> narration.get());
			this.u = u;
			this.mode = mode;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(g, mouseX, mouseY, partialTick);
			g.blit(TEXTURE, this.getX() + 2, this.getY() + 2, u, 6 + 16 * mode.getAsInt(), 16, 16);
		}
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(new TurretButton(TurretTileEnt.BUTTON_ID_TARGET_ANIMALS, leftPos - 22, topPos + 70, 176,
				() -> turret.attackAnimals ? 1 : 0));
		this.addRenderableWidget(new TurretButton(TurretTileEnt.BUTTON_ID_PVP_SETTING, leftPos - 22, topPos + 100, 193, turret::getPvpSetting));
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		int k = leftPos;
		int l = topPos;
		g.blit(WIDGETS, k - 22 - 5, l + 70 - 5, 195, 0, 22 + 5, 30);
		g.blit(WIDGETS, k - 22 - 5, l + 100 - 5, 195, 0, 22 + 5, 30);
		if (turret.turretDeath) {
			int i1 = turret.getRepairTimeScaled(52);
			g.blit(TEXTURE, k + 116, l + 74, 177, 1, i1 + 1, 4);
		} else if (menu.getTurretMaxHealth() > 0) {
			// health bar, same gradient sprite: red when low, green when full
			int w = Math.round(52 * Math.min(1f, menu.getTurretHealth() / menu.getTurretMaxHealth()));
			if (w > 0) {
				g.blit(TEXTURE, k + 116, l + 74, 177, 1, w + 1, 4);
			}
		}
	}

	@Override
	protected void addTooltips(List<Component> tooltip, int mx, int my) {
		super.addTooltips(tooltip, mx, my);
		if (isInRect(mx, my, 115, 73, 53, 5)) {
			if (turret.turretDeath) {
				tooltip.add(Component.translatable("techguns.container.repair").append(" " + turret.repairTime / 20 + "s"));
			} else if (menu.getTurretMaxHealth() > 0) {
				tooltip.add(Component.translatable("techguns.turret.health").append(": " + menu.getTurretHealth() + "/" + menu.getTurretMaxHealth()));
			}
		}
		if (isInRect(mx, my, -22, 70, 20, 20)) {
			tooltip.add(Component.translatable("techguns.turret.targetAnimals").append(": ")
					.append(turret.attackAnimals ? Component.translatable("techguns.yes").withStyle(ChatFormatting.RED)
							: Component.translatable("techguns.no").withStyle(ChatFormatting.GREEN)));
		} else if (isInRect(mx, my, -22, 100, 20, 20)) {
			tooltip.add(Component.translatable("techguns.turret.pvpsetting").append(": ")
					.append(Component.translatable("techguns.turret.pvpsetting." + turret.getPvpSetting())));
			tooltip.add(Component.translatable("techguns.container.security.tooltip.descr2"));
		}
	}
}
