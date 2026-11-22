package techguns.client.render.tileentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import techguns.Techguns;
import techguns.blocks.door.BlockDoor3x3;
import techguns.blocks.door.Door3x3Type;
import techguns.blocks.door.DoorState;
import techguns.tileentities.Door3x3TileEnt;

/**
 * Moving parts of an opening / closing 3x3 door (1.12 RenderDoor3x3Fast). The block model shows the frame while the
 * door moves, opened and closed doors are block models only.
 */
public class RenderDoor3x3 implements BlockEntityRenderer<Door3x3TileEnt> {

	public static final ModelResourceLocation DOOR_LEFT = model("techdoor3x3_left");
	public static final ModelResourceLocation DOOR_RIGHT = model("techdoor3x3_right");
	public static final ModelResourceLocation DOOR_LEFT_NETHER = model("techdoor3x3_left_nether");
	public static final ModelResourceLocation DOOR_RIGHT_NETHER = model("techdoor3x3_right_nether");
	public static final ModelResourceLocation HANGAR_MID = model("hangar_door_mid1j");
	public static final ModelResourceLocation HANGAR_MID2 = model("hangar_door_mid2j");
	public static final ModelResourceLocation HANGAR_MID3 = model("hangar_door_mid3j");
	public static final ModelResourceLocation HANGAR_UP_LOWER = model("hangar_door_lower_obj");
	public static final ModelResourceLocation HANGAR_DOWN_UPPER = model("hangar_door_down_upper_obj");

	/** 18 pixels */
	private static final float SLIDE_DISTANCE = 1.125f;

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(Techguns.rl("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] { DOOR_LEFT, DOOR_RIGHT, DOOR_LEFT_NETHER, DOOR_RIGHT_NETHER, HANGAR_MID, HANGAR_MID2,
				HANGAR_MID3, HANGAR_UP_LOWER, HANGAR_DOWN_UPPER }) {
			event.register(m);
		}
	}

	@Override
	public void render(Door3x3TileEnt te, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
		BlockState state = te.getBlockState();
		if (!(state.getBlock() instanceof BlockDoor3x3 block) || te.getLevel() == null) {
			return;
		}
		DoorState ds = state.getValue(BlockDoor3x3.STATE);
		if (!ds.isMoving()) {
			return;
		}
		float prog = Math.min(1.0f, Math.max(0f, (te.getLevel().getGameTime() - te.getLastStateChangeTime() + partialTicks) / BlockDoor3x3.DOOR_OPEN_TICKS));
		if (ds == DoorState.CLOSING) {
			prog = 1.0f - prog;
		}
		boolean zplane = state.getValue(BlockDoor3x3.ZPLANE);
		VertexConsumer vc = buffers.getBuffer(RenderType.cutout());

		ps.pushPose();
		if (zplane) {
			// same as the y=90 rotation of the block model
			ps.translate(0.5f, 0f, 0.5f);
			ps.mulPose(Axis.YN.rotationDegrees(90f));
			ps.translate(-0.5f, 0f, -0.5f);
		}
		Door3x3Type type = block.getType();
		switch (type) {
			case METAL, NETHER -> {
				boolean nether = type == Door3x3Type.NETHER;
				float d = SLIDE_DISTANCE * prog;
				draw(ps, vc, state, nether ? DOOR_LEFT_NETHER : DOOR_LEFT, -d, 0f, light, overlay);
				draw(ps, vc, state, nether ? DOOR_RIGHT_NETHER : DOOR_RIGHT, d, 0f, light, overlay);
			}
			case HANGAR_UP -> {
				float dist = prog * 3f;
				draw(ps, vc, state, HANGAR_UP_LOWER, 0f, dist, light, overlay);
				draw(ps, vc, state, HANGAR_MID, 0f, dist, light, overlay);
				if (dist <= 2f) {
					draw(ps, vc, state, HANGAR_MID2, 0f, dist, light, overlay);
				}
				if (dist <= 1f) {
					draw(ps, vc, state, HANGAR_MID3, 0f, dist, light, overlay);
				}
			}
			case HANGAR_DOWN -> {
				float dist = prog * 3f;
				draw(ps, vc, state, HANGAR_DOWN_UPPER, 0f, -dist, light, overlay);
				draw(ps, vc, state, HANGAR_MID3, 0f, -dist, light, overlay);
				if (dist <= 2f) {
					draw(ps, vc, state, HANGAR_MID2, 0f, -dist, light, overlay);
				}
				if (dist <= 1f) {
					draw(ps, vc, state, HANGAR_MID, 0f, -dist, light, overlay);
				}
			}
		}
		ps.popPose();
	}

	private static void draw(PoseStack ps, VertexConsumer vc, BlockState state, ModelResourceLocation loc, float dx, float dy, int light, int overlay) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(loc);
		ModelBlockRenderer r = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
		ps.pushPose();
		ps.translate(dx, dy, 0f);
		r.renderModel(ps.last(), vc, state, model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.cutout());
		ps.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(Door3x3TileEnt te) {
		return new AABB(te.getBlockPos()).inflate(3, 4, 3);
	}
}
