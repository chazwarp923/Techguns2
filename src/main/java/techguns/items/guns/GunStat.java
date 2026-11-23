package techguns.items.guns;

import java.util.function.ObjDoubleConsumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * A gun stat a datapack can set through the {@code techguns:gun_stats} data map ({@link GunStats}). Each one reads and
 * writes a field of the gun item; distances are in blocks, times in ticks.
 */
public enum GunStat implements StringRepresentable {
	/** damage at close range */
	DAMAGE("damage", false, 0, g -> true, g -> g.damage, (g, v) -> g.damage = (float) v),
	/** damage beyond damage_drop_end */
	DAMAGE_MIN("damage_min", false, 0, g -> true, g -> g.damageMin, (g, v) -> g.damageMin = (float) v),
	/** distance where damage starts to drop towards damage_min */
	DAMAGE_DROP_START("damage_drop_start", false, 0, g -> true, g -> g.damageDropStart, (g, v) -> g.damageDropStart = (float) v),
	/** distance from where damage_min applies */
	DAMAGE_DROP_END("damage_drop_end", false, 0, g -> true, g -> g.damageDropEnd, (g, v) -> g.damageDropEnd = (float) v),
	/** how far the projectile flies before it disappears */
	RANGE("range", true, 1, g -> true, g -> g.ticksToLive, (g, v) -> g.ticksToLive = (int) v),
	/** projectile speed, blocks per tick */
	BULLET_SPEED("bullet_speed", false, 0.01, g -> true, g -> g.speed, (g, v) -> g.speed = (float) v),
	/** projectile gravity, blocks per tick squared */
	GRAVITY("gravity", false, 0, g -> true, g -> g.gravity, (g, v) -> g.gravity = v),
	/** random spread of a single shot (0 is perfectly accurate) */
	INACCURACY("inaccuracy", false, 0, g -> true, g -> g.accuracy, (g, v) -> g.accuracy = (float) v),
	/** random spread of the extra pellets of a shotgun */
	PELLET_SPREAD("pellet_spread", false, 0, g -> g.shotgun, g -> g.spread, (g, v) -> g.spread = (float) v),
	/** projectiles per shot of a shotgun, the first one included */
	PELLETS("pellets", true, 1, g -> g.shotgun, g -> g.bulletcount + 1, (g, v) -> g.bulletcount = (int) v - 1),
	/** armor penetration */
	PENETRATION("penetration", false, 0, g -> true, g -> g.penetration, (g, v) -> g.penetration = (float) v),
	/** ticks between shots */
	FIRE_DELAY("fire_delay", true, 0, g -> true, g -> g.minFiretime, (g, v) -> g.minFiretime = (int) v),
	/** ticks a reload takes */
	RELOAD_TIME("reload_time", true, 0, g -> true, g -> g.reloadtime, (g, v) -> g.reloadtime = (int) v),
	/** ticks to fully charge a charge gun */
	CHARGE_TIME("charge_time", false, 1, g -> g instanceof GenericGunCharge, g -> ((GenericGunCharge) g).fullChargeTime,
			(g, v) -> ((GenericGunCharge) g).fullChargeTime = (float) v),
	/** ammo a fully charged shot uses */
	AMMO_PER_CHARGE("ammo_per_charge", true, 1, g -> g instanceof GenericGunCharge, g -> ((GenericGunCharge) g).ammoConsumedOnFullCharge,
			(g, v) -> ((GenericGunCharge) g).ammoConsumedOnFullCharge = (int) v),
	/** melee damage of a powered melee tool (chainsaw, power hammer...) */
	MELEE_DAMAGE("melee_damage", false, 0, g -> g instanceof GenericGunMeleeCharge, g -> g.meleeDamagePwr, (g, v) -> g.meleeDamagePwr = (float) v),
	/** melee damage of a melee tool without power */
	MELEE_DAMAGE_EMPTY("melee_damage_empty", false, 0, g -> g instanceof GenericGunMeleeCharge, g -> g.meleeDamageEmpty,
			(g, v) -> g.meleeDamageEmpty = (float) v),
	/** dig speed of a powered melee tool */
	MINING_SPEED("mining_speed", false, 0, g -> g instanceof GenericGunMeleeCharge, g -> g.digSpeed, (g, v) -> g.digSpeed = (float) v),
	/** how far NPCs and turrets with this gun shoot from */
	NPC_RANGE("npc_range", false, 1, g -> true, g -> g.AI_attackRange, (g, v) -> g.AI_attackRange = (float) v),
	/** ticks between NPC attacks */
	NPC_ATTACK_TIME("npc_attack_time", true, 1, g -> true, g -> g.AI_attackTime, (g, v) -> g.AI_attackTime = (int) v),
	/** shots per NPC burst (0: no bursts) */
	NPC_BURST_COUNT("npc_burst_count", true, 0, g -> true, g -> g.AI_burstCount, (g, v) -> g.AI_burstCount = (int) v),
	/** ticks between the shots of an NPC burst */
	NPC_BURST_DELAY("npc_burst_delay", true, 0, g -> true, g -> g.AI_burstAttackTime, (g, v) -> g.AI_burstAttackTime = (int) v);

	public static final Codec<GunStat> CODEC = StringRepresentable.fromEnum(GunStat::values);

	private final String key;
	private final boolean integer;
	private final double min;
	private final Predicate<GenericGun> appliesTo;
	private final ToDoubleFunction<GenericGun> getter;
	private final ObjDoubleConsumer<GenericGun> setter;

	GunStat(String key, boolean integer, double min, Predicate<GenericGun> appliesTo, ToDoubleFunction<GenericGun> getter,
			ObjDoubleConsumer<GenericGun> setter) {
		this.key = key;
		this.integer = integer;
		this.min = min;
		this.appliesTo = appliesTo;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public String getSerializedName() {
		return key;
	}

	/** whole numbers only (ticks, counts); a value with a fraction is rounded */
	public boolean isInteger() {
		return integer;
	}

	public double min() {
		return min;
	}

	/** whether this gun has the stat (shotgun, charge and melee stats only exist on those guns) */
	public boolean appliesTo(GenericGun gun) {
		return appliesTo.test(gun);
	}

	public double get(GenericGun gun) {
		return getter.applyAsDouble(gun);
	}

	/** sets the stat, rounded for whole number stats and raised to the minimum; returns the value set */
	public double set(GenericGun gun, double value) {
		double v = Math.max(min, integer ? Math.round(value) : value);
		setter.accept(gun, v);
		return v;
	}
}
