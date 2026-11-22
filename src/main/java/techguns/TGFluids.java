package techguns;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import techguns.api.damagesystem.DamageType;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.entities.projectiles.GenericProjectile;

/**
 * Techguns fluids (1.12 TGFluids): creeper acid with its fluid block and bucket. Milk is the NeoForge milk fluid,
 * water and lava are vanilla. Recipes use fluid tags, so fuels / oils of other mods can be added to them.
 */
public class TGFluids {

	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Techguns.MODID);
	public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID,
			Techguns.MODID);

	public static final DeferredHolder<FluidType, FluidType> ACID_TYPE = FLUID_TYPES.register("creeper_acid",
			() -> new FluidType(FluidType.Properties.create().descriptionId("block.techguns.creeper_acid").density(100).canSwim(true)
					.canDrown(true).supportsBoating(false)));

	public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> ACID = FLUIDS.register("creeper_acid",
			() -> new BaseFlowingFluid.Source(TGFluids.acidProps()));
	public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> ACID_FLOWING = FLUIDS
			.register("creeper_acid_flowing", () -> new BaseFlowingFluid.Flowing(TGFluids.acidProps()));

	public static final DeferredBlock<LiquidBlock> ACID_BLOCK = TGBlocks.registerNoItem("block_creeper_acid",
			p -> new AcidBlock(ACID.get(), p), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).replaceable().noCollission()
					.strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

	public static final DeferredItem<BucketItem> ACID_BUCKET = TGItems.register("creeper_acid_bucket",
			() -> new BucketItem(ACID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

	private static BaseFlowingFluid.Properties acidProps() {
		return new BaseFlowingFluid.Properties(ACID_TYPE, ACID, ACID_FLOWING).block(ACID_BLOCK).bucket(ACID_BUCKET).slopeFindDistance(4)
				.levelDecreasePerBlock(1);
	}

	/** called from the mod constructor, before registration */
	public static void init(net.neoforged.bus.api.IEventBus modBus) {
		NeoForgeMod.enableMilkFluid();
		FLUID_TYPES.register(modBus);
		FLUIDS.register(modBus);
	}

	/** acid hurts living things swimming in it (1.12 BlockFluidAcid) */
	public static class AcidBlock extends LiquidBlock {
		public AcidBlock(FlowingFluid fluid, Properties props) {
			super(fluid, props);
		}

		@Override
		protected void entityInside(BlockState state, Level level, BlockPos pos, Entity ent) {
			super.entityInside(state, level, pos, ent);
			if (!level.isClientSide && ent instanceof LivingEntity && GenericProjectile.BULLET_TARGETS.test(ent)) {
				TGDamageSource acid = new TGDamageSource(level, TGDamageTypes.POISON, null, null, DamageType.POISON, DeathType.BIO);
				acid.goreChance = 1.0f;
				acid.ignoreHurtresistTime = false;
				acid.knockbackMultiplier = 0.0f;
				ent.hurt(acid, 2.0f);
			}
		}
	}
}
