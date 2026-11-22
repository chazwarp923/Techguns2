package techguns.events;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.curios.TGCurios;

/**
 * 1.12 auto feeder: food in the Curios "Auto Food" slots is eaten when hunger drops below full. Nutrition the
 * player can't use yet is kept (TGExtendedPlayer.foodleft) and given later.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class AutoFeeder {

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player ply = event.getEntity();
		if (ply.level().isClientSide || TGConfig.disableAutofeeder) {
			return;
		}
		FoodData food = ply.getFoodData();
		if (food.getFoodLevel() > 19) {
			return;
		}
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		int needed = 20 - food.getFoodLevel();
		if (props.foodleft > 0) {
			if (props.foodleft <= needed) {
				food.eat(props.foodleft, props.lastSaturation);
				props.foodleft = 0;
				props.lastSaturation = 0.0f;
			} else {
				food.eat(needed, props.lastSaturation);
				props.foodleft -= needed;
			}
			return;
		}
		List<ItemStack> slots = TGCurios.getStacks(ply, TGCurios.SLOT_FOOD);
		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = slots.get(i);
			FoodProperties fp = stack.getFoodProperties(ply);
			if (stack.isEmpty() || fp == null) {
				continue;
			}
			ItemStack rest = stack.copy();
			rest.shrink(1);
			slots.set(i, rest);

			for (FoodProperties.PossibleEffect e : fp.effects()) {
				if (ply.getRandom().nextFloat() < e.probability()) {
					ply.addEffect(e.effect());
				}
			}
			fp.usingConvertsTo().ifPresent(container -> {
				if (!ply.getInventory().add(container.copy())) {
					ply.drop(container.copy(), false);
				}
			});
			ply.level().playSound(null, ply.getX(), ply.getY(), ply.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1f, 1f);

			int nutrition = fp.nutrition();
			// FoodProperties stores absolute saturation, FoodData.eat takes the 1.12 style modifier
			float modifier = nutrition > 0 ? fp.saturation() / (nutrition * 2.0f) : 0f;
			int left = nutrition - needed;
			if (left > 0) {
				food.eat(needed, modifier);
				props.foodleft = (short) left;
				props.lastSaturation = modifier;
			} else {
				food.eat(nutrition, modifier);
				props.foodleft = 0;
				props.lastSaturation = 0.0f;
			}
			// one item per tick
			return;
		}
	}
}
