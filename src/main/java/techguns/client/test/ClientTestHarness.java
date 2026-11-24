package techguns.client.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import techguns.TGDataComponents;
import techguns.Techguns;
import techguns.blocks.door.BlockDoor3x3;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.particle.TGFX;
import techguns.items.guns.GenericGun;
import techguns.tileentities.BasicTGTileEntity;
import techguns.util.TGFXHooks;

/**
 * Automated in-game checks, run by {@code ./gradlew runClientTest} (game dir run-clienttest/, system property
 * techguns.clientTest). It makes a fresh normal world (fixed seed, creative), then works through scripted steps on the
 * client tick:
 * <ul>
 * <li>audits: every Techguns item and block state has a model without missing textures, gun textures exist, every
 * sound event resolves to sound files, the particle effects parsed;</li>
 * <li>/locate (the same search) for every Techguns structure, in the overworld and the nether;</li>
 * <li>screenshots on a platform at y 200: all items in grids, every gun held (first person, fired, third person),
 * every block entity block placed (machines in all four wrench rotations) and its screen, every mob, every particle
 * effect, the Patchouli manual and the Ponder scenes.</li>
 * </ul>
 * Results go to run-clienttest/clienttest/report.txt (with the Techguns errors and warnings from the log) and the
 * screenshots to run-clienttest/clienttest/screenshots/; then the game quits. A step that throws is reported and
 * skipped; a run over 20 minutes stops itself.
 */
public final class ClientTestHarness {
	public static final String PROPERTY = "techguns.clientTest";
	public static final String WORLD = "tg_clienttest";
	public static final long SEED = 20261123L;
	private static final int MAX_TICKS = 20 * 60 * 20;
	/** the staging platform: player at P, things shown at SHOW (3 blocks ahead, the player looks south) */
	private static final BlockPos P = new BlockPos(0, 200, 0);
	private static final BlockPos SHOW = P.offset(0, 0, 3);

	public static void init() {
		if (Boolean.getBoolean(PROPERTY)) {
			Techguns.LOGGER.info("[TG-CLIENTTEST] client test harness on");
			NeoForge.EVENT_BUS.register(new ClientTestHarness());
		}
	}

	private interface Step {
		/** @return true when done */
		boolean tick(Minecraft mc) throws Exception;
	}

	private final Deque<Step> steps = new ArrayDeque<>();
	private final ClientTestReport report = new ClientTestReport();
	private final Path out = FMLPaths.GAMEDIR.get().resolve("clienttest");
	private int ticks = 0;
	private boolean finished = false;
	private final long startMillis = System.currentTimeMillis();

	private ClientTestHarness() {
		try {
			Files.createDirectories(this.out.resolve("screenshots"));
		} catch (IOException e) {
			throw new RuntimeException("can't make " + this.out.resolve("screenshots"), e);
		}
		script();
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent.Post event) {
		if (this.finished) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (++this.ticks > MAX_TICKS) {
			this.report.fail("harness", "timed out after " + MAX_TICKS + " ticks with " + this.steps.size() + " steps left");
			finish(mc);
			return;
		}
		Step step = this.steps.peekFirst();
		if (step == null) {
			finish(mc);
			return;
		}
		boolean done;
		try {
			done = step.tick(mc);
		} catch (Throwable t) {
			this.report.fail("harness", "step failed: " + t);
			Techguns.LOGGER.error("[TG-CLIENTTEST] step failed", t);
			done = true;
		}
		if (done) {
			this.steps.remove(step);
		}
	}

	/* ---------------------------------------------------------------- step helpers */

	private void add(Step s) {
		this.steps.addLast(s);
	}

	private static Step run(Runnable r) {
		return mc -> {
			r.run();
			return true;
		};
	}

	private static Step waitTicks(int n) {
		int[] left = { n };
		return mc -> --left[0] <= 0;
	}

