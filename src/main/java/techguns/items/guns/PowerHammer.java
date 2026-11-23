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

public class PowerHammer extends GenericGunMeleeCharge {

	public PowerHammer(ChargedProjectileSelector<? extends GenericProjectile> sel, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy, float fullChargeTime, int ammoConsumedOnFullCharge) {
		super(sel, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy, fullChargeTime, ammoConsumedOnFullCharge);
		this.setMiningHeads(TGItems.POWERHAMMERHEAD_OBSIDIAN, TGItems.POWERHAMMERHEAD_CARBON);
		this.setBaseHead(TGItems.POWERHAMMERHEAD_STEEL);
	}

	@Override
	protected TGDamageSource getMeleeDamageSource(Player player, ItemStack stack) {
		TGDamageSource src = TGDamageSource.causePlayerMeleeDamage(player, DamageType.PHYSICAL, DeathType.GORE);
		if (this.getCurrentAmmo(stack) > 0) {
			src.goreChance = 1.0f;
			src.armorPenetration = this.penetration;
			src.knockbackMultiplier = 1.5f;
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
	public void spawnChargedProjectile(Level level, LivingEntity player, ItemStack itemStack, float spread, float charge, int ammoConsumed,
			EnumBulletFirePos firePos) {
		float extraDmg = 0.5f * this.getMiningHeadLevel(itemStack);
		IChargedProjectileFactory<? extends GenericProjectile> fact = this.chargedProjectile_selector
				.getFactoryForType(this.getCurrentAmmoVariantKey(itemStack));
		GenericProjectile proj = fact.createChargedProjectile(level, player, damage + extraDmg, speed, this.getScaledTTL(), spread, this.damageDropStart,
				damageDropEnd, this.damageMin + extraDmg, penetration, getDoBlockDamage(player), firePos, radius, gravity, charge, ammoConsumed);
		if (proj != null) {
			level.addFreshEntity(proj);
		}
	}

	@Override
	public float getExtraDigSpeed(ItemStack stack) {
		return 2.0f * this.getMiningHeadLevel(stack);
	}

	@Override
	protected void playSweepSoundEffect(Player player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TGSounds.POWERHAMMER_IMPACT.get(), player.getSoundSource(), 1.0F, 1.0F);
	}
}
