package techguns.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.curios.TGCurios;
import techguns.gui.containers.CamoBenchMenu;
import techguns.items.armors.ICamoChangeable;
import techguns.tileentities.CamoBenchTileEnt;
import techguns.tileentities.RepairBenchTileEnt;

/**
 * Camo bench gui (1.12 CamoBenchGui): camo of the item in the bench, +/- for the worn armor, the player model left of
 * the gui. The face / back / hand items live in Curios slots now, they are shown with their camo number.
 */
public class CamoBenchScreen extends TGMachineScreen<CamoBenchMenu> {

	public static final ResourceLocation TEXTURE = Techguns.rl("textures/gui/camo_bench_gui.png");

	private static final String[] SPECIAL = { TGCurios.SLOT_FACE, TGCurios.SLOT_BACK, TGCurios.SLOT_HANDS };
	private static final int[] SPECIAL_X = { 44, 62, 80 };

	private final CamoBenchTileEnt bench;
	private final Player player;

	public CamoBenchScreen(CamoBenchMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, TEXTURE);
		this.bench = menu.getTile();
		this.player = inv.player;
		this.heightSecurityButton = 95 + 25;
	}

	@Override
	protected void init() {
		super.init();
		addMenuButton(CamoBenchTileEnt.BUTTON_ID_NEXT, leftPos + 14 + 12, topPos + 59, 12, 12, Component.literal(">"));
		addMenuButton(CamoBenchTileEnt.BUTTON_ID_PREV, leftPos + 14, topPos + 59, 12, 12, Component.literal("<"));
		for (int i = 0; i < 4; i++) {
			int x = leftPos + 102 + 18 * i;
			addMenuButton(CamoBenchTileEnt.BUTTON_ID_ARMOR + 2 * i, x, topPos + 59, 10, 10, Component.literal("+"));
			addMenuButton(CamoBenchTileEnt.BUTTON_ID_ARMOR + 2 * i + 1, x, topPos + 69, 10, 10, Component.literal("-"));
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		int k = leftPos;
		int l = topPos;
		// frame behind the offhand slot, player model panel, offhand slot background
		g.blit(WIDGETS, k - 22, l + 90, 195, 0, 22, 20);
		g.blit(WIDGETS, k - 22, l + 110, 195, 25, 22, 5);
		g.blit(TEXTURE, k - 45, l + 6, 178, 0, 45, 84);
		g.blit(TEXTURE, k - 18, l + 93, 7, 83, 18, 18);
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, k - 42, l + 8, k - 2, l + 88, 30, 0.0625f, mouseX, mouseY, player);
		// worn face / back / hand items
		for (int i = 0; i < SPECIAL.length; i++) {
			ItemStack s = TGCurios.getEquipped(player, SPECIAL[i]);
			if (!s.isEmpty()) {
				g.renderItem(s, k + SPECIAL_X[i], l + 18);
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		int color = 4210752;
		Component name = Component.translatable("techguns.item.invalidcamo");
		String count = "";
		ItemStack item = bench.getItem();
		if (item.getItem() instanceof ICamoChangeable camo) {
			name = camo.getCurrentCamoName(item);
			count = (camo.getCurrentCamoIndex(item) + 1) + "/" + camo.getCamoCount() + ":";
		} else if (!item.isEmpty()) {
			var r = bench.getBlockRecipe();
			if (r.isPresent()) {
				int i = r.get().indexOf(item) + 1;
				count = (i > 9 ? "" : " ") + i + "/" + r.get().items().size();
				name = Component.empty();
			}
		}
		g.drawString(font, count, 10, 40, color, false);
		g.drawString(font, name, 10, 50, color, false);
		for (int i = 0; i < 4; i++) {
			g.drawString(font, "" + camoNumber(player.getItemBySlot(RepairBenchTileEnt.ARMOR_SLOTS[i])), 104 + i * 18, 38, color, false);
		}
		for (int i = 0; i < SPECIAL.length; i++) {
			g.drawString(font, "" + camoNumber(TGCurios.getEquipped(player, SPECIAL[i])), SPECIAL_X[i] + 5, 38, color, false);
		}
	}

	private static int camoNumber(ItemStack s) {
		if (!s.isEmpty() && s.getItem() instanceof ICamoChangeable camo) {
			return camo.getCurrentCamoIndex(s) + 1;
		}
		return 0;
	}
}
