package techguns.client.render.entities.npcs;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import techguns.TGEntities;
import techguns.Techguns;
import techguns.client.models.ModelObj;
import techguns.client.render.gl.TGRenderState;
import techguns.entities.npcs.AlienBug;
import techguns.entities.npcs.AttackHelicopter;
import techguns.entities.npcs.Ghastling;

/** Models and renderers of the non humanoid Techguns mobs: alien bug, ghastling, attack helicopter. */
public final class CustomMobRenderers {

	public static final ModelLayerLocation ALIEN_BUG = new ModelLayerLocation(Techguns.rl("alienbug"), "main");
	public static final ModelLayerLocation GHASTLING = new ModelLayerLocation(Techguns.rl("ghastling"), "main");

	private CustomMobRenderers() {
	}

	public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ALIEN_BUG, AlienBugModel::createLayer);
		event.registerLayerDefinition(GHASTLING, GhastlingModel::createLayer);
	}

	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TGEntities.ALIEN_BUG.get(), AlienBugRenderer::new);
		event.registerEntityRenderer(TGEntities.GHASTLING.get(), GhastlingRenderer::new);
		event.registerEntityRenderer(TGEntities.ATTACK_HELICOPTER.get(), HelicopterRenderer::new);
	}

	/* ------------------------------------------------------------------ alien bug */

	public static class AlienBugModel extends HierarchicalModel<AlienBug> {
		private static final float LEG_Y = 2.792527F, LEG_Y2 = 0.349066F, LEG_Z = 0.523599F;
		private final ModelPart root, h1, jawU, jawL, l0, l1, l2, l3;

		public AlienBugModel(ModelPart root) {
			this.root = root;
			this.h1 = root.getChild("h1");
			this.jawU = h1.getChild("jaw_u");
			this.jawL = h1.getChild("jaw_l");
			this.l0 = root.getChild("l1_a");
			this.l1 = root.getChild("l1_a_1");
			this.l2 = root.getChild("l1_a_2");
			this.l3 = root.getChild("l1_a_3");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition r = mesh.getRoot();
			r.addOrReplaceChild("b1", CubeListBuilder.create().texOffs(0, 33).addBox(0, 0, 0, 5, 5, 9), PartPose.offset(-2.5F, 14, -2));
			r.addOrReplaceChild("b2", CubeListBuilder.create().texOffs(23, 33).addBox(0, 0, 0, 6, 4, 4), PartPose.offsetAndRotation(-3, 13, 6, 0.59184F, 0, 0));
			r.addOrReplaceChild("b3", CubeListBuilder.create().texOffs(43, 31).addBox(0, 0, 0, 4, 3, 4), PartPose.offsetAndRotation(-2, 11, 10, 0.22759F, 0, 0));
			r.addOrReplaceChild("b4", CubeListBuilder.create().texOffs(56, 30).addBox(0, 0, 0, 2, 2, 2), PartPose.offset(-1, 10.5F, 14.2F));
			PartDefinition h1 = r.addOrReplaceChild("h1", CubeListBuilder.create().texOffs(20, 9).addBox(-2.5F, -2.5F, -3, 5, 5, 6),
					PartPose.offsetAndRotation(0, 11.5F, -1, 0.13666F, 0, 0));
			h1.addOrReplaceChild("h2", CubeListBuilder.create().texOffs(36, 18).addBox(0, 0, -7, 6, 3, 8), PartPose.offsetAndRotation(-3, -6.5F, 3.5F, 0.349066F, 0, 0));
			h1.addOrReplaceChild("h3", CubeListBuilder.create().texOffs(22, 0).addBox(-4, 1, -7, 14, 0, 14), PartPose.offsetAndRotation(-3, -6.5F, 3.5F, 0.349066F, 0, 0));
			h1.addOrReplaceChild("jaw_u", CubeListBuilder.create().texOffs(2, 15).addBox(0, -2.5F, -10, 4, 4, 10), PartPose.offsetAndRotation(-2, -1, -2, -0.182038F, 0, 0));
			h1.addOrReplaceChild("jaw_l", CubeListBuilder.create().texOffs(21, 20).addBox(0, 0, -9, 3, 4, 9), PartPose.offsetAndRotation(-1.5F, -1.5F, -2.5F, 0.455356F, 0, 0));
			leg(r, "l1_a", -1.5F, 0, LEG_Y, LEG_Z);
			leg(r, "l1_a_1", 1.5F, 4, -LEG_Y2, -LEG_Z);
			leg(r, "l1_a_2", -1.5F, 4, -LEG_Y, LEG_Z);
			leg(r, "l1_a_3", 1.5F, 0, LEG_Y2, -LEG_Z);
			return LayerDefinition.create(mesh, 64, 64);
		}

		private static void leg(PartDefinition r, String name, float x, float z, float ry, float rz) {
			PartDefinition a = r.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 4).addBox(0, -1.5F, -1.5F, 9, 3, 3),
					PartPose.offsetAndRotation(x, 17, z, 0, ry, rz));
			a.addOrReplaceChild("b", CubeListBuilder.create().texOffs(0, 0).addBox(0, -1, -1, 11, 2, 2), PartPose.offsetAndRotation(7.5F, 0, 0, 0, 0, 1.745329F));
		}

		@Override
		public ModelPart root() {
			return root;
		}

		@Override
		public void setupAnim(AlienBug bug, float limbSwing, float amount, float age, float headYaw, float headPitch) {
			h1.yRot = headYaw * Mth.DEG_TO_RAD;
			h1.xRot = headPitch * Mth.DEG_TO_RAD;
			l0.setRotation(0, LEG_Y, LEG_Z);
			l1.setRotation(0, -LEG_Y2, -LEG_Z);
			l2.setRotation(0, -LEG_Y, LEG_Z);
			l3.setRotation(0, LEG_Y2, -LEG_Z);
			float f9 = -Mth.cos(limbSwing * 0.6662F * 2.0F) * 0.4F * amount;
			float f10 = -Mth.cos(limbSwing * 0.6662F * 2.0F + Mth.PI) * 0.4F * amount;
			float f11 = -Mth.cos(limbSwing * 0.6662F * 3.0F) * 0.4F * amount;
			float f12 = -Mth.cos(limbSwing * 0.6662F * 4.0F + Mth.PI) * 0.4F * amount;
			l0.yRot += f9 * 1.25f;
			l3.yRot -= f9 * 1.25f;
			l2.yRot += f10 * 1.25f;
			l1.yRot -= f10 * 1.25f;
			l0.xRot -= f9;
			l3.xRot += f9;
			l2.xRot -= f10;
			l1.xRot += f10;
			l0.zRot -= f11 * 0.75f;
			l3.zRot += f11 * 0.75f;
			l2.zRot -= f12 * 0.75f;
			l1.zRot += f12 * 0.75f;
			jawL.xRot = 0.455356F - Mth.cos(age * 0.2F) * 0.1F;
			jawU.xRot = -0.182038F + Mth.cos(age * 0.1F) * 0.1F;
			int a = bug.getAttackTimer();
			if (a > 5 && a <= 10) {
				float f = (a - 5) / 5.0f;
				float bite = (float) (0.5 * (1 - Math.cos(f * Math.PI)));
				jawU.xRot += bite * 0.3F;
				jawL.xRot -= bite * 0.4F;
			}
		}
	}

	public static class AlienBugRenderer extends MobRenderer<AlienBug, AlienBugModel> {
		private static final ResourceLocation TEXTURE = Techguns.rl("textures/entity/alienbug.png");

		public AlienBugRenderer(EntityRendererProvider.Context ctx) {
			super(ctx, new AlienBugModel(ctx.bakeLayer(ALIEN_BUG)), 0.8f);
		}

		@Override
		public ResourceLocation getTextureLocation(AlienBug entity) {
			return TEXTURE;
		}
	}

	/* ------------------------------------------------------------------ ghastling */

	/** small ghast: 1.12 ModelGhastling (rendered 0.6 blocks higher, baked into the pivots) */
	public static class GhastlingModel extends HierarchicalModel<Ghastling> {
		private final ModelPart root;
		private final ModelPart[] tentacles = new ModelPart[9];

		public GhastlingModel(ModelPart root) {
			this.root = root;
			for (int i = 0; i < tentacles.length; i++) {
				tentacles[i] = root.getChild("tentacle" + i);
			}
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition r = mesh.getRoot();
			float up = -9.6F;
			r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8, -8, -8, 16, 16, 16), PartPose.offset(0, 8 + up, 0));
			Random random = new Random(1660L);
			for (int j = 0; j < 9; ++j) {
				float x = (((j % 3) - (j / 3 % 2) * 0.5F + 0.25F) / 2.0F * 2.0F - 1.0F) * 5.0F;
				float z = ((j / 3) / 2.0F * 2.0F - 1.0F) * 5.0F;
				int k = random.nextInt(7) + 8;
				r.addOrReplaceChild("tentacle" + j, CubeListBuilder.create().texOffs(0, 0).addBox(-1, 0, -1, 2, k, 2), PartPose.offset(x, 15 + up, z));
			}
			return LayerDefinition.create(mesh, 64, 32);
		}

		@Override
		public ModelPart root() {
			return root;
		}

		@Override
		public void setupAnim(Ghastling entity, float limbSwing, float amount, float age, float headYaw, float headPitch) {
			for (int i = 0; i < tentacles.length; ++i) {
				tentacles[i].xRot = 0.2F * Mth.sin(age * 0.3F + i) + 0.4F;
			}
		}
	}

	public static class GhastlingRenderer extends MobRenderer<Ghastling, GhastlingModel> {
		private static final ResourceLocation GHAST = ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast.png");
		private static final ResourceLocation GHAST_SHOOTING = ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast_shooting.png");

		public GhastlingRenderer(EntityRendererProvider.Context ctx) {
			super(ctx, new GhastlingModel(ctx.bakeLayer(GHASTLING)), 0.25f);
		}

		@Override
		public ResourceLocation getTextureLocation(Ghastling entity) {
			return entity.isAttacking() ? GHAST_SHOOTING : GHAST;
		}
	}

	/* ------------------------------------------------------------------ attack helicopter */

	/** OBJ helicopter: hull, spinning rotor, pitching gun; spins down when dying */
	public static class HelicopterRenderer extends EntityRenderer<AttackHelicopter> {
		private static final ResourceLocation TEXTURE = Techguns.rl("textures/entity/apache.png");
		private static final ModelObj.Mesh MAIN = new ModelObj.Mesh("models/item/npc/helicopter0.obj", true);
		private static final ModelObj.Mesh ROTOR = new ModelObj.Mesh("models/item/npc/helicopter1.obj", true);
		private static final ModelObj.Mesh GUN = new ModelObj.Mesh("models/item/npc/helicopter2.obj", true);

		public HelicopterRenderer(EntityRendererProvider.Context ctx) {
			super(ctx);
			this.shadowRadius = 5.0f;
		}

		@Override
		public void render(AttackHelicopter entity, float entityYaw, float ptt, PoseStack ps, MultiBufferSource buffers, int light) {
			ps.pushPose();
			ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(ptt, entity.yRotO, entity.getYRot()) - 90f));
			ps.scale(2.5f, 2.5f, 2.5f);
			float rotorTime = (entity.level().getGameTime() % 60) + ptt;
			TGRenderState.begin(ps, buffers, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
			try {
				TGRenderState.bindTexture(TEXTURE);
				if (entity.deathTime == 0) {
					MAIN.render();
					ps.pushPose();
					ps.mulPose(Axis.YP.rotationDegrees(rotorTime * 24.0f));
					ROTOR.render();
					ps.popPose();
					ps.pushPose();
					float offsetX = 1.2f;
					ps.translate(offsetX, 0, 0);
					ps.mulPose(Axis.ZP.rotationDegrees(-Mth.lerp(ptt, entity.xRotO, entity.getXRot())));
					ps.translate(-offsetX, 0, 0);
					GUN.render();
					ps.popPose();
				} else {
					float p = (float) Math.pow((entity.deathTime + ptt) / AttackHelicopter.MAX_DEATH_TIME, 2);
					ps.translate(0, p * -4f, 0);
					ps.mulPose(Axis.YP.rotationDegrees(p * 720.0f * 2f));
					MAIN.render();
					GUN.render();
					ps.mulPose(Axis.YP.rotationDegrees(rotorTime * 12.0f));
					ROTOR.render();
				}
			} finally {
				TGRenderState.end();
			}
			ps.popPose();
			super.render(entity, entityYaw, ptt, ps, buffers, light);
		}

		@Override
		public ResourceLocation getTextureLocation(AttackHelicopter entity) {
			return TEXTURE;
		}
	}
}
