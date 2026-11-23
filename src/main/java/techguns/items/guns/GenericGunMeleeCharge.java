package techguns.items.guns;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import techguns.TGDataComponents;
import techguns.TGSounds;
import techguns.api.damagesystem.DamageType;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.ShooterValues;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.entities.projectiles.GenericProjectile;
import techguns.util.TextUtil;

/**
 * Charge weapon that is also a mining tool (power hammer, chainsaw, mining drill, shishkebap).
 * 1.12 tool classes / harvest levels are mapped to 1.21 block tags: tool class -> minecraft:mineable/* tag,
 * harvest level (+ mining head level) -> the incorrect_for_*_tool tier tag.
 */
public class GenericGunMeleeCharge extends GenericGunCharge {

	private static final Map<String, TagKey<Block>> TOOL_TAGS = Map.of("pickaxe", BlockTags.MINEABLE_WITH_PICKAXE, "shovel",
			BlockTags.MINEABLE_WITH_SHOVEL, "axe", BlockTags.MINEABLE_WITH_AXE, "hoe", BlockTags.MINEABLE_WITH_HOE, "sword", BlockTags.SWORD_EFFICIENT);
	private static final List<TagKey<Block>> ALL_MINEABLE = List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_SHOVEL,
			BlockTags.MINEABLE_WITH_AXE, BlockTags.MINEABLE_WITH_HOE);

	/** set while breaking the extra blocks of an area mining operation */
	private static final ThreadLocal<Boolean> AREA_MINING = ThreadLocal.withInitial(() -> false);

	/** tool class -> harvest level (0 wood, 1 stone, 2 iron, 3 diamond, 4+ netherite) */
	protected final Map<String, Integer> mininglevels = new LinkedHashMap<>();

	/** Extra radius for mining, 0 = 1 block, 1 = 3x3, 2 = 5x5, ... */
	protected int miningRadius = 0;
	protected int swingSoundDelay = 5;
	protected List<Supplier<? extends Item>> miningHeads = null;
	/** the head a new tool comes with (level 0), crafted into it; null: none */
	protected Supplier<? extends Item> baseHead = null;
	protected boolean hasCustomAnim = true;
	protected float melee_sound_volume = 0.65f;

	public GenericGunMeleeCharge(ChargedProjectileSelector<? extends GenericProjectile> projectile_selector, boolean semiAuto, int minFiretime,
			int clipsize, int reloadtime, float damage, Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy,
			float fullChargeTime, int ammoConsumedOnFullCharge) {
		super(projectile_selector, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy, fullChargeTime,
				ammoConsumedOnFullCharge);
	}

	// ------------------------------------------------------------------ builder

	public GenericGunMeleeCharge setMeleeDmg(float dmgPowered, float dmgUnpowered) {
		this.meleeDamagePwr = dmgPowered;
		this.meleeDamageEmpty = dmgUnpowered;
		return this;
	}

	public GenericGunMeleeCharge setTool(String toolclass, int harvestLevel) {
		this.mininglevels.put(toolclass, harvestLevel);
		return this;
	}

	public GenericGunMeleeCharge setDigSpeed(float speed) {
		this.digSpeed = speed;
		return this;
	}

	@SafeVarargs
	public final GenericGunMeleeCharge setMiningHeads(Supplier<? extends Item>... heads) {
		this.miningHeads = List.of(heads);
		return this;
	}

	/** the head a new tool is crafted with, shown as its level 0 head */
	public GenericGunMeleeCharge setBaseHead(Supplier<? extends Item> head) {
		this.baseHead = head;
		return this;
	}

	public GenericGunMeleeCharge setMiningRadius(int miningRadius) {
		this.miningRadius = miningRadius;
		return this;
	}

	public GenericGunMeleeCharge setSwingSoundDelay(int swingSoundDelay) {
		this.swingSoundDelay = swingSoundDelay;
		return this;
	}

	public GenericGunMeleeCharge setHasCustomAnim(boolean customAnim) {
		this.hasCustomAnim = customAnim;
		return this;
	}

	// ------------------------------------------------------------------ mining

	public int getMiningRadius(ItemStack stack) {
		return miningRadius + getExtraMiningRadius(stack);
	}

	public int getExtraMiningRadius(ItemStack stack) {
		return 0;
	}

	public int getMiningHeadLevel(ItemStack stack) {
		return stack.getOrDefault(TGDataComponents.MINING_HEAD.get(), 0);
	}

	public void setMiningHeadLevel(ItemStack stack, int level) {
		if (level <= 0) {
			stack.remove(TGDataComponents.MINING_HEAD.get());
		} else {
			stack.set(TGDataComponents.MINING_HEAD.get(), level);
		}
	}

	/** @return head level (1..n) for the given head item, 0 if it is no head of this tool */
	public int getMiningHeadLevelForHead(ItemStack head) {
		if (this.miningHeads != null) {
			for (int i = 0; i < this.miningHeads.size(); i++) {
				if (head.is(this.miningHeads.get(i).get())) {
					return i + 1;
				}
			}
		}
		return 0;
	}

	public float getExtraDigSpeed(ItemStack stack) {
		return 1.0f * getMiningHeadLevel(stack);
	}

	public float getEffectiveDigSpeed(ItemStack stack) {
		return this.digSpeed + this.getExtraDigSpeed(stack);
	}

	/** the tool class of ours that applies to this block, "" for blocks without a tool, null if not effective */
	@Nullable
	protected String getMatchingToolClass(BlockState state) {
		for (Map.Entry<String, Integer> e : mininglevels.entrySet()) {
			TagKey<Block> tag = TOOL_TAGS.get(e.getKey());
			if (tag != null && state.is(tag)) {
				return e.getKey();
			}
		}
		for (TagKey<Block> tag : ALL_MINEABLE) {
			if (state.is(tag)) {
				return null; // needs another tool
			}
		}
		return ""; // block without a harvest tool
	}

	public boolean isEffectiveToolForState(ItemStack stack, BlockState state) {
		return this.getCurrentAmmo(stack) >= this.getMiningAmmoConsumption() && getMatchingToolClass(state) != null;
	}

	protected static TagKey<Block> incorrectForLevel(int level) {
		return switch (Math.max(0, level)) {
			case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
			case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
			case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
			case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
			default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
		};
	}

	public int getHarvestLevel(ItemStack stack, String toolClass) {
		Integer lvl = toolClass.isEmpty() ? mininglevels.values().stream().max(Integer::compare).orElse(0) : mininglevels.get(toolClass);
		return lvl == null ? -1 : lvl + getMiningHeadLevel(stack);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return this.isEffectiveToolForState(stack, state) ? this.getEffectiveDigSpeed(stack) : 1.0f;
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		if (!this.isEffectiveToolForState(stack, state)) {
			return false;
		}
		String tool = getMatchingToolClass(state);
		return !state.is(incorrectForLevel(getHarvestLevel(stack, tool)));
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
		if (AREA_MINING.get()) {
			return true; // extra blocks of an area dig: no ammo / sound / recursion
		}
		if (this.getCurrentAmmo(stack) <= 0) {
			return false;
		}
		this.useAmmo(stack, 1);
		if (miningEntity instanceof Player player) {
			SoundEvent sound = this.getBlockBreakSound();
			if (sound != null) {
				level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, sound, SoundSource.PLAYERS, melee_sound_volume, 1.0f);
			}
			if (player instanceof ServerPlayer sp && !sp.isShiftKeyDown() && this.getMiningRadius(stack) > 0) {
				this.mineArea(sp, stack, pos);
			}
		}
		return true;
	}

	/** 1.12 onBlockStartBreak area mining: break the plane around pos (perpendicular to the hit side). */
	protected void mineArea(ServerPlayer player, ItemStack stack, BlockPos center) {
		Direction side = getSideHitMining(player);
		if (side == null) {
			return;
		}
		List<BlockPos> others = getAreaMiningBlocks(player, player.level(), stack, center, side.getAxis());
		AREA_MINING.set(true);
		try {
			for (BlockPos b : others) {
				player.gameMode.destroyBlock(b);
			}
		} finally {
			AREA_MINING.set(false);
		}
	}

	/**
	 * The other blocks an area mining swing at center breaks: the plane of getMiningRadius around it, perpendicular to
	 * axis, without block entities, air, or blocks this tool can't mine. Used for mining and for the client outlines.
	 */
	public List<BlockPos> getAreaMiningBlocks(Player player, Level level, ItemStack stack, BlockPos center, Direction.Axis axis) {
		int r = this.getMiningRadius(stack);
		List<BlockPos> result = new ArrayList<>();
		if (r <= 0) {
			return result;
		}
		for (BlockPos b : BlockPos.betweenClosed(center.offset(axis == Direction.Axis.X ? 0 : -r, axis == Direction.Axis.Y ? 0 : -r,
				axis == Direction.Axis.Z ? 0 : -r), center.offset(axis == Direction.Axis.X ? 0 : r, axis == Direction.Axis.Y ? 0 : r,
						axis == Direction.Axis.Z ? 0 : r))) {
			if (b.equals(center) || !level.isLoaded(b) || level.getBlockEntity(b) != null) {
				continue;
			}
			BlockState st = level.getBlockState(b);
			if (!st.isAir() && st.getDestroyProgress(player, level, b) > 0f && this.isEffectiveToolForState(stack, st)
					&& (!st.requiresCorrectToolForDrops() || this.isCorrectToolForDrops(stack, st))) {
				result.add(b.immutable());
			}
		}
		return result;
	}

	@Nullable
	public Direction getSideHitMining(Player player) {
		double reach = player.blockInteractionRange();
		HitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1f).scale(reach)),
				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (result instanceof BlockHitResult bhr && result.getType() == HitResult.Type.BLOCK) {
			return bhr.getDirection();
		}
		return null;
	}

	// ------------------------------------------------------------------ melee

	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
		double meleedmg = this.getCurrentAmmo(stack) > 0 ? this.meleeDamagePwr : this.meleeDamageEmpty;
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, meleedmg, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4000000953674316D, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build();
	}

	@Override
	protected TGDamageSource getMeleeDamageSource(Player player, ItemStack stack) {
		TGDamageSource src = TGDamageSource.causePlayerMeleeDamage(player, DamageType.PHYSICAL, DeathType.GORE);
		if (this.getCurrentAmmo(stack) > 0) {
			src.goreChance = 1.0f;
			src.armorPenetration = this.penetration;
			src.knockbackMultiplier = 1f;
		} else {
			src.deathType = DeathType.DEFAULT;
		}
		return src;
	}

	protected SoundEvent getSwingSound() {
		return TGSounds.POWERHAMMER_SWING.get();
	}

	@Nullable
	protected SoundEvent getBlockBreakSound() {
		return TGSounds.POWERHAMMER_IMPACT.get();
	}

	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
		if (this.getCurrentAmmo(stack) > 0) {
			if (entity.level().isClientSide) {
				if (ShooterValues.getRecoiltime(entity, false) < System.currentTimeMillis()) {
					ShooterValues.setRecoiltime(entity, false, System.currentTimeMillis() + 250, 250, (byte) 0);
				}
			} else {
				boolean sendSound = true;
				boolean openingContainer = false;
				if (entity instanceof Player ply) {
					openingContainer = ply.containerMenu != ply.inventoryMenu;
					TGExtendedPlayer props = TGExtendedPlayer.get(ply);
					if (props.swingSoundDelay > 0) {
						sendSound = false;
					} else {
						props.swingSoundDelay = this.swingSoundDelay;
					}
				}
				if (!openingContainer && sendSound) {
					entity.level().playSound(null, entity.getX(), entity.getEyeY(), entity.getZ(), getSwingSound(), SoundSource.PLAYERS, melee_sound_volume,
							1.0f);
				}
			}
			return this.hasCustomAnim;
		}
		return false;
	}

	// ------------------------------------------------------------------ tooltip

	@Override
	protected void addMiningTooltip(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag, boolean longTooltip) {
		super.addMiningTooltip(stack, context, list, flag, longTooltip);
		if (longTooltip) {
			if (this.miningHeads != null) {
				int lvl = this.getMiningHeadLevel(stack);
				Component head = lvl > 0 && lvl <= miningHeads.size() ? miningHeads.get(lvl - 1).get().getDescription()
						: baseHead != null ? baseHead.get().getDescription() : TextUtil.transTG("default");
				list.add(TextUtil.transTG("tooltip.mininghead").append(": ").append(head.copy().withStyle(ChatFormatting.WHITE)));
			}
			list.add(TextUtil.transTG("tooltip.toolclasses").append(":"));
			for (String s : this.mininglevels.keySet()) {
				list.add(Component.literal(" ").append(TextUtil.transTG("toolclass." + s)).append(": " + getHarvestLevel(stack, s)));
			}
			list.add(TextUtil.transTG("tooltip.breakspeed").append(": " + this.getEffectiveDigSpeed(stack)));
			int r = this.getMiningRadius(stack) * 2 + 1;
			list.add(TextUtil.transTG("tooltip.miningradius").append(": " + r + "x" + r));
		} else if (!this.mininglevels.isEmpty()) {
			MutableToolList tools = new MutableToolList();
			this.mininglevels.keySet().forEach(tools::add);
			list.add(TextUtil.transTG("tooltip.toolclasses").append(": " + tools));
		}
	}

	private static class MutableToolList {
		private final StringBuilder sb = new StringBuilder();

		void add(String s) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(s);
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}

}
