package techguns.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import techguns.TGConfig;
import techguns.TGDataComponents;
import techguns.TGItems;
import techguns.TGPermissions;
import techguns.TGSounds;
import techguns.TGuns;
import techguns.Techguns;
import techguns.api.guns.GunHandType;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.api.tginventory.TGSlotType;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.audio.TGSoundCategory;
import techguns.events.ArmorEvents;
import techguns.events.GunInteractEvents;
import techguns.events.TGTickHandler;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GenericGunMeleeCharge;
import techguns.packets.PacketPlaySound;
import techguns.packets.PacketSwapWeapon;
import techguns.packets.PacketTGKeybindPress;
import techguns.util.EntityCondition;
import techguns.util.SoundUtil;

/**
 * Game tests for the server side gameplay code: permissions and safe mode, weapon swaps, ammo change crafting refunds,
 * off hand rules, area mining selection, sounds and their packets. Players are NeoForge fake players, which take part in
 * the logic but drop every packet sent to them, so the tests call the handlers directly.
 */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class GameplayTests {

	private static ServerPlayer player(GameTestHelper helper) {
		return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tg_test"));
	}

	private static List<GenericGun> guns() {
		return BuiltInRegistries.ITEM.stream().filter(i -> i instanceof GenericGun && BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(Techguns.MODID))
				.map(i -> (GenericGun) i).toList();
	}

	private static GenericGun gunWithHands(GameTestHelper helper, GunHandType type) {
		for (GenericGun g : guns()) {
			if (g.getGunHandType() == type) {
				return g;
			}
		}
		throw new AssertionError("no gun with hand type " + type);
	}

	/* ---------------------------------------------------------------- permissions / safe mode */

	@GameTest(template = "empty")
	public static void unsafeModePermissionNode(GameTestHelper helper) {
		helper.assertTrue(PermissionAPI.getRegisteredNodes().contains(TGPermissions.ALLOW_UNSAFE_MODE), "permission node not registered");
		helper.assertTrue(TGPermissions.ALLOW_UNSAFE_MODE.getNodeName().equals("techguns.allowunsafemode"),
				"node name " + TGPermissions.ALLOW_UNSAFE_MODE.getNodeName());
		boolean old = TGConfig.limitUnsafeModeToOP;
		try {
			TGConfig.limitUnsafeModeToOP = false;
			helper.assertTrue(TGPermissions.canUseUnsafeMode(player(helper)), "the node should allow everyone by default");
		} finally {
			TGConfig.limitUnsafeModeToOP = old;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void restrictUnsafeModeToOps(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		var ops = helper.getLevel().getServer().getPlayerList();
		boolean old = TGConfig.limitUnsafeModeToOP;
		try {
			TGConfig.limitUnsafeModeToOP = true;
			helper.assertFalse(TGPermissions.canUseUnsafeMode(ply), "non-op allowed with RestrictUnsafeModeToOP");
			ops.op(ply.getGameProfile());
			helper.assertTrue(TGPermissions.canUseUnsafeMode(ply), "op refused with RestrictUnsafeModeToOP");
		} finally {
			ops.deop(ply.getGameProfile());
			TGConfig.limitUnsafeModeToOP = old;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void safemodeToggleRespectsPermission(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		boolean old = TGConfig.limitUnsafeModeToOP;
		try {
			TGConfig.limitUnsafeModeToOP = true;
			props.enableSafemode = true;
			PacketTGKeybindPress.handleOnServer(new PacketTGKeybindPress(PacketTGKeybindPress.TOGGLE_SAFEMODE), ply);
			helper.assertTrue(props.enableSafemode, "non-op could leave safe mode");

			props.enableSafemode = false;
			ArmorEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(ply));
			helper.assertTrue(props.enableSafemode, "login did not force safe mode for a player without permission");

			TGConfig.limitUnsafeModeToOP = false;
			PacketTGKeybindPress.handleOnServer(new PacketTGKeybindPress(PacketTGKeybindPress.TOGGLE_SAFEMODE), ply);
			helper.assertFalse(props.enableSafemode, "permitted player could not leave safe mode");
			PacketTGKeybindPress.handleOnServer(new PacketTGKeybindPress(PacketTGKeybindPress.TOGGLE_SAFEMODE), ply);
			helper.assertTrue(props.enableSafemode, "safe mode toggle did not switch back on");
		} finally {
			TGConfig.limitUnsafeModeToOP = old;
		}
		helper.succeed();
	}

	/* ---------------------------------------------------------------- weapon swap */

	@GameTest(template = "empty")
	public static void weaponSwapSwapsFireDelays(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		GenericGun one = gunWithHands(helper, GunHandType.ONE_HANDED);
		ItemStack a = new ItemStack(one);
		ItemStack b = new ItemStack(one);
		ply.setItemInHand(InteractionHand.MAIN_HAND, a);
		ply.setItemInHand(InteractionHand.OFF_HAND, b);
		TGTickHandler.onPlayerTickPre(new PlayerTickEvent.Pre(ply)); // records the guns in hand

		props.fireDelayMainhand = 5;
		props.fireDelayOffhand = 9;
		TGTickHandler.onPlayerTickPre(new PlayerTickEvent.Pre(ply)); // no swap: both just count down
		helper.assertTrue(props.fireDelayMainhand == 4 && props.fireDelayOffhand == 8,
				"unexpected delays without a swap: " + props.fireDelayMainhand + "/" + props.fireDelayOffhand);

		// the swap hands key moves the same stacks
		ply.setItemInHand(InteractionHand.MAIN_HAND, b);
		ply.setItemInHand(InteractionHand.OFF_HAND, a);
		TGTickHandler.onPlayerTickPre(new PlayerTickEvent.Pre(ply)); // counts down to 3/7, then swaps
		helper.assertTrue(props.fireDelayMainhand == 7 && props.fireDelayOffhand == 3,
				"delays not swapped: " + props.fireDelayMainhand + "/" + props.fireDelayOffhand);
		helper.assertTrue(props.gunMainHand == b && props.gunOffHand == a, "gun hands not updated");

		// a new stack in hand is not a swap
		ply.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(one));
		TGTickHandler.onPlayerTickPre(new PlayerTickEvent.Pre(ply));
		helper.assertTrue(props.fireDelayMainhand == 6 && props.fireDelayOffhand == 2,
				"delays swapped for a new stack: " + props.fireDelayMainhand + "/" + props.fireDelayOffhand);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void swapAttackTimesSwapsBothHands(GameTestHelper helper) {
		TGExtendedPlayer props = TGExtendedPlayer.get(player(helper));
		props.getAttackTime(false).setReloadTime(111L);
		props.getAttackTime(true).setReloadTime(222L);
		props.fireDelayMainhand = 1;
		props.fireDelayOffhand = 2;
		props.swapAttackTimes();
		helper.assertTrue(props.getAttackTime(false).getReloadTime() == 222L && props.getAttackTime(true).getReloadTime() == 111L,
				"attack times not swapped");
		helper.assertTrue(props.fireDelayMainhand == 2 && props.fireDelayOffhand == 1, "fire delays not swapped");
		helper.succeed();
	}

	/* ---------------------------------------------------------------- ammo change crafting */

	/** the first ammo item of a gun with a magazine, that counts as ammo for the ammo slot */
	private static ItemStack ammoFor(GameTestHelper helper, GenericGun gun) {
		ItemStack ammo = gun.getAmmoType().getAmmo(0)[0].copy();
		helper.assertTrue(ammo.getItem() instanceof ITGSpecialSlot s && s.getSlot(ammo) == TGSlotType.AMMOSLOT, ammo + " is not ammo slot ammo");
		return ammo;
	}

	/** count in the main inventory and the Techguns ammo slots (refunds go to the ammo slots first) */
	private static int count(Player ply, Item item) {
		int n = 0;
		for (ItemStack s : ply.getInventory().items) {
			if (s.is(item)) {
				n += s.getCount();
			}
		}
		for (ItemStack s : techguns.curios.TGCurios.getStacks(ply, techguns.curios.TGCurios.SLOT_AMMO)) {
			if (s.is(item)) {
				n += s.getCount();
			}
		}
		return n;
	}

	private static void clearInventory(Player ply) {
		ply.getInventory().clearContent();
		List<ItemStack> ammo = techguns.curios.TGCurios.getStacks(ply, techguns.curios.TGCurios.SLOT_AMMO);
		for (int i = 0; i < ammo.size(); i++) {
			ammo.set(i, ItemStack.EMPTY);
		}
	}

	@GameTest(template = "empty")
	public static void ammoChangeCraftingRefundsLoadedAmmo(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		clearInventory(ply);
		GenericGun gun = TGuns.AK47.get();
		ItemStack gunStack = new ItemStack(gun);
		gunStack.set(TGDataComponents.AMMO.get(), gun.getClipsize() / 2);
		List<ItemStack> expected = gun.getAmmoOnUnload(gunStack);
		helper.assertFalse(expected.isEmpty(), "half loaded AK47 unloads nothing");

		Container grid = new SimpleContainer(9);
		grid.setItem(0, gunStack);
		grid.setItem(1, ammoFor(helper, gun));
		GunInteractEvents.onCraft(new PlayerEvent.ItemCraftedEvent(ply, new ItemStack(gun), grid));
		for (ItemStack e : expected) {
			int have = count(ply, e.getItem());
			helper.assertTrue(have >= e.getCount(), "refund missing " + e + ", player has " + have);
		}

		// anything else in the grid: not an ammo change recipe, nothing refunded
		clearInventory(ply);
		grid.setItem(2, new ItemStack(Items.STICK));
		GunInteractEvents.onCraft(new PlayerEvent.ItemCraftedEvent(ply, new ItemStack(gun), grid));
		for (ItemStack e : expected) {
			helper.assertTrue(count(ply, e.getItem()) == 0, "refunded for a recipe that is not an ammo change");
		}
		helper.succeed();
	}

	/**
	 * Crafting a rocket launcher with a nuke rocket switches it to nukes and loads that rocket (as in 1.12); after
	 * firing it reloads from nuke rockets only.
	 */
	@GameTest(template = "empty")
	public static void rocketLauncherSwitchesToNukes(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		clearInventory(ply);
		GenericGun gun = TGuns.ROCKETLAUNCHER.get();
		var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(gun), new ItemStack(techguns.TGItems.ROCKET_NUKE.get())));
		var recipe = helper.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel());
		helper.assertTrue(recipe.isPresent(), "no recipe for rocket launcher + nuke rocket");
		ItemStack launcher = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
		helper.assertTrue(launcher.is(gun), "recipe made " + launcher);
		helper.assertTrue(techguns.items.guns.ammo.AmmoTypes.TYPE_NUKE.equals(gun.getCurrentAmmoVariantKey(launcher)),
				"variant after crafting: " + gun.getCurrentAmmoVariantKey(launcher));
		helper.assertTrue(gun.getCurrentAmmo(launcher) == gun.getClipsize(), "crafted launcher not loaded with the nuke: " + gun.getCurrentAmmo(launcher));

		// fired: reloads from a nuke rocket, not a plain one
		launcher.set(TGDataComponents.AMMO.get(), 0);
		ply.setItemInHand(InteractionHand.MAIN_HAND, launcher);
		ply.getInventory().add(new ItemStack(techguns.TGItems.ROCKET.get(), 3));
		ply.getInventory().add(new ItemStack(techguns.TGItems.ROCKET_NUKE.get(), 2));
		TGExtendedPlayer.get(ply).fireDelayMainhand = 0;
		gun.tryForcedReload(launcher, helper.getLevel(), ply, InteractionHand.MAIN_HAND);
		helper.assertTrue(gun.getCurrentAmmo(launcher) == gun.getClipsize(), "not reloaded, ammo " + gun.getCurrentAmmo(launcher));
		helper.assertTrue(count(ply, techguns.TGItems.ROCKET_NUKE.get()) == 1 && count(ply, techguns.TGItems.ROCKET.get()) == 3,
				"used the wrong rocket: nukes " + count(ply, techguns.TGItems.ROCKET_NUKE.get()) + ", rockets " + count(ply, techguns.TGItems.ROCKET.get()));
		helper.succeed();
	}

	/** The server data behind the Jade overlay: owner name, security and progress of a working machine. */
	@GameTest(template = "empty")
	public static void jadeMachineInfo(GameTestHelper helper) {
		BlockPos rel = new BlockPos(1, 1, 1);
		helper.setBlock(rel, techguns.TGBlocks.GRINDER.get());
		var grinder = (techguns.tileentities.BasicMachineTileEnt) helper.getBlockEntity(rel);
		ServerPlayer ply = player(helper);
		grinder.setOwner(ply);
		grinder.setSecurity((byte) 2);
		var old = techguns.factions.TGTeams.setNameLookupForTests((s, id) -> id.equals(ply.getUUID()) ? "tgOwner" : null);
		try {
			net.minecraft.nbt.CompoundTag idle = new net.minecraft.nbt.CompoundTag();
			techguns.plugins.jade.MachineInfo.write(idle, grinder, helper.getLevel().getServer());
			helper.assertTrue("tgOwner".equals(idle.getString(techguns.plugins.jade.MachineInfo.OWNER)), "owner: " + idle);
			helper.assertTrue(idle.getByte(techguns.plugins.jade.MachineInfo.SECURITY) == 2, "security: " + idle);
			helper.assertTrue(techguns.plugins.jade.MachineInfo.progress(idle) < 0, "idle machine shows progress: " + idle);

			grinder.totaltime = 200;
			grinder.progress = 50;
			net.minecraft.nbt.CompoundTag working = new net.minecraft.nbt.CompoundTag();
			techguns.plugins.jade.MachineInfo.write(working, grinder, helper.getLevel().getServer());
			helper.assertTrue(Math.abs(techguns.plugins.jade.MachineInfo.progress(working) - 0.25f) < 1e-6, "progress: " + working);

			net.minecraft.nbt.CompoundTag none = new net.minecraft.nbt.CompoundTag();
			techguns.plugins.jade.MachineInfo.write(none, null, helper.getLevel().getServer());
			helper.assertTrue(none.isEmpty(), "data for no block entity: " + none);
		} finally {
			techguns.factions.TGTeams.setNameLookupForTests(old);
		}
		helper.succeed();
	}

	/** Sandbags, camo nets and ladders can be waterlogged; sandbags and nets placed into water start waterlogged. */
	@GameTest(template = "empty")
	public static void waterloggableBlocks(GameTestHelper helper) {
		var water = net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
		List<net.minecraft.world.level.block.Block> blocks = List.of(techguns.TGBlocks.SANDBAGS.get(), techguns.TGBlocks.CAMONET.get(),
				techguns.TGBlocks.CAMONET_DESERT.get(), techguns.TGBlocks.CAMONET_SNOW.get(), techguns.TGBlocks.CAMONET_TOP.get(),
				techguns.TGBlocks.CAMONET_TOP_DESERT.get(), techguns.TGBlocks.CAMONET_TOP_SNOW.get(), techguns.TGBlocks.LADDER_METAL.get(),
				techguns.TGBlocks.LADDER_SHINY.get(), techguns.TGBlocks.LADDER_RUSTY.get(), techguns.TGBlocks.LADDER_CARBON.get(),
				techguns.TGBlocks.SLIMY_LADDER.get());
		List<String> problems = new java.util.ArrayList<>();
		for (var b : blocks) {
			var state = b.defaultBlockState();
			if (!(b instanceof net.minecraft.world.level.block.SimpleWaterloggedBlock) || !state.hasProperty(water)) {
				problems.add(BuiltInRegistries.BLOCK.getKey(b) + " not waterloggable");
				continue;
			}
			if (state.getValue(water) || !state.getFluidState().isEmpty()) {
				problems.add(BuiltInRegistries.BLOCK.getKey(b) + " starts with water");
			}
			if (!state.setValue(water, true).getFluidState().isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)) {
				problems.add(BuiltInRegistries.BLOCK.getKey(b) + " waterlogged state holds no water");
			}
		}
		// placed into a water source (clicking the floor below it), for the blocks that don't need a wall
		BlockPos floor = new BlockPos(1, 1, 1);
		helper.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE);
		for (var b : List.of(techguns.TGBlocks.SANDBAGS.get(), techguns.TGBlocks.CAMONET.get(), techguns.TGBlocks.CAMONET_TOP.get())) {
			for (boolean wet : new boolean[] { true, false }) {
				helper.setBlock(floor.above(), wet ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.AIR);
				var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(floor)),
						net.minecraft.core.Direction.UP, helper.absolutePos(floor), false);
				var ctx = new net.minecraft.world.item.context.BlockPlaceContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, new ItemStack(b), hit);
				var placed = b.getStateForPlacement(ctx);
				if (placed == null || placed.getValue(water) != wet) {
					problems.add(BuiltInRegistries.BLOCK.getKey(b) + " placed " + (wet ? "in water" : "in air") + ": " + placed);
				}
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/* ---------------------------------------------------------------- off hand rules */

	@GameTest(template = "empty")
	public static void twoHandedGunBlocksOffhand(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		GenericGun two = gunWithHands(helper, GunHandType.TWO_HANDED);
		GenericGun one = gunWithHands(helper, GunHandType.ONE_HANDED);

		ply.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(two));
		ply.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BREAD));
		var off = new PlayerInteractEvent.RightClickItem(ply, InteractionHand.OFF_HAND);
		GunInteractEvents.rightClickItem(off);
		helper.assertTrue(off.isCanceled(), "off hand use allowed next to a two handed gun");
		var main = new PlayerInteractEvent.RightClickItem(ply, InteractionHand.MAIN_HAND);
		GunInteractEvents.rightClickItem(main);
		helper.assertFalse(main.isCanceled(), "main hand use blocked");

		ply.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
		ply.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(two));
		var entity = new PlayerInteractEvent.EntityInteract(ply, InteractionHand.OFF_HAND, ply);
		GunInteractEvents.entityInteract(entity);
		helper.assertTrue(entity.isCanceled(), "off hand entity use allowed with a two handed gun in the off hand");

		ply.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(one));
		ply.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BREAD));
		var ok = new PlayerInteractEvent.RightClickItem(ply, InteractionHand.OFF_HAND);
		GunInteractEvents.rightClickItem(ok);
		helper.assertFalse(ok.isCanceled(), "off hand use blocked next to a one handed gun");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void sneakingWithOffhandGunUsesBlock(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		GenericGun one = gunWithHands(helper, GunHandType.ONE_HANDED);
		BlockPos pos = helper.absolutePos(BlockPos.ZERO);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		ply.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
		ply.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(one));

		ply.setShiftKeyDown(true);
		var ev = new PlayerInteractEvent.RightClickBlock(ply, InteractionHand.MAIN_HAND, pos, hit);
		GunInteractEvents.rightClickBlock(ev);
		helper.assertTrue(ev.getUseBlock() == TriState.TRUE, "sneaking did not force block use");

		ply.setShiftKeyDown(false);
		var ev2 = new PlayerInteractEvent.RightClickBlock(ply, InteractionHand.MAIN_HAND, pos, hit);
		GunInteractEvents.rightClickBlock(ev2);
		helper.assertTrue(ev2.getUseBlock() == TriState.DEFAULT, "block use changed without sneaking");
		helper.succeed();
	}

	/* ---------------------------------------------------------------- area mining */

	@GameTest(template = "empty")
	public static void areaMiningSelection(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		GenericGunMeleeCharge drill = (GenericGunMeleeCharge) TGuns.MININGDRILL.get();
		ItemStack stack = new ItemStack(drill);
		ply.setItemInHand(InteractionHand.MAIN_HAND, stack);
		helper.assertTrue(drill.getMiningRadius(stack) == 1, "mining drill radius " + drill.getMiningRadius(stack));

		// a 3x3 plane facing z around the centre
		BlockPos c = helper.absolutePos(new BlockPos(2, 2, 1));
		var level = helper.getLevel();
		level.setBlockAndUpdate(c, Blocks.STONE.defaultBlockState());
		BlockPos[] around = { c.offset(-1, -1, 0), c.offset(0, -1, 0), c.offset(1, -1, 0), c.offset(-1, 0, 0), c.offset(1, 0, 0), c.offset(-1, 1, 0),
				c.offset(0, 1, 0), c.offset(1, 1, 0) };
		var states = new net.minecraft.world.level.block.state.BlockState[] { Blocks.STONE.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState(),
				Blocks.CHEST.defaultBlockState(), Blocks.AIR.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), Blocks.BEDROCK.defaultBlockState(),
				Blocks.DIRT.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState() };
		for (int i = 0; i < around.length; i++) {
			level.setBlockAndUpdate(around[i], states[i]);
		}
		// in front of the plane, must not be included
		level.setBlockAndUpdate(c.offset(0, 0, 1), Blocks.STONE.defaultBlockState());

		Set<BlockPos> got = new HashSet<>(drill.getAreaMiningBlocks(ply, level, stack, c, Direction.Axis.Z));
		// stone, obsidian, dirt, deepslate; not: centre, chest (block entity), air, planks (axe), bedrock, block outside the plane
		Set<BlockPos> want = Set.of(around[0], around[1], around[6], around[7]);
		helper.assertTrue(got.equals(want), "area mining picked " + got + ", expected " + want);

		ItemStack empty = new ItemStack(drill);
		empty.set(TGDataComponents.AMMO.get(), 0);
		ply.setItemInHand(InteractionHand.MAIN_HAND, empty);
		helper.assertTrue(drill.getAreaMiningBlocks(ply, level, empty, c, Direction.Axis.Z).isEmpty(), "an empty drill still mines an area");

		GenericGunMeleeCharge chainsaw = (GenericGunMeleeCharge) TGuns.CHAINSAW.get();
		ItemStack saw = new ItemStack(chainsaw);
		helper.assertTrue(chainsaw.getAreaMiningBlocks(ply, level, saw, c, Direction.Axis.Z).isEmpty(), "a tool without mining radius mines an area");

		for (BlockPos p : around) {
			level.removeBlock(p, false);
		}
		level.removeBlock(c, false);
		level.removeBlock(c.offset(0, 0, 1), false);
		helper.succeed();
	}

	/* ---------------------------------------------------------------- ambient effects */

	@GameTest(template = "empty")
	public static void ambientEffectNeedsFuel(GameTestHelper helper) {
		GenericGun shishkebap = TGuns.SHISHKEBAP.get();
		ItemStack fueled = new ItemStack(shishkebap);
		helper.assertTrue(shishkebap.isAmbientEffectActive(fueled), "fueled Shishkebap has no flames");
		ItemStack empty = new ItemStack(shishkebap);
		empty.set(TGDataComponents.AMMO.get(), 0);
		helper.assertFalse(shishkebap.isAmbientEffectActive(empty), "empty Shishkebap still burns");
		for (GenericGun g : guns()) {
			if (g != shishkebap) {
				helper.assertFalse(g.isAmbientEffectActive(new ItemStack(g)), BuiltInRegistries.ITEM.getKey(g) + " has an ambient effect");
			}
		}
		helper.succeed();
	}

	/* ---------------------------------------------------------------- sounds and packets */

	@GameTest(template = "empty")
	public static void soundsResolveAndPlay(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		// the sound packet carries registry ids: every Techguns sound must have one
		for (var holder : TGSounds.SOUNDS.getEntries()) {
			helper.assertTrue(BuiltInRegistries.SOUND_EVENT.getKey(holder.get()) != null, "sound " + holder.getId() + " has no registry id");
		}
		// every category must survive the id round trip used by the packet
		for (int i = 0; TGSoundCategory.get(i) != null; i++) {
			helper.assertTrue(TGSoundCategory.get(i).getId() == i, "sound category id " + i);
		}
		// server side calls must not throw (no real players around, so nothing is sent)
		var level = helper.getLevel();
		SoundUtil.playSoundOnEntityGunPosition(level, ply, TGSounds.SHISHKEBAP_SWING.get(), 1f, 1f, false, true, false, TGSoundCategory.GUN_FIRE,
				EntityCondition.CHARGING_WEAPON);
		SoundUtil.playSoundOnEntityGunPosition(level, ply, TGSounds.SHISHKEBAP_SWING.get(), 1f, 1f, false, true, TGSoundCategory.GUN_FIRE);
		SoundUtil.playReloadSoundOnEntity(level, ply, TGSounds.SHISHKEBAP_SWING.get(), 1f, 1f, false, true, TGSoundCategory.RELOAD);
		SoundUtil.playSoundOnEntity(level, ply, TGSounds.SHISHKEBAP_SWING.get(), 1f, 1f, false, TGSoundCategory.PLAYER_EFFECT);
		SoundUtil.playSoundAtEntityPos(level, ply, TGSounds.SHISHKEBAP_SWING.get(), 1f, 1f, false, TGSoundCategory.PLAYER_EFFECT);
		SoundUtil.playSoundOnEntity(level, ply, null, 1f, 1f, false, TGSoundCategory.PLAYER_EFFECT);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void packetsRoundTrip(GameTestHelper helper) {
		PacketPlaySound sound = new PacketPlaySound(ResourceLocation.fromNamespaceAndPath("techguns", "guns.shishkebabfire"), 1234, 4f, 0.5f, true, true,
				false, true, TGSoundCategory.RELOAD.getId());
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		PacketPlaySound.CODEC.encode(buf, sound);
		PacketPlaySound decoded = PacketPlaySound.CODEC.decode(buf);
		helper.assertTrue(sound.equals(decoded), "PacketPlaySound round trip: " + decoded);
		helper.assertTrue(buf.readableBytes() == 0, "PacketPlaySound left bytes");

		PacketSwapWeapon swap = new PacketSwapWeapon(77);
		FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
		PacketSwapWeapon.CODEC.encode(buf2, swap);
		helper.assertTrue(swap.equals(PacketSwapWeapon.CODEC.decode(buf2)), "PacketSwapWeapon round trip");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void entityConditions(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		TGExtendedPlayer props = TGExtendedPlayer.get(ply);
		props.isChargingWeapon = true;
		helper.assertTrue(EntityCondition.CHARGING_WEAPON.evaluate(ply), "charging not detected");
		props.isChargingWeapon = false;
		helper.assertFalse(EntityCondition.CHARGING_WEAPON.evaluate(ply), "charging still detected");
		helper.assertTrue(EntityCondition.NONE.evaluate(ply), "NONE must never stop");
		for (EntityCondition c : EntityCondition.values()) {
			helper.assertTrue(EntityCondition.fromByte(c.id) == c, "condition id " + c);
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void startTrackingSyncDoesNotThrow(GameTestHelper helper) {
		ServerPlayer watcher = player(helper);
		ServerPlayer flyer = player(helper);
		TGExtendedPlayer.get(flyer).isJumpkeyPressed = true;
		ArmorEvents.onStartTracking(new PlayerEvent.StartTracking(watcher, flyer));
		TGExtendedPlayer.get(flyer).isJumpkeyPressed = false;
		ArmorEvents.onStartTracking(new PlayerEvent.StartTracking(watcher, flyer));
		helper.succeed();
	}

	/* ---------------------------------------------------------------- vibrations (sculk sensors, Wardens) */

	private static final BlockPos SENSOR = new BlockPos(1, 1, 1);

	/** A sculk sensor at SENSOR and a fake player 5 blocks away, facing up so bullets don't land near the sensor. */
	private static ServerPlayer listenedPlayer(GameTestHelper helper, boolean sneaking) {
		helper.setBlock(SENSOR, net.minecraft.world.level.block.Blocks.SCULK_SENSOR);
		ServerPlayer ply = player(helper);
		clearInventory(ply);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(5.5, 1, 5.5));
		ply.moveTo(at.x, at.y, at.z, 0.0f, -90.0f);
		ply.setShiftKeyDown(sneaking);
		ply.setPose(sneaking ? net.minecraft.world.entity.Pose.CROUCHING : net.minecraft.world.entity.Pose.STANDING);
		return ply;
	}

	private static void succeedWhenSensorActive(GameTestHelper helper, String what) {
		helper.succeedWhen(() -> helper.assertTrue(
				net.minecraft.world.level.block.SculkSensorBlock.getPhase(helper.getBlockState(SENSOR)) == net.minecraft.world.level.block.state.properties.SculkSensorPhase.ACTIVE,
				"the sculk sensor didn't hear " + what));
	}

	/** Gunfire triggers a sculk sensor, even from a sneaking player (vanilla's projectile_shoot would not). */
	@GameTest(template = "empty_large")
	public static void gunfireTriggersSculkSensor(GameTestHelper helper) {
		ServerPlayer ply = listenedPlayer(helper, true);
		helper.assertTrue(ply.isShiftKeyDown() && ply.isCrouching(), "fake player isn't sneaking");
		GenericGun gun = TGuns.PISTOL.get();
		ItemStack stack = new ItemStack(gun);
		ply.setItemInHand(InteractionHand.MAIN_HAND, stack);
		TGExtendedPlayer.get(ply).fireDelayMainhand = 0;
		gun.shootGunPrimary(stack, helper.getLevel(), ply, false, InteractionHand.MAIN_HAND, null);
		helper.assertTrue(gun.getCurrentAmmo(stack) == gun.getClipsize() - 1, "the gun didn't fire");
		succeedWhenSensorActive(helper, "a sneaking player's gunshot");
	}

	/** Reloading is a vanilla item-use vibration: heard when standing (and hidden by sneaking, like eating). */
	@GameTest(template = "empty_large")
	public static void reloadTriggersSculkSensor(GameTestHelper helper) {
		ServerPlayer ply = listenedPlayer(helper, false);
		GenericGun gun = TGuns.PISTOL.get();
		ItemStack stack = new ItemStack(gun);
		stack.set(TGDataComponents.AMMO.get(), 0);
		ply.setItemInHand(InteractionHand.MAIN_HAND, stack);
		ply.getInventory().add(ammoFor(helper, gun));
		TGExtendedPlayer.get(ply).fireDelayMainhand = 0;
		gun.tryForcedReload(stack, helper.getLevel(), ply, InteractionHand.MAIN_HAND);
		helper.assertTrue(gun.getCurrentAmmo(stack) > 0, "the gun didn't reload");
		succeedWhenSensorActive(helper, "a reload");
	}

	/** Techguns explosions don't go through vanilla's Explosion.explode(), so they send the explode vibration themselves. */
	@GameTest(template = "empty_large")
	public static void explosionTriggersSculkSensor(GameTestHelper helper) {
		helper.setBlock(SENSOR, net.minecraft.world.level.block.Blocks.SCULK_SENSOR);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(5.5, 2, 5.5));
		new techguns.damagesystem.TGExplosion(helper.getLevel(), null, null, at.x, at.y, at.z, 0, 0, 1, 1, 0.0).doExplosion(false);
		succeedWhenSensorActive(helper, "an explosion");
	}

	/** The gunfire event's data: vibration and Warden tags, not muted by sneaking, frequency 3 from the data map. */
	@GameTest(template = "empty")
	public static void gunfireVibrationData(GameTestHelper helper) {
		var ev = techguns.TGGameEvents.GUNFIRE;
		helper.assertTrue(ev.is(net.minecraft.tags.GameEventTags.VIBRATIONS), "gunfire not in #minecraft:vibrations");
		helper.assertTrue(ev.is(net.minecraft.tags.GameEventTags.WARDEN_CAN_LISTEN), "gunfire not in #minecraft:warden_can_listen");
		helper.assertFalse(ev.is(net.minecraft.tags.GameEventTags.IGNORE_VIBRATIONS_SNEAKING), "sneaking would hide gunfire");
		int freq = net.minecraft.world.level.gameevent.vibrations.VibrationSystem.getGameEventFrequency(ev);
		helper.assertTrue(freq == 3, "gunfire frequency " + freq);
		helper.succeed();
	}

	/* ---------------------------------------------------------------- Mekanism radiation bridge */

	/**
	 * With Mekanism (-Pcompat_mods=mekanism or true): Techguns armor shields against Mekanism radiation, Mekanism
	 * armor gives Techguns RAD resistance, and Mekanism's own radiate() lets nothing through full Techguns hazmat.
	 * Without Mekanism there is nothing to check (the bridge isn't loaded).
	 */
	@GameTest(template = "empty")
	public static void mekanismRadiationBridge(GameTestHelper helper) {
		if (net.neoforged.fml.ModList.get().isLoaded("mekanism")) {
			MekanismRadiationChecks.run(helper);
		}
		helper.succeed();
	}

	/**
	 * Armor and slot item bonuses are vanilla / NeoForge attributes: each item's modifiers, applied to a player as
	 * equipping does, give the 1.12 bonus values. Powered armor without power and broken armor give nothing.
	 */
	@GameTest(template = "empty")
	public static void armorBonusAttributes(GameTestHelper helper) {
		java.util.List<String> problems = new java.util.ArrayList<>();

		// scout boots: jump +0.1, safe fall +1 block, 20% less fall damage
		Player scout = fresh(helper, "tg_attr_scout");
		wear(scout, new ItemStack(techguns.TGArmors.T1_SCOUT_BOOTS.get()));
		near(problems, "scout jump strength", scout, net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH, 0.42 + 0.1);
		near(problems, "scout safe fall distance", scout, net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE, 3.0 + 1.0);
		near(problems, "scout fall damage multiplier", scout, net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER, 0.8);

		// full miner suit: 4 x 5% mining speed; commando: 4 x 125% underwater mining; t1 combat: knockback 0.4
		Player miner = fresh(helper, "tg_attr_miner");
		for (var item : java.util.List.of(techguns.TGArmors.T1_MINER_HELMET, techguns.TGArmors.T1_MINER_CHESTPLATE, techguns.TGArmors.T1_MINER_LEGGINGS,
				techguns.TGArmors.T1_MINER_BOOTS)) {
			wear(miner, new ItemStack(item.get()));
		}
		near(problems, "miner block break speed", miner, net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED, 1.2);
		near(problems, "miner fall damage (boots 20%)", miner, net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER, 0.8);
		Player commando = fresh(helper, "tg_attr_commando");
		for (var item : java.util.List.of(techguns.TGArmors.T2_COMMANDO_HELMET, techguns.TGArmors.T2_COMMANDO_CHESTPLATE,
				techguns.TGArmors.T2_COMMANDO_LEGGINGS, techguns.TGArmors.T2_COMMANDO_BOOTS)) {
			wear(commando, new ItemStack(item.get()));
		}
		near(problems, "commando submerged mining speed", commando, net.minecraft.world.entity.ai.attributes.Attributes.SUBMERGED_MINING_SPEED, 0.2 * (1 + 4 * 1.25));
		Player combat = fresh(helper, "tg_attr_combat");
		for (var item : java.util.List.of(techguns.TGArmors.T1_COMBAT_HELMET, techguns.TGArmors.T1_COMBAT_CHESTPLATE, techguns.TGArmors.T1_COMBAT_LEGGINGS,
				techguns.TGArmors.T1_COMBAT_BOOTS)) {
			wear(combat, new ItemStack(item.get()));
		}
		near(problems, "t1 combat knockback resistance", combat, net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.4);

		// steam chestplate: +2 health only with power
		ItemStack steam = new ItemStack(techguns.TGArmors.STEAM_CHESTPLATE.get());
		steam.set(TGDataComponents.ARMOR_POWER.get(), 0);
		Player unpowered = fresh(helper, "tg_attr_steam0");
		wear(unpowered, steam.copy());
		near(problems, "unpowered steam chestplate health", unpowered, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 20.0);
		steam.set(TGDataComponents.ARMOR_POWER.get(), 100);
		Player powered = fresh(helper, "tg_attr_steam1");
		wear(powered, steam);
		near(problems, "powered steam chestplate health", powered, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 22.0);

		// broken scout boots give nothing
		ItemStack broken = new ItemStack(techguns.TGArmors.T1_SCOUT_BOOTS.get());
		broken.setDamageValue(broken.getMaxDamage() - 1);
		Player brokenPly = fresh(helper, "tg_attr_broken");
		wear(brokenPly, broken);
		if (techguns.items.armors.GenericArmor.isBroken(broken)) {
			near(problems, "broken scout boots jump", brokenPly, net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH, 0.42);
		} else {
			problems.add("couldn't break the scout boots for the test");
		}

		// slot items through Curios: antigrav pack flight (only with charge), working gloves mining speed
		Player flyer = fresh(helper, "tg_attr_antigrav");
		ItemStack pack = new ItemStack(TGItems.ANTI_GRAV_PACK.get());
		curio(flyer, pack);
		if (!flyer.mayFly()) {
			problems.add("the antigrav pack doesn't let the player fly");
		}
		Player emptyFlyer = fresh(helper, "tg_attr_antigrav_empty");
		ItemStack emptyPack = new ItemStack(TGItems.ANTI_GRAV_PACK.get());
		emptyPack.setDamageValue(emptyPack.getMaxDamage());
		curio(emptyFlyer, emptyPack);
		if (emptyFlyer.mayFly()) {
			problems.add("an empty antigrav pack still lets the player fly");
		}
		Player gloves = fresh(helper, "tg_attr_gloves");
		curio(gloves, new ItemStack(TGItems.WORKING_GLOVES.get()));
		near(problems, "working gloves block break speed", gloves, net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED, 1.1);
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** the antigrav start ring and sound only for flight the pack gives, not creative mode or other flight */
	@GameTest(template = "empty")
	public static void antigravSoundOnlyForPackFlight(GameTestHelper helper) {
		List<String> problems = new java.util.ArrayList<>();
		Player survival = fresh(helper, "tg_antigrav_survival");
		helper.assertTrue(techguns.curios.TGCurios.getStacks(survival, techguns.curios.TGCurios.SLOT_BACK).size() > 0, "no Curios back slot");
		techguns.curios.TGCurios.getStacks(survival, techguns.curios.TGCurios.SLOT_BACK).set(0, new ItemStack(TGItems.ANTI_GRAV_PACK.get()));
		if (!techguns.events.ArmorEvents.fliesByTechgunsGear(survival)) {
			problems.add("survival flight with a charged pack isn't pack flight");
		}

		Player creative = fresh(helper, "tg_antigrav_creative");
		techguns.curios.TGCurios.getStacks(creative, techguns.curios.TGCurios.SLOT_BACK).set(0, new ItemStack(TGItems.ANTI_GRAV_PACK.get()));
		creative.getAbilities().mayfly = true; // what creative and spectator mode set
		if (techguns.events.ArmorEvents.fliesByTechgunsGear(creative)) {
			problems.add("creative flight with the pack on counts as pack flight");
		}

		Player empty = fresh(helper, "tg_antigrav_empty_pack");
		ItemStack emptyPack = new ItemStack(TGItems.ANTI_GRAV_PACK.get());
		emptyPack.setDamageValue(emptyPack.getMaxDamage());
		techguns.curios.TGCurios.getStacks(empty, techguns.curios.TGCurios.SLOT_BACK).set(0, emptyPack);
		if (techguns.events.ArmorEvents.fliesByTechgunsGear(empty)) {
			problems.add("an empty pack counts as pack flight");
		}

		Player other = fresh(helper, "tg_antigrav_other_flight");
		var flight = other.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT);
		flight.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(Techguns.rl("test_other_flight"), 1.0,
				net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
		if (techguns.events.ArmorEvents.fliesByTechgunsGear(other)) {
			problems.add("another mod's creative_flight counts as pack flight");
		}
		flight.removeModifier(Techguns.rl("test_other_flight"));

		for (Player p : List.of(survival, creative, empty)) {
			techguns.curios.TGCurios.getStacks(p, techguns.curios.TGCurios.SLOT_BACK).set(0, ItemStack.EMPTY);
		}
		creative.getAbilities().mayfly = false;
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	private static Player fresh(GameTestHelper helper, String name) {
		return FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(name.getBytes()), name));
	}

	/** what equipping does (LivingEntity.collectEquipmentChanges): the item's modifiers for its slot */
	private static void wear(Player ply, ItemStack stack) {
		var slot = ((net.minecraft.world.item.ArmorItem) stack.getItem()).getEquipmentSlot();
		stack.forEachModifier(slot, (attr, mod) -> {
			var inst = ply.getAttribute(attr);
			if (inst != null) {
				inst.removeModifier(mod.id());
				inst.addTransientModifier(mod);
			}
		});
	}

	/** what Curios does for an item in its matching slot */
	private static void curio(Player ply, ItemStack stack) {
		String slot = techguns.curios.TGCurios.slotFor(((ITGSpecialSlot) stack.getItem()).getSlot(stack));
		var ctx = new top.theillusivec4.curios.api.SlotContext(slot, ply, 0, false, true);
		ResourceLocation id = Techguns.rl("test_curio." + slot);
		techguns.curios.TGCurioItem.INSTANCE.getAttributeModifiers(ctx, id, stack).forEach((attr, mod) -> {
			var inst = ply.getAttribute(attr);
			if (inst != null) {
				inst.removeModifier(mod.id());
				inst.addTransientModifier(mod);
			}
		});
	}

	private static void near(java.util.List<String> problems, String what, Player ply,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double expected) {
		double actual = ply.getAttributeValue(attr);
		if (Math.abs(actual - expected) > 1.0e-4) {
			problems.add(what + ": " + actual + " instead of " + expected);
		}
	}
}
