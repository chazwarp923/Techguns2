package techguns.client.gui;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.gui.containers.RepairBenchMenu;
import techguns.tileentities.RepairBenchTileEnt;

/** Repair bench gui (1.12 RepairBenchGui): repair buttons below the armor, offhand and repair slots. */
public class RepairBenchScreen extends TGMachineScreen<RepairBenchMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/repair_bench_gui.png");

	private final RepairBenchTileEnt bench;
	private final Inventory playerInv;

	public RepairBenchScreen(RepairBenchMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
		this.bench = menu.getTile();
		this.playerInv = inv;
		this.heightSecurityButton = 10;
		this.showInventoryText = false;
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < 4; i++) {
			this.addRenderableWidget(new RepairButton(RepairBenchTileEnt.BUTTON_ID_ARMOR0 + i, leftPos + 90 + i * 20, topPos + 39));
		}
		this.addRenderableWidget(new RepairButton(RepairBenchTileEnt.BUTTON_ID_OFFHAND, leftPos + 65, topPos + 39));
		this.addRenderableWidget(new RepairButton(RepairBenchTileEnt.BUTTON_ID_SLOT, leftPos + 9, topPos + 39));
	}

	@Override
	protected void addTooltips(List<Component> tooltip, int mx, int my) {
		super.addTooltips(tooltip, mx, my);
		for (int i = 0; i < 4; i++) {
			if (isInRect(mx, my, 90 + i * 20, 39, 14, 14)) {
				repairTooltip(tooltip, playerInv.player.getItemBySlot(RepairBenchTileEnt.ARMOR_SLOTS[i]));
			}
		}
		if (isInRect(mx, my, 65, 39, 14, 14)) {
			repairTooltip(tooltip, playerInv.player.getOffhandItem());
		}
		if (isInRect(mx, my, 9, 39, 14, 14)) {
			repairTooltip(tooltip, bench.getInventory().getStackInSlot(RepairBenchTileEnt.SLOT_REPAIR));
		}
	}

	private void repairTooltip(List<Component> tooltip, ItemStack item) {
		if (item.isEmpty()) {
			tooltip.add(Component.translatable("techguns.repairBench.noarmor"));
			return;
		}
		List<ItemStack> mats = RepairBenchTileEnt.getRepairMats(item);
		if (mats == null) {
			tooltip.add(Component.translatable("techguns.repairBench.cantrepair"));
		} else if (item.getDamageValue() <= 0 || mats.isEmpty()) {
			tooltip.add(Component.translatable("techguns.repairBench.fullcondition"));
		} else {
			tooltip.add(Component.translatable("techguns.repairBench.requiredMats"));
			for (ItemStack m : mats) {
				Component line = Component.literal(m.getCount() + "x ").append(m.getHoverName());
				tooltip.add(bench.countMissing(m) > 0 ? line.copy().withStyle(ChatFormatting.RED) : line);
			}
		}
	}

	/** 14x14 button with the wrench icon from the repair bench texture */
	private class RepairButton extends Button {
		RepairButton(int id, int x, int y) {
			super(x, y, 14, 14, Component.empty(), b -> sendButton(id), DEFAULT_NARRATION);
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(g, mouseX, mouseY, partialTick);
			g.blit(TEXTURE, this.getX() + 1, this.getY() + 1, 176, 0, 12, 12);
		}
	}
}
