package techguns.client.render.gl;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Extra render types (additive blending for muzzle flashes, lasers, glow parts). */
public final class TGRenderTypes extends RenderType {

	private TGRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
			Runnable setup, Runnable clear) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
	}

	private static final Function<ResourceLocation, RenderType> ADDITIVE = Util.memoize(tex -> create("techguns_additive",
			DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
			CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
					.setTextureState(new RenderStateShard.TextureStateShard(tex, false, false)).setTransparencyState(LIGHTNING_TRANSPARENCY)
					.setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setOverlayState(OVERLAY).createCompositeState(false)));

	public static RenderType additive(ResourceLocation texture) {
		return ADDITIVE.apply(texture);
	}

	private static final ShaderStateShard TEX_COLOR_SHADER = new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);

	private record FxKey(ResourceLocation texture, techguns.client.render.TGRenderHelper.RenderType mode) {
	}

	private static final Function<FxKey, RenderType> FX = Util.memoize(key -> {
		CompositeState.CompositeStateBuilder b = CompositeState.builder().setShaderState(TEX_COLOR_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(key.texture(), false, false)).setCullState(NO_CULL)
				.setOutputState(PARTICLES_TARGET);
		switch (key.mode()) {
			case SOLID -> b.setTransparencyState(NO_TRANSPARENCY).setWriteMaskState(COLOR_DEPTH_WRITE);
			case ALPHA, ALPHA_SHADED -> b.setTransparencyState(TRANSLUCENT_TRANSPARENCY).setWriteMaskState(COLOR_WRITE);
			case ADDITIVE -> b.setTransparencyState(LIGHTNING_TRANSPARENCY).setWriteMaskState(COLOR_WRITE);
			case NO_Z_TEST -> b.setTransparencyState(LIGHTNING_TRANSPARENCY).setWriteMaskState(COLOR_WRITE).setDepthTestState(NO_DEPTH_TEST);
		}
		return create("techguns_fx_" + key.mode().name().toLowerCase(), DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false,
				false, b.createCompositeState(false));
	});

	/** Render type for Techguns particles (1.12 TGParticle blend modes), vertex format POSITION_TEX_COLOR. */
	public static RenderType fx(ResourceLocation texture, techguns.client.render.TGRenderHelper.RenderType mode) {
		return FX.apply(new FxKey(texture, mode));
	}
}
