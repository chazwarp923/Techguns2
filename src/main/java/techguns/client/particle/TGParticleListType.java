package techguns.client.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class TGParticleListType extends TGFXType {

	ArrayList<String> particleSystems = new ArrayList<>();

	public TGParticleListType() {
		isList = true;
	}

	public void addParticleSystem(String particleSystem) {
		particleSystems.add(particleSystem);
	}

	private List<TGParticleSystem> collect(Function<TGFXType, List<TGParticleSystem>> f) {
		ArrayList<TGParticleSystem> list = new ArrayList<>();
		for (String system : particleSystems) {
			TGFXType fxtype = TGFX.FXList.get(system);
			if (fxtype != null) {
				list.addAll(f.apply(fxtype));
			}
		}
		return list;
	}

	@Override
	public List<TGParticleSystem> createParticleSystems(Level world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		return collect(t -> t.createParticleSystems(world, posX, posY, posZ, motionX, motionY, motionZ));
	}

	@Override
	public List<TGParticleSystem> createParticleSystemsOnEntity(Entity ent) {
		return collect(t -> t.createParticleSystemsOnEntity(ent));
	}

	@Override
	public List<TGParticleSystem> createParticleSystemsOnEntityItemAttached(Entity ent, InteractionHand hand) {
		return collect(t -> t.createParticleSystemsOnEntityItemAttached(ent, hand));
	}

	@Override
	public List<TGParticleSystem> createParticleSystemsOnParticle(Level worldIn, TGParticle ent) {
		return collect(t -> t.createParticleSystemsOnParticle(worldIn, ent));
	}
}
