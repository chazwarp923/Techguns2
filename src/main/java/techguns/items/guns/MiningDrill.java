package techguns.items.guns;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GenericProjectile;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.TGPackets;
import techguns.util.TextUtil;

/** Sneaking = silk touch, see techguns.events.MiningToolEvents. */
public class MiningDrill extends GenericGunMeleeCharge {

	public MiningDrill(ChargedProjectileSelector<? extends GenericProjectile> sel, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy, float fullChargeTime, int ammoConsumedOnFullCharge) {
		super(sel, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy, fullChargeTime, ammoConsumedOnFullCharge);
		this.setMiningHeads(TGItems.MININGDRILLHEAD_CARBON);
	}

	@Override
	protected TGDamageSource getMeleeDamageSource(Player player, ItemStack stack) {
		TGDamageSource src = TGDamageSource.causePlayerMeleeDamage(player, DamageType.PHYSICAL, DeathType.GORE);
		if (this.getCurrentAmmo(stack) > 0) {
			src.goreChance = 0.5f;
			src.armorPenetration = this.penetration;
			src.knockbackMultiplier = 1f;
		} else {
			src.deathType = DeathType.DEFAULT;
		}
		return src;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		this.shootGunPrimary(stack, level, player, false, hand, null);
		return InteractionResultHolder.fail(stack);
	}

	@Override
	protected void spawnSweepParticle(Level w, double x, double y, double z, double motionX, double motionY, double motionZ) {
		TGPackets.sendToAllAround(new PacketSpawnParticle("PowerhammerImpact", x, y, z), w, x, y, z, 32.0);
	}

	@Override
	public float getExtraDigSpeed(ItemStack stack) {
		return 2.0f * this.getMiningHeadLevel(stack);
	}

	@Override
	protected void addMiningTooltip(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag, boolean longTooltip) {
		super.addMiningTooltip(stack, context, list, flag, longTooltip);
		if (longTooltip) {
			list.add(TextUtil.transTG("tooltip.sneaksilktouch"));
		}
	}

	@Override
	protected SoundEvent getSwingSound() {
		return TGSounds.DRILLER_SWING.get();
	}

	@Override
	protected SoundEvent getBlockBreakSound() {
		return TGSounds.DRILLER_BREAK.get();
	}

	@Override
	protected void playSweepSoundEffect(Player player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TGSounds.DRILLER_HIT.get(), player.getSoundSource(), 1.0F, 1.0F);
	}
}
