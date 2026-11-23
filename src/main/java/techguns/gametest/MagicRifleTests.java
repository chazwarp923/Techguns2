package techguns.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGDataComponents;
import techguns.TGuns;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.entities.projectiles.MagicRifleProjectile;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GenericGunCharge;

/** The Fabric port's magic rifle, fully charged and fired by a fake player looking along +x, with each bullet. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class MagicRifleTests {

	/** fires a full charge with the given ammo variant loaded; returns the shooter */
	private static ServerPlayer fire(GameTestHelper helper, String variant) {
		ServerPlayer ply = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tg_magic"));
		Vec3 p = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 1, 3)));
		ply.moveTo(p.x, p.y, p.z, -90.0F, 0.0F);
		ply.setYHeadRot(-90.0F);
		GenericGunCharge gun = (GenericGunCharge) TGuns.MAGIC_RIFLE.get();
		ItemStack stack = new ItemStack(gun);
		stack.set(TGDataComponents.AMMO.get(), 1);
		gun.setCurrentAmmoVariantKey(stack, variant);
		ply.setItemInHand(InteractionHand.MAIN_HAND, stack);
		TGExtendedPlayer.get(ply).fireDelayMainhand = 0;
		gun.use(helper.getLevel(), ply, InteractionHand.MAIN_HAND);
		helper.assertTrue(ply.isUsingItem(), "not charging the magic rifle");
		gun.releaseUsing(stack, helper.getLevel(), ply, gun.getUseDuration(stack, ply) - (int) gun.fullChargeTime);
		ply.stopUsingItem();
		helper.assertTrue(gun.getCurrentAmmo(stack) == 0, "the shot used no bullet");
		List<MagicRifleProjectile> bolts = bolts(helper);
		helper.assertTrue(bolts.size() == 1, "bolts fired: " + bolts.size());
		helper.assertTrue(bolts.get(0).chargeAmount == 1.0f, "charge " + bolts.get(0).chargeAmount);
		return ply;
	}

	private static AABB area(GameTestHelper helper) {
		return new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
	}

	private static List<MagicRifleProjectile> bolts(GameTestHelper helper) {
		return helper.getLevel().getEntitiesOfClass(MagicRifleProjectile.class, area(helper));
	}

	/** golem in the line of fire, and one beside it */
	private static IronGolem[] golems(GameTestHelper helper) {
		return new IronGolem[] { helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(5, 1, 3)),
				helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(5, 1, 5)) };
	}

	/** A plain magic bullet: the explosion hurts the golem beside, not the one hit (which only takes the bolt). */
	@GameTest(template = "empty_large")
	public static void arcaneBoltSparesHitTarget(GameTestHelper helper) {
		IronGolem[] g = golems(helper);
		fire(helper, "default");
		helper.assertTrue(bolts(helper).get(0).magicType == MagicRifleProjectile.TYPE_ARCANE, "not an arcane bolt");
		float max = g[0].getMaxHealth();
		helper.succeedWhen(() -> {
			helper.assertTrue(g[1].getHealth() < max, "the explosion missed the golem beside");
			helper.assertTrue(g[0].getHealth() < max, "the bolt missed");
			// the bolt does at most 25; the explosion at full charge would add about 49
			helper.assertTrue(max - g[0].getHealth() <= 30, "the explosion hurt the golem hit: " + (max - g[0].getHealth()));
		});
	}

	/** A fire magic bullet sets the golem hit on fire, and the explosion the golem beside. */
	@GameTest(template = "empty_large")
	public static void fireBoltBurns(GameTestHelper helper) {
		IronGolem[] g = golems(helper);
		fire(helper, "fire");
		helper.assertTrue(bolts(helper).get(0).magicType == MagicRifleProjectile.TYPE_FIRE, "not a fire bolt");
		helper.succeedWhen(() -> {
			helper.assertTrue(g[0].getRemainingFireTicks() > 0, "the golem hit isn't burning");
			helper.assertTrue(g[1].getRemainingFireTicks() > 0, "the golem beside isn't burning");
		});
	}

	/** A charged lightning magic bullet stays at the wall it hits for a while and calls several bolts. */
	@GameTest(template = "empty_large", timeoutTicks = 100)
	public static void lightningBoltLingers(GameTestHelper helper) {
		for (int y = 1; y < 6; y++) {
			for (int z = 0; z < 7; z++) {
				helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
			}
		}
		fire(helper, "lightning");
		MagicRifleProjectile bolt = bolts(helper).get(0);
		helper.assertTrue(bolt.magicType == MagicRifleProjectile.TYPE_LIGHTNING, "not a lightning bolt");
		Set<UUID> strikes = new HashSet<>();
		boolean[] lingered = { false };
		helper.onEachTick(() -> {
			helper.getLevel().getEntitiesOfClass(LightningBolt.class, area(helper).inflate(8)).forEach(b -> strikes.add(b.getUUID()));
			if (bolt.postImpactTicks > 0 && !bolt.isRemoved()) {
				lingered[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(lingered[0], "the bolt didn't stay after the hit");
			helper.assertTrue(strikes.size() >= 2, "lightning strikes: " + strikes.size());
			helper.assertTrue(bolt.isRemoved(), "the bolt is still there");
		});
	}
}
