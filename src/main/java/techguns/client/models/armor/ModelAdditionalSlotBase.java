package techguns.client.models.armor;

import net.minecraft.world.entity.Entity;
import techguns.client.models.ModelRenderer;

/**
 * Base of the Curios face / back / hand item models. The biped part poses are copied from the vanilla model by
 * {@link techguns.client.render.armor.TGCurioRenderer}, then {@link #copyRotateAngles()} lets the model follow them.
 */
public abstract class ModelAdditionalSlotBase extends ModelBipedTG {

	public ModelAdditionalSlotBase() {
		super();
	}

	public ModelAdditionalSlotBase(float modelSize, float yOffset, int textureWidthIn, int textureHeightIn) {
		super(modelSize, yOffset, textureWidthIn, textureHeightIn);
	}

	public ModelAdditionalSlotBase(float modelSize) {
		super(modelSize);
	}

	public abstract void render(float scale, Entity entityIn);

	public abstract void copyRotateAngles();

	protected void setRotation(ModelRenderer model, float x, float y, float z) {
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}
}