	private Step waitUntil(String what, BooleanSupplier cond, int timeout) {
		int[] left = { timeout };
		return mc -> {
			if (cond.getAsBoolean()) {
				return true;
			}
			if (--left[0] <= 0) {
				this.report.fail("harness", "timed out waiting for " + what);
				return true;
			}
			return false;
		};
	}

	/** a step that, when reached, puts the given steps in its place (for lists known only once the world is up) */
	private Step expand(Supplier<List<Step>> gen) {
		return mc -> {
			List<Step> list = gen.get();
			for (int i = list.size() - 1; i >= 0; i--) {
				this.steps.addFirst(list.get(i));
			}
			// the runner removes this step by identity, behind the ones just added
			return true;
		};
	}

	/** runs on the server thread and waits for it */
	private Step server(Consumer<MinecraftServer> action) {
		CompletableFuture<?>[] f = { null };
		return mc -> {
			MinecraftServer server = mc.getSingleplayerServer();
			if (server == null) {
				throw new IllegalStateException("no integrated server");
			}
			if (f[0] == null) {
				f[0] = server.submit(() -> action.accept(server));
			}
			if (!f[0].isDone()) {
				return false;
			}
			f[0].join();
			return true;
		};
	}

	private Step shot(String name) {
		return mc -> {
			Screenshot.grab(this.out.toFile(), safe(name) + ".png", mc.getMainRenderTarget(), c -> {
			});
			return true;
		};
	}

	private static String safe(String s) {
		return s.replaceAll("[^a-zA-Z0-9_.-]", "_");
	}

	private static void command(MinecraftServer server, String cmd) {
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
	}

	private static ServerPlayer serverPlayer(MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
	}

	private static <T> List<T> ours(net.minecraft.core.Registry<T> reg) {
		List<T> list = new ArrayList<>();
		reg.entrySet().stream().filter(e -> e.getKey().location().getNamespace().equals(Techguns.MODID))
				.sorted(Comparator.comparing(e -> e.getKey().location().getPath())).forEach(e -> list.add(e.getValue()));
		return list;
	}

	/* ---------------------------------------------------------------- the script */

