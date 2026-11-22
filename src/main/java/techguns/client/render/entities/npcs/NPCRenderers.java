package techguns.client.render.entities.npcs;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import techguns.TGEntities;
import techguns.Techguns;
import techguns.api.guns.IGenericGun;
import techguns.entities.npcs.GenericNPC;
import techguns.entities.npcs.SuperMutantBasic;
import techguns.client.models.npcs.NPCModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;

/** Renderers of the humanoid Techguns NPCs (1.12 RenderGenericNPC and subclasses). */
public final class NPCRenderers {

	public static final ModelLayerLocation SKELETON_SOLDIER = new ModelLayerLocation(Techguns.rl("skeleton_soldier"), "main");

	private static final ResourceLocation ARMY_SOLDIER = tex("army_soldier");
	private static final ResourceLocation ZOMBIE_SOLDIER = tex("zombie_soldier");

	private NPCRenderers() {
	}

	private static ResourceLocation tex(String name) {
		return Techguns.rl("textures/entity/" + name + ".png");
	}

	public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(SKELETON_SOLDIER, SkeletonModel::createBodyLayer);
		event.registerLayerDefinition(NPCModels.SUPER_MUTANT, NPCModels::superMutant);
		event.registerLayerDefinition(NPCModels.CYBER_DEMON, NPCModels::cyberDemon);
	}

	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		humanoid(event, TGEntities.ZOMBIE_SOLDIER.get(), ZOMBIE_SOLDIER);
		humanoid(event, TGEntities.ZOMBIE_FARMER.get(), ZOMBIE_SOLDIER);
		humanoid(event, TGEntities.ZOMBIE_MINER.get(), ZOMBIE_SOLDIER);
		humanoid(event, TGEntities.ZOMBIE_POLICEMAN.get(), tex("zombie_policeman"));
		humanoid(event, TGEntities.ARMY_SOLDIER.get(), ARMY_SOLDIER);
		humanoid(event, TGEntities.BANDIT.get(), tex("bandit"));
		humanoid(event, TGEntities.COMMANDO.get(), ARMY_SOLDIER);
		humanoid(event, TGEntities.DICTATOR_DAVE.get(), tex("dictator"));
		humanoid(event, TGEntities.PSYCHO_STEVE.get(), ARMY_SOLDIER);
		humanoid(event, TGEntities.STORM_TROOPER.get(), ARMY_SOLDIER);
		humanoid(event, TGEntities.OUTCAST.get(), ARMY_SOLDIER);
		event.registerEntityRenderer(TGEntities.SKELETON_SOLDIER.get(),
				ctx -> new Renderer<>(ctx, new NPCModel<>(ctx.bakeLayer(SKELETON_SOLDIER)), ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png"),
						ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR));
		event.registerEntityRenderer(TGEntities.ZOMBIE_PIGMAN_SOLDIER.get(),
				ctx -> new Renderer<>(ctx, new NPCPiglinModel<>(ctx.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN)),
						ResourceLocation.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png"), ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR,
						ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR));
		event.registerEntityRenderer(TGEntities.DUMMY_SPAWN.get(), NoopRenderer::new);
		event.registerEntityRenderer(TGEntities.CYBER_DEMON.get(),
				ctx -> new Renderer<>(ctx, new NPCModel<>(ctx.bakeLayer(NPCModels.CYBER_DEMON)), tex("cyberdemon"), null, null));
		superMutant(event, TGEntities.SUPER_MUTANT_BASIC.get());
		superMutant(event, TGEntities.SUPER_MUTANT_HEAVY.get());
		superMutant(event, TGEntities.SUPER_MUTANT_ELITE.get());
	}

	private static final ResourceLocation[] SUPER_MUTANT_TEXTURES = { tex("supermutant_texture_1"), tex("supermutant_texture_2"), tex("supermutant_texture_3") };

	private static <T extends SuperMutantBasic> void superMutant(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type) {
		event.registerEntityRenderer(type, ctx -> new Renderer<T, NPCModels.SuperMutantModel<T>>(ctx, new NPCModels.SuperMutantModel<>(ctx.bakeLayer(NPCModels.SUPER_MUTANT)),
				SUPER_MUTANT_TEXTURES[0], null, null) {
			@Override
			public ResourceLocation getTextureLocation(T entity) {
				return SUPER_MUTANT_TEXTURES[entity.getMutantType()];
			}

			@Override
			public void render(T entity, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light) {
				ps.pushPose();
				ps.translate(0, entity.getModelHeightOffset(), 0);
				super.render(entity, yaw, partialTicks, ps, buffers, light);
				ps.popPose();
			}
		});
	}

	private static <T extends GenericNPC> void humanoid(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type, ResourceLocation texture) {
		event.registerEntityRenderer(type, ctx -> new Renderer<>(ctx, new NPCModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), texture,
				ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR));
	}

	/** guns are held aimed (bow pose), other items normally */
	static <T extends GenericNPC> void setArmPoses(HumanoidModel<T> model, T npc) {
		ItemStack main = npc.getMainHandItem();
		HumanoidModel.ArmPose pose = main.isEmpty() || !npc.hasWeaponArmPose() ? HumanoidModel.ArmPose.EMPTY
				: main.getItem() instanceof IGenericGun ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.ITEM;
		if (npc.getMainArm() == HumanoidArm.RIGHT) {
			model.rightArmPose = pose;
			model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
		} else {
			model.leftArmPose = pose;
			model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
		}
	}

	public static class NPCModel<T extends GenericNPC> extends HumanoidModel<T> {
		public NPCModel(ModelPart root) {
			super(root);
		}

		@Override
		public void prepareMobModel(T npc, float limbSwing, float limbSwingAmount, float partialTick) {
			setArmPoses(this, npc);
			super.prepareMobModel(npc, limbSwing, limbSwingAmount, partialTick);
		}
	}

	public static class NPCPiglinModel<T extends GenericNPC> extends PiglinModel<T> {
		public NPCPiglinModel(ModelPart root) {
			super(root);
		}

		@Override
		public void prepareMobModel(T npc, float limbSwing, float limbSwingAmount, float partialTick) {
			setArmPoses(this, npc);
			super.prepareMobModel(npc, limbSwing, limbSwingAmount, partialTick);
		}
	}

	/** held items, moved by the NPC's weapon position (1.12 LayerHeldItemTranslateGun) */
	public static class GunInHandLayer<T extends GenericNPC, M extends HumanoidModel<T>> extends ItemInHandLayer<T, M> {
		private final ItemInHandRenderer itemRenderer;

		public GunInHandLayer(RenderLayerParent<T, M> parent, ItemInHandRenderer itemRenderer) {
			super(parent, itemRenderer);
			this.itemRenderer = itemRenderer;
		}

		@Override
		protected void renderArmWithItem(LivingEntity ent, ItemStack stack, ItemDisplayContext ctx, HumanoidArm arm, PoseStack ps, MultiBufferSource buffers,
				int light) {
			if (stack.isEmpty()) {
				return;
			}
			ps.pushPose();
			this.getParentModel().translateToHand(arm, ps);
			ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
			ps.mulPose(Axis.YP.rotationDegrees(180.0F));
			boolean left = arm == HumanoidArm.LEFT;
			ps.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
			if (ent instanceof GenericNPC npc) {
				ps.translate(left ? -npc.getWeaponPosX() : npc.getWeaponPosX(), npc.getWeaponPosY(), npc.getWeaponPosZ());
			}
			this.itemRenderer.renderItem(ent, stack, ctx, left, ps, buffers, light);
			ps.popPose();
		}
	}

	public static class Renderer<T extends GenericNPC, M extends HumanoidModel<T>> extends HumanoidMobRenderer<T, M> {
		private final ResourceLocation texture;

		/** armor layers may be null for models without armor */
		public Renderer(EntityRendererProvider.Context ctx, M model, ResourceLocation texture, ModelLayerLocation innerArmor, ModelLayerLocation outerArmor) {
			super(ctx, model, 0.5f);
			this.texture = texture;
			if (innerArmor != null) {
				this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(ctx.bakeLayer(innerArmor)), new HumanoidArmorModel<>(ctx.bakeLayer(outerArmor)),
						ctx.getModelManager()));
			}
			this.layers.removeIf(l -> l instanceof ItemInHandLayer);
			this.addLayer(new GunInHandLayer<>(this, ctx.getItemInHandRenderer()));
		}

		@Override
		public ResourceLocation getTextureLocation(T entity) {
			return texture;
		}
	}
}
