package techguns.client.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import techguns.Techguns;
import techguns.client.render.gl.TGRenderState;

/**
 * Minimal Wavefront OBJ mesh drawn with the currently bound texture (replaces the 1.12 ModelBaseBaked,
 * which baked Forge OBJ item models). Vertices are in block units, as the 1.12 baked quads were.
 * Supports v / vt / vn and triangle or quad faces; faces without normals get a computed face normal.
 */
public class ModelObj extends ModelMultipart {

	/** one mesh = one obj file */
	public static class Mesh {
		private final ResourceLocation location;
		private final boolean flipV;
		private float[] data = null; // per vertex: x y z u v nx ny nz, 4 vertices per face

		public Mesh(String objPath, boolean flipV) {
			this.location = Techguns.rl(objPath);
			this.flipV = flipV;
		}

		private void load() {
			List<float[]> pos = new ArrayList<>();
			List<float[]> uv = new ArrayList<>();
			List<float[]> nrm = new ArrayList<>();
			List<Float> out = new ArrayList<>();
			try (BufferedReader r = Minecraft.getInstance().getResourceManager().openAsReader(location)) {
				String line;
				while ((line = r.readLine()) != null) {
					String[] t = line.trim().split("\\s+");
					if (t.length == 0) continue;
					switch (t[0]) {
						case "v" -> pos.add(new float[] { Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]) });
						case "vt" -> uv.add(new float[] { Float.parseFloat(t[1]), t.length > 2 ? Float.parseFloat(t[2]) : 0f });
						case "vn" -> nrm.add(new float[] { Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]) });
						case "f" -> addFace(t, pos, uv, nrm, out);
						default -> {
						}
					}
				}
			} catch (IOException | RuntimeException e) {
				Techguns.LOGGER.error("Could not load OBJ model {}", location, e);
			}
			data = new float[out.size()];
			for (int i = 0; i < data.length; i++) data[i] = out.get(i);
		}

		private void addFace(String[] t, List<float[]> pos, List<float[]> uv, List<float[]> nrm, List<Float> out) {
			int n = t.length - 1;
			if (n < 3) return;
			int[][] idx = new int[n][];
			for (int i = 0; i < n; i++) {
				String[] p = t[i + 1].split("/");
				idx[i] = new int[] { index(p, 0, pos.size()), index(p, 1, uv.size()), index(p, 2, nrm.size()) };
			}
			// face normal for faces without vn
			float[] a = pos.get(idx[0][0]), b = pos.get(idx[1][0]), c = pos.get(idx[2][0]);
			Vector3f fn = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]).cross(c[0] - a[0], c[1] - a[1], c[2] - a[2]);
			if (fn.lengthSquared() > 0) fn.normalize();
			// emit as quads (fan for n>4 split into quads/tris)
			for (int s = 1; s + 1 < n; s += 2) {
				int[] order = s + 2 < n ? new int[] { 0, s, s + 1, s + 2 } : new int[] { 0, s, s + 1, s + 1 };
				for (int k : order) {
					float[] p = pos.get(idx[k][0]);
					float[] tc = idx[k][1] >= 0 ? uv.get(idx[k][1]) : new float[] { 0, 0 };
					float[] nn = idx[k][2] >= 0 ? nrm.get(idx[k][2]) : new float[] { fn.x, fn.y, fn.z };
					out.add(p[0]); out.add(p[1]); out.add(p[2]);
					out.add(tc[0]); out.add(flipV ? 1f - tc[1] : tc[1]);
					out.add(nn[0]); out.add(nn[1]); out.add(nn[2]);
				}
			}
		}

		private static int index(String[] p, int i, int size) {
			if (p.length <= i || p[i].isEmpty()) return -1;
			int v = Integer.parseInt(p[i]);
			return v < 0 ? size + v : v - 1;
		}

		public void render() {
			if (data == null) load();
			if (!TGRenderState.isActive() || data.length == 0) return;
			VertexConsumer vc = TGRenderState.buffer();
			PoseStack.Pose pose = TGRenderState.pose().last();
			int light = TGRenderState.light();
			int overlay = TGRenderState.overlay();
			int color = TGRenderState.packedColor();
			for (int i = 0; i < data.length; i += 8) {
				vc.addVertex(pose, data[i], data[i + 1], data[i + 2]).setColor(color).setUv(data[i + 3], data[i + 4]).setOverlay(overlay)
						.setLight(light).setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
			}
		}
	}

	protected final Mesh[] meshes;

	public ModelObj(Mesh... meshes) {
		this.meshes = meshes;
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale,
			int ammoLeft, float reloadProgress, ItemDisplayContext transformType, int part, float fireProgress, float chargeProgress) {
		meshes[0].render();
	}

	/** 1.12 ModelBaseBakedGrenadeLauncher: the drum rotates when firing. */
	public static class GrenadeLauncher extends ModelObj {
		public GrenadeLauncher(Mesh body, Mesh drum) {
			super(body, drum);
		}

		@Override
		public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
				float scale, int ammoLeft, float reloadProgress, ItemDisplayContext transformType, int part, float fireProgress, float chargeProgress) {
			meshes[0].render();
			PoseStack ps = TGRenderState.pose();
			ps.pushPose();
			ps.translate(0, -2f, 0);
			ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(60.0f * fireProgress));
			ps.translate(0, 2f, 0);
			meshes[1].render();
			ps.popPose();
		}
	}
}
