package techguns.deatheffects;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.Techguns;
import techguns.damagesystem.TGDamageSource;
import techguns.packets.PacketEntityDeathType;

/** 1.12 EntityDeathUtils + TGEventHandler death part: tells clients to play special death effects. */
@EventBusSubscriber(modid = Techguns.MODID)
public final class DeathEffectEvents {

	/** entities that burst into gibs on gore deaths (1.12 EntityDeathUtils.goreMap) */
	public static final TagKey<EntityType<?>> GORE_DEATH = TagKey.create(Registries.ENTITY_TYPE, Techguns.rl("gore_death"));

	private DeathEffectEvents() {
	}

	public static boolean hasSpecialDeathAnim(LivingEntity entity, DeathType deathtype) {
		if (deathtype == DeathType.BIO || deathtype == DeathType.LASER) {
			return true;
		}
		return entity.getType().is(GORE_DEATH);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.level().isClientSide || !(event.getSource() instanceof TGDamageSource tgs) || tgs.deathType == null
				|| tgs.deathType == DeathType.DEFAULT) {
			return;
		}
		if (entity.getRandom().nextFloat() < tgs.goreChance && hasSpecialDeathAnim(entity, tgs.deathType)) {
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new PacketEntityDeathType(entity, tgs.deathType));
		}
	}
}
