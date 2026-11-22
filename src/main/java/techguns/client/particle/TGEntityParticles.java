package techguns.client.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.Entity;

/**
 * Client-only particle attachments of an entity (1.12: TGExtendedPlayerClient / TGShooterValues
 * capability fields). Held weakly, so unloaded entities drop their particles.
 */
public class TGEntityParticles implements ITGParticleAttachments {

	private static final Map<Entity, TGEntityParticles> MAP = new WeakHashMap<>();

	public static TGEntityParticles get(Entity ent) {
		return MAP.computeIfAbsent(ent, e -> new TGEntityParticles());
	}

	public static TGEntityParticles getIfPresent(Entity ent) {
		return MAP.get(ent);
	}

	public static void tickAll() {
		MAP.entrySet().removeIf(e -> e.getKey().isRemoved());
		MAP.values().forEach(TGEntityParticles::tickParticles);
	}

	public static void clearAll() {
		MAP.clear();
	}

	protected List<ITGParticle> entityParticles;
	protected List<ITGParticle> particlesMH;
	protected List<ITGParticle> particlesOH;
	protected List<TGParticleSystem> sysMH;
	protected List<TGParticleSystem> sysOH;

	@Override
	public List<ITGParticle> getEntityParticles() {
		return entityParticles;
	}

	@Override
	public List<ITGParticle> getEntityParticlesMH() {
		return particlesMH;
	}

	@Override
	public List<ITGParticle> getEntityParticlesOH() {
		return particlesOH;
	}

	@Override
	public List<TGParticleSystem> getParticleSysMainhand() {
		return sysMH;
	}

	@Override
	public List<TGParticleSystem> getParticleSysOffhand() {
		return sysOH;
	}

	@Override
	public List<ITGParticle> getOrInitEntityParticles() {
		if (entityParticles == null) entityParticles = new ArrayList<>();
		return entityParticles;
	}

	@Override
	public List<ITGParticle> getOrInitEntityParticlesOH() {
		if (particlesOH == null) particlesOH = new ArrayList<>();
		return particlesOH;
	}

	@Override
	public List<ITGParticle> getOrInitEntityParticlesMH() {
		if (particlesMH == null) particlesMH = new ArrayList<>();
		return particlesMH;
	}

	@Override
	public List<TGParticleSystem> getOrInitParticleSysMainhand() {
		if (sysMH == null) sysMH = new ArrayList<>();
		return sysMH;
	}

	@Override
	public List<TGParticleSystem> getOrInitParticleSysOffhand() {
		if (sysOH == null) sysOH = new ArrayList<>();
		return sysOH;
	}
}
