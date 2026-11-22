package techguns.damagesystem;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import techguns.TGConfig;
import techguns.api.damagesystem.DamageType;
import techguns.api.damagesystem.ITGArmor;
import techguns.api.npc.INpcTGDamageSystem;
import techguns.util.MathUtil;

/**
 * Techguns damage handling. 1.12.2 replaced EntityLivingBase#attackEntityFrom with a reflective
 * copy; in 1.21 the same behaviour is achieved with NeoForge damage events:
 * <ul>
 * <li>{@link TGDamageSource#ignoreHurtresistTime}: damage type tag bypasses_cooldown plus no new invulnerability ticks</li>
 * <li>Techguns armor formula (penetration vs toughness): an ARMOR reduction modifier on the DamageContainer</li>
 * <li>{@link TGDamageSource#knockbackMultiplier}: LivingKnockBackEvent</li>
 * </ul>
 */
public class DamageSystem {

	/** Source currently being applied (hurt() is synchronous); used to scale knockback. */
	private static final ThreadLocal<TGDamageSource> CURRENT = new ThreadLocal<>();

	public static float getDamageFactor(LivingEntity attacker, LivingEntity target) {
		if (attacker instanceof Player && target instanceof Player) {
			MinecraftServer server = target.getServer();
			if (server != null && server.isPvpAllowed()) {
				return TGConfig.damagePvP;
			}
			return 0.0f;
		} else if (target instanceof Player) {
			if (isTurret(attacker)) {
				return TGConfig.damageTurretToPlayer;
			}
			return TGConfig.damageFactorNPC;
		} else if (attacker instanceof Player) {
			return 1.0f;
		}
		return TGConfig.damageFactorNPC;
	}

	/** Hook for NPCTurret once it is ported. */
	public static java.util.function.Predicate<LivingEntity> TURRET_CHECK = e -> e instanceof techguns.entities.npcs.NPCTurret;

	private static boolean isTurret(LivingEntity e) {
		return e != null && TURRET_CHECK.test(e);
	}

	public static float getTotalArmorAgainstType(Player ply, DamageType type) {
		float value = 0.0f;
		for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
			ItemStack armor = ply.getItemBySlot(slot);
			if (armor.isEmpty()) {
				continue;
			}
			if (armor.getItem() instanceof ITGArmor tg) {
				value += tg.getArmorValue(armor, type);
			} else if (armor.getItem() instanceof ArmorItem ai) {
				value += getArmorAgainstDamageTypeDefault(ply, ai.getDefense(), type);
			}
		}
		return value;
	}

	/** Default behaviour for armor without Techguns stats. */
	public static float getArmorAgainstDamageTypeDefault(LivingEntity elb, float armor, DamageType damageType) {
		switch (damageType) {
			case PHYSICAL:
			case PROJECTILE:
				return armor;
			case EXPLOSION:
			case ENERGY:
			case ICE:
			case LIGHTNING:
			case DARK:
				return armor * 0.5f;
			case FIRE:
				return elb.fireImmune() ? armor * 2 : armor * 0.5f;
			case POISON:
			case RADIATION:
			case UNRESISTABLE:
			default:
				return 0;
		}
	}

	/** Based on the old 1.7 damage formula. */
	public static double getDamageAfterAbsorb_TGFormula(float damage, float totalArmor, float toughnessAttribute, float penetration) {
		float pen = Math.max(penetration - toughnessAttribute, 0);
		double armor = MathUtil.clamp(totalArmor - pen, 0.0, 24.0);
		return damage * (1.0 - armor / 25.0);
	}

	/** Converts a vanilla damage source into the closest Techguns damage type. */
	public static DamageType getTGType(DamageSource src) {
		if (src instanceof TGDamageSource tg) {
			return tg.damageType;
		}
		if (src.is(DamageTypeTags.BYPASSES_ARMOR)) return DamageType.UNRESISTABLE;
		if (src.is(DamageTypeTags.IS_EXPLOSION)) return DamageType.EXPLOSION;
		if (src.is(DamageTypeTags.IS_FIRE)) return DamageType.FIRE;
		if (src.is(DamageTypeTags.IS_PROJECTILE)) return DamageType.PROJECTILE;
		if (src.is(DamageTypeTags.IS_LIGHTNING)) return DamageType.LIGHTNING;
		if (src.is(DamageTypeTags.IS_FREEZING)) return DamageType.ICE;
		if (src.is(DamageTypeTags.WITCH_RESISTANT_TO)) return DamageType.POISON;
		return DamageType.PHYSICAL;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		LivingEntity ent = event.getEntity();
		DamageSource source = event.getSource();
		DamageContainer container = event.getContainer();

		TGDamageSource tg = source instanceof TGDamageSource t ? t : null;
		CURRENT.set(tg);

		if (tg != null && tg.ignoreHurtresistTime) {
			// legacy: neither checked nor applied hurt resistance time; keep the existing i-frames
			container.setPostAttackInvulnerabilityTicks(ent.invulnerableTime);
		}

		// 1.12: Techguns armor (ISpecialArmor) used the Techguns formula for every damage source
		boolean tgFormula = ent instanceof INpcTGDamageSystem
				|| (ent instanceof Player p && (tg != null || techguns.items.armors.ArmorBonusUtil.isWearingTechgunsArmor(p)));
		if (tgFormula && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
			final float penetration = tg != null ? tg.armorPenetration * 4f : 0f;
			container.addModifier(DamageContainer.Reduction.ARMOR, (c, vanillaReduction) -> {
				float toughness = (float) ent.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
				float totalArmor;
				if (ent instanceof INpcTGDamageSystem npc) {
					totalArmor = npc.getTotalArmorAgainstType(tg != null ? tg : asTG(source));
				} else {
					totalArmor = getTotalArmorAgainstType((Player) ent, getTGType(source));
				}
				float dmg = c.getNewDamage();
				float after = (float) getDamageAfterAbsorb_TGFormula(dmg, totalArmor, toughness, penetration);
				return Math.max(0f, dmg - after);
			});
		}
	}

	/** Wraps a vanilla damage source so NPC armor code can query its Techguns damage type. */
	private static TGDamageSource asTG(DamageSource source) {
		TGDamageSource src = new TGDamageSource(source.typeHolder(), source.getDirectEntity(), source.getEntity(), getTGType(source),
				techguns.deatheffects.DeathType.DEFAULT);
		return src;
	}

	/** 1.12 calculateShieldDamage: shields only absorb part of Techguns damage, depending on ShieldStats */
	@SubscribeEvent
	public static void onShieldBlock(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event) {
		if (!event.getBlocked() || !(event.getDamageSource() instanceof TGDamageSource tg)) {
			return;
		}
		LivingEntity ent = event.getEntity();
		ShieldStats stats = ShieldStats.getStats(ent.getUseItem(), ent);
		if (stats == null) {
			return;
		}
		float amount = event.getDamageContainer().getNewDamage();
		float through = stats.getAmount(amount, tg);
		event.setBlockedDamage(Math.max(0f, amount - through));
		event.setShieldDamage(amount);
		ShieldStats.playBlockSound(ent);
	}

	@SubscribeEvent
	public static void onKnockback(LivingKnockBackEvent event) {
		TGDamageSource tg = CURRENT.get();
		if (tg != null && tg.knockbackMultiplier != 1.0f) {
			if (tg.knockbackMultiplier <= 0f) {
				event.setCanceled(true);
			} else {
				event.setStrength(event.getStrength() * tg.knockbackMultiplier);
			}
		}
	}

	@SubscribeEvent
	public static void onDamagePost(LivingDamageEvent.Post event) {
		CURRENT.remove();
	}
}
