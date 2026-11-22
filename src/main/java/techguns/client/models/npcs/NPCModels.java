package techguns.client.models.npcs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import techguns.Techguns;
import techguns.client.render.entities.npcs.NPCRenderers;
import techguns.entities.npcs.SuperMutantBasic;

/** Layer definitions of the custom humanoid NPC models (1.12 ModelSuperMutant, ModelCyberDemon). */
public final class NPCModels {

	public static final ModelLayerLocation SUPER_MUTANT = new ModelLayerLocation(Techguns.rl("super_mutant"), "main");
	public static final ModelLayerLocation CYBER_DEMON = new ModelLayerLocation(Techguns.rl("cyber_demon"), "main");

	private NPCModels() {
	}

	private static CubeListBuilder box(int u, int v, boolean mirror, float x, float y, float z, float w, float h, float d) {
		return CubeListBuilder.create().texOffs(u, v).mirror(mirror).addBox(x, y, z, w, h, d);
	}

	public static LayerDefinition superMutant() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", box(0, 0, false, -4, -8, -4, 8, 8, 8), PartPose.ZERO);
		root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.5f)), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body", box(16, 16, false, -4, 0, -2, 8, 12, 4), PartPose.ZERO);
		body.addOrReplaceChild("chest_front", box(21, 34, false, -4, 0, -2, 6, 7, 1), PartPose.offset(1, 1, -1));
		body.addOrReplaceChild("chest_back", box(21, 44, false, -4, 0, -2, 6, 7, 1), PartPose.offset(1, 1, 4));
		body.addOrReplaceChild("belt", box(0, 53, false, 0, 0, 0, 9, 4, 5), PartPose.offset(-4.5f, 9, -2.5f));
		PartDefinition ra = root.addOrReplaceChild("right_arm", box(40, 16, false, -3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
		ra.addOrReplaceChild("upper", box(40, 33, false, -3, -2, -2, 5, 7, 5), PartPose.offset(-0.9f, -0.2f, -0.5f));
		PartDefinition la = root.addOrReplaceChild("left_arm", box(40, 16, true, -1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
		la.addOrReplaceChild("upper", box(40, 33, true, -1, -2, -2, 5, 7, 5), PartPose.offset(-0.1f, -0.2f, -0.5f));
		PartDefinition rl = root.addOrReplaceChild("right_leg", box(0, 16, false, -2, 0, -2, 4, 12, 4), PartPose.offset(-1.9f, 11, 0));
		rl.addOrReplaceChild("boot", box(0, 33, false, -2, 0, -2, 5, 6, 5), PartPose.offset(-0.6f, 6, -0.7f));
		PartDefinition ll = root.addOrReplaceChild("left_leg", box(0, 16, true, -2, 0, -2, 4, 12, 4), PartPose.offset(1.9f, 11, 0));
		ll.addOrReplaceChild("boot", box(0, 33, true, -2, 0, -2, 5, 6, 5), PartPose.offset(-0.4f, 6, -0.7f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	public static LayerDefinition cyberDemon() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		head.addOrReplaceChild("skull", box(0, 0, false, -4, -8, -3, 8, 8, 7), PartPose.ZERO);
		head.addOrReplaceChild("h2", box(48, 0, false, -3, -5, -5, 6, 5, 2), PartPose.ZERO);
		head.addOrReplaceChild("h3", box(52, 7, false, 4, -7, -2, 2, 4, 4), PartPose.ZERO);
		head.addOrReplaceChild("h4", box(30, 8, false, -1, 0, -5, 2, 2, 5), PartPose.offsetAndRotation(-7.5f, -5.5f, -1, -0.4363323f, 0, 0));
		head.addOrReplaceChild("h5", box(52, 7, false, -6, -7, -2, 2, 4, 4), PartPose.ZERO);
		head.addOrReplaceChild("h6", box(24, 0, false, 0, -1.5f, -3, 4, 3, 3), PartPose.offsetAndRotation(6, -5, 1.5f, 0, 0.6981317f, 0));
		head.addOrReplaceChild("h7", box(24, 0, false, -4, -1.5f, -3, 4, 3, 3), PartPose.offsetAndRotation(-6, -5, 1.5f, 0, -0.6981317f, 0));
		head.addOrReplaceChild("h8", box(30, 8, false, -1, 0, -5, 2, 2, 5), PartPose.offsetAndRotation(7.5f, -5.5f, -1, -0.4363323f, 0, 0));
		root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
		body.addOrReplaceChild("torso", box(0, 15, false, -6, 0, -3, 12, 7, 6), PartPose.ZERO);
		body.addOrReplaceChild("b2", box(0, 28, false, -4.5f, 7, -2.5f, 9, 5, 5), PartPose.ZERO);
		PartDefinition ra = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2, 0));
		ra.addOrReplaceChild("upper", box(44, 16, false, -6, -2, -2.5f, 5, 7, 5), PartPose.ZERO);
		ra.addOrReplaceChild("lower", box(48, 28, false, -5.5f, 3, 0.5f, 4, 8, 4), PartPose.rotation(-0.5205006f, 0, 0));
		PartDefinition la = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5, 2, 0));
		la.addOrReplaceChild("upper", box(44, 16, false, 1, -2, -2.5f, 5, 7, 5), PartPose.ZERO);
		la.addOrReplaceChild("lower", box(48, 28, false, 1.5f, 3, 0.5f, 4, 8, 4), PartPose.rotation(-0.5205006f, 0, 0));
		PartDefinition rl = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12, 0));
		rl.addOrReplaceChild("upper", box(0, 38, false, -3.6f, -1, -2.5f, 5, 7, 5), PartPose.rotation(-0.5235988f, 0, 0));
		rl.addOrReplaceChild("lower", box(20, 39, false, -3.1f, 3, 1.5f, 4, 7, 4), PartPose.rotation(-0.5235988f, 0, 0));
		rl.addOrReplaceChild("foot", box(0, 50, false, -3.6f, 9, -4, 5, 3, 5), PartPose.ZERO);
		PartDefinition ll = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12, 0));
		ll.addOrReplaceChild("upper", box(0, 38, false, -1.4f, -1, -2.5f, 5, 7, 5), PartPose.rotation(-0.5235988f, 0, 0));
		ll.addOrReplaceChild("lower", box(20, 39, false, -0.9f, 3, 1.5f, 4, 7, 4), PartPose.rotation(-0.5235988f, 0, 0));
		ll.addOrReplaceChild("foot", box(0, 50, false, -1.4f, 9, -4, 5, 3, 5), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** super mutant: the body model is drawn scaled around the model origin, the held gun is not (as in 1.12) */
	public static class SuperMutantModel<T extends SuperMutantBasic> extends NPCRenderers.NPCModel<T> {
		private float scale = 1f;

		public SuperMutantModel(ModelPart root) {
			super(root);
		}

		@Override
		public void prepareMobModel(T npc, float limbSwing, float limbSwingAmount, float partialTick) {
			this.scale = npc.getModelScale();
			super.prepareMobModel(npc, limbSwing, limbSwingAmount, partialTick);
		}

		@Override
		public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
			ps.pushPose();
			ps.scale(scale, scale, scale);
			super.renderToBuffer(ps, vc, light, overlay, color);
			ps.popPose();
		}
	}
}
