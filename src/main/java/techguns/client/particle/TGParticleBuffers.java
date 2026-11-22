package techguns.client.particle;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import techguns.client.render.TGRenderHelper.RenderType;
import techguns.client.render.gl.TGRenderTypes;

/**
 * Replaces the 1.12 Tessellator usage of particles. Particles request a buffer for their texture and
 * blend mode; consecutive particles with the same type share a batch, a change of type flushes the
 * previous one, so the depth sorted draw order is kept. Vertex positions are camera relative.
 * <p>
 * An optional pose (item attached particles) transforms positions before they are written.
 */
public class TGParticleBuffers {

	private static final ByteBufferBuilder BYTES = new ByteBufferBuilder(1 << 16);

	private final MultiBufferSource source;
	private final PoseStack.Pose pose;

	/** camera position */
	public final Vec3 cam;

	private TGParticleBuffers(MultiBufferSource source, PoseStack.Pose pose, Vec3 cam) {
		this.source = source;
		this.pose = pose;
		this.cam = cam;
	}

	/** World particles: buffers are flushed by {@link #finish()}. */
	public static TGParticleBuffers world(Vec3 cam) {
		return new TGParticleBuffers(MultiBufferSource.immediate(BYTES), null, cam);
	}

	/** Particles rendered in an item's local space with the given pose, written into an existing buffer source. */
	public static TGParticleBuffers posed(MultiBufferSource source, PoseStack.Pose pose) {
		return new TGParticleBuffers(source, pose, Vec3.ZERO);
	}

	public VertexConsumer get(ResourceLocation texture, RenderType mode) {
		return source.getBuffer(TGRenderTypes.fx(texture, mode));
	}

	public void vertex(VertexConsumer vc, double x, double y, double z, float u, float v, float r, float g, float b, float a) {
		if (pose != null) {
			vc.addVertex(pose, (float) x, (float) y, (float) z);
		} else {
			vc.addVertex((float) x, (float) y, (float) z);
		}
		vc.setUv(u, v).setColor(r, g, b, a);
	}

	public void finish() {
		if (source instanceof MultiBufferSource.BufferSource bs) {
			bs.endBatch();
		}
	}
}
