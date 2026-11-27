package techguns.gametest;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGuns;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.entities.projectiles.GrapplingHookProjectile;
import techguns.items.guns.GenericGun;

/**
 * The Fabric port's grappling hook, fired from the gun by a fake player (not ticked, so it doesn't move; the tests
 * look at the motion the hook gives).
 */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class GrapplingHookTests {

	/** a player at rel, looking along +x, holding use with a loaded grappling hook */
	private static ServerPlayer fire(GameTestHelper helper, BlockPos rel, boolean sneak) {
		ServerPlayer ply = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tg_hook"));
		Vec3 p = helper.absoluteVec(Vec3.atBottomCenterOf(rel));
		ply.moveTo(p.x, p.y, p.z, -90.0F, 0.0F);
		ply.setYHeadRot(-90.0F);
		ply.setShiftKeyDown(sneak);
		GenericGun gun = TGuns.GRAPPLING_HOOK.get();
		ItemStack stack = new ItemStack(gun);
		stack.set(techguns.TGDataComponents.AMMO.get(), gun.getClipsize());
		ply.setItemInHand(InteractionHand.MAIN_HAND, stack);
		TGExtendedPlayer.get(ply).fireDelayMainhand = 0;
		gun.use(helper.getLevel(), ply, InteractionHand.MAIN_HAND);
		helper.assertTrue(ply.isUsingItem(), "not using the grappling hook");
		return ply;
	}

	/** the shooter's hooks (tests run side by side, so not the others') */
	private static List<GrapplingHookProjectile> hooks(GameTestHelper helper, ServerPlayer shooter) {
		AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
		return helper.getLevel().getEntitiesOfClass(GrapplingHookProjectile.class, box, h -> h.getShooterEntity() == shooter);
	}

	/**
	 * Fired at a wall, the hook sticks and pulls the player towards it; letting go removes it. It used one round.
	 */
	@GameTest(template = "empty_large")
	public static void hookPullsToBlock(GameTestHelper helper) {
		for (int y = 1; y < 6; y++) {
			for (int z = 0; z < 7; z++) {
				helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
			}
		}
		ServerPlayer ply = fire(helper, new BlockPos(0, 1, 3), false);
		GenericGun gun = TGuns.GRAPPLING_HOOK.get();
		helper.assertTrue(gun.getCurrentAmmo(ply.getMainHandItem()) == gun.getClipsize() - 1,
				"ammo after firing: " + gun.getCurrentAmmo(ply.getMainHandItem()));
		helper.assertTrue(hooks(helper, ply).size() == 1, "hooks fired: " + hooks(helper, ply).size());
		helper.startSequence().thenWaitUntil(() -> {
			var h = hooks(helper, ply);
			helper.assertTrue(h.size() == 1 && h.get(0).getStatus() == GrapplingHookProjectile.Status.GRAPPLING_BLOCK, "the hook didn't grab the wall");
			Vec3 at = helper.relativeVec(h.get(0).position());
			helper.assertTrue(Math.abs(at.x - 6.0) < 0.3, "the hook grabbed the wall but sits at " + at + ", not on its face at x 6");
			helper.assertTrue(ply.getDeltaMovement().x > 0.1, "the player isn't pulled to the wall: " + ply.getDeltaMovement());
		}).thenExecute(() -> {
			ply.stopUsingItem();
			ply.setDeltaMovement(Vec3.ZERO);
		}).thenWaitUntil(() -> helper.assertTrue(hooks(helper, ply).isEmpty(), "the hook stayed after letting go"))
				.thenExecuteAfter(2, () -> helper.assertTrue(ply.getDeltaMovement().lengthSqr() == 0, "still pulled after letting go"))
				.thenSucceed();
	}

	/**
	 * Fired at a mob while sneaking, the hook pulls the mob (which doesn't move by itself) to the player and lets go
	 * within a block; without sneaking it pulls the player to the mob.
	 */
	@GameTest(template = "empty_large")
	public static void hookPullsEntity(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 1, 3));
		ServerPlayer ply = fire(helper, new BlockPos(0, 1, 3), true);
		ServerPlayer[] second = new ServerPlayer[1];
		helper.startSequence().thenWaitUntil(() -> {
			double dx = zombie.getX() - ply.getX(), dz = zombie.getZ() - ply.getZ();
			helper.assertTrue(Math.sqrt(dx * dx + dz * dz) < 1.5, "the zombie isn't pulled in: " + zombie.position() + " player " + ply.position());
			helper.assertTrue(hooks(helper, ply).isEmpty(), "the hook didn't let go of the pulled-in zombie");
		}).thenExecute(() -> {
			ply.stopUsingItem();
			ply.setShiftKeyDown(false);
		}).thenWaitUntil(() -> helper.assertTrue(hooks(helper, ply).isEmpty(), "the hook stayed after letting go")).thenExecute(() -> {
			zombie.setDeltaMovement(Vec3.ZERO);
			zombie.teleportTo(helper.absoluteVec(new Vec3(5.5, 1, 3.5)).x, zombie.getY(), helper.absoluteVec(new Vec3(5.5, 1, 3.5)).z);
			second[0] = fire(helper, new BlockPos(0, 1, 3), false);
		}).thenWaitUntil(() -> {
			var h = hooks(helper, second[0]);
			helper.assertTrue(h.size() == 1 && h.get(0).getStatus() == GrapplingHookProjectile.Status.GRAPPLING_ENTITY, "no grab without sneaking");
			var shooter = h.get(0).getShooterEntity();
			helper.assertTrue(shooter.getDeltaMovement().x > 0.1, "the player isn't pulled to the zombie: " + shooter.getDeltaMovement());
		}).thenSucceed();
	}
}
