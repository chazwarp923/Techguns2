package techguns.plugins.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.blocks.OreClusterType;

/**
 * Ponder scenes for the multiblock machines (hold W over a machine part). Only loaded when Ponder is installed, see
 * TGClientSetup. Scene text lives in the lang file under techguns.ponder.*.
 */
public class TGPonderPlugin implements PonderPlugin {

	public static final ResourceLocation TAG_MULTIBLOCKS = Techguns.rl("multiblocks");

	public static void register() {
		PonderIndex.addPlugin(new TGPonderPlugin());
	}

	@Override
	public String getModId() {
		return Techguns.MODID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemLike> h = helper.withKeyFunction(i -> BuiltInRegistries.ITEM.getKey(i.asItem()));
		h.forComponents(TGBlocks.FABRICATOR_CONTROLLER.get(), TGBlocks.FABRICATOR_HOUSING.get(), TGBlocks.FABRICATOR_GLASS.get())
				.addStoryBoard("fabricator", TGPonderScenes::fabricator, TAG_MULTIBLOCKS);
		h.forComponents(TGBlocks.REACTIONCHAMBER_CONTROLLER.get(), TGBlocks.REACTIONCHAMBER_HOUSING.get(), TGBlocks.REACTIONCHAMBER_GLASS.get())
				.addStoryBoard("reaction_chamber", TGPonderScenes::reactionChamber, TAG_MULTIBLOCKS);
		h.forComponents(TGBlocks.OREDRILL_CONTROLLER.get(), TGBlocks.OREDRILL_ROD.get(), TGBlocks.OREDRILL_ENGINE.get(),
				TGBlocks.OREDRILL_FRAME.get(), TGBlocks.OREDRILL_SCAFFOLD.get(), TGBlocks.ORE_CLUSTERS.get(OreClusterType.COMMON_METAL).get())
				.addStoryBoard("ore_drill_small", TGPonderScenes::oreDrillSmall, TAG_MULTIBLOCKS)
				.addStoryBoard("ore_drill", TGPonderScenes::oreDrill, TAG_MULTIBLOCKS);
	}

	@Override
	public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
		PonderTagRegistrationHelper<ItemLike> h = helper.withKeyFunction(i -> BuiltInRegistries.ITEM.getKey(i.asItem()));
		h.registerTag(TAG_MULTIBLOCKS).addToIndex().item(TGBlocks.FABRICATOR_CONTROLLER.get(), true, false)
				.title("Techguns Multiblocks").description("Machines built from several blocks").register();
		h.addToTag(TAG_MULTIBLOCKS).add(TGBlocks.FABRICATOR_CONTROLLER.get()).add(TGBlocks.REACTIONCHAMBER_CONTROLLER.get())
				.add(TGBlocks.OREDRILL_CONTROLLER.get());
	}
}
