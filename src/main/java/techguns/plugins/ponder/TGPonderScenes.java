package techguns.plugins.ponder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import techguns.TGItems;
import techguns.blocks.machines.multiblocks.MultiBlockSchematic;
import techguns.tileentities.MultiBlockMachineTileEntMaster;

/**
 * The multiblock scenes. Schematics are in assets/techguns/ponder, the base plate is y=0 and every controller faces
 * north, toward the camera. Every .text() is a lang key techguns.ponder.&lt;scene&gt;.text_N (in call order).
 */
public final class TGPonderScenes {

	private TGPonderScenes() {
	}

	public static void fabricator(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("fabricator", "Building the Fabricator");
		scene.configureBasePlate(0, 0, 4);
		scene.showBasePlate();
		scene.idle(10);

		BlockPos controller = util.grid().at(1, 1, 1);
		Selection housings = util.select().position(2, 1, 1).add(util.select().fromTo(1, 1, 2, 2, 1, 2));
		Selection glass = util.select().layer(2);

		scene.world().showSection(housings, Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70).attachKeyFrame().text("The Fabricator is a 2x2x2 multiblock. The bottom layer is 3 Fabricator Housings...")
				.pointAt(util.vector().topOf(2, 1, 2)).placeNearTarget();
		scene.idle(80);

		scene.world().showSection(util.select().position(controller), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70).attachKeyFrame().text("...and the Controller, at the front right corner")
				.pointAt(util.vector().blockSurface(controller, Direction.NORTH)).placeNearTarget();
		scene.idle(80);

		scene.world().showSection(glass, Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(60).attachKeyFrame().text("The top layer is 4 Fabricator Glass blocks")
				.pointAt(util.vector().topOf(1, 2, 2)).placeNearTarget();
		scene.idle(70);

		formWithRightClick(scene, util, controller, Direction.NORTH);
		scene.overlay().showText(80).attachKeyFrame()
				.text("Right-click the front of the Controller to form the machine. The side you click becomes its front")
				.pointAt(util.vector().blockSurface(controller, Direction.NORTH)).placeNearTarget();
		scene.idle(90);

		scene.overlay().showText(90).attachKeyFrame()
				.text("It needs power, and has slots for the main input, wires, powder or parts, plates and an upgrade")
				.pointAt(util.vector().topOf(1, 2, 1)).placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void reactionChamber(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("reaction_chamber", "Building the Reaction Chamber");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(10);

		BlockPos controller = util.grid().at(2, 1, 1);

		scene.world().showSection(util.select().layer(1), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(80).attachKeyFrame()
				.text("The Reaction Chamber is 3x4x3. The bottom layer is Housings with the Controller in the middle of the front")
				.pointAt(util.vector().blockSurface(controller, Direction.NORTH)).placeNearTarget();
		scene.idle(90);

		scene.world().showSection(util.select().layers(2, 2), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(60).attachKeyFrame().text("Then two layers of Reaction Chamber Glass")
				.pointAt(util.vector().blockSurface(util.grid().at(1, 3, 1), Direction.NORTH)).placeNearTarget();
		scene.idle(70);

		scene.world().showSection(util.select().layer(4), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(60).attachKeyFrame().text("And a layer of Housings on top")
				.pointAt(util.vector().topOf(2, 4, 2)).placeNearTarget();
		scene.idle(70);

		formWithRightClick(scene, util, controller, Direction.NORTH);
		scene.overlay().showText(70).attachKeyFrame().text("Right-click the front of the Controller to form the chamber")
				.pointAt(util.vector().blockSurface(controller, Direction.NORTH)).placeNearTarget();
		scene.idle(80);

		scene.overlay().showOutlineWithText(util.select().position(2, 4, 2).add(util.select().position(controller)), 80)
				.colored(PonderPalette.RED).attachKeyFrame()
				.text("Power goes into the middle of the top layer or straight into the Controller")
				.pointAt(util.vector().topOf(2, 4, 2)).placeNearTarget();
		scene.idle(90);

		Selection ports = util.select().position(1, 1, 2).add(util.select().position(3, 1, 2)).add(util.select().position(2, 1, 3));
		scene.overlay().showOutlineWithText(ports, 90).colored(PonderPalette.INPUT).attachKeyFrame()
				.text("Items and liquids are piped in and out at the middle Housing of each other side of the bottom layer, from the side or from below")
				.pointAt(util.vector().blockSurface(util.grid().at(1, 1, 2), Direction.WEST)).placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void oreDrillSmall(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("ore_drill_small", "Building a small Ore Drill");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(10);

		BlockPos controller = util.grid().at(2, 3, 2);

		scene.world().showSection(util.select().layer(1), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(80).attachKeyFrame()
				.text("Ore Drills mine Ore Clusters. Clusters are found in the world, can't be broken and never run out")
				.pointAt(util.vector().topOf(2, 1, 2)).placeNearTarget();
		scene.idle(90);

		scene.world().showSection(util.select().position(2, 2, 2), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70).attachKeyFrame().text("Start with the Drill Rods next to the cluster")
				.pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.NORTH)).placeNearTarget();
		scene.idle(80);

		scene.world().showSection(util.select().position(controller), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70).attachKeyFrame().text("The smallest drill is just a Controller on a single Rod")
				.pointAt(util.vector().blockSurface(controller, Direction.NORTH)).placeNearTarget();
		scene.idle(80);

		formWithRightClick(scene, util, controller, Direction.NORTH);
		scene.overlay().showText(70).attachKeyFrame().text("Right-click the Controller to form the drill")
				.pointAt(util.vector().blockSurface(controller, Direction.NORTH)).placeNearTarget();
		scene.idle(80);

		scene.overlay().showControls(util.vector().topOf(controller), Pointing.DOWN, 60)
				.withItem(new ItemStack(TGItems.OREDRILLHEAD_STEEL.get()));
		scene.overlay().showText(90).attachKeyFrame()
				.text("Put a drill head into the Controller: a small one for this drill. Power it with FE, furnace fuel or liquid fuel")
				.pointAt(util.vector().topOf(controller)).placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void oreDrill(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("ore_drill", "Building a bigger Ore Drill");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(10);

		BlockPos controller = util.grid().at(2, 6, 2);

		scene.world().showSection(util.select().layer(1), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().column(2, 2).substract(util.select().layersFrom(4)).substract(util.select().layer(0))
				.substract(util.select().layer(1)), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70).attachKeyFrame().text("Bigger drills have Rods on the cluster side...")
				.pointAt(util.vector().blockSurface(util.grid().at(2, 3, 2), Direction.NORTH)).placeNearTarget();
		scene.idle(80);

		scene.world().showSection(util.select().fromTo(2, 4, 2, 2, 5, 2), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70).attachKeyFrame().text("...and Engines between the Rods and the Controller")
				.pointAt(util.vector().blockSurface(util.grid().at(2, 5, 2), Direction.NORTH)).placeNearTarget();
		scene.idle(80);

		Selection ring = util.select().fromTo(1, 2, 1, 3, 5, 3).substract(util.select().column(2, 2));
		scene.world().showSection(ring, Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(90).attachKeyFrame()
				.text("Frames cover the ends and corners, Scaffolds the sides in between. Wider engines need a bigger drill head")
				.pointAt(util.vector().blockSurface(util.grid().at(1, 3, 1), Direction.NORTH)).placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(controller), Direction.DOWN);
		scene.idle(10);
		formWithRightClick(scene, util, controller, Direction.NORTH);
		scene.overlay().showText(80).attachKeyFrame()
				.text("The Controller goes on the end of the Engines. Right-click it to form the drill")
				.pointAt(util.vector().blockSurface(controller, Direction.NORTH)).placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}

	/** shows a right-click on a side of the controller, then forms the multiblock like the real controller would */
	private static void formWithRightClick(SceneBuilder scene, SceneBuildingUtil util, BlockPos controller, Direction side) {
		scene.overlay().showControls(util.vector().blockSurface(controller, side), Pointing.DOWN, 40).rightClick();
		scene.idle(20);
		scene.addInstruction(ponder -> {
			form(ponder.getWorld(), controller, side);
			ponder.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
		});
		scene.idle(20);
	}

	/** runs the machine's own schematic on the scene's virtual level */
	static void form(PonderLevel level, BlockPos controller, Direction side) {
		if (!(level.getBlockEntity(controller) instanceof MultiBlockMachineTileEntMaster master)) {
			return;
		}
		MultiBlockSchematic schematic = master.getSchematic();
		if (schematic == null) {
			return;
		}
		schematic.form(level, master, side);
		// the virtual level does not hand changed states to its block entities (the renderers check them)
		BlockPos.betweenClosedStream(controller.offset(-8, -8, -8), controller.offset(8, 8, 8)).forEach(p -> {
			BlockEntity be = level.getBlockEntity(p);
			if (be != null) {
				be.setBlockState(level.getBlockState(p));
			}
		});
	}
}
