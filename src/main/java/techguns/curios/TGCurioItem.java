package techguns.curios;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForgeMod;
import techguns.items.additionalslots.ItemGasMask;
import techguns.items.armors.ArmorBonusUtil;
import techguns.items.armors.TGArmorBonus;
import techguns.radiation.TGRadiationSystem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.capabilities.TGExtendedPlayer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Curio behaviour of the Techguns face / back / hand items: ticking and right click to equip. */
public class TGCurioItem implements ICurioItem {

	public static final TGCurioItem INSTANCE = new TGCurioItem();

	/** only the slot matching the item's Techguns slot type */
	private static boolean rightSlot(SlotContext ctx, ItemStack stack) {
		return stack.getItem() instanceof ITGSpecialSlot s && ctx.identifier().equals(TGCurios.slotFor(s.getSlot(stack)));
	}

	@Override
	public void curioTick(SlotContext ctx, ItemStack stack) {
		if (ctx.entity() instanceof Player ply && rightSlot(ctx, stack) && !ctx.cosmetic()) {
			((ITGSpecialSlot) stack.getItem()).onPlayerTick(stack, ply);
		}
	}

	/**
	 * Passive bonuses of worn slot items as attributes: gas mask RAD resistance, antigrav pack creative flight, working
	 * gloves mining speed. Curios re-reads these when the stack changes, so an empty antigrav pack stops the flight. The
	 * jump pack's boost and fall cushion depend on the jetpack toggle and use charge, so they stay in ArmorEvents.
	 */
	@Override
	public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext ctx, ResourceLocation id, ItemStack stack) {
		Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
		if (!rightSlot(ctx, stack)) {
			return map;
		}
		if (stack.getItem() instanceof ItemGasMask mask && mask.getRadresist() > 0) {
			map.put(TGRadiationSystem.RADIATION_RESISTANCE, new AttributeModifier(id, mask.getRadresist(), AttributeModifier.Operation.ADD_VALUE));
		}
		if (ArmorBonusUtil.slotItemWorks(stack)) {
			ITGSpecialSlot item = (ITGSpecialSlot) stack.getItem();
			Player ply = ctx.entity() instanceof Player p ? p : null;
			if (item.getBonus(TGArmorBonus.CREATIVE_FLIGHT, stack, false, ply) > 0) {
				map.put(NeoForgeMod.CREATIVE_FLIGHT, new AttributeModifier(id, 1.0, AttributeModifier.Operation.ADD_VALUE));
			}
			float breakspeed = item.getBonus(TGArmorBonus.BREAKSPEED, stack, false, ply);
			if (breakspeed != 0) {
				map.put(Attributes.BLOCK_BREAK_SPEED, new AttributeModifier(id, breakspeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}
		return map;
	}

	@Override
	public void onUnequip(SlotContext ctx, ItemStack newStack, ItemStack stack) {
		if (ctx.entity() instanceof Player ply) {
			TGExtendedPlayer props = TGExtendedPlayer.get(ply);
			props.isGliding = false;
			props.isJumpkeyPressed = false;
		}
	}

	@Override
	public boolean canEquip(SlotContext ctx, ItemStack stack) {
		return rightSlot(ctx, stack);
	}

	@Override
	public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) {
		return true;
	}

	@Override
	public ICurio.SoundInfo getEquipSound(SlotContext ctx, ItemStack stack) {
		return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0f, 1.0f);
	}
}
