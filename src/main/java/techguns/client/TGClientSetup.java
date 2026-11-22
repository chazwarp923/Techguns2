package techguns.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import techguns.TGEntities;
import techguns.TGuns;
import techguns.Techguns;
import techguns.client.render.TGItemRenderer;
import techguns.client.render.entities.projectiles.RenderGenericProjectile;

/** Client mod-bus setup (was ClientProxy). */
@EventBusSubscriber(modid = Techguns.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TGClientSetup {

	@SubscribeEvent
	public static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
		techguns.radiation.RadiationHud.register(event);
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(techguns.TGFluids.ACID.get(), net.minecraft.client.renderer.RenderType.translucent());
			net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(techguns.TGFluids.ACID_FLOWING.get(), net.minecraft.client.renderer.RenderType.translucent());
			TGGunRenderers.init();
			techguns.client.render.armor.TGArmorModels.initItemRenderers();
			initShields();
			initCurioRenderers();
		});
		techguns.util.TGFXHooks.setHandler(ClientFX.INSTANCE);
		techguns.util.SoundUtil.setClientPlayer((ent, sound, volume, pitch, repeat, moving, gunPosition, category, condition) -> net.minecraft.client.Minecraft
				.getInstance().getSoundManager().play(new techguns.client.audio.TGSound(sound, ent, volume, pitch, repeat, moving, gunPosition, category, condition)));
	}

	private static net.minecraft.resources.ResourceLocation shieldTex(String name) {
		return Techguns.rl("textures/armors/" + name + ".png");
	}

	private static void initCurioRenderers() {
		curio(techguns.TGItems.GAS_MASK, () -> new techguns.client.models.armor.ModelGasMask(), "gasmask", 1);
		curio(techguns.TGItems.GLIDER, () -> new techguns.client.models.armor.ModelGlider(), "glider", 1);
		curio(techguns.TGItems.JUMPPACK, () -> new techguns.client.models.armor.ModelJetPack(1), "jetpack", 4);
		curio(techguns.TGItems.JETPACK, () -> new techguns.client.models.armor.ModelJetPack(0), "jetpack", 4);
		curio(techguns.TGItems.SCUBA_TANKS, () -> new techguns.client.models.armor.ModelOxygenTanks(), "oxygentanks", 1);
		curio(techguns.TGItems.NIGHTVISION_GOGGLES, () -> new techguns.client.models.armor.ModelNightVisionGoggles(), "nightvisiongoggles", 1);
		curio(techguns.TGItems.ANTI_GRAV_PACK, () -> new techguns.client.models.armor.ModelAntiGravPack(), "antigravpack", 5);
		curio(techguns.TGItems.TACTICAL_MASK, () -> new techguns.client.models.armor.ModelFaceMask(true), "tacticalmask", 4);
		curio(techguns.TGItems.OXYGEN_MASK, () -> new techguns.client.models.armor.ModelFaceMask(true), "oxygenmask", 1);
		top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(techguns.TGItems.WORKING_GLOVES.get(),
				() -> new techguns.client.render.armor.TGCurioRenderer(new techguns.client.models.armor.ModelGloves(0.45f, false),
						new techguns.client.models.armor.ModelGloves(0.45f, true),
						new net.minecraft.resources.ResourceLocation[] { Techguns.rl("textures/models/armor/working_gloves.png") },
						new net.minecraft.resources.ResourceLocation[] { Techguns.rl("textures/models/armor/working_gloves_slim.png") }));
	}

	private static void curio(java.util.function.Supplier<? extends net.minecraft.world.item.Item> item,
			java.util.function.Supplier<techguns.client.models.armor.ModelAdditionalSlotBase> model, String texture, int camos) {
		top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(item.get(),
				() -> new techguns.client.render.armor.TGCurioRenderer(model.get(),
						techguns.client.render.armor.TGCurioRenderer.camoTextures(texture, camos)));
	}

	private static void initShields() {
		var blocking = net.minecraft.resources.ResourceLocation.withDefaultNamespace("blocking");
		for (var s : techguns.TGArmors.SHIELDS) {
			net.minecraft.client.renderer.item.ItemProperties.register(s.get(), blocking,
					(stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
		}
		techguns.client.render.item.ShieldItemRenderer.register(techguns.TGArmors.RIOT_SHIELD.get(), new techguns.client.models.armor.ModelRiotShield(),
				shieldTex("riot_shield"), shieldTex("riot_shield_black"), shieldTex("riot_shield_darkgreen"), shieldTex("riot_shield_grey"));
		techguns.client.render.item.ShieldItemRenderer.register(techguns.TGArmors.BALLISTIC_SHIELD.get(),
				new techguns.client.models.armor.ModelBallisticShield(), shieldTex("ballistic_shield"), shieldTex("ballistic_shield_black"),
				shieldTex("ballistic_shield_green"), shieldTex("ballistic_shield_grey"));
		techguns.client.render.item.ShieldItemRenderer.register(techguns.TGArmors.ADVANCED_SHIELD.get(),
				new techguns.client.models.armor.ModelAdvancedShield(), shieldTex("advanced_shield"), shieldTex("advanced_shield_silver"),
				shieldTex("advanced_shield_green"));
	}

	@SubscribeEvent
	public static void registerReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(techguns.client.particle.TGFX.RELOAD_LISTENER);
	}

	@SubscribeEvent
	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		IClientItemExtensions ext = new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return TGItemRenderer.get();
			}
		};
		IClientItemExtensions gunExt = new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return TGItemRenderer.get();
			}

			/** 1.12 TGEventHandler: guns are held aimed, the off hand gun only while shooting it */
			@Override
			public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity ent, net.minecraft.world.InteractionHand hand,
					net.minecraft.world.item.ItemStack stack) {
				if (stack.getItem() instanceof techguns.items.guns.GenericGun gun && gun.hasBowAnim()) {
					if (hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
						return net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
					}
					boolean mainGun = ent.getMainHandItem().getItem() instanceof techguns.items.guns.GenericGun g && g.hasBowAnim();
					if (!mainGun && techguns.client.ShooterValues.getIsCurrentlyUsingGun(ent, true)) {
						return net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
					}
				}
				return null;
			}
		};
		TGuns.GUNS.forEach(g -> event.registerItem(gunExt, g.get()));
		event.registerItem(ext, TGuns.STIELGRANATE.get(), TGuns.FRAGGRENADE.get());
		techguns.TGArmors.ARMORS.forEach(a -> {
			if (techguns.client.render.armor.TGArmorModels.hasItem3d(a.getId().getPath())) {
				event.registerItem(techguns.client.render.armor.TGArmorModels.ARMOR_EXTENSION_3D, a.get());
			} else if (a.get().getArmorModelKey() != null) {
				event.registerItem(techguns.client.render.armor.TGArmorModels.ARMOR_EXTENSION, a.get());
			}
		});
		IClientItemExtensions shieldExt = new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return techguns.client.render.item.ShieldItemRenderer.get();
			}
		};
		techguns.TGArmors.SHIELDS.forEach(s -> event.registerItem(shieldExt, s.get()));
		for (String name : TGGunRenderers.SPECIAL_ITEMS) {
			event.registerItem(ext, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Techguns.rl(name)));
		}
		techguns.client.render.tileentities.TGMachineRenderers.registerItems(event);
		event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
			@Override
			public net.minecraft.resources.ResourceLocation getStillTexture() {
				return Techguns.rl("blocks/acid_still");
			}

			@Override
			public net.minecraft.resources.ResourceLocation getFlowingTexture() {
				return Techguns.rl("blocks/acid_flow");
			}
		}, techguns.TGFluids.ACID_TYPE.get());
	}

	@SubscribeEvent
	public static void registerAdditionalModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
		techguns.client.render.tileentities.RenderDoor3x3.registerModels(event);
	}

	@SubscribeEvent
	public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
		techguns.client.render.tileentities.TGMachineRenderers.registerScreens(event);
	}

	@SubscribeEvent
	public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		techguns.client.render.entities.npcs.NPCRenderers.registerLayers(event);
		techguns.client.render.entities.npcs.CustomMobRenderers.registerLayers(event);
	}

	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		techguns.client.render.entities.npcs.NPCRenderers.registerRenderers(event);
		techguns.client.render.entities.npcs.CustomMobRenderers.registerRenderers(event);
		techguns.client.render.tileentities.TGMachineRenderers.registerRenderers(event);
		event.registerEntityRenderer(TGEntities.GENERIC_PROJECTILE.get(), RenderGenericProjectile::new);
		event.registerEntityRenderer(TGEntities.RADIATION.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
		event.registerEntityRenderer(TGEntities.GENERIC_PROJECTILE_INCENDIARY.get(), RenderGenericProjectile::new);
		event.registerEntityRenderer(TGEntities.GENERIC_PROJECTILE_EXPLOSIVE.get(), RenderGenericProjectile::new);
		event.registerEntityRenderer(TGEntities.STONE_BULLET.get(), ctx -> new RenderGenericProjectile<>(ctx, RenderGenericProjectile.STONE_BULLET));
		event.registerEntityRenderer(TGEntities.ADVANCED_BULLET.get(), ctx -> new RenderGenericProjectile<>(ctx, RenderGenericProjectile.BULLET_BLUE));
		event.registerEntityRenderer(TGEntities.GAUSS.get(), ctx -> new RenderGenericProjectile<>(ctx, RenderGenericProjectile.BULLET_BLUE));
		event.registerEntityRenderer(TGEntities.BLASTER.get(), ctx -> new RenderGenericProjectile<>(ctx, RenderGenericProjectile.LASER).setAdditive());
		event.registerEntityRenderer(TGEntities.LASER.get(), techguns.client.render.entities.projectiles.RenderLaserProjectile::new);
		event.registerEntityRenderer(TGEntities.TESLA.get(), techguns.client.render.entities.projectiles.RenderTeslaProjectile::new);
		event.registerEntityRenderer(TGEntities.NDR.get(), techguns.client.render.entities.projectiles.RenderNDRProjectile::new);
		event.registerEntityRenderer(TGEntities.SONIC_SHOTGUN.get(), techguns.client.render.entities.projectiles.RenderSonicShotgunProjectile::new);
		event.registerEntityRenderer(TGEntities.ROCKET.get(), techguns.client.render.entities.projectiles.RenderRocketProjectile::new);
		event.registerEntityRenderer(TGEntities.ROCKET_HV.get(), techguns.client.render.entities.projectiles.RenderRocketProjectile::new);
		event.registerEntityRenderer(TGEntities.ROCKET_NUKE.get(), techguns.client.render.entities.projectiles.RenderRocketProjectile::new);
		event.registerEntityRenderer(TGEntities.GRENADE_40MM.get(), techguns.client.render.entities.projectiles.RenderGrenade40mmProjectile::new);
		event.registerEntityRenderer(TGEntities.FLAMETHROWER.get(), techguns.client.render.entities.projectiles.RenderInvisibleProjectile::new);
		event.registerEntityRenderer(TGEntities.ALIEN_BLASTER.get(), techguns.client.render.entities.projectiles.RenderInvisibleProjectile::new);
		event.registerEntityRenderer(TGEntities.DEATOMIZER.get(), techguns.client.render.entities.projectiles.RenderInvisibleProjectile::new);
		event.registerEntityRenderer(TGEntities.CYBERDEMON_BLASTER.get(), techguns.client.render.entities.projectiles.RenderInvisibleProjectile::new);
		event.registerEntityRenderer(TGEntities.BIOGUN.get(), techguns.client.render.entities.projectiles.RenderBioGunProjectile::new);
		event.registerEntityRenderer(TGEntities.TFG.get(), techguns.client.render.entities.projectiles.RenderInvisibleProjectile::new);
		event.registerEntityRenderer(TGEntities.POWERHAMMER.get(), techguns.client.render.entities.projectiles.RenderInvisibleProjectile::new);
		event.registerEntityRenderer(TGEntities.CHAINSAW.get(), techguns.client.render.entities.projectiles.RenderInvisibleProjectile::new);
		event.registerEntityRenderer(TGEntities.GUIDED_MISSILE.get(), techguns.client.render.entities.projectiles.RenderRocketProjectile::new);
		event.registerEntityRenderer(TGEntities.GUIDED_MISSILE_HV.get(), techguns.client.render.entities.projectiles.RenderRocketProjectile::new);
		event.registerEntityRenderer(TGEntities.GRENADE.get(), ctx -> new techguns.client.render.entities.projectiles.RenderGrenadeProjectile<>(ctx,
				new techguns.client.models.guns.ModelStielgranate(), Techguns.rl("textures/guns/stielgranate.png")));
		event.registerEntityRenderer(TGEntities.FRAG_GRENADE.get(), ctx -> new techguns.client.render.entities.projectiles.RenderGrenadeProjectile<>(ctx,
				new techguns.client.models.guns.ModelFragGrenade(false), Techguns.rl("textures/guns/frag_grenade_texture.png")));
	}
}
