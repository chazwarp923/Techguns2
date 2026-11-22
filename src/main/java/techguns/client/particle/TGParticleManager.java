package techguns.client.particle;

import java.util.Comparator;
import java.util.Iterator;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import techguns.TGConfig;
import techguns.client.particle.list.ParticleList;
import techguns.client.particle.list.ParticleList.ParticleListIterator;

public class TGParticleManager {

	public static final TGParticleManager INSTANCE = new TGParticleManager();

	/** camera position for the current frame; particle vertices are relative to it */
	public static double interpPosX;
	public static double interpPosY;
	public static double interpPosZ;

	protected ParticleList<TGParticleSystem> list_systems = new ParticleList<>();
	protected ParticleList<ITGParticle> list = new ParticleList<>();
	protected ParticleList<ITGParticle> list_nosort = new ParticleList<>();
	protected ComparatorParticleDepth compare = new ComparatorParticleDepth();

	public void addEffect(ITGParticle effect) {
		if (effect == null) return;
		if (effect instanceof TGParticleSystem sys) {
			list_systems.add(sys);
		} else if (effect.doNotSort()) {
			list_nosort.add(effect);
		} else {
			list.add(effect);
		}
	}

	public void clear() {
		list_systems = new ParticleList<>();
		list = new ParticleList<>();
		list_nosort = new ParticleList<>();
	}

	public void tickParticles() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.isPaused()) return;

		Entity viewEnt = mc.getCameraEntity();

		Iterator<TGParticleSystem> sysit = list_systems.iterator();
		while (sysit.hasNext()) {
			TGParticleSystem p = sysit.next();
			p.updateTick();
			if (p.shouldRemove()) {
				sysit.remove();
			}
		}

		ParticleListIterator<ITGParticle> it = list.iterator();
		while (it.hasNext()) {
			ITGParticle p = it.next();
			p.updateTick();
			if (p.shouldRemove()) {
				it.remove();
			} else if (viewEnt != null) {
				p.setDepth(this.distanceToPlane(viewEnt, p.getPos()));
			}
		}

		Iterator<ITGParticle> it2 = list_nosort.iterator();
		while (it2.hasNext()) {
			ITGParticle p = it2.next();
			p.updateTick();
			if (p.shouldRemove()) {
				it2.remove();
			}
		}

		if (TGConfig.cl_sortPassesPerTick > 0) {
			this.list.doBubbleSort(TGConfig.cl_sortPassesPerTick, compare);
		}
	}

	public void renderParticles(Camera camera, float partialTicks) {
		float yaw = camera.getYRot() * Mth.DEG_TO_RAD;
		float pitch = camera.getXRot() * Mth.DEG_TO_RAD;
		float f1 = Mth.cos(yaw);
		float f2 = Mth.sin(yaw);
		float f3 = -f2 * Mth.sin(pitch);
		float f4 = f1 * Mth.sin(pitch);
		float f5 = Mth.cos(pitch);

		Vec3 cam = camera.getPosition();
		interpPosX = cam.x;
		interpPosY = cam.y;
		interpPosZ = cam.z;

		TGParticleBuffers buffers = TGParticleBuffers.world(cam);
		this.list.forEach(p -> p.doRender(buffers, partialTicks, f1, f5, f2, f3, f4));
		this.list_nosort.forEach(p -> p.doRender(buffers, partialTicks, f1, f5, f2, f3, f4));
		buffers.finish();
	}

	public double distanceToPlane(Entity viewEntity, Vec3 pos) {
		// Formula from: http://geomalgorithms.com/a04-_planes.html
		Vec3 n = viewEntity.getLookAngle();
		double dot1 = -n.dot(pos.subtract(viewEntity.position()));
		double dot2 = n.dot(n);
		double f = dot1 / dot2;

		Vec3 pos2 = pos.add(n.scale(f));
		return pos.distanceToSqr(pos2);
	}

	public static class ComparatorParticleDepth implements Comparator<ITGParticle> {
		@Override
		public int compare(ITGParticle p1, ITGParticle p2) {
			if (p1.doNotSort() && p2.doNotSort()) {
				return 0;
			}
			double dist1 = p1.getDepth();
			double dist2 = p2.getDepth();
			return Double.compare(dist2, dist1);
		}
	}
}
