package techguns.entities.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import techguns.api.npc.factions.ITGNpcTeam;
import techguns.api.npc.factions.TGNpcFaction;

/** Retaliation that ignores attackers of a friendly Techguns faction (1.12 EntityAIHurtByTargetTGFactions). */
public class TGHurtByTargetGoal extends HurtByTargetGoal {

	public TGHurtByTargetGoal(PathfinderMob mob, Class<?>... toIgnoreDamage) {
		super(mob, toIgnoreDamage);
	}

	@Override
	public boolean canUse() {
		LivingEntity attacker = this.mob.getLastHurtByMob();
		if (attacker instanceof ITGNpcTeam t && this.mob instanceof ITGNpcTeam own && !TGNpcFaction.isHostile(own.getTGFaction(), t.getTGFaction())) {
			return false;
		}
		return super.canUse();
	}
}
