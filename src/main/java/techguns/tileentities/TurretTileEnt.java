package techguns.tileentities;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import techguns.TGBlockEntities;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.api.tginventory.TGSlotType;
import techguns.entities.npcs.NPCTurret;
import techguns.factions.TGTeams;
import techguns.gui.containers.TurretMenu;
import techguns.recipes.ChargingStationRecipe;
import techguns.items.guns.GenericGun;
import techguns.tileentities.operation.ItemStackHandlerPlus;

/**
 * Turret base (1.12 TurretTileEnt): keeps an NPCTurret entity on top (or below) of the block alive, feeds it ammo from
 * the input slots, heals it with power and rebuilds it after it died.
 */
public class TurretTileEnt extends BasicPoweredTileEnt {

	public static final int SLOT_INPUT1 = 0;
	public static final int INPUTS_SIZE = 9;
	public static final int SLOT_OUTPUT1 = SLOT_INPUT1 + INPUTS_SIZE;
	public static final int OUTPUTS_SIZE = 9;
	public static final int SLOT_WEAPON = SLOT_OUTPUT1 + OUTPUTS_SIZE;
	public static final int SLOT_ARMOR = SLOT_WEAPON + 1;

	public static final int BUTTON_ID_TARGET_ANIMALS = BUTTON_ID_REDSTONE + 1;
	public static final int BUTTON_ID_PVP_SETTING = BUTTON_ID_REDSTONE + 2;

	/** 1.12 had 5000, raised so a turret can pay an energy cell (50000 FE) with FE */
	public static final int ENERGY_CAPACITY = 100000;
	/** FE for one energy cell when no charging station recipe for it exists */
	public static final int DEFAULT_CELL_COST = 50000;

	/** ticks a turret entity may stay missing while its chunk has entities loaded before a new one is built */
	protected static final int MISSING_TURRET_TIMEOUT = 40;

	@Nullable
	protected UUID turretId;
	@Nullable
	protected NPCTurret turret;
	protected int missingTicks = 0;

	public boolean turretDeath = false;
	public int repairTime = 0;
	public int turretHealTime = 0;
	public boolean attackAnimals = false;

	/**
	 * 0: no PvP, 1: attack enemies, 2: attack neutrals, 3: attack team members (all but owner), 4: attack allies
	 */
	protected byte pvpsetting = 0;

