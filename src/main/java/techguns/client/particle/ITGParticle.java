package techguns.client.particle;

import net.minecraft.world.phys.Vec3;

public interface ITGParticle {

	Vec3 getPos();

	boolean shouldRemove();

	void updateTick();

	void doRender(TGParticleBuffers buffers, float partialTickTime, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ);

	default boolean doNotSort() {
		return false;
	}

	double getDepth();

	void setDepth(double depth);

	void setItemAttached();
}
