package techguns.gui.containers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.entities.npcs.NPCTurret;
import techguns.gui.TGMenus;
import techguns.gui.widgets.TGSlots;
import techguns.tileentities.TurretTileEnt;

/** Turret base menu (1.12 TurretContainer): 9 ammo inputs, 9 empty mag outputs, gun and turret armor slot. */
public class TurretMenu extends TGBaseMenu<TurretTileEnt> {

	public static final net.minecraft.resources.ResourceLocation AMMO_ICON = Techguns.rl("gui/emptyslots/emptyslot_bullet");
	public static final net.minecraft.resources.ResourceLocation AMMO_EMPTY_ICON = Techguns.rl("gui/emptyslots/emptyslot_bullet_empty");
	public static final net.minecraft.resources.ResourceLocation GUN_ICON = Techguns.rl("gui/emptyslots/emptyslot_turretgun");
	public static final net.minecraft.resources.ResourceLocation ARMOR_ICON = Techguns.rl("gui/emptyslots/emptyslot_turretarmor");

	/** client copies of the turret health, x10 */
	protected int health = 0;
	protected int maxHealth = 0;

	public TurretMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		this(id, inv, (TurretTileEnt) inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public TurretMenu(int id, Inventory inv, TurretTileEnt tile) {
		super(TGMenus.TURRET.get(), id, inv, tile);
		var h = tile.getInventory();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				this.addSlot(new TGSlots.MachineInput(h, TurretTileEnt.SLOT_INPUT1 + i * 3 + j, 18 + 18 * i, 17 + 18 * j).withBackground(AMMO_ICON));
			}
		}
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				this.addSlot(new TGSlots.Output(h, TurretTileEnt.SLOT_OUTPUT1 + i * 3 + j, 116 + 18 * i, 17 + 18 * j).withBackground(AMMO_EMPTY_ICON));
			}
		}
		this.addSlot(new TGSlots.MachineInput(h, TurretTileEnt.SLOT_WEAPON, 85, 19).withBackground(GUN_ICON));
		this.addSlot(new TGSlots.MachineInput(h, TurretTileEnt.SLOT_ARMOR, 85, 42).withBackground(ARMOR_ICON));
		this.addPlayerInventorySlots(inv);

		addData(() -> tile.turretDeath ? 1 : 0, v -> tile.turretDeath = v != 0);
		addData(() -> tile.repairTime, v -> tile.repairTime = v);
		addData(() -> {
			NPCTurret t = tile.getTurret();
			return t == null ? 0 : (int) Math.ceil(t.getHealth() * 10);
		}, v -> health = v);
		addData(() -> {
			NPCTurret t = tile.getTurret();
			return t == null ? 0 : (int) Math.ceil(t.getMaxHealth() * 10);
		}, v -> maxHealth = v);
	}

	public float getTurretHealth() {
		return health / 10f;
	}

	public float getTurretMaxHealth() {
		return maxHealth / 10f;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return quickMove(player, index, tile::getValidSlotForItemInMachine);
	}
}
