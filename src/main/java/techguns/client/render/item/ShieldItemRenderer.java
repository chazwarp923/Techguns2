package techguns.client.render.item;

import java.util.IdentityHashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.ModelBase;
import techguns.client.render.gl.TGRenderState;
import techguns.items.armors.GenericShield;

/** 1.12 TileEntityItemRendererTGShield: shield model with one texture per camo, uses the vanilla shield transforms. */
public class ShieldItemRenderer extends BlockEntityWithoutLevelRenderer {

	private static ShieldItemRenderer INSTANCE;

	private record Entry(ModelBase model, ResourceLocation[] textures) {
	}

	private static final Map<Item, Entry> ENTRIES = new IdentityHashMap<>();

	public static ShieldItemRenderer get() {
		if (INSTANCE == null) {
			Minecraft mc = Minecraft.getInstance();
			INSTANCE = new ShieldItemRenderer(mc);
		}
		return INSTANCE;
	}

	private ShieldItemRenderer(Minecraft mc) {
		super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
	}

	public static void register(Item item, ModelBase model, ResourceLocation... textures) {
		ENTRIES.put(item, new Entry(model, textures));
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffer, int light, int overlay) {
		Entry e = ENTRIES.get(stack.getItem());
		if (e == null || !(stack.getItem() instanceof GenericShield s)) {
			return;
		}
		int index = s.getCurrentCamoIndex(stack);
		if (index < 0 || index >= e.textures.length) {
			index = 0;
		}
		ps.pushPose();
		TGRenderState.begin(ps, buffer, light, overlay);
		try {
			TGRenderState.bindTexture(e.textures[index]);
			ps.scale(1.0F, -1.0F, -1.0F);
			e.model.render(null, 0, 0, 0, 0, 0, 0.0625F);
		} finally {
			TGRenderState.end();
			ps.popPose();
		}
	}
}
