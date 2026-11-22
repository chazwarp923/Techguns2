package techguns.client.render.entities.npcs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.npcs.ModelTurret;
import techguns.client.render.gl.TGRenderState;
import techguns.entities.npcs.NPCTurret;
import techguns.items.guns.GenericGun;

/** Turret entity (1.12 RenderNPCTurret): mount turns with the head yaw, the gun with the pitch. */
public class RenderNPCTurret extends EntityRenderer<NPCTurret> {

	private static final ModelTurret MODEL = new ModelTurret();
	private static final float GUN_OFFSET_Y = 0.85f;

	public RenderNPCTurret(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 0f;
	}

	@Override
	public ResourceLocation getTextureLocation(NPCTurret entity) {
		return entity.getTexture();
	}

	@Override
	public void render(NPCTurret turret, float entityYaw, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light) {
		ps.pushPose();
		Direction facing = turret.getTurretFacing();
		float mirrorYaw = 1f;
		float mirrorPitch = 1f;
		float yawOffset = 0f;
		if (facing == Direction.DOWN) {
			mirrorYaw = -1f;
			mirrorPitch = -1f;
			yawOffset = 180f;
			ps.translate(0, -0.6f, 0);
		} else {
			ps.translate(0, 1.5f, 0);
			ps.mulPose(Axis.XP.rotationDegrees(180f));
		}

		float yaw = Mth.rotLerp(partialTicks, turret.yHeadRotO, turret.yHeadRot);
		float pitch = Mth.lerp(partialTicks, turret.xRotO, turret.getXRot());
		ps.mulPose(Axis.YP.rotationDegrees(yaw * mirrorYaw + yawOffset));

		int overlay = OverlayTexture.pack(OverlayTexture.u(0f), OverlayTexture.v(turret.hurtTime > 0 || turret.deathTime > 0));
		TGRenderState.begin(ps, buffers, light, overlay);
		try {
			TGRenderState.bindTexture(getTextureLocation(turret));
			MODEL.render(turret, 0, 0, 0, 0, 0, 0.0625F, 0, 0.0f, ItemDisplayContext.FIXED, 0, 0.0f, 0f);
			float rotoffset = 0.9f;
			ps.translate(0, rotoffset, 0);
			ps.mulPose(Axis.XP.rotationDegrees(pitch * mirrorPitch));
			ps.translate(0, -rotoffset, 0);
			MODEL.render(turret, 0, 0, 0, 0, 0, 0.0625F, 0, 0.0f, ItemDisplayContext.FIXED, 1, 0.0f, 0f);
		} finally {
			TGRenderState.end();
		}

		ItemStack gunstack = turret.getMainHandItem();
		if (!gunstack.isEmpty() && gunstack.getItem() instanceof GenericGun gun) {
			ps.pushPose();
			ps.translate(gun.turretPosOffsetX, gun.turretPosOffsetY + GUN_OFFSET_Y, gun.turretPosOffsetZ);
			ps.mulPose(Axis.XP.rotationDegrees(180f));
			ps.mulPose(Axis.YP.rotationDegrees(180f));
			Minecraft.getInstance().getItemRenderer().renderStatic(turret, gunstack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, ps, buffers,
					turret.level(), light, OverlayTexture.NO_OVERLAY, turret.getId());
			ps.popPose();
		}
		ps.popPose();
		super.render(turret, entityYaw, partialTicks, ps, buffers, light);
	}
}