	public TurretTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.TURRET.get(), pos, state, SLOT_ARMOR + 1, false, ENERGY_CAPACITY);
	}

	@Override
	protected ItemStackHandlerPlus createInventory(int size) {
		return new ItemStackHandlerPlus(size) {
			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged(slot);
				if (slot == SLOT_WEAPON || slot == SLOT_ARMOR) {
					TurretTileEnt.this.updateWeapon();
				}
			}

			@Override
			protected boolean allowItemInSlot(int slot, ItemStack stack) {
				if (slot == SLOT_WEAPON) {
					return stack.getItem() instanceof GenericGun;
				} else if (slot == SLOT_ARMOR) {
					return isTurretArmor(stack);
				}
				return slot >= SLOT_INPUT1 && slot < SLOT_INPUT1 + INPUTS_SIZE;
			}

			@Override
			protected boolean allowExtractFromSlot(int slot, int amount) {
				return slot >= SLOT_OUTPUT1 && slot < SLOT_OUTPUT1 + OUTPUTS_SIZE;
			}
		};
	}

	public static boolean isTurretArmor(ItemStack stack) {
		return stack.getItem() instanceof ITGSpecialSlot s && s.getSlot(stack) == TGSlotType.TURRETARMOR;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new TurretMenu(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container.turretbase");
	}

	public int getValidSlotForItemInMachine(ItemStack stack) {
		if (stack.getItem() instanceof GenericGun) {
			return SLOT_WEAPON;
		} else if (isTurretArmor(stack)) {
			return SLOT_ARMOR;
		}
		for (int i = SLOT_INPUT1; i < SLOT_INPUT1 + INPUTS_SIZE; i++) {
			ItemStack s = inventory.getStackInSlot(i);
			if (s.isEmpty() || (ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize())) {
				return i;
			}
		}
		return -1;
	}

	public Direction getFacing() {
		BlockState state = getBlockState();
		return state.hasProperty(BlockStateProperties.VERTICAL_DIRECTION) ? state.getValue(BlockStateProperties.VERTICAL_DIRECTION) : Direction.UP;
	}

	public ItemStack getWeapon() {
		return this.inventory.getStackInSlot(SLOT_WEAPON);
	}

	@Nullable
	public NPCTurret getTurret() {
		if (turret != null && turret.isRemoved()) {
			turret = null;
		}
		if (turret == null && turretId != null && level instanceof ServerLevel sl && sl.getEntity(turretId) instanceof NPCTurret t) {
			turret = t;
		}
		return turret;
	}

	public boolean isMountedTurret(NPCTurret t) {
		return t.getUUID().equals(turretId);
	}

	/** called by a turret entity that lost its base reference (after loading) */
	public void setMountedTurret(NPCTurret t) {
		this.turret = t;
		this.turretId = t.getUUID();
		this.missingTicks = 0;
		this.setChanged();
	}

	@Override
	public void readClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.readClientData(tags, registries);
		this.turretId = tags.hasUUID("turret") ? tags.getUUID("turret") : null;
		this.turretDeath = tags.getBoolean("turretDeath");
		this.repairTime = tags.getInt("repairTime");
		this.turretHealTime = tags.getInt("turretHealTime");
		this.attackAnimals = tags.getBoolean("attackAnimals");
		this.pvpsetting = tags.getByte("pvpsetting");
	}

	@Override
	public void writeClientData(CompoundTag tags, HolderLookup.Provider registries) {
		super.writeClientData(tags, registries);
		if (turretId != null) {
			tags.putUUID("turret", turretId);
		}
		tags.putBoolean("turretDeath", this.turretDeath);
		tags.putInt("repairTime", this.repairTime);
		tags.putInt("turretHealTime", this.turretHealTime);
		tags.putBoolean("attackAnimals", this.attackAnimals);
		tags.putByte("pvpsetting", this.pvpsetting);
	}

	/** take one shot from the gun, reloads from the input slots when empty */
	public boolean consumeAmmo() {
		ItemStack gunstack = this.getWeapon();
		if (!gunstack.isEmpty() && gunstack.getItem() instanceof GenericGun gun) {
			if (gun.getCurrentAmmo(gunstack) <= 0) {
				this.doReload(gunstack, gun);
			}
			if (gun.getCurrentAmmo(gunstack) >= 1) {
				gun.useAmmo(gunstack, 1);
				return true;
			}
		}
		return false;
	}

	/**
	 * Reload like a player does (GenericGun): every load consumes the full ammo set (e.g. gauss slugs + energy cell) and
	 * gives the empty mags, guns with ammoCount > 1 take several loads. The input and output slots are only locked for
	 * automation, so this uses the unchecked handler access.
	 */
	protected void doReload(ItemStack gunstack, GenericGun gun) {
		ItemStack[] ammo = gun.getAmmoType().getAmmo(gun.getCurrentAmmoVariant(gunstack));
		int loads = 0;
		int maxLoads = Math.max(1, gun.getAmmoCount());
		ItemStack[] emptyMags = gun.getAmmoType().getEmptyMag();
		boolean[] paidWithPower;
		while (loads < maxLoads && (paidWithPower = consumeAmmoSet(ammo)) != null) {
			loads++;
			for (int i = 0; i < emptyMags.length; i++) {
				// a cell paid with FE leaves no empty cell
				if (!emptyMags[i].isEmpty() && !(i < paidWithPower.length && paidWithPower[i])) {
					outputOrDrop(emptyMags[i].copyWithCount(1));
				}
			}
		}
		if (loads == 0) {
			return;
		}
		if (gun.getAmmoCount() > 1) {
			gun.reloadAmmo(gunstack, loads);
		} else {
			gun.reloadAmmo(gunstack);
		}
		this.setChanged();
	}

	/**
	 * All-or-nothing consume of one ammo set. Energy cells are paid with FE from the buffer when there is enough, else
	 * taken from the input slots.
	 *
	 * @return per ammo index whether it was paid with FE, null if the set could not be consumed
	 */
	@Nullable
	protected boolean[] consumeAmmoSet(ItemStack[] ammo) {
		boolean[] power = new boolean[ammo.length];
		int powerNeeded = 0;
		int cellCost = -1;
		for (int k = 0; k < ammo.length; k++) {
			ItemStack a = ammo[k];
			if (a.isEmpty()) {
				continue;
			}
			if (a.is(TGItems.ENERGY_CELL.get())) {
				if (cellCost < 0) {
					cellCost = getEnergyCellCost();
				}
				int cost = cellCost * a.getCount();
				if (TGConfig.machinesNeedNoPower || energy.getEnergyStored() - powerNeeded >= cost) {
					power[k] = true;
					powerNeeded += cost;
					continue;
				}
			}
			if (countInInputs(a) < a.getCount()) {
				return null;
			}
		}
		if (powerNeeded > 0 && !consumePower(powerNeeded)) {
			return null;
		}
		for (int k = 0; k < ammo.length; k++) {
			ItemStack a = ammo[k];
			if (a.isEmpty() || power[k]) {
				continue;
			}
			int toRemove = a.getCount();
			for (int i = SLOT_INPUT1; i < SLOT_INPUT1 + INPUTS_SIZE && toRemove > 0; i++) {
				if (ItemStack.isSameItemSameComponents(inventory.getStackInSlot(i), a)) {
					toRemove -= inventory.extractWithoutCheck(i, toRemove, false).getCount();
				}
			}
		}
		return power;
	}

	/** FE for one energy cell: the charging station recipe that charges an empty cell */
	protected int getEnergyCellCost() {
		if (level != null) {
			var r = ChargingStationRecipe.find(level, new ItemStack(TGItems.ENERGY_CELL_EMPTY.get()));
			if (r.isPresent() && r.get().value().result().is(TGItems.ENERGY_CELL.get())) {
				var rec = r.get().value();
				return Math.max(1, rec.chargeAmount() / Math.max(1, rec.result().getCount()));
			}
		}
		return DEFAULT_CELL_COST;
	}

	protected int countInInputs(ItemStack a) {
		int n = 0;
		for (int i = SLOT_INPUT1; i < SLOT_INPUT1 + INPUTS_SIZE; i++) {
			ItemStack s = inventory.getStackInSlot(i);
			if (ItemStack.isSameItemSameComponents(s, a)) {
				n += s.getCount();
			}
		}
		return n;
	}

	protected void outputOrDrop(ItemStack stack) {
		ItemStack rest = stack;
		for (int i = SLOT_OUTPUT1; i < SLOT_OUTPUT1 + OUTPUTS_SIZE && !rest.isEmpty(); i++) {
			rest = inventory.insertItemNoCheck(i, rest, false);
		}
		if (!rest.isEmpty() && level != null && !level.isClientSide) {
			level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, rest));
		}
	}

	protected int getRepairTimeMax() {
		return 10 * 20;
	}

	public int getRepairTimeScaled(int max) {
		return max - (this.repairTime * max / this.getRepairTimeMax());
	}

	protected int getRepairPowerAmount() {
		return 50;
	}

	protected int getOperationPowerAmount() {
		return 5;
	}

	protected int getPowerAmountHealTick() {
		return 500;
	}

	protected int getHealTickDelay() {
		return 20 * 3;
	}

	public int getTurretTier() {
		ItemStack armor = this.inventory.getStackInSlot(SLOT_ARMOR);
		if (armor.isEmpty()) {
			return 0;
		} else if (armor.is(TGItems.TURRET_ARMOR_IRON.get())) {
			return 1;
		} else if (armor.is(TGItems.TURRET_ARMOR_STEEL.get())) {
			return 2;
		} else if (armor.is(TGItems.TURRET_ARMOR_OBSIDIAN_STEEL.get())) {
			return 3;
		} else if (armor.is(TGItems.TURRET_ARMOR_CARBON.get())) {
			return 4;
		}
		return 0;
	}

	public int getTurretArmorValue() {
		return getTurretTier() * 5;
	}

	public byte getPvpSetting() {
		return pvpsetting;
	}

	@Override
	public void buttonClicked(int id, Player ply) {
		if (id < BUTTON_ID_TARGET_ANIMALS) {
			super.buttonClicked(id, ply);
		} else if (id == BUTTON_ID_TARGET_ANIMALS && this.isUseableByPlayer(ply)) {
			this.attackAnimals = !this.attackAnimals;
			this.needUpdate();
		} else if (id == BUTTON_ID_PVP_SETTING && this.isOwnedByPlayer(ply)) {
			if (TGTeams.isTeamModLoaded()) {
				this.pvpsetting = (byte) ((this.pvpsetting + 1) % 5);
			} else {
				this.pvpsetting = (byte) (this.pvpsetting == 0 ? 4 : 0);
			}
			this.needUpdate();
		}
	}

	/** weapon or armor changed: new attack ai, new texture */
	public void updateWeapon() {
		if (level != null && !level.isClientSide) {
			NPCTurret t = getTurret();
			if (t != null) {
				t.updateFromBase(this);
			}
		}
	}

	/** called by the turret entity when it dies */
	public void onTurretDeath() {
		if (level != null && !level.isClientSide) {
			this.turret = null;
			this.turretId = null;
			this.turretDeath = true;
			this.repairTime = this.getRepairTimeMax();
			this.needUpdate();
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TurretTileEnt te) {
		if (level.isClientSide) {
			return;
		}
		if (te.turretDeath) {
			if (te.isRedstoneEnabled()) {
				if (te.consumePower(te.getRepairPowerAmount())) {
					te.repairTime--;
				}
				if (te.repairTime <= 0) {
					te.repairTime = 0;
					te.spawnTurret(10.0f);
				}
			}
			return;
		}

		NPCTurret t = te.getTurret();
		if (t == null) {
			// placed without turret or the entity got lost: wait until the entities around are loaded, then rebuild
			BlockPos entPos = pos.relative(te.getFacing());
			if (te.turretId == null) {
				te.spawnTurret(-1f);
			} else if (level instanceof ServerLevel sl && sl.areEntitiesLoaded(ChunkPos.asLong(entPos))) {
				if (++te.missingTicks > MISSING_TURRET_TIMEOUT) {
					te.spawnTurret(-1f);
				}
			}
			return;
		}
		te.missingTicks = 0;

		if (te.isRedstoneEnabled() && te.consumePower(te.getOperationPowerAmount())) {
			t.setActive(true);
			if (te.turretHealTime <= 0) {
				if (t.getHealth() < t.getMaxHealth() && te.consumePower(te.getPowerAmountHealTick())) {
					t.heal(1.0f);
					te.turretHealTime = te.getHealTickDelay();
				}
			} else {
				te.turretHealTime--;
			}
		} else {
			t.setActive(false);
		}
	}

	/** health <= 0 means full health */
	public void spawnTurret(float health) {
		if (level == null || level.isClientSide) {
			return;
		}
		NPCTurret old = getTurret();
		if (old != null) {
			old.discard();
		}
		NPCTurret t = new NPCTurret(level, this);
		if (health > 0) {
			t.setHealth(health);
		}
		this.turret = t;
		this.turretId = t.getUUID();
		this.turretDeath = false;
		this.missingTicks = 0;
		level.addFreshEntity(t);
		this.needUpdate();
	}

	@Override
	public void onBlockBreak() {
		super.onBlockBreak();
		NPCTurret t = getTurret();
		if (t != null) {
			t.discard();
		}
	}
}
