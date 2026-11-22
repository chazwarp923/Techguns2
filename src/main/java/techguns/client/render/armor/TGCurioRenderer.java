package techguns.client.render.armor;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.client.models.armor.ModelAdditionalSlotBase;
import techguns.client.render.gl.TGRenderState;
import techguns.curios.TGCurios;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.ICamoChangeable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Draws a worn face / back / hand item with its 1.12 model (1.12 RenderAdditionalSlotItem + TGLayerRendererer). */
public class TGCurioRenderer implements ICurioRenderer {

	private static final float SCALE = 0.0625f;

	private final ModelAdditionalSlotBase model;
	@Nullable
	private final ModelAdditionalSlotBase modelSlim;
	private final ResourceLocation[] textures;
	@Nullable
	private final ResourceLocation[] texturesSlim;

	public TGCurioRenderer(ModelAdditionalSlotBase model, ResourceLocation... textures) {
		this(model, null, textures, null);
	}

	public TGCurioRenderer(ModelAdditionalSlotBase model, @Nullable ModelAdditionalSlotBase modelSlim, ResourceLocation[] textures,
			@Nullable ResourceLocation[] texturesSlim) {
		this.model = model;
		this.modelSlim = modelSlim;
		this.textures = textures;
		this.texturesSlim = texturesSlim;
	}

	/** textures/armors/name.png, name_1.png, ... (one per camo) */
	public static ResourceLocation[] camoTextures(String name, int count) {
		ResourceLocation[] t = new ResourceLocation[count];
		for (int i = 0; i < count; i++) {
			t[i] = Techguns.rl("textures/armors/" + name + (i == 0 ? "" : "_" + i) + ".png");
		}
		return t;
	}

	/** armor that hides the slot (1.12 TGLayerRendererer#isSlotVisible) */
	private static boolean isHidden(LivingEntity ent, String slot) {
		if (TGCurios.SLOT_FACE.equals(slot)) {
			return ent.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GenericArmor a && a.isHideFaceslot();
		}
		if (ent.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof GenericArmor a) {
			return TGCurios.SLOT_BACK.equals(slot) ? a.isHideBackslot() : a.isHideGloveslot();
		}
		return false;
	}

	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext ctx, PoseStack ps,
			RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTicks,
			float ageInTicks, float netHeadYaw, float headPitch) {
		LivingEntity ent = ctx.entity();
		if (isHidden(ent, ctx.identifier()) || !(parent.getModel() instanceof HumanoidModel<?> hm)) {
			return;
		}
		boolean slim = modelSlim != null && ent instanceof AbstractClientPlayer acp && acp.getSkin().model() == PlayerSkin.Model.SLIM;
		ModelAdditionalSlotBase m = slim ? modelSlim : model;
		ResourceLocation[] tex = slim && texturesSlim != null ? texturesSlim : textures;

		TGArmorModel.copy(hm.head, m.bipedHead);
		TGArmorModel.copy(hm.hat, m.bipedHeadwear);
		TGArmorModel.copy(hm.body, m.bipedBody);
		TGArmorModel.copy(hm.rightArm, m.bipedRightArm);
		TGArmorModel.copy(hm.leftArm, m.bipedLeftArm);
		TGArmorModel.copy(hm.rightLeg, m.bipedRightLeg);
		TGArmorModel.copy(hm.leftLeg, m.bipedLeftLeg);
		m.isSneak = hm.crouching;
		m.copyRotateAngles();

		int index = stack.getItem() instanceof ICamoChangeable c ? c.getCurrentCamoIndex(stack) : 0;
		if (index < 0 || index >= tex.length) {
			index = 0;
		}
		TGRenderState.begin(ps, buffers, light, OverlayTexture.NO_OVERLAY);
		try {
			TGRenderState.bindTexture(tex[index]);
			m.render(SCALE, ent);
		} finally {
			TGRenderState.end();
		}
	}
}
