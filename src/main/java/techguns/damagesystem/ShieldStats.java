package techguns.damagesystem;

import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ItemAbilities;
import techguns.TGSounds;
import techguns.api.damagesystem.DamageType;

/**
 * Defensive stats of a shield against Techguns damage.
 */
public class ShieldStats {

	public static final Map<Item, ShieldStats> SHIELD_STATS = new IdentityHashMap<>();

	/** damage threshold: subtracted from the reduced damage (minus 2x armor penetration) */
	protected double dt = 0f;
	/** reduction rate per damage type, 0.0 = take full damage, 1.0 = take no damage */
	protected double[] rates;
	/** used for sound effects */
	protected ShieldMaterial mat;

	public static final ShieldStats VANILLA_SHIELD = new ShieldStats(ShieldMaterial.WOOD, 0d, new double[] {
		1.0d,	//PHYSICAL
		0.35d,	//PROJECTILE
		0.35d,	//FIRE
		0.35d,	//EXPLOSION
		0.35d,	//ENERGY
		0.35d,	//POISON
		0.0d,	//UNRESISTABLE
		0.35d,	//ICE
		0.35d,	//LIGHTNING
		0.0d,	//RADIATION
		0.0d	//DARK
	});
	
	/**
	 * used for modded shields by default
	 */
	public static final ShieldStats DEFAULT_STATS = new ShieldStats(ShieldMaterial.METAL, 0d, new double[] {
			1.0d,	//PHYSICAL
			0.5d,	//PROJECTILE
			0.5d,	//FIRE
			0.5d,	//EXPLOSION
			0.5d,	//ENERGY
			0.5d,	//POISON
			0.0d,	//UNRESISTABLE
			0.5d,	//ICE
			0.5d,	//LIGHTNING
			0.25d,	//RADIATION
			0.25d	//DARK
		});
	
	public static final ShieldStats RIOT_SHIELD_STATS = new ShieldStats(ShieldMaterial.GLASS, 0d, new double[] {
			1.0d,	//PHYSICAL
			0.65d,	//PROJECTILE
			0.60d,	//FIRE
			0.60d,	//EXPLOSION
			0.60d,	//ENERGY
			0.60d,	//POISON
			0.0d,	//UNRESISTABLE
			0.60d,	//ICE
			0.60d,	//LIGHTNING
			0.40d,	//RADIATION
			0.40d	//DARK
		});
	
	public static final ShieldStats BALLISTIC_SHIELD_STATS = new ShieldStats(ShieldMaterial.METAL, 1.5d, new double[] {
			1.0d,	//PHYSICAL
			0.8d,	//PROJECTILE
			0.70d,	//FIRE
			0.70d,	//EXPLOSION
			0.70d,	//ENERGY
			0.70d,	//POISON
			0.0d,	//UNRESISTABLE
			0.70d,	//ICE
			0.70d,	//LIGHTNING
			0.60d,	//RADIATION
			0.60d	//DARK
		});
	
	public static final ShieldStats ADVANCED_SHIELD_STATS = new ShieldStats(ShieldMaterial.METAL, 3d, new double[] {
			1.0d,	//PHYSICAL
			0.9d,	//PROJECTILE
			0.80d,	//FIRE
			0.80d,	//EXPLOSION
			0.80d,	//ENERGY
			0.80d,	//POISON
			0.0d,	//UNRESISTABLE
			0.80d,	//ICE
			0.80d,	//LIGHTNING
			0.75d,	//RADIATION
			0.75d	//DARK
		});
	
	public ShieldStats(double dt, double[] rates) {
		this(ShieldMaterial.METAL, dt, rates);
	}

	public ShieldStats(ShieldMaterial mat, double dt, double[] rates) {
		this.mat = mat;
		this.dt = dt;
		this.rates = rates;
	}

	public ShieldStats(ShieldMaterial mat, double dt, double allrates) {
		this.mat = mat;
		this.dt = dt;
		this.rates = new double[DamageType.values().length];
		java.util.Arrays.fill(this.rates, allrates);
	}

	/** for the tooltip */
	public double getReductionPercentAgainstType(DamageType dt) {
		return this.rates[dt.ordinal()] * 100d;
	}

	/** damage that gets through the shield */
	public float getAmount(float amount, TGDamageSource src) {
		double d = amount * (1d - rates[src.damageType.ordinal()]);
		double effective_dt = Math.max(dt - (src.armorPenetration * 2d), 0d);
		return (float) Math.max(d - effective_dt, 0d);
	}

	public enum ShieldMaterial {
		WOOD,
		STONE,
		METAL,
		GLASS,
		GROUND,
		NONE
	}

	public static void playBlockSound(LivingEntity ent) {
		if (!ent.level().isClientSide) {
			ShieldStats s = getStats(ent.getUseItem(), ent);
			if (s != null) {
				SoundEvent snd = getSound(s.mat);
				if (snd != null) {
					ent.level().playSound(null, ent.getX(), ent.getY(), ent.getZ(), snd, SoundSource.PLAYERS, 1.0f, 1.0f);
				}
			}
		}
	}

	@Nullable
	protected static SoundEvent getSound(ShieldMaterial mat) {
		return switch (mat) {
			case GLASS -> TGSounds.BULLET_IMPACT_GLASS.get();
			case GROUND -> TGSounds.BULLET_IMPACT_DIRT.get();
			case METAL -> TGSounds.BULLET_IMPACT_METAL.get();
			case STONE -> TGSounds.BULLET_IMPACT_STONE.get();
			case WOOD -> TGSounds.BULLET_IMPACT_WOOD.get();
			default -> null;
		};
	}

	@Nullable
	public static ShieldStats getStats(ItemStack stack, @Nullable LivingEntity entity) {
		if (stack.isEmpty() || !stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
			return null;
		}
		if (stack.is(Items.SHIELD)) {
			return VANILLA_SHIELD;
		}
		return SHIELD_STATS.getOrDefault(stack.getItem(), DEFAULT_STATS);
	}
}
