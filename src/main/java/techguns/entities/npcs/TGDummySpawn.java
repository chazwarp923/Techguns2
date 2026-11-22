package techguns.entities.npcs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Natural spawn placeholder for Techguns NPCs (1.12 TGDummySpawn): biomes spawn this, {@link techguns.entities.spawn.TGSpawnManager}
 * replaces it with an NPC fitting the danger level of the position before it joins the world.
 */
public class TGDummySpawn extends Monster {

	public TGDummySpawn(EntityType<? extends TGDummySpawn> type, Level level) {
		super(type, level);
	}
}
