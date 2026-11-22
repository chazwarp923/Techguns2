package techguns.tileentities.operation;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import techguns.recipes.ReactionChamberRecipe;
import techguns.tileentities.ReactionChamberTileEntMaster;

/**
 * Running reaction (1.12 ReactionChamberOperation): every {@link #RECIPE_TICKRATE} ticks the reaction completes one step
 * if powered, the focus still matches and intensity / liquid level are right. Unstable recipes move the wanted
 * intensity around.
 */
public class ReactionChamberOperation extends MachineOperation {

	public static final int RECIPE_TICKRATE = 60;

	@Nullable
	protected ResourceLocation recipeId;
	@Nullable
	protected ReactionChamberRecipe recipe;
	public int completion = 0;
	public int requiredIntensity = 0;
	public int nextTick = RECIPE_TICKRATE;

	public ReactionChamberOperation() {
	}

	public ReactionChamberOperation(RecipeHolder<ReactionChamberRecipe> holder, ItemStack input) {
		super(holder.value().results(), List.of(input.copyWithCount(1)));
		this.recipeId = holder.id();
		this.recipe = holder.value();
		this.requiredIntensity = recipe.intensity();
		this.setPowerPerTick(recipe.power());
	}

	@Nullable
	public ReactionChamberRecipe getRecipe(@Nullable Level level) {
		if (recipe == null && recipeId != null && level != null) {
			level.getRecipeManager().byKey(recipeId).ifPresent(h -> {
				if (h.value() instanceof ReactionChamberRecipe r) {
					this.recipe = r;
				}
			});
		}
		return recipe;
	}

	/** reaction tick, returns true if something changed the server has to send */
	public boolean tick(ReactionChamberTileEntMaster tile, ReactionChamberRecipe rec, RandomSource rnd) {
		nextTick--;
		boolean client = tile.getLevel().isClientSide;
		if (!client && nextTick <= 0 && tile.progress < tile.totaltime) {
			boolean powered = tile.consumeReactionPower(rec.power());
			boolean focusMatch = tile.getFocus().is(rec.focus());
			boolean success = powered && focusMatch && this.requiredIntensity == tile.getIntensity() && rec.liquidLevel() == tile.getLiquidLevel();
			if (success) {
				completion++;
			}
			doRadiation(tile, success);
			if (!focusMatch) {
				tile.progress = tile.totaltime;
				this.completion = 0;
			} else if (rec.instability() > 0.0f && rnd.nextFloat() < rec.instability()) {
				int val = 1;
				if (rec.intensityMargin() > 1) {
					val += rnd.nextInt(rec.intensityMargin());
				}
				if (rnd.nextBoolean()) {
					this.requiredIntensity = Math.max(0, this.requiredIntensity - val);
				} else {
					this.requiredIntensity = Math.min(ReactionChamberRecipe.MAX_LEVEL, this.requiredIntensity + val);
				}
			}
			nextTick = RECIPE_TICKRATE;
			return true;
		}
		return isFinished(tile, rec);
	}

	/** reactions give off radiation depending on focus and intensity, more when failing */
	private void doRadiation(ReactionChamberTileEntMaster tile, boolean success) {
		if (!techguns.radiation.TGRadiationSystem.isEnabled()) {
			return;
		}
		ReactionBeamFocus focus = ReactionBeamFocus.get(tile.getFocus());
		if (focus == null) {
			return;
		}
		int rad = (int) Math.ceil((success ? focus.radiationFactorSuccess() : focus.radiationFactorFailure()) * tile.getIntensity());
		if (rad > 0) {
			net.minecraft.core.BlockPos p = tile.getBlockPos();
			techguns.radiation.TGRadiationSystem.applyRadToEntities(tile.getLevel(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, focus.radRadiusOuter(), 62,
					rad - 1, focus.radRadiusInner(), 0);
		}
	}

	public boolean isFinished(ReactionChamberTileEntMaster tile, ReactionChamberRecipe rec) {
		return tile.progress >= tile.totaltime || isSuccess(rec);
	}

	public boolean isSuccess(ReactionChamberRecipe rec) {
		return this.completion >= rec.requiredCompletion();
	}

	@Override
	protected void writeSubclassData(CompoundTag tags, HolderLookup.Provider reg) {
		if (recipeId != null) {
			tags.putString("recipe", recipeId.toString());
		}
		tags.putByte("completion", (byte) completion);
		tags.putByte("requiredIntensity", (byte) requiredIntensity);
		tags.putByte("nextTick", (byte) nextTick);
	}

	@Override
	protected void readSubclassData(CompoundTag tags, HolderLookup.Provider reg) {
		this.recipeId = tags.contains("recipe") ? ResourceLocation.tryParse(tags.getString("recipe")) : null;
		this.recipe = null;
		this.completion = tags.getByte("completion");
		this.requiredIntensity = tags.getByte("requiredIntensity");
		this.nextTick = tags.getByte("nextTick");
	}
}
