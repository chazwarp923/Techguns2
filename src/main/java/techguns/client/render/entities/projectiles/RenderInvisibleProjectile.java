package techguns.client.render.entities.projectiles;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Projectiles that are only visible through their particle trails. */
public class RenderInvisibleProjectile<T extends Entity> extends EntityRenderer<T> {

	public RenderInvisibleProjectile(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
		return false;
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return MissingTextureAtlasSprite.getLocation();
	}
}
