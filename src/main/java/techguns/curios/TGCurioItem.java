package techguns.curios;

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

	/** gas mask RAD resistance */
	@Override
	public com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(
			SlotContext ctx, net.minecraft.resources.ResourceLocation id, ItemStack stack) {
		com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> map = com.google.common.collect.LinkedHashMultimap
				.create();
		if (rightSlot(ctx, stack) && stack.getItem() instanceof techguns.items.additionalslots.ItemGasMask mask && mask.getRadresist() > 0) {
			map.put(techguns.radiation.TGRadiationSystem.RADIATION_RESISTANCE,
					new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, mask.getRadresist(), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
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
