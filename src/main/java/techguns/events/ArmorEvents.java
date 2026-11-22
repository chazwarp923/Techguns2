package techguns.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.armors.ArmorBonusUtil;
import techguns.items.armors.PoweredArmor;
import techguns.items.armors.TGArmorBonus;
import techguns.items.guns.GenericGunMeleeCharge;
import techguns.util.TGFXHooks;

/**
 * Applies Techguns armor / special slot bonuses (1.12 TGTickHandler armor part and TGEventHandler jump / fall /
 * break speed / cooling events). Bonuses come from {@link ArmorBonusUtil#getArmorBonusForPlayer}.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class ArmorEvents {

	private static final ResourceLocation SPEED_ID = Techguns.rl("armor_speed");
	private static final ResourceLocation HEALTH_ID = Techguns.rl("armor_health");
	private static final ResourceLocation KNOCKBACK_ID = Techguns.rl("armor_knockback_resistance");
	private static final ResourceLocation STEP_ID = Techguns.rl("armor_step_assist");

	private static final float DEFAULT_FLYSPEED = 0.05f;

	private static float bonus(Player ply, TGArmorBonus type, boolean consume) {
		return ArmorBonusUtil.getArmorBonusForPlayer(ply, type, consume);
	}

	private static void setModifier(Player p, Holder<Attribute> attr, ResourceLocation id, double value, AttributeModifier.Operation op) {
		AttributeInstance inst = p.getAttribute(attr);
		if (inst == null) {
			return;
		}
		if (value == 0) {
			inst.removeModifier(id);
		} else {
			AttributeModifier cur = inst.getModifier(id);
			if (cur == null || cur.amount() != value || cur.operation() != op) {
				inst.addOrUpdateTransientModifier(new AttributeModifier(id, value, op));
			}
		}
	}

	private static boolean inWaterOrLava(Player p) {
		return p.isEyeInFluid(FluidTags.WATER) || p.isEyeInFluid(FluidTags.LAVA);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		TGExtendedPlayer props = TGExtendedPlayer.get(player);
		boolean client = player.level().isClientSide;

		if (ArmorBonusUtil.isWearingTechgunsArmor(player)) {
			if (!client) {
				PoweredArmor.calculateConsumptionTick(player);
			}
			if (player.isOnFire() && bonus(player, TGArmorBonus.COOLING_SYSTEM, false) >= 1.0f) {
				player.clearFire();
			}
		}

		float speed = bonus(player, TGArmorBonus.SPEED, false);
		if (player.isInWater() || player.isInLava()) {
			speed += bonus(player, TGArmorBonus.SPEED_WATER, false);
		}
		if (speed > 0.0f && player.isSprinting()) {
			speed *= 2.0f;
		}
		setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, bonus(player, TGArmorBonus.EXTRA_HEART, false), AttributeModifier.Operation.ADD_VALUE);
		setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, bonus(player, TGArmorBonus.KNOCKBACK_RESISTANCE, false),
				AttributeModifier.Operation.ADD_VALUE);

		// step assist: 0.6 -> 1.1 blocks (toggle key: TGExtendedPlayer.enableStepAssist)
		float stepassist = bonus(player, TGArmorBonus.STEPASSIST, false);
		setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, props.enableStepAssist && stepassist > 0 ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);

		boolean nvEnabled = props.enableNightVision;
		float nightvision = bonus(player, TGArmorBonus.NIGHTVISION, nvEnabled);
		if (nightvision > 0.0f && nvEnabled && !client) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
		}

		tickFlight(player, props, client);
	}

	/** antigrav pack creative flight / jetpack fly speed */
	private static void tickFlight(Player player, TGExtendedPlayer props, boolean client) {
		float flightbonus = bonus(player, TGArmorBonus.CREATIVE_FLIGHT, false);
		var abilities = player.getAbilities();
		if (flightbonus > 0) {
			if (!abilities.mayfly) {
				abilities.mayfly = true;
				if (!client) {
					player.onUpdateAbilities();
				}
			}
			props.gotCreativeFlightLastTick = true;
			if (abilities.flying) {
				if (!props.wasFlying) {
					if (client) {
						TGFXHooks.createFXOnEntity("AntiGravRing", player);
					} else {
						player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TGSounds.ANTI_GRAV_START.get(), player.getSoundSource(), 1.0f,
								1.0f);
					}
				}
				props.wasFlying = true;
			} else {
				props.wasFlying = false;
			}
		} else {
			if (props.gotCreativeFlightLastTick && !player.isCreative() && !player.isSpectator()) {
				abilities.mayfly = false;
				abilities.flying = false;
				if (!client) {
					player.onUpdateAbilities();
				}
			}
			props.gotCreativeFlightLastTick = false;
			props.wasFlying = false;
		}

		if (client) {
			float flyspeedBonus = bonus(player, TGArmorBonus.FLYSPEED, false);
			if (flyspeedBonus > 0) {
				abilities.setFlyingSpeed(props.enableJetpack ? (1.0f + flyspeedBonus) * DEFAULT_FLYSPEED : DEFAULT_FLYSPEED);
			} else if (props.gotCreativeFlightLastTick && abilities.getFlyingSpeed() != DEFAULT_FLYSPEED) {
				abilities.setFlyingSpeed(DEFAULT_FLYSPEED);
			}
		}
	}

	private static void syncToggles(Player p) {
		if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
			net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, techguns.packets.PacketTGToggleSync.of(TGExtendedPlayer.get(sp), -1, false));
		}
	}

	@SubscribeEvent
	public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
		// 1.12 forced safe mode on load for players without the unsafe mode permission
		if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp && !techguns.TGPermissions.canUseUnsafeMode(sp)) {
			TGExtendedPlayer.get(sp).enableSafemode = true;
		}
		syncToggles(event.getEntity());
	}

	/** 1.12 onStartTracking sync: a player who starts seeing another one learns whether its jetpack is boosting (flames) */
	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof Player tracked && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
			TGExtendedPlayer props = TGExtendedPlayer.get(tracked);
			if (props.isJumpkeyPressed) {
				net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new techguns.packets.PacketJetpackState(tracked.getId(), true));
			}
		}
	}

	@SubscribeEvent
	public static void onRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
		syncToggles(event.getEntity());
	}

	@SubscribeEvent
	public static void onChangeDim(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
		syncToggles(event.getEntity());
	}

	/** fires on the client (motion) and the server (power use, sound) */
	@SubscribeEvent
	public static void onJump(LivingEvent.LivingJumpEvent event) {
		if (event.getEntity() instanceof Player ply) {
			float jumpbonus = bonus(ply, TGArmorBonus.JUMP, true);
			if (jumpbonus != 0f) {
				ply.setDeltaMovement(ply.getDeltaMovement().add(0, jumpbonus, 0));
			}
		}
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (event.getEntity() instanceof Player ply) {
			boolean consume = event.getDistance() > 3.0f;
			float fallbonus = bonus(ply, TGArmorBonus.FALLDMG, consume);
			float reduction = fallbonus < 1 ? 1 - fallbonus : 0.0f;
			float freeheight = bonus(ply, TGArmorBonus.FREEHEIGHT, false);
			event.setDistance(Math.max(0f, event.getDistance() - freeheight) * reduction);
		}
	}

	/** area mining tools dig as slow as the hardest block of the plane */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onBreakSpeedHigh(PlayerEvent.BreakSpeed event) {
		Player ply = event.getEntity();
		ItemStack item = ply.getMainHandItem();
		if (item.getItem() instanceof GenericGunMeleeCharge g && g.getMiningRadius(item) > 0 && !ply.isShiftKeyDown()) {
			BlockPos center = event.getPosition().orElse(null);
			Direction side = center != null ? g.getSideHitMining(ply) : null;
			if (side != null) {
				float mainHardness = event.getState().getDestroySpeed(ply.level(), center);
				int r = g.getMiningRadius(item);
				Direction.Axis axis = side.getAxis();
				float maxHardness = 0f;
				for (BlockPos p : BlockPos.betweenClosed(center.offset(axis == Direction.Axis.X ? 0 : -r, axis == Direction.Axis.Y ? 0 : -r,
						axis == Direction.Axis.Z ? 0 : -r), center.offset(axis == Direction.Axis.X ? 0 : r, axis == Direction.Axis.Y ? 0 : r,
								axis == Direction.Axis.Z ? 0 : r))) {
					if (p.equals(center) || ply.level().getBlockEntity(p) != null) {
						continue;
					}
					BlockState s = ply.level().getBlockState(p);
					if (s.isAir() || !g.isEffectiveToolForState(item, s)) {
						continue;
					}
					maxHardness = Math.max(maxHardness, s.getDestroySpeed(ply.level(), p));
				}
				if (maxHardness > mainHardness && maxHardness > 0) {
					event.setNewSpeed(event.getNewSpeed() * mainHardness / maxHardness);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		Player ply = event.getEntity();
		float b = 1.0f + bonus(ply, TGArmorBonus.BREAKSPEED, true);
		float waterbonus = 1.0f;
		if (inWaterOrLava(ply)) {
			waterbonus += bonus(ply, TGArmorBonus.BREAKSPEED_WATER, true);
		}
		event.setNewSpeed(event.getNewSpeed() * b * waterbonus);
	}

	/** cooling system: immune to fire / lava damage */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity() instanceof Player ply) {
			var src = event.getSource();
			if (src.is(DamageTypes.LAVA) || src.is(DamageTypes.ON_FIRE) || src.is(DamageTypes.IN_FIRE)) {
				if (bonus(ply, TGArmorBonus.COOLING_SYSTEM, ply.level().getGameTime() % 5 == 0) >= 1.0f) {
					event.setCanceled(true);
				}
			}
		}
	}
}
