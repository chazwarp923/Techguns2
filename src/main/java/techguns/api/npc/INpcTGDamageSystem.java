package techguns.api.npc;

import techguns.damagesystem.TGDamageSource;

/** Entities that use the Techguns armor formula against all damage. Must be a LivingEntity. */
public interface INpcTGDamageSystem {
	float getTotalArmorAgainstType(TGDamageSource dmgsrc);
}
