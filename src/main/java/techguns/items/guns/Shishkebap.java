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

public class Shishkebap extends GenericGunMeleeCharge {

	public Shishkebap(ChargedProjectileSelector<? extends GenericProjectile> sel, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy, float fullChargeTime, int ammoConsumedOnFullCharge) {
		super(sel, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy, fullChargeTime, ammoConsumedOnFullCharge);
		this.setHasCustomAnim(false);
		this.setNoBowAnim();
	}

	@Override
	protected TGDamageSource getMeleeDamageSource(Player player, ItemStack stack) {
		TGDamageSource src = TGDamageSource.causePlayerMeleeDamage(player, DamageType.FIRE, DeathType.GORE);
		if (this.getCurrentAmmo(stack) > 0) {
			src.goreChance = 0.25f;
			src.armorPenetration = this.penetration;
			src.knockbackMultiplier = 1f;
		} else {
			src.deathType = DeathType.DEFAULT;
		}
		return src;
	}

	@Override
	protected void spawnSweepParticle(Level w, double x, double y, double z, double motionX, double motionY, double motionZ) {
		TGPackets.sendToAllAround(new PacketSpawnParticle("PowerhammerImpact", x, y, z), w, x, y, z, 32.0);
	}

	@Override
	public DamageType getDamageType(ItemStack stack) {
		return DamageType.FIRE;
	}

	@Override
	protected void onMeleeHitTarget(ItemStack stack, Entity target) {
		if (this.getAmmoLeft(stack) > 0) {
			target.igniteForSeconds(3);
		}
	}

	@Override
	protected SoundEvent getSwingSound() {
		return TGSounds.SHISHKEBAP_SWING.get();
	}

	@Override
	protected SoundEvent getBlockBreakSound() {
		return null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return InteractionResultHolder.pass(player.getItemInHand(hand));
	}

	@Override
	public boolean canCharge() {
		return false;
	}

	@Override
	public void releaseUsing(ItemStack item, Level level, LivingEntity entityLiving, int timeLeft) {
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 0;
	}
}
