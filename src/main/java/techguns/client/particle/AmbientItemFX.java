package techguns.client.particle;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import techguns.Techguns;
import techguns.api.render.IItemRenderer;
import techguns.client.render.TGItemRenderer;
import techguns.client.render.item.RenderItemBase;
import techguns.items.guns.GenericGun;

/**
 * Ambient particle effects of held guns (the Shishkebap's flames). 1.12 started and stopped them from server packets
 * (PacketNotifyAmbientEffectChange on equipment change and when the fuel ran out); the client knows both the held items
 * and their ammo, so this checks every tick instead.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class AmbientItemFX {

	/** per entity: the item whose effect is running in each hand (main, off), null if none */
	private static final Map<LivingEntity, Item[]> ACTIVE = new WeakHashMap<>();

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			ACTIVE.clear();
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		ACTIVE.keySet().removeIf(Entity::isRemoved);
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof LivingEntity living) {
				update(living, InteractionHand.MAIN_HAND, 0);
				update(living, InteractionHand.OFF_HAND, 1);
			}
		}
	}

	private static void update(LivingEntity ent, InteractionHand hand, int index) {
		ItemStack stack = ent.getItemInHand(hand);
		String fx = ambientFx(stack);
		Item want = fx != null ? stack.getItem() : null;
		Item[] active = ACTIVE.get(ent);
		Item running = active != null ? active[index] : null;
		if (want == running) {
			return;
		}
		if (active == null) {
			active = new Item[2];
			ACTIVE.put(ent, active);
		}
		active[index] = want;
		TGEntityParticles attachments = TGEntityParticles.get(ent);
		if (fx == null) {
			attachments.clearAttachedSystemsHand(hand);
			return;
		}
		List<TGParticleSystem> systems = TGFX.createFXOnEntityItemAttached(ent, hand, fx);
		if (systems != null) {
			attachments.addSystemsHand(hand, systems);
		} else {
			attachments.clearAttachedSystemsHand(hand);
		}
	}

	/** the ambient FX of a held gun that still has ammo, or null */
	private static String ambientFx(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof GenericGun gun) || !gun.isAmbientEffectActive(stack)) {
			return null;
		}
		IItemRenderer renderer = TGItemRenderer.getRenderer(stack.getItem());
		return renderer instanceof RenderItemBase rib ? rib.getAmbientParticleFX() : null;
	}
}
