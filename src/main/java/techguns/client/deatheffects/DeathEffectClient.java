package techguns.client.deatheffects;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import techguns.TGConfig;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.client.particle.TGFX;
import techguns.client.particle.TGFXType;
import techguns.client.particle.TGParticleManager;
import techguns.client.particle.TGParticleSystem;
import techguns.client.particle.TGParticleSystemType;
import techguns.deatheffects.DeathType;
import techguns.packets.PacketEntityDeathType;
import techguns.util.TGFXHooks;

/**
 * Client half of the 1.12 death effects (DeathEffect, DeathEffectEntityRenderer, FlyingGibs, RenderFlyingGibs):
 * <ul>
 * <li>GORE: blood burst, the dead mob is hidden and its model parts fly apart as gibs that bounce and leave blood
 * trails</li>
 * <li>BIO: green burst, the corpse swells, turns green and fades with a bio overlay</li>
 * <li>LASER: fire and ash, the corpse turns red and fades with a burn overlay</li>
 * </ul>
 * Gibs are client side objects ticked and drawn here, the model parts come from the mob's own renderer.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class DeathEffectClient {

	private static final int MAX_DEATH_TIME = 20;
	private static final ResourceLocation BIO_FX = Techguns.rl("textures/fx/bio.png");
	private static final ResourceLocation LASER_FX = Techguns.rl("textures/fx/laserdeath.png");
	private static final ResourceLocation GENERIC_GORE = Techguns.rl("textures/entity/gore.png");
	/** player model parts that are overlays of other parts, not gibs of their own */
	private static final Set<String> SKIP_FIELDS = Set.of("hat", "jacket", "leftSleeve", "rightSleeve", "leftPants", "rightPants", "cloak", "ear");

	/** entity id -> death type, for the render override while the corpse is still around */
	private static final Map<Integer, DeathType> DEATHS = new HashMap<>();
	private static final List<Gib> GIBS = new ArrayList<>();
	private static final Map<EntityType<?>, GoreData> GORE = new HashMap<>();
	private static final GoreData DEFAULT_GORE = new GoreData(160, 21, 31, 1.0f);
	private static ModelPart genericGibModel;
	private static Field childrenField;

	private DeathEffectClient() {
	}

	/* ------------------------------------------------------------------ gore data (1.12 DeathEffect.goreStats) */

	public static class GoreData {
		final int r, g, b;
		final float fxScale;
		private TGParticleSystemType main, trail;
		private boolean init;

		public GoreData(int r, int g, int b, float fxScale) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.fxScale = fxScale;
		}

		/** blood colored copies of the gore particle systems, created lazily because the FX list is a resource */
		void init() {
			if (!init) {
				init = true;
				main = tinted("GoreFX_Blood");
				trail = tinted("GoreTrailFX_Blood");
			}
		}

		private TGParticleSystemType tinted(String name) {
			TGFXType t = TGFX.FXList.get(name.toLowerCase());
			return t instanceof TGParticleSystemType pst ? pst.tinted(r, g, b, fxScale) : null;
		}
	}

	public static void addGoreData(EntityType<?> type, GoreData data) {
		GORE.put(type, data);
	}

	private static GoreData gore(EntityType<?> type) {
		if (GORE.isEmpty()) {
			initGore();
		}
		GoreData d = GORE.getOrDefault(type, DEFAULT_GORE);
		d.init();
		return d;
	}

	private static void initGore() {
		GoreData zombie = new GoreData(110, 21, 41, 1f), human = new GoreData(160, 21, 31, 1f), animal = new GoreData(170, 26, 37, 1f);
		GoreData bone = new GoreData(255, 255, 255, 1f), pigman = new GoreData(110, 51, 11, 1f), green = new GoreData(85, 156, 17, 1f);
		GoreData mutant = new GoreData(109, 60, 25, 1.2f);
		put(human, EntityType.PLAYER, TGEntities.ARMY_SOLDIER.get(), TGEntities.STORM_TROOPER.get(), TGEntities.COMMANDO.get(),
				TGEntities.DICTATOR_DAVE.get(), TGEntities.PSYCHO_STEVE.get(), TGEntities.BANDIT.get(), TGEntities.OUTCAST.get(), EntityType.WITCH);
		put(zombie, EntityType.ZOMBIE, EntityType.HUSK, EntityType.ZOMBIE_VILLAGER, EntityType.DROWNED, EntityType.EVOKER, EntityType.VINDICATOR,
				EntityType.PILLAGER, TGEntities.ZOMBIE_SOLDIER.get(), TGEntities.ZOMBIE_FARMER.get(), TGEntities.ZOMBIE_MINER.get(),
				TGEntities.ZOMBIE_POLICEMAN.get());
		put(bone, EntityType.SKELETON, EntityType.STRAY, TGEntities.SKELETON_SOLDIER.get());
		put(new GoreData(150, 21, 51, 1f), EntityType.VILLAGER, EntityType.WANDERING_TRADER);
		put(animal, EntityType.COW, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
				EntityType.WOLF, EntityType.GOAT, EntityType.FOX, EntityType.CAT, EntityType.OCELOT);
		put(new GoreData(170, 26, 37, 0.8f), EntityType.SHEEP, EntityType.PIG, EntityType.POLAR_BEAR, EntityType.MOOSHROOM);
		put(new GoreData(170, 26, 37, 0.5f), EntityType.CHICKEN, EntityType.PARROT);
		put(new GoreData(170, 26, 37, 0.3f), EntityType.RABBIT);
		put(new GoreData(50, 175, 57, 1f), EntityType.CREEPER);
		put(new GoreData(160, 36, 167, 1f), EntityType.ENDERMAN);
		put(green, EntityType.SPIDER, TGEntities.CYBER_DEMON.get());
		put(new GoreData(85, 156, 17, 0.7f), EntityType.CAVE_SPIDER);
		put(pigman, EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN, EntityType.ZOGLIN,
				TGEntities.ZOMBIE_PIGMAN_SOLDIER.get());
		put(mutant, TGEntities.SUPER_MUTANT_BASIC.get(), TGEntities.SUPER_MUTANT_ELITE.get(), TGEntities.SUPER_MUTANT_HEAVY.get());
		put(new GoreData(40, 255, 40, 1f), EntityType.SLIME);
		put(new GoreData(92, 26, 0, 1f), EntityType.MAGMA_CUBE);
		put(new GoreData(235, 255, 70, 1f), TGEntities.ALIEN_BUG.get());
		put(new GoreData(255, 255, 255, 1f), TGEntities.GHASTLING.get());
		put(new GoreData(90, 16, 27, 0.4f), EntityType.SILVERFISH, EntityType.ENDERMITE);
		put(new GoreData(215, 215, 215, 0.4f), EntityType.VEX);
		put(new GoreData(125, 0, 106, 1.5f), EntityType.SHULKER);
		put(new GoreData(50, 50, 50, 1.4f), EntityType.WITHER_SKELETON);
		put(new GoreData(255, 255, 255, 3.5f), EntityType.GHAST);
		put(new GoreData(40, 60, 120, 1f), EntityType.SQUID, EntityType.GLOW_SQUID);
	}

	private static void put(GoreData d, EntityType<?>... types) {
		for (EntityType<?> t : types) {
			GORE.put(t, d);
		}
	}

	/* ------------------------------------------------------------------ packet */

	public static void onDeathPacket(PacketEntityDeathType m) {
		Minecraft mc = Minecraft.getInstance();
		DeathType type = DeathType.byValue(m.deathType());
		if (mc.level == null || !TGConfig.cl_enableDeathFX || (type == DeathType.GORE && !TGConfig.cl_enableDeathFX_Gore)) {
			return;
		}
		if (!(mc.level.getEntity(m.entityId()) instanceof LivingEntity entity)) {
			return;
		}
		Vec3 motion = m.motion();
		entity.setDeltaMovement(motion);
		DEATHS.put(entity.getId(), type);
		createDeathEffect(mc.level, entity, type, motion);
	}

	private static void createDeathEffect(ClientLevel level, LivingEntity entity, DeathType type, Vec3 motion) {
		double x = entity.getX(), y = entity.getY() + entity.getBbHeight() / 2.0, z = entity.getZ();
		switch (type) {
			case GORE, DISMEMBER -> {
				GoreData data = gore(entity.getType());
				playSound(level, x, y, z, TGSounds.DEATH_GORE.get());
				if (data.main != null) {
					TGParticleManager.INSTANCE.addEffect(new TGParticleSystem(level, data.main, x, entity.getY(), z, motion.x, motion.y, motion.z));
				}
				spawnGibs(level, entity, data, x, y, z, motion);
			}
			case BIO -> {
				TGFXHooks.createFX("biodeath", level, x, y, z, motion.x, motion.y, motion.z);
				playSound(level, x, y, z, TGSounds.DEATH_BIO.get());
			}
			case LASER -> {
				TGFXHooks.createFX("laserdeathFire", level, x, y, z, motion.x, 0, motion.z);
				TGFXHooks.createFX("laserdeathAsh", level, x, y, z, motion.x, 0, motion.z);
				playSound(level, x, y, z, TGSounds.DEATH_LASER.get());
			}
			default -> {
			}
		}
	}

	private static void playSound(ClientLevel level, double x, double y, double z, SoundEvent sound) {
		level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, 1.0f, 1.0f, false);
	}

	/* ------------------------------------------------------------------ gibs */

	private static class Gib {
		final ModelPart part;
		final ResourceLocation texture;
		final Vector3f axis;
		final Entity anchor;
		final TGParticleSystem trail;
		final int maxTimeToLive;
		Vec3 pos, prevPos, motion;
		int timeToLive, hitGroundTTL, age;
		boolean onGround;

		Gib(ModelPart part, ResourceLocation texture, Vec3 pos, Vec3 motion, RandomSource rand, Entity anchor, TGParticleSystem trail) {
			this.part = part;
			this.texture = texture;
			this.pos = this.prevPos = pos;
			this.motion = motion;
			this.maxTimeToLive = this.timeToLive = 75 + rand.nextInt(50);
			Vector3f a = new Vector3f(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
			this.axis = a.lengthSquared() > 1.0E-4f ? a.normalize() : new Vector3f(0, 1, 0);
			this.anchor = anchor;
			this.trail = trail;
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private static void spawnGibs(ClientLevel level, LivingEntity entity, GoreData data, double x, double y, double z, Vec3 motion) {
		EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
		List<ModelPart> parts = List.of();
		ResourceLocation texture = GENERIC_GORE;
		if (renderer instanceof LivingEntityRenderer lr) {
			parts = gibParts(lr.getModel());
			@SuppressWarnings("unchecked")
			ResourceLocation tex = lr.getTextureLocation(entity);
			texture = tex;
		}
		if (parts.isEmpty()) {
			parts = genericParts();
			texture = GENERIC_GORE;
		}
		RandomSource rand = level.random;
		for (ModelPart part : parts) {
			double vx = (0.5 - rand.nextDouble()) * 0.35;
			double vy = entity.onGround() ? rand.nextDouble() * 0.35 : (0.5 - rand.nextDouble()) * 0.35;
			double vz = (0.5 - rand.nextDouble()) * 0.35;
			Entity anchor = EntityType.MARKER.create(level);
			TGParticleSystem trail = null;
			if (anchor != null) {
				anchor.setPos(x, y, z);
				if (data.trail != null) {
					trail = new TGParticleSystem(anchor, data.trail);
					TGParticleManager.INSTANCE.addEffect(trail);
				}
			}
			GIBS.add(new Gib(part, texture, new Vec3(x, y, z), new Vec3(motion.x * 0.35 + vx, motion.y * 0.35 + vy, motion.z * 0.35 + vz), rand,
					anchor, trail));
		}
	}

	/** top level parts of a model; reflection on the model's ModelPart fields for non hierarchical models */
	static List<ModelPart> gibParts(EntityModel<?> model) {
		List<ModelPart> out = new ArrayList<>();
		if (model instanceof HierarchicalModel<?> h) {
			List<ModelPart> top = children(h.root());
			if (top.size() == 1) {
				List<ModelPart> inner = children(top.get(0));
				top = inner.isEmpty() ? top : inner;
			}
			out.addAll(top);
		} else {
			Set<ModelPart> found = new HashSet<>();
			for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
				for (Field f : c.getDeclaredFields()) {
					if (f.getType() != ModelPart.class || Modifier.isStatic(f.getModifiers()) || SKIP_FIELDS.contains(f.getName())) {
						continue;
					}
					try {
						f.setAccessible(true);
						ModelPart p = (ModelPart) f.get(model);
						if (p != null) {
							found.add(p);
						}
					} catch (ReflectiveOperationException | RuntimeException e) {
						// inaccessible, skip
					}
				}
			}
			// drop parts that are children of other found parts, they fly with their parent
			Set<ModelPart> nested = new HashSet<>();
			for (ModelPart p : found) {
				p.getAllParts().filter(q -> q != p).forEach(nested::add);
			}
			for (ModelPart p : found) {
				if (!nested.contains(p)) {
					out.add(p);
				}
			}
		}
		out.removeIf(p -> !p.visible || p.isEmpty());
		return out.size() > 16 ? out.subList(0, 16) : out;
	}

	@SuppressWarnings("unchecked")
	private static List<ModelPart> children(ModelPart part) {
		try {
			if (childrenField == null) {
				childrenField = ModelPart.class.getDeclaredField("children");
				childrenField.setAccessible(true);
			}
			return new ArrayList<>(((Map<String, ModelPart>) childrenField.get(part)).values());
		} catch (ReflectiveOperationException | RuntimeException e) {
			return List.of();
		}
	}

	/** 1.12 generic gore: biped parts with the gore texture, used for mobs without usable model parts */
	private static List<ModelPart> genericParts() {
		if (genericGibModel == null) {
			genericGibModel = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ZOMBIE);
		}
		return children(genericGibModel).stream().filter(p -> !p.isEmpty()).toList();
	}

	private static void tickGibs(ClientLevel level) {
		Iterator<Gib> it = GIBS.iterator();
		while (it.hasNext()) {
			Gib g = it.next();
			g.age++;
			if (--g.timeToLive <= 0) {
				if (g.trail != null) {
					g.trail.setExpired();
				}
				it.remove();
				continue;
			}
			g.prevPos = g.pos;
			Vec3 m = g.motion.add(0, -0.03, 0);
			if (level.getBlockState(BlockPos.containing(g.pos)).getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
				m = new Vec3((level.random.nextFloat() - level.random.nextFloat()) * 0.2, 0.2, (level.random.nextFloat() - level.random.nextFloat()) * 0.2);
			}
			double x = g.pos.x, y = g.pos.y, z = g.pos.z;
			if (solid(level, x + m.x, y, z)) {
				m = new Vec3(-m.x * 0.3, m.y, m.z);
			} else {
				x += m.x;
			}
			if (solid(level, x, y, z + m.z)) {
				m = new Vec3(m.x, m.y, -m.z * 0.3);
			} else {
				z += m.z;
			}
			g.onGround = false;
			if (solid(level, x, y + m.y, z)) {
				if (m.y < 0) {
					g.onGround = true;
				}
				m = new Vec3(m.x, -m.y * 0.5, m.z);
			} else {
				y += m.y;
			}
			g.pos = new Vec3(x, y, z);
			float f = 0.98f;
			if (g.onGround) {
				if (g.hitGroundTTL == 0) {
					g.hitGroundTTL = g.timeToLive;
					if (g.trail != null) {
						g.trail.setExpired();
					}
				}
				f = 0.6f * 0.98f;
			}
			g.motion = new Vec3(m.x * f, m.y * 0.98, m.z * f);
			if (g.anchor != null) {
				g.anchor.setPos(g.pos);
				g.anchor.xo = g.prevPos.x;
				g.anchor.yo = g.prevPos.y;
				g.anchor.zo = g.prevPos.z;
			}
		}
	}

	private static boolean solid(ClientLevel level, double x, double y, double z) {
		BlockPos bp = BlockPos.containing(x, y, z);
		BlockState state = level.getBlockState(bp);
		if (state.isAir()) {
			return false;
		}
		VoxelShape shape = state.getCollisionShape(level, bp);
		if (shape.isEmpty()) {
			return false;
		}
		double lx = x - bp.getX(), ly = y - bp.getY(), lz = z - bp.getZ();
		return shape.toAabbs().stream().anyMatch(bb -> bb.contains(lx, ly, lz));
	}

	private static void renderGibs(PoseStack ps, Vec3 cam, float ptt) {
		if (GIBS.isEmpty()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		for (Gib g : GIBS) {
			Vec3 p = g.prevPos.lerp(g.pos, ptt);
			ps.pushPose();
			ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
			float rot = 90.0f;
			if (g.onGround) {
				float angle = 5 + ((float) g.hitGroundTTL / g.maxTimeToLive) * 15.0f;
				rot += (g.maxTimeToLive - g.hitGroundTTL) * angle;
				if (g.timeToLive <= 20) {
					ps.translate(0, ((20 - g.timeToLive) + ptt) * -0.05f, 0);
				}
			} else {
				float angle = 5 + ((float) g.timeToLive / g.maxTimeToLive) * 15.0f;
				rot += (g.age + ptt) * angle;
			}
			ps.mulPose(new Quaternionf().rotateAxis(rot * Mth.DEG_TO_RAD, g.axis));
			ps.scale(-1, -1, 1);
			PartPose saved = g.part.storePose();
			g.part.setPos(0, 0, 0);
			g.part.setRotation(0, 0, 0);
			int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p));
			g.part.render(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(g.texture)), light, OverlayTexture.NO_OVERLAY);
			g.part.loadPose(saved);
			ps.popPose();
		}
		buffers.endBatch();
	}

	/* ------------------------------------------------------------------ dying entity rendering */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void renderDying(LivingEntityRenderer renderer, LivingEntity entity, DeathType type, float ptt, PoseStack ps, MultiBufferSource buffers,
			int light) {
		EntityModel model = renderer.getModel();
		ResourceLocation tex = renderer.getTextureLocation(entity);
		float prog = Math.min(1.0f, (entity.deathTime + ptt) / MAX_DEATH_TIME);
		ps.pushPose();
		float bodyYaw = Mth.rotLerp(ptt, entity.yBodyRotO, entity.yBodyRot);
		ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
		if (type == DeathType.BIO) {
			float s = 1.0f + 0.35f * prog;
			ps.scale(s, s, s);
		}
		ps.scale(-1.0F, -1.0F, 1.0F);
		ps.translate(0.0F, -1.501F, 0.0F);
		float limbSpeed = Math.min(1.0f, entity.walkAnimation.speed(ptt));
		float limbPos = entity.walkAnimation.position(ptt);
		float headYaw = Mth.rotLerp(ptt, entity.yHeadRotO, entity.yHeadRot) - bodyYaw;
		float pitch = Mth.lerp(ptt, entity.xRotO, entity.getXRot());
		model.attackTime = 0;
		model.riding = false;
		model.young = entity.isBaby();
		model.prepareMobModel(entity, limbPos, limbSpeed, ptt);
		model.setupAnim(entity, limbPos, limbSpeed, entity.tickCount + ptt, headYaw, pitch);
		float main = 1.0f - prog * prog;
		float alpha = (1.0f - prog) * (1.0f - prog);
		int color = type == DeathType.BIO ? FastColor.ARGB32.colorFromFloat(alpha, main, 1.0f, main) : FastColor.ARGB32.colorFromFloat(alpha, 1.0f, main, main);
		model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucent(tex)), light, OverlayTexture.NO_OVERLAY, color);
		float ov = (float) (0.5 + Math.sin((Math.sqrt(prog) + 0.75) * 2.0 * Math.PI) / 2);
		model.renderToBuffer(ps, buffers.getBuffer(RenderType.eyes(type == DeathType.BIO ? BIO_FX : LASER_FX)), light, OverlayTexture.NO_OVERLAY,
				FastColor.ARGB32.colorFromFloat(1.0f, ov, ov, ov));
		ps.popPose();
	}

	/* ------------------------------------------------------------------ events */

	@SubscribeEvent
	public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
		if (DEATHS.isEmpty()) {
			return;
		}
		LivingEntity entity = event.getEntity();
		DeathType type = DEATHS.get(entity.getId());
		if (type == null || !entity.isDeadOrDying()) {
			return;
		}
		switch (type) {
			case GORE, DISMEMBER -> event.setCanceled(true);
			case BIO, LASER -> {
				event.setCanceled(true);
				renderDying(event.getRenderer(), entity, type, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(),
						event.getPackedLight());
			}
			default -> {
			}
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			DEATHS.clear();
			GIBS.clear();
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		if (!DEATHS.isEmpty()) {
			DEATHS.keySet().removeIf(id -> {
				Entity e = mc.level.getEntity(id);
				return e == null || e.isRemoved();
			});
		}
		tickGibs(mc.level);
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			renderGibs(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
		}
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		DEATHS.clear();
		GIBS.clear();
	}
}
