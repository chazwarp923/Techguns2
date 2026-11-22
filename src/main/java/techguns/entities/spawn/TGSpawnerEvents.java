package techguns.entities.spawn;

import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import techguns.Techguns;
import techguns.tileentities.TGSpawnerTileEnt;

/** 1.12 ITGSpawnerNPC / TGSpawnerNPCData: count kills of spawner mobs, relink them after chunk reloads. */
@EventBusSubscriber(modid = Techguns.MODID)
public final class TGSpawnerEvents {

	private TGSpawnerEvents() {
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Mob mob) {
			TGSpawnerTileEnt spawner = TGSpawnerTileEnt.getSpawner(mob);
			if (spawner != null) {
				spawner.killedEntity(mob);
			}
		}
	}

	@SubscribeEvent
	public static void onJoin(EntityJoinLevelEvent event) {
		if (event.loadedFromDisk() && event.getEntity() instanceof Mob mob) {
			TGSpawnerTileEnt spawner = TGSpawnerTileEnt.getSpawner(mob);
			if (spawner != null) {
				spawner.relinkNPC(mob);
			}
		}
	}
}
