package techguns.tileentities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.event.EventHooks;
import techguns.TGBlockEntities;

/**
 * 1.12 TGSpawnerTileEnt: dungeon spawner that keeps up to maxActive mobs alive until mobsLeft of them were killed, then
 * removes itself. Spawned mobs remember the spawner position (see {@link #SPAWNER_TAG}), so kills are counted even
 * after chunk reloads.
 */
public class TGSpawnerTileEnt extends BlockEntity {

	/** persistent data key on spawned mobs: the spawner position as long */
	public static final String SPAWNER_TAG = "techguns_spawner";

	public record MobType(EntityType<?> type, int weight) {
	}

	protected final RandomSource rand = RandomSource.create();
	protected int delay = 200;
	protected int spawndelay = 200;
	protected int mobsLeft = 5;
	protected int maxActive = 3;
	protected int spawnHeightOffset = 0;
	protected double spawnrange = 2d;
	protected final List<MobType> mobtypes = new ArrayList<>();
	protected final LinkedList<Mob> activeMobs = new LinkedList<>();
	protected ItemStack weaponOverride = ItemStack.EMPTY;

	public TGSpawnerTileEnt(BlockPos pos, BlockState state) {
		super(TGBlockEntities.TG_SPAWNER.get(), pos, state);
	}

	public void addMobType(EntityType<?> type, int weight) {
		this.mobtypes.add(new MobType(type, weight));
		this.setChanged();
	}

	public void setParams(int mobsleft, int maxactive, int spawndelay, int spawnrange) {
		this.mobsLeft = mobsleft;
		this.maxActive = maxactive;
		this.spawndelay = spawndelay;
		this.delay = spawndelay;
		this.spawnrange = spawnrange;
		this.setChanged();
	}

	public TGSpawnerTileEnt setWeaponOverride(ItemStack weapon) {
		this.weaponOverride = weapon;
		this.setChanged();
		return this;
	}

	public void setSpawnHeightOffset(int offset) {
		this.spawnHeightOffset = offset;
		this.setChanged();
	}

	/* ---- linked mobs ---- */

	public void killedEntity(Mob ent) {
		if (this.activeMobs.remove(ent)) {
			this.mobsLeft--;
			this.setChanged();
		}
	}

	public void relinkNPC(Mob ent) {
		if (!this.activeMobs.contains(ent)) {
			this.activeMobs.add(ent);
		}
	}

	/** spawner of a mob, if it came from one and the spawner still exists */
	@Nullable
	public static TGSpawnerTileEnt getSpawner(Entity ent) {
		CompoundTag data = ent.getPersistentData();
		if (!data.contains(SPAWNER_TAG, Tag.TAG_LONG) || ent.level().isClientSide) {
			return null;
		}
		BlockPos pos = BlockPos.of(data.getLong(SPAWNER_TAG));
		if (!ent.level().isLoaded(pos)) {
			return null;
		}
		return ent.level().getBlockEntity(pos) instanceof TGSpawnerTileEnt spawner ? spawner : null;
	}

	/* ---- ticking ---- */

	public static void serverTick(Level level, BlockPos pos, BlockState state, TGSpawnerTileEnt tile) {
		tile.tick((ServerLevel) level);
	}

	protected void tick(ServerLevel level) {
		this.delay--;
		if (this.delay <= 0 && activeMobs.size() < Math.min(maxActive, mobsLeft) && !mobtypes.isEmpty()) {
			this.delay = this.spawndelay;
			if (level.getDifficulty() != Difficulty.PEACEFUL) {
				spawnMob(level);
			}
		} else if (this.delay <= 0) {
			this.delay = this.spawndelay;
			Iterator<Mob> it = this.activeMobs.iterator();
			while (it.hasNext()) {
				Mob m = it.next();
				if (!m.isAlive() || m.isRemoved() || m.level() != level) {
					it.remove();
				}
			}
		}
		if (this.mobsLeft <= 0) {
			level.removeBlock(this.worldPosition, false);
		}
	}

	protected void spawnMob(ServerLevel level) {
		EntityType<?> type = pickType();
		if (type == null) {
			return;
		}
		BlockPos bp = this.worldPosition;
		double x = bp.getX() + (rand.nextDouble() - rand.nextDouble()) * this.spawnrange + 0.5D;
		double y = bp.getY() + 1 + this.spawnHeightOffset;
		double z = bp.getZ() + (rand.nextDouble() - rand.nextDouble()) * this.spawnrange + 0.5D;
		Entity e = type.create(level);
		if (!(e instanceof Mob mob)) {
			if (e != null) {
				e.discard();
			}
			return;
		}
		mob.moveTo(x, y, z, rand.nextFloat() * 360f, 0f);
		mob.getPersistentData().putLong(SPAWNER_TAG, bp.asLong());
		EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.SPAWNER, null);
		if (mob instanceof PathfinderMob pm) {
			pm.restrictTo(bp, 10);
		}
		if (!weaponOverride.isEmpty()) {
			mob.setItemSlot(EquipmentSlot.MAINHAND, this.weaponOverride.copy());
		}
		if (!level.tryAddFreshEntityWithPassengers(mob)) {
			return;
		}
		level.levelEvent(2004, bp, 0);
		mob.spawnAnim();
		this.activeMobs.add(mob);
	}

	@Nullable
	private EntityType<?> pickType() {
		int total = 0;
		for (MobType t : mobtypes) {
			total += Math.max(0, t.weight());
		}
		if (total <= 0) {
			return null;
		}
		int r = rand.nextInt(total);
		for (MobType t : mobtypes) {
			r -= Math.max(0, t.weight());
			if (r < 0) {
				return t.type();
			}
		}
		return null;
	}

	/* ---- nbt ---- */

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putByte("mobsLeft", (byte) this.mobsLeft);
		tag.putShort("delay", (short) this.delay);
		tag.putByte("maxActive", (byte) this.maxActive);
		tag.putShort("spawnDelay", (short) this.spawndelay);
		tag.putByte("spawnRange", (byte) this.spawnrange);
		tag.putShort("spawnHeightOffset", (short) this.spawnHeightOffset);
		ListTag list = new ListTag();
		for (MobType t : mobtypes) {
			CompoundTag c = new CompoundTag();
			c.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(t.type()).toString());
			c.putInt("weight", t.weight());
			list.add(c);
		}
		tag.put("mobtypes", list);
		if (!weaponOverride.isEmpty()) {
			tag.put("weapon", this.weaponOverride.save(registries));
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.mobsLeft = tag.getByte("mobsLeft");
		this.delay = tag.getShort("delay");
		this.spawndelay = tag.getShort("spawnDelay");
		if (spawndelay < 1) {
			spawndelay = 200;
		}
		this.maxActive = Math.max(1, tag.getByte("maxActive"));
		this.spawnrange = tag.getByte("spawnRange");
		this.spawnHeightOffset = tag.getShort("spawnHeightOffset");
		if (tag.contains("mobtypes", Tag.TAG_LIST)) {
			this.mobtypes.clear();
			ListTag list = tag.getList("mobtypes", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag c = list.getCompound(i);
				ResourceLocation id = ResourceLocation.tryParse(c.getString("id"));
				Optional<EntityType<?>> type = id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
				type.ifPresent(t -> this.mobtypes.add(new MobType(t, c.contains("weight") ? c.getInt("weight") : 1)));
			}
		}
		this.weaponOverride = tag.contains("weapon", Tag.TAG_COMPOUND) ? ItemStack.parseOptional(registries, tag.getCompound("weapon")) : ItemStack.EMPTY;
	}
}
