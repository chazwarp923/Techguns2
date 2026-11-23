package techguns;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Techguns.MODID)
public class Techguns {
	public static final String MODID = "techguns";
	public static final Logger LOGGER = LogUtils.getLogger();

	public Techguns(IEventBus modBus, ModContainer container) {
		container.registerConfig(ModConfig.Type.COMMON, TGConfig.COMMON_SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, TGConfig.CLIENT_SPEC);
		modBus.register(TGConfig.class);
		modBus.register(techguns.packets.TGPackets.class);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(techguns.damagesystem.DamageSystem.class);

		TGSounds.SOUNDS.register(modBus);
		TGDataComponents.COMPONENTS.register(modBus);
		TGEntities.ENTITIES.register(modBus);
		TGFluids.init(modBus);
		TGBlocks.BLOCKS.register(modBus);
		techguns.recipes.TGRecipes.SERIALIZERS.register(modBus);
		techguns.recipes.TGRecipes.TYPES.register(modBus);
		TGBlockEntities.BLOCK_ENTITIES.register(modBus);
		techguns.gui.TGMenus.MENUS.register(modBus);
		TGuns.init();
		TGArmors.init();
		TGArmors.ARMOR_MATERIALS.register(modBus);
		TGItems.ITEMS.register(modBus);
		TGTabs.TABS.register(modBus);
		TGAttachments.ATTACHMENTS.register(modBus);
		techguns.radiation.TGRadiationSystem.register(modBus);
		techguns.entities.spawn.TGSpawnManager.register(modBus);
		techguns.world.gen.TGStructures.register(modBus);
		techguns.world.gen.ConfigOrePlacement.register(modBus);

		modBus.addListener(TGEntities::registerAttributes);
		modBus.addListener(TGEntities::registerSpawnPlacements);
		modBus.addListener(this::commonSetup);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(TGItems::init);
		event.enqueueWork(techguns.curios.TGCurios::init);
	}

	public static ResourceLocation rl(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
