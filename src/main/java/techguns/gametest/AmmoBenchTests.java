package techguns.gametest;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGBlocks;
import techguns.TGDataComponents;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.gui.containers.AmmoBenchMenu;
import techguns.items.ItemAmmoBox;
import techguns.items.guns.GenericGun;
import techguns.util.InventoryUtil;

/** The Fabric port's ammo bench and ammo boxes: crafting at the bench, what a box holds, and reloading from a box. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class AmmoBenchTests {

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer ply = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tg_test"));
		ply.getInventory().clearContent();
		List<ItemStack> ammo = techguns.curios.TGCurios.getStacks(ply, techguns.curios.TGCurios.SLOT_AMMO);
		for (int i = 0; i < ammo.size(); i++) {
			ammo.set(i, ItemStack.EMPTY);
		}
		return ply;
	}

	private static int count(ServerPlayer ply, Item item) {
		int n = 0;
		for (ItemStack s : ply.getInventory().items) {
			if (s.is(item)) {
				n += s.getCount();
			}
		}
		return n;
	}

	/**
	 * One core, casing and powder list all 5 bench recipes; the picked one gives its rounds and uses one of each input.
	 * Shift-clicking the result crafts until an input runs out. Items that are no input don't go in.
	 */
	@GameTest(template = "empty")
	public static void ammoBenchCrafts(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		helper.getLevel().setBlockAndUpdate(pos, TGBlocks.AMMO_BENCH.get().defaultBlockState());
		ServerPlayer ply = player(helper);
		AmmoBenchMenu menu = new AmmoBenchMenu(0, ply.getInventory(), ContainerLevelAccess.create(helper.getLevel(), pos));
		helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "dirt goes in the core slot");
		helper.assertFalse(menu.getSlot(2).mayPlace(new ItemStack(TGItems.INGOT_STEEL.get())), "steel goes in the powder slot");

		menu.getSlot(0).set(new ItemStack(TGItems.INGOT_LEAD.get(), 2));
		menu.getSlot(1).set(new ItemStack(Items.COPPER_INGOT, 2));
		helper.assertTrue(menu.getNumRecipes() == 0, "recipes without powder: " + menu.getNumRecipes());
		// shift-click puts gunpowder from the inventory into the powder slot
		ply.getInventory().setItem(9, new ItemStack(Items.GUNPOWDER, 2));
		menu.quickMoveStack(ply, 4);
		helper.assertTrue(menu.getSlot(2).getItem().is(Items.GUNPOWDER) && menu.getSlot(2).getItem().getCount() == 2,
				"powder slot after shift-click: " + menu.getSlot(2).getItem());
		helper.assertTrue(menu.getNumRecipes() == 5, "recipes: " + menu.getNumRecipes());

		int pistol = -1;
		for (int i = 0; i < menu.getNumRecipes(); i++) {
			if (menu.getRecipes().get(i).value().result().is(TGItems.PISTOL_ROUNDS.get())) {
				pistol = i;
			}
		}
		helper.assertTrue(pistol >= 0, "no pistol round recipe");
		menu.clickMenuButton(ply, pistol);
		ItemStack result = menu.getSlot(AmmoBenchMenu.RESULT_SLOT).getItem();
		helper.assertTrue(result.is(TGItems.PISTOL_ROUNDS.get()) && result.getCount() == 12, "result: " + result);

		menu.quickMoveStack(ply, AmmoBenchMenu.RESULT_SLOT);
		helper.assertTrue(count(ply, TGItems.PISTOL_ROUNDS.get()) == 12, "pistol rounds after one craft: " + count(ply, TGItems.PISTOL_ROUNDS.get()));
		for (int i = 0; i < 3; i++) {
			helper.assertTrue(menu.getSlot(i).getItem().getCount() == 1, "input " + i + " after one craft: " + menu.getSlot(i).getItem());
		}
		helper.assertTrue(menu.getSlot(AmmoBenchMenu.RESULT_SLOT).getItem().getCount() == 12, "no second result");
		menu.quickMoveStack(ply, AmmoBenchMenu.RESULT_SLOT);
		helper.assertTrue(count(ply, TGItems.PISTOL_ROUNDS.get()) == 24, "pistol rounds after two crafts: " + count(ply, TGItems.PISTOL_ROUNDS.get()));
		helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getNumRecipes() == 0
				&& menu.getSlot(AmmoBenchMenu.RESULT_SLOT).getItem().isEmpty(), "inputs or result left after the last craft");
		helper.succeed();
	}

	/** The bench: its shape is not a full block, it turns to face the player, and it drops itself. */
	@GameTest(template = "empty")
	public static void ammoBenchBlock(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			BlockState s = TGBlocks.AMMO_BENCH.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
			var shape = s.getShape(helper.getLevel(), pos);
			helper.assertTrue(Shapes.joinIsNotEmpty(shape, Shapes.block(), BooleanOp.NOT_SAME), d + ": full block shape");
			// the back board (the part above the table top) is on the side away from where the bench faces
			var board = shape.toAabbs().stream().filter(b -> b.minY >= 0.6).findFirst().orElseThrow();
			double cx = (board.minX + board.maxX) / 2 - 0.5, cz = (board.minZ + board.maxZ) / 2 - 0.5;
			helper.assertTrue(Math.signum(Math.round(cx * 2)) == -d.getStepX() && Math.signum(Math.round(cz * 2)) == -d.getStepZ(),
					d + ": back board at " + board);
		}
		helper.getLevel().setBlockAndUpdate(pos, TGBlocks.AMMO_BENCH.get().defaultBlockState());
		var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, null);
		helper.assertTrue(drops.size() == 1 && drops.get(0).is(TGBlocks.AMMO_BENCH.get().asItem()), "drops " + drops);
		helper.succeed();
	}

	/**
	 * A small box takes ammo up to 4 stacks and nothing else, gives it back first in, first out of the top, and keeps
	 * its contents through saving.
	 */
	@GameTest(template = "empty")
	public static void ammoBoxHoldsAmmo(GameTestHelper helper) {
		ItemStack box = new ItemStack(TGItems.AMMO_BOX.get());
		ItemAmmoBox item = TGItems.AMMO_BOX.get();
		helper.assertTrue(item.insert(box, new ItemStack(Items.DIRT, 10)) == 0, "a box took dirt");
		helper.assertTrue(item.insert(box, new ItemStack(TGItems.AMMO_BOX_LARGE.get())) == 0, "a box took a box");
		ItemStack rounds = new ItemStack(TGItems.PISTOL_ROUNDS.get(), 64);
		for (int i = 0; i < 5; i++) {
			item.insert(box, rounds);
			rounds.setCount(64);
		}
		helper.assertTrue(ItemAmmoBox.count(box, new ItemStack(TGItems.PISTOL_ROUNDS.get())) == 256, "rounds in a small box: "
				+ ItemAmmoBox.count(box, new ItemStack(TGItems.PISTOL_ROUNDS.get())));
		helper.assertTrue(ItemAmmoBox.occupancy(box) == item.getCapacity(), "not full: " + ItemAmmoBox.occupancy(box));
		helper.assertTrue(item.insert(box, new ItemStack(TGItems.ROCKET.get())) == 0, "a full box took a rocket");

		ItemAmmoBox.remove(box, new ItemStack(TGItems.PISTOL_ROUNDS.get()), 100);
		helper.assertTrue(item.insert(box, new ItemStack(TGItems.ROCKET.get(), 3)) == 3, "rockets didn't go in");
		helper.assertTrue(ItemAmmoBox.removeFirst(box).map(s -> s.is(TGItems.ROCKET.get()) && s.getCount() == 3).orElse(false),
				"the last stack put in doesn't come out first");

		var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		var saved = ItemStack.CODEC.encodeStart(ops, box).getOrThrow();
		ItemStack loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
		helper.assertTrue(ItemStack.isSameItemSameComponents(loaded, box) && ItemAmmoBox.count(loaded, new ItemStack(TGItems.PISTOL_ROUNDS.get())) == 156,
				"contents after saving: " + loaded.get(TGDataComponents.AMMO_BOX_CONTENTS.get()));
		helper.succeed();
	}

	/** A gun reloads from a box in the inventory, and an empty magazine from a reload goes into the box. */
	@GameTest(template = "empty")
	public static void reloadFromAmmoBox(GameTestHelper helper) {
		ServerPlayer ply = player(helper);
		ItemStack box = new ItemStack(TGItems.AMMO_BOX.get());
		TGItems.AMMO_BOX.get().insert(box, new ItemStack(TGItems.ROCKET.get(), 2));
		ply.getInventory().setItem(5, box);
		GenericGun gun = TGuns.ROCKETLAUNCHER.get();
		ItemStack launcher = new ItemStack(gun);
		launcher.set(TGDataComponents.AMMO.get(), 0);
		ply.setItemInHand(InteractionHand.MAIN_HAND, launcher);
		TGExtendedPlayer.get(ply).fireDelayMainhand = 0;
		gun.tryForcedReload(launcher, helper.getLevel(), ply, InteractionHand.MAIN_HAND);
		helper.assertTrue(gun.getCurrentAmmo(launcher) == gun.getClipsize(), "not reloaded from the box, ammo " + gun.getCurrentAmmo(launcher));
		ItemStack inBox = ply.getInventory().getItem(5);
		helper.assertTrue(ItemAmmoBox.count(inBox, new ItemStack(TGItems.ROCKET.get())) == 1, "rockets left in the box: "
				+ ItemAmmoBox.count(inBox, new ItemStack(TGItems.ROCKET.get())));

		// the ammo slots come first: fill them
		List<ItemStack> ammoSlots = techguns.curios.TGCurios.getStacks(ply, techguns.curios.TGCurios.SLOT_AMMO);
		for (int i = 0; i < ammoSlots.size(); i++) {
			ammoSlots.set(i, new ItemStack(TGItems.ROCKET_HIGH_VELOCITY.get(), 64));
		}
		int left = InventoryUtil.addAmmoToPlayerInventory(ply, new ItemStack(TGItems.PISTOL_MAGAZINE_EMPTY.get(), 2));
		helper.assertTrue(left == 0 && ItemAmmoBox.count(inBox, new ItemStack(TGItems.PISTOL_MAGAZINE_EMPTY.get())) == 2
				&& count(ply, TGItems.PISTOL_MAGAZINE_EMPTY.get()) == 0, "empty magazines didn't go into the box");
		helper.succeed();
	}
}
