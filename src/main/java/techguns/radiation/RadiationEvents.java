package techguns.radiation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.packets.PacketRadSync;

/** Player radiation: radioactive inventory items, radiation sickness, RAD lost on death, client sync, item tooltips. */
@EventBusSubscriber(modid = Techguns.MODID)
public class RadiationEvents {

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer ply)) {
			return;
		}
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		long time = ply.level().getGameTime();

		if (TGRadiationSystem.isEnabled() && time % TGRadiationSystem.INVENTORY_RADIATION_INTERVAL == 0) {
			int maxrad = 0;
			for (ItemStack stack : ply.getInventory().items) {
				TGRadiationSystem.ItemRadiationData data = TGRadiationSystem.getRadiationData(stack);
				if (data != null && data.radamount() > maxrad) {
					maxrad = data.radamount();
				}
			}
			if (maxrad > 0) {
				ply.addEffect(new MobEffectInstance(TGRadiationSystem.RADIATION, TGRadiationSystem.INVENTORY_RADIATION_INTERVAL, maxrad - 1, false, false));
			}
		}

		if (time % 20 == 0) {
			int rad = props.radlevel;
			if (rad >= TGRadiationSystem.MINOR_POISONING && rad < TGRadiationSystem.SEVERE_POISONING) {
				ply.addEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0, false, false));
			} else if (rad >= TGRadiationSystem.SEVERE_POISONING) {
				ply.addEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 1, false, false));
				ply.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 1, false, false));
				ply.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1, false, false));
			}
			if (rad >= TGRadiationSystem.LETHAL_POISONING) {
				ply.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30, 1, false, false));
				ply.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
				ply.hurt(TGDamageSource.causeLethalRadPoisoningDamage(ply.level(), null, null, DeathType.LASER), 2f);
			}
		}

		if (props.radlevel != props.lastSyncedRadlevel) {
			sync(ply, props);
		}
	}

	private static void sync(ServerPlayer ply, TGExtendedPlayer props) {
		props.lastSyncedRadlevel = props.radlevel;
		PacketDistributor.sendToPlayer(ply, new PacketRadSync(props.radlevel));
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player ply && !ply.level().isClientSide) {
			TGExtendedPlayer.get(ply).addRadiation(-TGRadiationSystem.RADLOST_ON_DEATH);
		}
	}

	/** new player entity / client world after respawn, login and dimension change: send again */
	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		resync(event.getEntity());
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		resync(event.getEntity());
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		resync(event.getEntity());
	}

	private static void resync(Player ply) {
		if (ply instanceof ServerPlayer sp) {
			TGExtendedPlayer.get(sp).lastSyncedRadlevel = -1;
		}
	}

	@SubscribeEvent
	public static void onRegisterBrewing(net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent event) {
		brewingRecipes().forEach(event.getBuilder()::addRecipe);
	}

	/** also shown in JEI */
	public static java.util.List<RadBrewingRecipe> brewingRecipes() {
		var awkward = net.minecraft.world.item.alchemy.Potions.AWKWARD;
		return java.util.List.of(new RadBrewingRecipe(awkward, Ingredient.of(tag("dusts/uranium")), TGRadiationSystem.RAD_POTION),
				new RadBrewingRecipe(awkward, Ingredient.of(techguns.TGItems.ENRICHED_URANIUM.get()), TGRadiationSystem.RAD_POTION_SEVERE),
				new RadBrewingRecipe(awkward, Ingredient.of(tag("ingots/lead")), TGRadiationSystem.RAD_RESISTANCE_POTION),
				new RadBrewingRecipe(TGRadiationSystem.RAD_POTION,
						Ingredient.of(net.minecraft.world.item.Items.GOLDEN_CARROT, net.minecraft.world.item.Items.GLISTERING_MELON_SLICE),
						TGRadiationSystem.RAD_REGENERATION_POTION));
	}

	private static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag(String path) {
		return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", path));
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		TGRadiationSystem.ItemRadiationData data = TGRadiationSystem.getRadiationData(event.getItemStack());
		if (data != null) {
			event.getToolTip().add(Component.translatable("techguns.radiation").append(" " + data.radamount()).withStyle(ChatFormatting.GREEN));
		}
	}
}
