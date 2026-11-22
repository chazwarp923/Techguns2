package techguns.gametest;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;
import techguns.world.gen.GenWorld;
import techguns.world.gen.PlaceContext;
import techguns.world.gen.PlaceContext.BiomeColorType;
import techguns.world.gen.ProcGenerator;
import techguns.world.gen.ProcStructures;
import techguns.world.gen.ScannedStructure;
import techguns.world.gen.TGStructureDefs;
import techguns.world.gen.TerrainView;
import techguns.world.gen.dungeon.DungeonTemplate;

/**
 * Smoke tests for the structure code, run with the gameTestServer task: every template and structure must build and
 * place without throwing. They don't check what the result looks like.
 */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class WorldgenTests {

	private static final String[] TEMPLATES = { "ncdung1", "nclower1", "ncmid1", "ncupper1", "nctop1", "ncroof1", "nether0", "nether1",
			"nether_b0", "nether_b1", "tfmain", "tfupper" };

	@GameTest(template = "empty")
	public static void dungeonTemplatesLoad(GameTestHelper helper) {
		for (String name : TEMPLATES) {
			DungeonTemplate t = DungeonTemplate.get(name, "chests/castle");
			int n = 0;
			for (techguns.world.gen.dungeon.SegmentType type : techguns.world.gen.dungeon.SegmentType.values()) {
				if (t.segment(type) != null) {
					n++;
				}
			}
			helper.assertTrue(n == 16, name + " has " + n + " segments");
		}
		helper.succeed();
	}

	/** builds every procedural structure for several seeds, then places one of each into the level */
	@GameTest(template = "empty", timeoutTicks = 2400)
	public static void proceduralStructuresBuild(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TerrainView terrain = new TerrainView(level.getChunkSource().getGenerator(), level, level.getChunkSource().randomState());
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(256, 0, 0);
		int i = 0;
		for (String name : new String[] { "castle", "military_base", "alien_bug_nest" }) {
			ProcGenerator gen = ProcStructures.get(name);
			helper.assertTrue(gen != null, "no generator " + name);
			BlockPos o = base.offset(i++ * 128, 0, 0);
			for (long seed = 1; seed <= 8; seed++) {
				RandomSource rnd = RandomSource.create(seed);
				ProcGenerator.Layout layout = gen.layout(o.getX() >> 4, o.getZ() >> 4, rnd, terrain);
				if (layout == null) {
					layout = new ProcGenerator.Layout(new BlockPos(o.getX(), terrain.ground(o.getX(), o.getZ()), o.getZ()), 40, 30, 40);
				}
				GenWorld w = ProcStructures.build(name, layout, BiomeColorType.WOODLAND, seed * 31 + i, terrain);
				helper.assertTrue(!w.isEmpty(), name + " seed " + seed + " built nothing");
				if (seed == 1) {
					BoundingBox box = ProcStructures.boundingBox(gen, layout, w);
					PlaceContext ctx = new PlaceContext(level, box, seed, BiomeColorType.WOODLAND, 0);
					if (gen.flattenGround()) {
						ProcStructures.flatten(ctx, layout);
					}
					w.apply(ctx);
					ctx.finishShapes();
					checkDoors(helper, level, box, name);
				}
			}
		}
		helper.succeed();
	}

	/** places every scanned structure into the level */
	@GameTest(template = "empty", timeoutTicks = 2400)
	public static void scannedStructuresPlace(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(0, 0, 256);
		int i = 0;
		for (Map.Entry<String, ScannedStructure> e : TGStructureDefs.all().entrySet()) {
			BlockPos origin = base.offset((i % 6) * 80, 0, (i / 6) * 80);
			i++;
			BoundingBox box = new BoundingBox(origin.getX() - 40, level.getMinBuildHeight(), origin.getZ() - 40, origin.getX() + 40,
					level.getMaxBuildHeight() - 1, origin.getZ() + 40);
			for (int dir = 0; dir < 4; dir++) {
				// clear between rotations so the door check sees one rotation at a time
				BlockPos.betweenClosed(origin.offset(-40, -24, -40), origin.offset(40, 40, 40))
						.forEach(p -> level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16));
				e.getValue().place(new PlaceContext(level, box, 7 + dir, BiomeColorType.WOODLAND, 0), origin, dir);
				checkDoors(helper, level, new BoundingBox(origin.getX() - 40, origin.getY() - 24, origin.getZ() - 40, origin.getX() + 40,
						origin.getY() + 40, origin.getZ() + 40), e.getKey() + " rotation " + dir);
			}
		}
		helper.succeed();
	}

	/** every door lower half has a matching upper half above it (same facing, hinge, open state) */
	private static void checkDoors(GameTestHelper helper, ServerLevel level, BoundingBox box, String what) {
		for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY() - 1, box.maxZ())) {
			net.minecraft.world.level.block.state.BlockState s = level.getBlockState(p);
			if (s.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
					&& s.getValue(net.minecraft.world.level.block.DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
				net.minecraft.world.level.block.state.BlockState up = level.getBlockState(p.above());
				boolean ok = up.getBlock() == s.getBlock()
						&& up.getValue(net.minecraft.world.level.block.DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER
						&& up.getValue(net.minecraft.world.level.block.DoorBlock.FACING) == s.getValue(net.minecraft.world.level.block.DoorBlock.FACING)
						&& up.getValue(net.minecraft.world.level.block.DoorBlock.HINGE) == s.getValue(net.minecraft.world.level.block.DoorBlock.HINGE)
						&& up.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) == s.getValue(net.minecraft.world.level.block.DoorBlock.OPEN);
				helper.assertTrue(ok, what + ": mismatched door at " + p.toShortString() + " " + s + " / " + up);
			}
		}
	}
}
