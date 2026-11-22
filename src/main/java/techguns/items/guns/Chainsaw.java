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

public class Chainsaw extends GenericGunMeleeCharge {

	public Chainsaw(ChargedProjectileSelector<? extends GenericProjectile> sel, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy, float fullChargeTime, int ammoConsumedOnFullCharge) {
		super(sel, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy, fullChargeTime, ammoConsumedOnFullCharge);
		this.setMiningHeads(TGItems.CHAINSAWBLADES_OBSIDIAN, TGItems.CHAINSAWBLADES_CARBON);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		this.shootGunPrimary(stack, level, player, false, hand, null);
		return InteractionResultHolder.fail(stack);
	}

	@Override
	public float getExtraDigSpeed(ItemStack stack) {
		return 3.0f * this.getMiningHeadLevel(stack);
	}

	@Override
	protected SoundEvent getSwingSound() {
		return TGSounds.CHAINSAW_LOOP.get();
	}

	@Override
	protected SoundEvent getBlockBreakSound() {
		return TGSounds.CHAINSAW_HIT.get();
	}

	@Override
	protected void playSweepSoundEffect(Player player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TGSounds.CHAINSAW_HIT.get(), player.getSoundSource(), melee_sound_volume,
				1.0F);
	}
}
