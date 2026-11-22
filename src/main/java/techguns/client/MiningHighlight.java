package techguns.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import techguns.Techguns;
import techguns.items.guns.GenericGunMeleeCharge;

/**
 * 1.12 TGEventHandler.onBlockHighlight: with an area mining tool (mining radius > 0, has ammo, not sneaking) the blocks
 * around the targeted one that would be mined as well get a selection outline too.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class MiningHighlight {

	@SubscribeEvent
	public static void onBlockHighlight(RenderHighlightEvent.Block event) {
		Minecraft mc = Minecraft.getInstance();
		Player ply = mc.player;
		if (ply == null || ply.isShiftKeyDown()) {
			return;
		}
		ItemStack item = ply.getMainHandItem();
		if (!(item.getItem() instanceof GenericGunMeleeCharge g) || g.getMiningRadius(item) <= 0 || g.getAmmoLeft(item) <= 0) {
			return;
		}
		BlockHitResult target = event.getTarget();
		Level level = ply.level();
		Vec3 cam = event.getCamera().getPosition();
		VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
		CollisionContext ctx = CollisionContext.of(ply);
		for (BlockPos p : g.getAreaMiningBlocks(ply, level, item, target.getBlockPos(), target.getDirection().getAxis())) {
			LevelRenderer.renderVoxelShape(event.getPoseStack(), lines, level.getBlockState(p).getShape(level, p, ctx), p.getX() - cam.x,
					p.getY() - cam.y, p.getZ() - cam.z, 0f, 0f, 0f, 0.4f, true);
		}
	}
}
