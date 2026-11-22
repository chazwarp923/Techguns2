package techguns.radiation;

import java.util.Set;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.EffectCure;
import techguns.TGSounds;
import techguns.capabilities.TGExtendedPlayer;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;

/**
 * Radiation (1.12 RadiationPotion): every second, players collect (amplifier + 1 - RAD resistance) RAD, mobs take
 * half of that as radiation damage. Milk does not cure it.
 */
public class RadiationEffect extends MobEffect {

	public RadiationEffect() {
		super(MobEffectCategory.HARMFUL, 0xf8ff00);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity ent, int amplifier) {
		if (ent.level().isClientSide) {
			return true;
		}
		int res = TGRadiationSystem.getRadResistance(ent);
		int amount = Math.clamp(amplifier + 1 - res, 0, 1000);
		if (ent instanceof Player ply) {
			if (!ply.isCreative() && !ply.isSpectator()) {
				TGExtendedPlayer.get(ply).addRadiation(amount);
				if (amount > 0 && TGRadiationSystem.isEnabled() && ply instanceof ServerPlayer sp) {
					sp.playNotifySound(amplifier >= 2 ? TGSounds.GEIGER_HIGH.get() : TGSounds.GEIGER_LOW.get(), SoundSource.PLAYERS, 1f, 1f);
				}
			}
		} else if (ent instanceof Mob) {
			float damage = amount * 0.5f;
			if (damage >= 1.0f) {
				TGDamageSource src = TGDamageSource.causeRadiationDamage(ent.level(), null, null, DeathType.BIO);
				ent.hurt(src, damage);
			}
		}
		return true;
	}

	@Override
	public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
		// no milk
	}
}
