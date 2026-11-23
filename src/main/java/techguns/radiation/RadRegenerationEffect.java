package techguns.radiation;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import techguns.capabilities.TGExtendedPlayer;

/** RAD regeneration (1.12 RadRegenerationPotion): removes (amplifier + 1) RAD per second. */
public class RadRegenerationEffect extends MobEffect {

	public RadRegenerationEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xfa2000);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity ent, int amplifier) {
		if (!ent.level().isClientSide && ent instanceof Player ply) {
			TGExtendedPlayer.get(ply).addRadiation(-(amplifier + 1));
			TGRadiationSystem.externalRadRegeneration.accept(ply, amplifier + 1);
		}
		return true;
	}
}
