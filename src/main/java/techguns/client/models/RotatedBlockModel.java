package techguns.client.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Quaternionf;

import com.mojang.math.Transformation;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import techguns.tileentities.BasicTGTileEntity;

/**
 * A JSON block model turned by its block entity's rotation (BasicTGTileEntity.rotation, 0-3, changed with the
 * wrench) instead of a blockstate property. The rotation comes in as {@link BasicTGTileEntity#ROTATION} model data;
 * rotation r turns the model clockwise (seen from above) by r * 90 degrees, like RenderMachine did. The rotated quads
 * are made once per rotation and render type.
 */
public class RotatedBlockModel extends BakedModelWrapper<BakedModel> {

	private final Map<Key, List<BakedQuad>> rotated = new ConcurrentHashMap<>();

	private record Key(int rotation, RenderType renderType) {
	}

	public RotatedBlockModel(BakedModel original) {
		super(original);
	}

	/** wraps the models of every state of these blocks */
	public static void wrap(ModelEvent.ModifyBakingResult event, Block... blocks) {
		for (Block block : blocks) {
			for (BlockState state : block.getStateDefinition().getPossibleStates()) {
				ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(state);
				event.getModels().computeIfPresent(loc, (l, model) -> model instanceof RotatedBlockModel ? model : new RotatedBlockModel(model));
			}
		}
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
		Integer r = data.get(BasicTGTileEntity.ROTATION);
		int rotation = r == null ? 0 : Math.floorMod(r, 4);
		if (rotation == 0) {
			return super.getQuads(state, side, rand, data, renderType);
		}
		if (side != null) {
			// the machine models have no cull faces; a culled quad would need its side rotated too
			List<BakedQuad> culled = super.getQuads(state, rotateBack(side, rotation), rand, data, renderType);
			return culled.isEmpty() ? culled : rotate(culled, rotation);
		}
		return rotated.computeIfAbsent(new Key(rotation, renderType), k -> rotate(super.getQuads(state, null, rand, data, renderType), rotation));
	}

	private static List<BakedQuad> rotate(List<BakedQuad> quads, int rotation) {
		Transformation t = new Transformation(null, new Quaternionf().rotationY((float) Math.toRadians(-90.0 * rotation)), null, null).blockCenterToCorner();
		IQuadTransformer transformer = QuadTransformers.applying(t);
		List<BakedQuad> out = new ArrayList<>(quads.size());
		for (BakedQuad q : quads) {
			BakedQuad moved = transformer.process(q);
			out.add(new BakedQuad(moved.getVertices(), moved.getTintIndex(), turn(q.getDirection(), rotation), moved.getSprite(), moved.isShade(),
					moved.hasAmbientOcclusion()));
		}
		return List.copyOf(out);
	}

	/** a direction turned clockwise (seen from above) by rotation * 90 degrees */
	static Direction turn(Direction d, int rotation) {
		if (d.getAxis().isVertical()) {
			return d;
		}
		for (int i = 0; i < rotation; i++) {
			d = d.getClockWise();
		}
		return d;
	}

	private static Direction rotateBack(Direction d, int rotation) {
		return turn(d, (4 - rotation) % 4);
	}
}
