package techguns.items.guns;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.api.damagesystem.DamageType;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GrenadeProjectile;

public interface IGrenadeProjectileFactory<T extends GrenadeProjectile> {

	T createProjectile(Level level, LivingEntity p, float damage, float speed, int TTL, float spread, float dmgDropStart, float dmgDropEnd,
			float dmgMin, float penetration, boolean blockdamage, EnumBulletFirePos firePos, float radius, double gravity, float charge, int bounces);

	DamageType getDamageType();
}
