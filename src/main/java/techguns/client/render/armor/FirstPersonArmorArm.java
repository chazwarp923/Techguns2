package techguns.client.render.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import techguns.Techguns;
import techguns.items.armors.GenericArmor;

/**
 * Draws the arm of a worn Techguns chestplate (custom 3d model or flat texture) over the first person arm (vanilla draws no
 * armor there). Runs before vanilla draws the arm, so the skin arm is still rendered underneath.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class FirstPersonArmorArm {

	@SubscribeEvent
	public static void onRenderArm(RenderArmEvent event) {
		AbstractClientPlayer player = event.getPlayer();
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (!(chest.getItem() instanceof GenericArmor ga)) {
			return;
		}
		TGArmorModel model = ga.getArmorModelKey() != null ? TGArmorModels.get(ga.getArmorModelKey()) : null;
		if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer pr)) {
			return;
		}
		// same model setup vanilla does in PlayerRenderer#renderHand right after this event
		PlayerModel<AbstractClientPlayer> pm = pr.getModel();
		pm.attackTime = 0.0F;
		pm.crouching = false;
		pm.swimAmount = 0.0F;
		pm.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		boolean right = event.getArm() == HumanoidArm.RIGHT;
		ModelPart arm = right ? pm.rightArm : pm.leftArm;
		arm.xRot = 0.0F;

		ResourceLocation tex = ga.getArmorTexture(chest, player, EquipmentSlot.CHEST, null, false);
		var buffer = event.getMultiBufferSource().getBuffer(RenderType.armorCutoutNoCull(tex));
		if (model != null) {
			model.renderFirstPersonArm(event.getPoseStack(), buffer, event.getPackedLight(), arm, right);
		} else {
			// flat texture armor: the arm of the vanilla outer armor model (chestplates use layer 1)
			ModelPart armorArm = right ? outerArmor().rightArm : outerArmor().leftArm;
			armorArm.copyFrom(arm);
			armorArm.visible = true;
			armorArm.render(event.getPoseStack(), buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
		}
	}

	private static EntityModelSet bakedFrom;
	private static HumanoidModel<AbstractClientPlayer> outerArmor;

	/** re-baked after a resource reload replaced the model set */
	private static HumanoidModel<AbstractClientPlayer> outerArmor() {
		EntityModelSet set = Minecraft.getInstance().getEntityModels();
		if (outerArmor == null || bakedFrom != set) {
			outerArmor = new HumanoidModel<>(set.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
			bakedFrom = set;
		}
		return outerArmor;
	}
}
