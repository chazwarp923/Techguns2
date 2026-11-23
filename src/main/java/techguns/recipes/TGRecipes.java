package techguns.recipes;

import java.util.function.Function;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.Techguns;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.api.tginventory.TGSlotType;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GenericGunMeleeCharge;

/**
 * The 1.12 custom crafting recipes (_factories.json). Each one reads like a vanilla shaped / shapeless recipe and
 * only changes the crafted result, so recipe book and JEI treat them like vanilla.
 */
public class TGRecipes {

	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Techguns.MODID);
	public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE,
			Techguns.MODID);

	/* ---- machine recipes ---- */
	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<MetalPressRecipe>> METAL_PRESS = TYPES
			.register("metal_press", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("metal_press")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MetalPressRecipe>> METAL_PRESS_SERIALIZER = SERIALIZERS
			.register("metal_press", () -> new Simple<>(MetalPressRecipe.CODEC, MetalPressRecipe.STREAM_CODEC));
	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE = TYPES
			.register("blast_furnace", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("blast_furnace")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER = SERIALIZERS
			.register("blast_furnace", () -> new Simple<>(BlastFurnaceRecipe.CODEC, BlastFurnaceRecipe.STREAM_CODEC));
	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<GrinderRecipe>> GRINDER = TYPES
			.register("grinder", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("grinder")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GrinderRecipe>> GRINDER_SERIALIZER = SERIALIZERS
			.register("grinder", () -> new Simple<>(GrinderRecipe.CODEC, GrinderRecipe.STREAM_CODEC));

	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<FabricatorRecipe>> FABRICATOR = TYPES
			.register("fabricator", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("fabricator")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FabricatorRecipe>> FABRICATOR_SERIALIZER = SERIALIZERS
			.register("fabricator", () -> new Simple<>(FabricatorRecipe.CODEC, FabricatorRecipe.STREAM_CODEC));

	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<ReactionChamberRecipe>> REACTION_CHAMBER = TYPES
			.register("reaction_chamber", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("reaction_chamber")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReactionChamberRecipe>> REACTION_CHAMBER_SERIALIZER = SERIALIZERS
			.register("reaction_chamber", () -> new Simple<>(ReactionChamberRecipe.CODEC, ReactionChamberRecipe.STREAM_CODEC));

	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<ChargingStationRecipe>> CHARGING_STATION = TYPES
			.register("charging_station", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("charging_station")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChargingStationRecipe>> CHARGING_STATION_SERIALIZER = SERIALIZERS
			.register("charging_station", () -> new Simple<>(ChargingStationRecipe.CODEC, ChargingStationRecipe.STREAM_CODEC));

	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<ChemLabRecipe>> CHEM_LAB = TYPES
			.register("chem_lab", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("chem_lab")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChemLabRecipe>> CHEM_LAB_SERIALIZER = SERIALIZERS
			.register("chem_lab", () -> new Simple<>(ChemLabRecipe.CODEC, ChemLabRecipe.STREAM_CODEC));

	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<CamoBenchRecipe>> CAMO_BENCH = TYPES
			.register("camo_bench", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("camo_bench")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CamoBenchRecipe>> CAMO_BENCH_SERIALIZER = SERIALIZERS
			.register("camo_bench", () -> new Simple<>(CamoBenchRecipe.CODEC, CamoBenchRecipe.STREAM_CODEC));
	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<UpgradeBenchRecipe>> UPGRADE_BENCH = TYPES
			.register("upgrade_bench", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("upgrade_bench")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UpgradeBenchRecipe>> UPGRADE_BENCH_SERIALIZER = SERIALIZERS
			.register("upgrade_bench", () -> new Simple<>(UpgradeBenchRecipe.CODEC, UpgradeBenchRecipe.STREAM_CODEC));

	/** bullet core + casing + powder, picked like at a stonecutter (Fabric port) */
	public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<AmmoBenchRecipe>> AMMO_BENCH = TYPES
			.register("ammo_bench", () -> net.minecraft.world.item.crafting.RecipeType.simple(Techguns.rl("ammo_bench")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoBenchRecipe>> AMMO_BENCH_SERIALIZER = SERIALIZERS
			.register("ammo_bench", () -> new Simple<>(AmmoBenchRecipe.CODEC, AmmoBenchRecipe.STREAM_CODEC));

	/** gun + ammo item: switch the gun's ammo variant and load that item (1.12 AmmoSwitchRecipeFactory) */
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoSwitchRecipe>> AMMO_CHANGE_CRAFTING = SERIALIZERS
			.register("ammo_change_crafting", () -> shapeless(AmmoSwitchRecipe::new));
	/** mining tool + head item: install the head (1.12 MiningToolUpgradeHeadRecipeFactory) */
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MiningToolUpgradeRecipe>> MININGTOOL_UPGRADE = SERIALIZERS
			.register("miningtool_upgrade", () -> shapeless(MiningToolUpgradeRecipe::new));
	/** shaped, result keeps the components of the gun used (1.12 ShapedOreRecipeCopyNBTFactory) */
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CopyComponentsRecipe>> COPY_NBT = SERIALIZERS
			.register("copy_nbt", () -> shaped(CopyComponentsRecipe::new));
	/** shaped, result is loaded with the sum of the guns' ammo (1.12 AmmoSumRecipeFactory) */
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoSumRecipe>> AMMO_SUM_RECIPE = SERIALIZERS
			.register("ammo_sum_recipe", () -> shaped(AmmoSumRecipe::new));

	/* ---- serializers wrapping the vanilla codecs ---- */

	private static <T extends ShapelessRecipe> RecipeSerializer<T> shapeless(Function<ShapelessRecipe, T> factory) {
		MapCodec<T> codec = RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(factory, r -> r);
		StreamCodec<RegistryFriendlyByteBuf, T> stream = RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(factory, r -> r);
		return new Simple<>(codec, stream);
	}

	private static <T extends ShapedRecipe> RecipeSerializer<T> shaped(Function<ShapedRecipe, T> factory) {
		MapCodec<T> codec = RecipeSerializer.SHAPED_RECIPE.codec().xmap(factory, r -> r);
		StreamCodec<RegistryFriendlyByteBuf, T> stream = RecipeSerializer.SHAPED_RECIPE.streamCodec().map(factory, r -> r);
		return new Simple<>(codec, stream);
	}

	private record Simple<T extends net.minecraft.world.item.crafting.Recipe<?>>(MapCodec<T> codec,
			StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) implements RecipeSerializer<T> {
	}

	/* ---- recipes ---- */

	public static class AmmoSwitchRecipe extends ShapelessRecipe {
		public AmmoSwitchRecipe(ShapelessRecipe r) {
			super(r.getGroup(), r.category(), r.getResultItem(null), r.getIngredients());
		}

		@Override
		public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
			ItemStack gun = ItemStack.EMPTY;
			ItemStack ammo = ItemStack.EMPTY;
			for (ItemStack s : input.items()) {
				if (s.getItem() instanceof GenericGun) {
					gun = s;
				} else if (s.getItem() instanceof ITGSpecialSlot sl && sl.getSlot(s) == TGSlotType.AMMOSLOT) {
					ammo = s;
				}
			}
			if (!(gun.getItem() instanceof GenericGun g)) {
				return super.assemble(input, registries);
			}
			ItemStack out = gun.copyWithCount(1);
			g.setCurrentAmmoVariantKey(out, g.getAmmoType().getAmmoVariantKeyfor(ammo, 0));
			// the ammo item used in the recipe is loaded (as in 1.12): a full clip for one-item reloads, one item's
			// worth otherwise. The ammo the gun held before goes back to the player (GunInteractEvents.onCraft).
			int perItem = g.getAmmoCount() > 1 ? Math.max(1, g.getClipsize() / g.getAmmoCount()) : g.getClipsize();
			out.set(techguns.TGDataComponents.AMMO.get(), ammo.isEmpty() ? 0 : perItem);
			return out;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return AMMO_CHANGE_CRAFTING.get();
		}
	}

	public static class MiningToolUpgradeRecipe extends ShapelessRecipe {
		public MiningToolUpgradeRecipe(ShapelessRecipe r) {
			super(r.getGroup(), r.category(), r.getResultItem(null), r.getIngredients());
		}

		private static ItemStack[] find(CraftingInput input) {
			ItemStack tool = ItemStack.EMPTY;
			ItemStack head = ItemStack.EMPTY;
			for (ItemStack s : input.items()) {
				if (s.isEmpty()) {
					continue;
				}
				if (s.getItem() instanceof GenericGunMeleeCharge) {
					tool = s;
				} else {
					head = s;
				}
			}
			return new ItemStack[] { tool, head };
		}

		@Override
		public boolean matches(CraftingInput input, Level level) {
			if (!super.matches(input, level)) {
				return false;
			}
			// 1.12 itemstackHasNBTInt ingredient: heads must be installed in order
			ItemStack[] th = find(input);
			if (!(th[0].getItem() instanceof GenericGunMeleeCharge g)) {
				return false;
			}
			int lvl = g.getMiningHeadLevelForHead(th[1]);
			return lvl > 0 && g.getMiningHeadLevel(th[0]) == lvl - 1;
		}

		@Override
		public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
			ItemStack[] th = find(input);
			if (!(th[0].getItem() instanceof GenericGunMeleeCharge g)) {
				return super.assemble(input, registries);
			}
			ItemStack out = th[0].copyWithCount(1);
			g.setMiningHeadLevel(out, g.getMiningHeadLevelForHead(th[1]));
			return out;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return MININGTOOL_UPGRADE.get();
		}
	}

	public static class CopyComponentsRecipe extends ShapedRecipe {
		public CopyComponentsRecipe(ShapedRecipe r) {
			super(r.getGroup(), r.category(), r.pattern, r.getResultItem(null), r.showNotification());
		}

		@Override
		public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
			ItemStack out = super.assemble(input, registries);
			for (ItemStack s : input.items()) {
				if (s.getItem() instanceof GenericGun) {
					out.applyComponents(s.getComponentsPatch());
					break;
				}
			}
			return out;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return COPY_NBT.get();
		}
	}

	public static class AmmoSumRecipe extends ShapedRecipe {
		public AmmoSumRecipe(ShapedRecipe r) {
			super(r.getGroup(), r.category(), r.pattern, r.getResultItem(null), r.showNotification());
		}

		@Override
		public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
			ItemStack out = super.assemble(input, registries);
			int sum = 0;
			for (ItemStack s : input.items()) {
				if (s.getItem() instanceof GenericGun g) {
					sum += g.getCurrentAmmo(s);
				}
			}
			out.set(techguns.TGDataComponents.AMMO.get(), sum);
			return out;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return AMMO_SUM_RECIPE.get();
		}
	}
}
