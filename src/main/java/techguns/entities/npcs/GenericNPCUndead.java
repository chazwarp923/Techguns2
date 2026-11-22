package techguns.entities.npcs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Undead NPCs (1.12 GenericNPCUndead), burn in daylight unless shouldBurnInDay is false. Undead via the entity type tag. */
public abstract class GenericNPCUndead extends GenericNPC {

	protected GenericNPCUndead(EntityType<? extends GenericNPCUndead> type, Level level) {
		super(type, level);
	}

	protected boolean shouldBurnInDay() {
		return true;
	}

	@Override
	public void aiStep() {
		if (this.isAlive() && this.shouldBurnInDay() && this.isSunBurnTick()) {
			this.igniteForSeconds(8);
		}
		super.aiStep();
	}
}