	private void script() {
		// title screen (first start shows the accessibility onboarding instead)
		add(waitUntil("the title screen", () -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen instanceof AccessibilityOnboardingScreen) {
				mc.options.onboardAccessibility = false;
				mc.setScreen(new TitleScreen());
			}
			return mc.screen instanceof TitleScreen;
		}, 20 * 120));
		add(run(this::setOptions));
		add(run(this::createWorld));
		add(waitUntil("the world to load", () -> {
			Minecraft mc = Minecraft.getInstance();
			return mc.level != null && mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null;
		}, 20 * 180));
		add(server(s -> {
			for (String c : new String[] { "time set noon", "weather clear", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
					"gamerule doMobSpawning false", "gamerule doFireTick false", "gamerule mobGriefing false" }) {
				command(s, c);
			}
		}));
		add(waitTicks(20));

		// audits
		add(run(this::auditItemModels));
		add(run(this::auditBlockModels));
		add(run(this::auditGunTextures));
		add(run(this::auditSounds));
		add(run(this::auditParticles));

		// structures
		add(expand(this::locateSteps));

		// staging platform
		add(server(this::buildPlatform));
		add(waitTicks(60));
		add(run(() -> look(0, 0)));

		add(expand(this::itemGridSteps));
		add(expand(this::gunSteps));
		add(expand(this::blockEntitySteps));
		add(expand(this::mobSteps));
		add(expand(this::jetpackSteps));
		add(expand(this::particleSteps));
		add(expand(this::manualSteps));
	}

	private void setOptions() {
		Minecraft mc = Minecraft.getInstance();
		mc.options.pauseOnLostFocus = false;
		mc.options.onboardAccessibility = false;
		mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
		mc.options.renderDistance().set(8);
		mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
		mc.options.save();
		this.report.info("harness", "game dir " + FMLPaths.GAMEDIR.get() + ", seed " + SEED);
	}

	private void createWorld() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getLevelSource().levelExists(WORLD)) {
			try (var access = mc.getLevelSource().createAccess(WORLD)) {
				access.deleteLevel();
			} catch (IOException e) {
				throw new RuntimeException("can't delete the old test world", e);
			}
		}
		GameRules rules = new GameRules();
		LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT);
		mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(SEED, true, false), WorldPresets::createNormalWorldDimensions,
				new TitleScreen());
	}

	/** client-side view; the server takes the client's rotation */
	private static void look(float yaw, float pitch) {
		Minecraft mc = Minecraft.getInstance();
		mc.player.setYRot(yaw);
		mc.player.setXRot(pitch);
		mc.player.setYHeadRot(yaw);
		mc.player.yRotO = yaw;
		mc.player.xRotO = pitch;
	}

	/* ---------------------------------------------------------------- audits */

	private static boolean missing(TextureAtlasSprite sprite) {
		return sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation());
	}

	private static boolean hasMissingQuad(BakedModel model, BlockState state) {
		RandomSource rand = RandomSource.create(0);
		for (Direction d : new Direction[] { null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
			for (BakedQuad q : model.getQuads(state, d, rand, ModelData.EMPTY, null)) {
				if (missing(q.getSprite())) {
					return true;
				}
			}
		}
		return false;
	}

	private void auditItemModels() {
		Minecraft mc = Minecraft.getInstance();
		BakedModel missingModel = mc.getModelManager().getMissingModel();
		int ok = 0, custom = 0;
		for (Item item : ours(BuiltInRegistries.ITEM)) {
			String id = BuiltInRegistries.ITEM.getKey(item).toString();
			try {
				BakedModel m = mc.getItemRenderer().getModel(new ItemStack(item), mc.level, mc.player, 0);
				if (m == missingModel) {
					this.report.fail("item model", id + ": no model");
				} else if (m.isCustomRenderer()) {
					custom++; // builtin/entity: drawn by its BlockEntityWithoutLevelRenderer, no quads to check
				} else if (missing(m.getParticleIcon(ModelData.EMPTY)) && m.getQuads(null, null, RandomSource.create(0), ModelData.EMPTY, null).isEmpty()) {
					this.report.fail("item model", id + ": missing texture (particle)");
				} else if (hasMissingQuad(m, null)) {
					this.report.fail("item model", id + ": missing texture");
				} else {
					ok++;
				}
			} catch (Throwable t) {
				this.report.fail("item model", id + ": " + t);
			}
		}
		this.report.pass("item model", ok + " item models without missing textures, " + custom + " drawn by custom renderers");
	}

	private void auditBlockModels() {
		Minecraft mc = Minecraft.getInstance();
		BakedModel missingModel = mc.getModelManager().getMissingModel();
		int ok = 0;
		for (Block block : ours(BuiltInRegistries.BLOCK)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).toString();
			Set<String> problems = new HashSet<>();
			for (BlockState state : block.getStateDefinition().getPossibleStates()) {
				try {
					BakedModel m = mc.getBlockRenderer().getBlockModel(state);
					if (m == missingModel) {
						problems.add("no model for " + state);
					} else if (hasMissingQuad(m, state)) {
						problems.add("missing texture in " + state);
					} else if (missing(m.getParticleIcon(ModelData.EMPTY))) {
						problems.add("missing particle texture in " + state);
					}
				} catch (Throwable t) {
					problems.add(state + ": " + t);
				}
			}
			if (problems.isEmpty()) {
				ok++;
			} else {
				this.report.fail("block model", id + ": " + problems.size() + " states, e.g. " + problems.iterator().next());
			}
		}
		this.report.pass("block model", ok + " blocks with all states modelled");
	}

	private void auditGunTextures() {
		Minecraft mc = Minecraft.getInstance();
		int ok = 0;
		for (Item item : ours(BuiltInRegistries.ITEM)) {
			if (item instanceof GenericGun gun) {
				for (ResourceLocation tex : gun.textures) {
					if (mc.getResourceManager().getResource(tex).isPresent()) {
						ok++;
					} else {
						this.report.fail("gun texture", BuiltInRegistries.ITEM.getKey(item) + ": " + tex + " not found");
					}
				}
			}
		}
		this.report.pass("gun texture", ok + " gun and camo textures found");
	}

	private void auditSounds() {
		Minecraft mc = Minecraft.getInstance();
		RandomSource rand = RandomSource.create(0);
		int ok = 0;
		for (SoundEvent ev : ours(BuiltInRegistries.SOUND_EVENT)) {
			ResourceLocation id = ev.getLocation();
			WeighedSoundEvents w = mc.getSoundManager().getSoundEvent(id);
			if (w == null) {
				this.report.fail("sound", id + ": not in sounds.json");
				continue;
			}
			Set<ResourceLocation> files = new HashSet<>();
			for (int i = 0; i < 48; i++) {
				var s = w.getSound(rand);
				if (s != null && s != net.minecraft.client.sounds.SoundManager.EMPTY_SOUND && s != net.minecraft.client.sounds.SoundManager.INTENTIONALLY_EMPTY_SOUND) {
					files.add(s.getPath());
				}
			}
			List<ResourceLocation> absent = files.stream().filter(f -> mc.getResourceManager().getResource(f).isEmpty()).toList();
			if (files.isEmpty()) {
				this.report.fail("sound", id + ": no sound files");
			} else if (!absent.isEmpty()) {
				this.report.fail("sound", id + ": missing " + absent);
			} else {
				ok++;
			}
		}
		this.report.pass("sound", ok + " sound events with their files");
	}

	private void auditParticles() {
		this.report.pass("particles", TGFX.FXList.size() + " effect lists loaded (parse errors show in the log section)");
	}

	/* ---------------------------------------------------------------- structures */

	private List<Step> locateSteps() {
		List<Step> list = new ArrayList<>();
		MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
		var reg = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
		List<ResourceLocation> ids = reg.keySet().stream().filter(k -> k.getNamespace().equals(Techguns.MODID)).sorted().toList();
		if (ids.isEmpty()) {
			this.report.fail("structure", "no Techguns structures in the world's registry (datapack not loaded?)");
			return list;
		}
		boolean oilMod = ModList.get().getMods().stream().anyMatch(m -> m.getModId().contains("oil") || m.getModId().equals("immersivepetroleum")
				|| m.getModId().equals("pneumaticcraft") || m.getModId().equals("thermal"));
		for (ResourceLocation id : ids) {
			list.add(server(s -> {
				Holder<Structure> holder = reg.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, id));
				String found = null;
				for (ServerLevel level : new ServerLevel[] { s.overworld(), s.getLevel(Level.NETHER) }) {
					if (level == null) {
						continue;
					}
					Pair<BlockPos, Holder<Structure>> r = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder),
							BlockPos.ZERO, 100, false);
					if (r != null) {
						found = level.dimension().location().getPath() + " at " + r.getFirst().toShortString() + " ("
								+ (int) Math.sqrt(r.getFirst().distSqr(BlockPos.ZERO)) + " blocks)";
						break;
					}
				}
				if (found != null) {
					this.report.pass("structure", id + " found in " + found);
				} else if (id.getPath().equals("desert_oil_cluster") && !oilMod) {
					this.report.info("structure", id + " not found (needs an oil mod; none loaded)");
				} else {
					this.report.fail("structure", id + " not found within 100 chunks of 0,0 (overworld and nether)");
				}
			}));
		}
		return list;
	}

	/* ---------------------------------------------------------------- staging */

	private void buildPlatform(MinecraftServer s) {
		ServerLevel level = s.overworld();
		for (int x = -6; x <= 6; x++) {
			for (int z = -4; z <= 10; z++) {
				level.setBlock(P.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
				for (int y = 0; y < 6; y++) {
					level.setBlock(P.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		ServerPlayer p = serverPlayer(s);
		p.teleportTo(level, P.getX() + 0.5, P.getY(), P.getZ() + 0.5, 0f, 0f);
		p.getAbilities().flying = false;
		p.onUpdateAbilities();
	}

	private List<Step> itemGridSteps() {
		List<Step> list = new ArrayList<>();
		List<ItemStack> items = new ArrayList<>(ours(BuiltInRegistries.ITEM).stream().map(ItemStack::new).toList());
		int per = ItemGridScreen.COLS * ItemGridScreen.ROWS;
		int pages = (items.size() + per - 1) / per;
		int[] errors = { 0 };
		for (int page = 0; page < pages; page++) {
			List<ItemStack> sub = new ArrayList<>(items.subList(page * per, Math.min(items.size(), (page + 1) * per)));
			String name = "items_" + (page + 1);
			list.add(run(() -> Minecraft.getInstance().setScreen(new ItemGridScreen("Techguns items " + name, sub, e -> {
				errors[0]++;
				this.report.fail("item render", e);
			}))));
			list.add(waitTicks(5));
			list.add(shot(name));
			list.add(run(() -> Minecraft.getInstance().setScreen(null)));
		}
		list.add(run(() -> this.report.pass("item render", items.size() + " items drawn on " + pages + " pages, " + errors[0] + " failed")));
		return list;
	}

	private List<Step> gunSteps() {
		List<Step> list = new ArrayList<>();
		for (Item item : ours(BuiltInRegistries.ITEM)) {
			if (!(item instanceof GenericGun gun)) {
				continue;
			}
			String id = BuiltInRegistries.ITEM.getKey(item).getPath();
			list.add(server(s -> {
				ItemStack stack = new ItemStack(gun);
				stack.set(TGDataComponents.AMMO.get(), gun.getClipsize());
				serverPlayer(s).setItemInHand(InteractionHand.MAIN_HAND, stack);
			}));
			list.add(run(() -> {
				Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
				look(0, 0);
			}));
			list.add(waitTicks(6));
			list.add(shot("gun_" + id + "_1_held"));
			// fire: the server spawns the projectile, the client plays recoil and muzzle flash
			list.add(server(s -> {
				ServerPlayer p = serverPlayer(s);
				TGExtendedPlayer.get(p).fireDelayMainhand = 0;
				gun.shootGunPrimary(p.getMainHandItem(), p.level(), p, false, InteractionHand.MAIN_HAND, null);
			}));
			list.add(run(() -> {
				var p = Minecraft.getInstance().player;
				TGExtendedPlayer.get(p).fireDelayMainhand = 0;
				gun.shootGunPrimary(p.getMainHandItem(), p.level(), p, false, InteractionHand.MAIN_HAND, null);
			}));
			list.add(waitTicks(2));
			list.add(shot("gun_" + id + "_2_fired"));
			list.add(run(() -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT)));
			list.add(waitTicks(12));
			list.add(shot("gun_" + id + "_3_third_person"));
			list.add(run(() -> this.report.pass("gun", id + " held, fired, third person")));
		}
		list.add(run(() -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON)));
		list.add(server(s -> {
			serverPlayer(s).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			s.getAllLevels().forEach(l -> l.getEntities().getAll().forEach(e -> {
				if (!(e instanceof net.minecraft.world.entity.player.Player) && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace()
						.equals(Techguns.MODID)) {
					e.discard();
				}
			}));
		}));
		return list;
	}

	private List<Step> blockEntitySteps() {
		List<Step> list = new ArrayList<>();
		for (Block block : ours(BuiltInRegistries.BLOCK)) {
			if (!(block instanceof EntityBlock)) {
				continue;
			}
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			boolean[] rotates = { false };
			list.add(server(s -> {
				ServerLevel level = s.overworld();
				clearShow(level);
				if (block instanceof BlockDoor3x3) {
					// the default state is an invisible slave block: place the whole door as ItemDoor3x3 does,
					// master in the middle, so SHOW (its bottom middle) is a slave that opens the master's screen
					BlockState slave = block.defaultBlockState().setValue(BlockDoor3x3.MASTER, false).setValue(BlockDoor3x3.ZPLANE, false);
					BlockPos master = SHOW.above();
					for (BlockPos p : BlockDoor3x3.slaves(master, false)) {
						level.setBlock(p, slave, 3);
					}
					level.setBlock(master, slave.setValue(BlockDoor3x3.MASTER, true), 3);
				} else {
					level.setBlock(SHOW, block.defaultBlockState(), 3);
				}
				BlockEntity be = level.getBlockEntity(SHOW);
				rotates[0] = be instanceof BasicTGTileEntity tg && tg.hasRotation();
			}));
			list.add(run(() -> look(0, 22)));
			list.add(waitTicks(8));
			list.add(expand(() -> {
				List<Step> sub = new ArrayList<>();
				if (rotates[0]) {
					for (int r = 0; r < 4; r++) {
						int rot = r;
						sub.add(server(s -> {
							ServerLevel level = s.overworld();
							if (level.getBlockEntity(SHOW) instanceof BasicTGTileEntity tg) {
								tg.rotation = (byte) rot;
								tg.setChanged();
								BlockState st = level.getBlockState(SHOW);
								level.sendBlockUpdated(SHOW, st, st, 3);
							}
						}));
						sub.add(waitTicks(6));
						sub.add(shot("block_" + id + "_rot" + rot));
					}
				} else {
					sub.add(shot("block_" + id));
				}
				// its screen, if it has one
				sub.add(server(s -> {
					ServerLevel level = s.overworld();
					BlockState st = level.getBlockState(SHOW);
					st.useWithoutItem(level, serverPlayer(s), new BlockHitResult(Vec3.atCenterOf(SHOW), Direction.NORTH, SHOW, false));
				}));
				sub.add(waitTicks(8));
				sub.add(mc -> {
					if (mc.screen != null) {
						this.report.pass("screen", id + " opens " + mc.screen.getClass().getSimpleName());
						return true;
					}
					return true;
				});
				sub.add(mc -> {
					if (mc.screen != null) {
						Screenshot.grab(this.out.toFile(), safe("screen_" + id) + ".png", mc.getMainRenderTarget(), c -> {
						});
					}
					return true;
				});
				sub.add(run(() -> {
					Minecraft mc = Minecraft.getInstance();
					if (mc.screen != null) {
						mc.player.closeContainer();
						mc.setScreen(null);
					}
				}));
				sub.add(waitTicks(2));
				sub.add(run(() -> this.report.pass("block", id + (rotates[0] ? " placed, 4 rotations" : " placed"))));
				return sub;
			}));
		}
		list.add(server(s -> clearShow(s.overworld())));
		list.add(run(() -> look(0, 0)));
		return list;
	}

	/** empties the 3x3x3 around the show spot, so multi-block placements (3x3 doors) leave nothing behind */
	private static void clearShow(ServerLevel level) {
		// a door's master first: removing a slave makes the master drop the door item
		level.setBlock(SHOW.above(), Blocks.AIR.defaultBlockState(), 3 | Block.UPDATE_SUPPRESS_DROPS);
		for (BlockPos p : BlockPos.betweenClosed(SHOW.offset(-1, 0, -1), SHOW.offset(1, 2, 1))) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3 | Block.UPDATE_SUPPRESS_DROPS);
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(SHOW).inflate(4)).forEach(Entity::discard);
	}

	private List<Step> mobSteps() {
		List<Step> list = new ArrayList<>();
		for (EntityType<?> type : ours(BuiltInRegistries.ENTITY_TYPE)) {
			if (type.getCategory() == MobCategory.MISC) {
				continue;
			}
			String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
			Entity[] spawned = { null };
			list.add(server(s -> {
				ServerLevel level = s.overworld();
				Entity e = type.create(level);
				if (e == null) {
					this.report.fail("mob", id + ": can't be created");
					return;
				}
				double back = Math.max(4.0, e.getBbWidth() * 1.5 + 2.5);
				e.moveTo(P.getX() + 0.5, P.getY(), P.getZ() + 0.5 + back, 180f, 0f);
				if (e instanceof Mob m) {
					// as /summon does, so NPCs get their guns and armor
					EventHooks.finalizeMobSpawn(m, level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
					m.setNoAi(true);
					m.setPersistenceRequired();
					m.setYHeadRot(180f);
					m.setYBodyRot(180f);
				}
				level.addFreshEntity(e);
				spawned[0] = e;
			}));
			list.add(run(() -> look(0, 10)));
			list.add(waitTicks(10));
			list.add(shot("mob_" + id));
			list.add(server(s -> {
				if (spawned[0] != null) {
					spawned[0].discard();
					this.report.pass("mob", id + " spawned and drawn");
				}
			}));
		}
		list.add(run(() -> look(0, 0)));
		return list;
	}

	/** sounds the client played since the last clear (listeners run even with the master volume at 0) */
	private final Set<ResourceLocation> heard = new HashSet<>();
	private boolean listening = false;

	private void listen() {
		if (!this.listening) {
			this.listening = true;
			Minecraft.getInstance().getSoundManager().addListener((sound, events, range) -> this.heard.add(sound.getLocation()));
		}
		this.heard.clear();
	}

	private Step expectHeard(String section, String what, String... sounds) {
		return mc -> {
			List<String> missing = new ArrayList<>();
			for (String s : sounds) {
				if (!this.heard.contains(Techguns.rl(s))) {
					missing.add(s);
				}
			}
			if (missing.isEmpty()) {
				this.report.pass(section, what + ": heard " + String.join(", ", sounds));
			} else {
				this.report.fail(section, what + ": didn't hear " + missing + " (heard " + this.heard + ")");
			}
			this.heard.clear();
			return true;
		};
	}

	/** jetpack sounds follow the boost flag (set directly: no jetpack is worn, so nothing else changes it) */
	private List<Step> jetpackSteps() {
		List<Step> list = new ArrayList<>();
		list.add(run(this::listen));
		list.add(run(() -> TGExtendedPlayer.get(Minecraft.getInstance().player).isJumpkeyPressed = true));
		list.add(waitTicks(10));
		list.add(expectHeard("jetpack", "boost start", "jetpack.start", "jetpack.loop"));
		list.add(run(() -> TGExtendedPlayer.get(Minecraft.getInstance().player).isJumpkeyPressed = false));
		list.add(waitTicks(5));
		list.add(expectHeard("jetpack", "boost stop", "jetpack.stop"));
		return list;
	}

	private List<Step> particleSteps() {
		List<Step> list = new ArrayList<>();
		List<String> names = TGFX.FXList.keySet().stream().sorted().toList();
		for (String name : names) {
			list.add(run(() -> {
				Minecraft mc = Minecraft.getInstance();
				TGFXHooks.createFX(name, mc.level, SHOW.getX() + 0.5, SHOW.getY() + 1.0, SHOW.getZ() + 0.5, 0, 0, 0);
			}));
			list.add(waitTicks(8));
			list.add(shot("fx_" + name));
		}
		list.add(run(() -> this.report.pass("particles", names.size() + " effect lists spawned")));
		list.add(waitTicks(60));
		return list;
	}

	private List<Step> manualSteps() {
		List<Step> list = new ArrayList<>();
		if (ModList.get().isLoaded("patchouli")) {
			list.add(server(s -> command(s, "open-patchouli-book " + serverPlayer(s).getGameProfile().getName() + " techguns:techguns_manual")));
			list.add(waitTicks(20));
			list.add(mc -> {
				if (mc.screen == null) {
					this.report.fail("manual", "the Techguns manual didn't open");
				} else {
					this.report.pass("manual", "opens " + mc.screen.getClass().getSimpleName());
				}
				return true;
			});
			list.add(shot("manual"));
			list.add(run(() -> Minecraft.getInstance().setScreen(null)));
		} else {
			this.report.info("manual", "Patchouli not loaded");
		}
		if (ModList.get().isLoaded("ponder")) {
			for (ItemStack stack : PonderHook.controllers()) {
				String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
				list.add(run(() -> PonderHook.open(stack)));
				list.add(waitTicks(100));
				list.add(mc -> {
					if (mc.screen == null) {
						this.report.fail("ponder", id + ": no scene opened");
					} else {
						this.report.pass("ponder", id + " opens " + mc.screen.getClass().getSimpleName());
					}
					return true;
				});
				list.add(shot("ponder_" + id));
				list.add(run(() -> Minecraft.getInstance().setScreen(null)));
				list.add(waitTicks(5));
			}
		} else {
			this.report.info("ponder", "Ponder not loaded");
		}
		return list;
	}

	/* ---------------------------------------------------------------- end */

	private void finish(Minecraft mc) {
		this.finished = true;
		scanLog();
		try (var files = Files.list(this.out.resolve("screenshots"))) {
			this.report.info("harness", files.count() + " screenshots in " + this.out.resolve("screenshots"));
		} catch (IOException e) {
			this.report.fail("harness", "no screenshots: " + e);
		}
		try {
			this.report.write(this.out.resolve("report.txt"));
		} catch (IOException e) {
			Techguns.LOGGER.error("[TG-CLIENTTEST] can't write the report", e);
		}
		Techguns.LOGGER.info("[TG-CLIENTTEST] done in {} s: {}", (System.currentTimeMillis() - this.startMillis) / 1000, this.report.summary());
		mc.stop();
	}

	/** other mods' messages that name Techguns things but aren't Techguns problems */
	private static final String[] LOG_NOISE = { "ftblibrary.icon.EntityIconLoader", "VanillaPackResourcesBuilder" };

	/**
	 * Techguns errors and warnings from this run's log: lines that name techguns, and errors whose stack trace goes
	 * through techguns code
	 */
	private void scanLog() {
		Path log = FMLPaths.GAMEDIR.get().resolve("logs/latest.log");
		try {
			List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
			int errors = 0, warns = 0;
			for (int i = 0; i < lines.size(); i++) {
				String l = lines.get(i);
				boolean error = l.contains("/ERROR]");
				boolean warn = l.contains("/WARN]");
				if (!(error || warn) || l.contains("[TG-CLIENTTEST]")) {
					continue;
				}
				boolean noise = false;
				for (String n : LOG_NOISE) {
					noise |= l.contains(n);
				}
				if (noise) {
					continue;
				}
				// the entry's continuation lines (exception and stack trace) up to the next log line
				String trace = null;
				for (int j = i + 1; j < lines.size() && !lines.get(j).startsWith("["); j++) {
					if (trace == null && j == i + 1) {
						trace = lines.get(j).trim();
					}
					if (lines.get(j).contains("techguns.") && trace != null && !trace.contains("  @ ")) {
						trace = trace + "  @ " + lines.get(j).trim();
					}
				}
				boolean ours = l.toLowerCase().contains("techguns") || (trace != null && trace.contains("  @ "));
				if (!ours) {
					continue;
				}
				String entry = l.trim() + (trace != null ? "  | " + trace : "");
				if (error) {
					if (++errors <= 100) {
						this.report.fail("log", entry);
					}
				} else if (++warns <= 100) {
					this.report.info("log", entry);
				}
			}
			this.report.info("log", errors + " Techguns errors, " + warns + " warnings in " + log.getFileName());
		} catch (IOException e) {
			this.report.info("log", "can't read " + log + ": " + e);
		}
	}
}
