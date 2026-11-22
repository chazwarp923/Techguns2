package techguns.items;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.advancements.CriteriaTriggers;

/**
 * RadAway / Anti-RAD pills (1.12 ItemRadAway, ItemRadpills): used like food (also when not hungry), give their effects
 * and leave the empty container.
 */
public class ItemRadConsumable extends Item {

	private final Supplier<List<MobEffectInstance>> effects;
	private final Supplier<? extends Item> container;
	private final Supplier<SoundEvent> sound;
	private final UseAnim anim;
	private final List<Component> lines;

	public ItemRadConsumable(Properties props, Supplier<List<MobEffectInstance>> effects, Supplier<? extends Item> container, Supplier<SoundEvent> sound,
			UseAnim anim, List<Component> lines) {
		super(props);
		this.effects = effects;
		this.container = container;
		this.sound = sound;
		this.anim = anim;
		this.lines = lines;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 32;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return anim;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity ent) {
		level.playSound(null, ent.getX(), ent.getY(), ent.getZ(), sound.get(), SoundSource.PLAYERS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f);
		if (!level.isClientSide) {
			for (MobEffectInstance e : effects.get()) {
				ent.addEffect(new MobEffectInstance(e));
			}
		}
		if (ent instanceof ServerPlayer sp) {
			CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
			sp.awardStat(Stats.ITEM_USED.get(this));
		}
		ItemStack empty = new ItemStack(container.get());
		if (ent instanceof Player ply && ply.getAbilities().instabuild) {
			if (!ply.getInventory().contains(empty)) {
				ply.getInventory().add(empty);
			}
			return stack;
		}
		stack.shrink(1);
		if (stack.isEmpty()) {
			return empty;
		}
		if (ent instanceof Player ply && !ply.getInventory().add(empty)) {
			ply.drop(empty, false);
		}
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		for (Component c : lines) {
			tooltip.add(c.copy().withStyle(ChatFormatting.BLUE));
		}
	}
}
