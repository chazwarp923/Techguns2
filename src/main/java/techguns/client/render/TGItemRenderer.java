package techguns.client.render;

import java.util.IdentityHashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.api.guns.GunManager;
import techguns.api.render.IItemRenderer;
import techguns.client.render.gl.TGRenderState;

/**
 * BEWLR for all Techguns items with a custom (Java model) renderer; replaces the 1.12.2
 * ItemRenderHack coremod hook. Item models use parent "builtin/entity" without display transforms,
 * so the matrix here is the vanilla hand/GUI transform translated by (-0.5,-0.5,-0.5).
 */
public class TGItemRenderer extends BlockEntityWithoutLevelRenderer {

	private static TGItemRenderer INSTANCE;

	private static final Map<Item, IItemRenderer> RENDERERS = new IdentityHashMap<>();

	/** Entity currently being rendered (set around RenderLivingEvent), used for third person held items. */
	public static LivingEntity currentEntity = null;

	public static TGItemRenderer get() {
		if (INSTANCE == null) {
			Minecraft mc = Minecraft.getInstance();
			INSTANCE = new TGItemRenderer(mc);
		}
		return INSTANCE;
	}

	private TGItemRenderer(Minecraft mc) {
		super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
	}

	public static void register(Item item, IItemRenderer renderer) {
		RENDERERS.put(item, renderer);
	}

	public static IItemRenderer getRenderer(Item item) {
		return RENDERERS.get(item);
	}

	public static boolean hasRenderer(Item item) {
		return RENDERERS.containsKey(item);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
		IItemRenderer renderer = RENDERERS.get(stack.getItem());
		if (renderer == null) {
			return;
		}
		LivingEntity entity = getEntityFor(ctx);
		if (!shouldRenderItem(stack, entity, ctx)) {
			return;
		}
		boolean leftHand = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

		poseStack.pushPose();
		// undo ItemRenderer's -0.5 offset to get the 1.12 ItemRenderHack origin
		poseStack.translate(0.5f, 0.5f, 0.5f);
		TGRenderState.begin(poseStack, buffer, light, overlay);
		try {
			renderer.renderItem(ctx, stack, entity, leftHand);
		} finally {
			TGRenderState.end();
			poseStack.popPose();
		}
	}

	private static LivingEntity getEntityFor(ItemDisplayContext ctx) {
		switch (ctx) {
			case FIRST_PERSON_LEFT_HAND:
			case FIRST_PERSON_RIGHT_HAND:
				return Minecraft.getInstance().player;
			case THIRD_PERSON_LEFT_HAND:
			case THIRD_PERSON_RIGHT_HAND:
			case HEAD:
				return currentEntity;
			default:
				return null;
		}
	}

	/** Don't render an off hand gun if the dual wield rules forbid using it. */
	private static boolean shouldRenderItem(ItemStack stack, LivingEntity elb, ItemDisplayContext transform) {
		if (elb == null || !(transform == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || transform == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
				|| transform == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || transform == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
			return true;
		}
		boolean mainhand = transform == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || transform == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
		if (elb.getMainArm() == HumanoidArm.LEFT) {
			mainhand = !mainhand;
		}
		return mainhand || GunManager.canUseOffhand(elb.getMainHandItem(), stack, elb);
	}
}
