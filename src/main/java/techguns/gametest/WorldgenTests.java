package techguns.gametest;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
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

	/**
	 * Every scanned structure has its template (data/techguns/structure/scanned), the footprint the code rotates it
	 * around, a rule for each of its data markers and no rule it doesn't use; every template there belongs to a
	 * structure. Chests in the templates have loot tables and Techguns spawners have mobs.
	 */
	@GameTest(template = "empty")
	public static void scannedTemplatesMatchRules(GameTestHelper helper) {
		var manager = helper.getLevel().getServer().getStructureManager();
		java.util.List<String> problems = new java.util.ArrayList<>();
		java.util.Set<String> defined = new java.util.HashSet<>();
		for (Map.Entry<String, ScannedStructure> e : TGStructureDefs.all().entrySet()) {
			ScannedStructure def = e.getValue();
			defined.add(def.templateId().getPath());
			var template = manager.get(def.templateId());
			if (template.isEmpty()) {
				problems.add(e.getKey() + ": no template " + def.templateId());
				continue;
			}
			var size = template.get().getSize();
			if (size.getX() != def.sizeX || size.getZ() != def.sizeZ) {
				problems.add(e.getKey() + ": template is " + size.toShortString() + ", the structure " + def.sizeX + " x " + def.sizeZ);
			}
			java.util.Set<String> used = new java.util.TreeSet<>(def.templateMarkers(manager));
			for (String m : used) {
				if (!def.markerRules().contains(m)) {
					problems.add(e.getKey() + ": no rule for the data marker \"" + m + "\"");
				}
			}
			for (String m : def.markerRules()) {
				if (!used.contains(m)) {
					problems.add(e.getKey() + ": rule \"" + m + "\" isn't used by the template");
				}
			}
			net.minecraft.nbt.CompoundTag tag = template.get().save(new net.minecraft.nbt.CompoundTag());
			for (net.minecraft.nbt.Tag t : tag.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
				net.minecraft.nbt.CompoundTag nbt = ((net.minecraft.nbt.CompoundTag) t).getCompound("nbt");
				if (nbt.getString("id").equals("minecraft:chest") && !nbt.contains("LootTable")) {
					problems.add(e.getKey() + ": chest without a loot table");
				}
				if (nbt.contains("mobtypes") && nbt.getList("mobtypes", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty()) {
					problems.add(e.getKey() + ": spawner without mobs");
				}
			}
		}
		manager.listTemplates().filter(id -> id.getNamespace().equals(Techguns.MODID) && id.getPath().startsWith("scanned/")).forEach(id -> {
			// the desert oil cluster only exists when a mod adds a worldspawn oil
			boolean oilOnly = id.getPath().equals("scanned/desert_oil_cluster") && techguns.world.gen.WorldspawnOil.block() == null;
			if (!defined.contains(id.getPath()) && !oilOnly) {
				problems.add("template " + id + " has no structure");
			}
		});
		helper.assertTrue(problems.isEmpty(), problems.size() + " problems: " + String.join("; ", problems));
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

	/**
	 * Every step of the castle's staircases has room to walk up: at least 3.5 blocks from the step's top to the lowest
	 * collision above it (4 almost everywhere; 3.5 under the top slabs holding ncmid1's chests). 1.12 had 3, and you
	 * hit your head walking up.
	 */
	@GameTest(template = "empty", timeoutTicks = 2400)
	public static void castleStairHeadroom(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TerrainView terrain = new TerrainView(level.getChunkSource().getGenerator(), level, level.getChunkSource().randomState());
		ProcGenerator gen = ProcStructures.get("castle");
		int steps = 0;
		java.util.List<String> low = new java.util.ArrayList<>();
		for (long seed = 1; seed <= 3; seed++) {
			BlockPos o = helper.absolutePos(BlockPos.ZERO).offset(-300 - (int) seed * 150, 0, 300);
			ProcGenerator.Layout layout = gen.layout(o.getX() >> 4, o.getZ() >> 4, RandomSource.create(seed), terrain);
			if (layout == null) {
				layout = new ProcGenerator.Layout(new BlockPos(o.getX(), terrain.ground(o.getX(), o.getZ()), o.getZ()), 40, 30, 40);
			}
			GenWorld w = ProcStructures.build("castle", layout, BiomeColorType.WOODLAND, seed * 31, terrain);
			BoundingBox box = ProcStructures.boundingBox(gen, layout, w);
			PlaceContext ctx = new PlaceContext(level, box, seed, BiomeColorType.WOODLAND, 0);
			w.apply(ctx);
			ctx.finishShapes();
			for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
				net.minecraft.world.level.block.state.BlockState s = level.getBlockState(p);
				if (!isStep(s)) {
					continue;
				}
				BlockPos next = p.relative(s.getValue(net.minecraft.world.level.block.StairBlock.FACING)).above();
				if (!isStep(level.getBlockState(next))) {
					continue; // a single stair, not part of a flight
				}
				steps++;
				double room = 5;
				for (int dy = 1; dy <= 5; dy++) {
					BlockPos q = p.above(dy);
					var shape = level.getBlockState(q).getCollisionShape(level, q);
					if (!shape.isEmpty()) {
						room = dy - 1 + shape.min(net.minecraft.core.Direction.Axis.Y);
						break;
					}
				}
				if (room < 3.5 && low.size() < 5) {
					low.add("seed " + seed + " " + s.getBlock().getName().getString() + " at " + p.subtract(box.getCenter()).toShortString() + ": " + room);
				}
			}
		}
		helper.assertTrue(steps > 50, "only " + steps + " castle steps found");
		helper.assertTrue(low.isEmpty(), "steps with less than 3.5 blocks of headroom: " + String.join("; ", low));
		helper.succeed();
	}

	private static boolean isStep(net.minecraft.world.level.block.state.BlockState s) {
		return s.getBlock() instanceof net.minecraft.world.level.block.StairBlock
				&& s.getValue(net.minecraft.world.level.block.StairBlock.HALF) == net.minecraft.world.level.block.state.properties.Half.BOTTOM;
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

	/**
	 * Structures turn their blocks with BlockState.rotate / mirror (MBlock.rotatedState), which only works when the
	 * block overrides them. For every Techguns block state: each Direction property turns and mirrors with the
	 * structure, and so do the per-side booleans (north, east, south, west) of blocks like the lanterns. The lamps
	 * had no override, so rotated structures kept their unrotated facing.
	 */
	@GameTest(template = "empty")
	public static void blockStatesRotateWithStructures(GameTestHelper helper) {
		var sides = net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION;
		var rotations = List.of(net.minecraft.world.level.block.Rotation.CLOCKWISE_90, net.minecraft.world.level.block.Rotation.CLOCKWISE_180,
				net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90);
		var mirrors = List.of(net.minecraft.world.level.block.Mirror.LEFT_RIGHT, net.minecraft.world.level.block.Mirror.FRONT_BACK);
		java.util.Map<String, String> problems = new java.util.TreeMap<>();
		int checked = 0;
		for (var block : BuiltInRegistries.BLOCK) {
			var id = BuiltInRegistries.BLOCK.getKey(block);
			if (!id.getNamespace().equals(Techguns.MODID)) {
				continue;
			}
			for (var state : block.getStateDefinition().getPossibleStates()) {
				for (var prop : state.getProperties()) {
					if (!(state.getValue(prop) instanceof net.minecraft.core.Direction d)) {
						continue;
					}
					checked++;
					for (var rot : rotations) {
						var r = state.rotate(rot);
						Object got = r.hasProperty(prop) ? r.getValue(prop) : null;
						if (r.getBlock() != block || got != rot.rotate(d)) {
							problems.putIfAbsent(id + " " + prop.getName(), prop.getName() + "=" + d + " turned " + rot + " gives " + got + ", not " + rot.rotate(d));
						}
					}
					for (var mir : mirrors) {
						var r = state.mirror(mir);
						Object got = r.hasProperty(prop) ? r.getValue(prop) : null;
						if (r.getBlock() != block || got != mir.mirror(d)) {
							problems.putIfAbsent(id + " " + prop.getName() + " mirror", prop.getName() + "=" + d + " mirrored " + mir + " gives " + got + ", not " + mir.mirror(d));
						}
					}
				}
				for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
					var prop = sides.get(d);
					if (!state.hasProperty(prop) || !state.getValue(prop)) {
						continue;
					}
					checked++;
					for (var rot : rotations) {
						var r = state.rotate(rot);
						var to = sides.get(rot.rotate(d));
						if (r.getBlock() != block || !r.hasProperty(to) || !r.getValue(to)) {
							problems.putIfAbsent(id + " sides", d + " turned " + rot + " isn't " + rot.rotate(d));
						}
					}
					for (var mir : mirrors) {
						var r = state.mirror(mir);
						var to = sides.get(mir.mirror(d));
						if (r.getBlock() != block || !r.hasProperty(to) || !r.getValue(to)) {
							problems.putIfAbsent(id + " sides mirror", d + " mirrored " + mir + " isn't " + mir.mirror(d));
						}
					}
				}
				if (state.hasProperty(techguns.blocks.BlockSandbags.CORNER_NE) && state.getValue(techguns.blocks.BlockSandbags.CORNER_NE)) {
					checked++;
					// the north-east corner turns to south-east, and mirrors to south-east (north/south) or north-west
					var turned = state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
					var lr = state.mirror(net.minecraft.world.level.block.Mirror.LEFT_RIGHT);
					var fb = state.mirror(net.minecraft.world.level.block.Mirror.FRONT_BACK);
					if (!turned.getValue(techguns.blocks.BlockSandbags.CORNER_ES) || !lr.getValue(techguns.blocks.BlockSandbags.CORNER_ES)
							|| !fb.getValue(techguns.blocks.BlockSandbags.CORNER_WN)) {
						problems.putIfAbsent(id + " corners", "the north-east corner turned/mirrored isn't south-east / south-east / north-west");
					}
				}
			}
		}
		helper.assertTrue(checked > 0, "no Techguns block has a direction");
		List<String> lines = new java.util.ArrayList<>();
		problems.forEach((k, v) -> lines.add(k + ": " + v));
		helper.assertTrue(lines.isEmpty(), lines.size() + " problems: " + String.join("; ", lines));
		helper.succeed();
	}
}
