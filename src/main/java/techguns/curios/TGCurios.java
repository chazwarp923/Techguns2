package techguns.curios;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import techguns.Techguns;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.api.tginventory.TGSlotType;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Curios integration, replaces the 1.12 Techguns player inventory tab. Face / back / hand items use the Curios
 * presets "head" / "back" / "hands" (item tags in data/curios/tags/item); the 8 ammo and 3 auto-food slots are
 * Techguns slot types (data/techguns/curios/slots), all assigned to players in data/techguns/curios/entities.
 */
public final class TGCurios {

	public static final String SLOT_FACE = "head";
	public static final String SLOT_BACK = "back";
	public static final String SLOT_HANDS = "hands";
	public static final String SLOT_AMMO = "techguns_ammo";
	public static final String SLOT_FOOD = "techguns_food";

	public static final String[] SPECIAL_SLOTS = { SLOT_FACE, SLOT_BACK, SLOT_HANDS };

	private TGCurios() {
	}

	/** common setup: slot validators and ICurioItem for every face / back / hand item */
	public static void init() {
		CuriosApi.registerCurioPredicate(Techguns.rl("ammo"),
				r -> r.stack().getItem() instanceof ITGSpecialSlot s && s.getSlot(r.stack()) == TGSlotType.AMMOSLOT);
		CuriosApi.registerCurioPredicate(Techguns.rl("food"), r -> r.stack().has(DataComponents.FOOD));
		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof ITGSpecialSlot s && isWearable(s.getSlot(new ItemStack(item)))) {
				CuriosApi.registerCurio(item, TGCurioItem.INSTANCE);
			}
		}
	}

	public static boolean isWearable(TGSlotType type) {
		return type == TGSlotType.FACESLOT || type == TGSlotType.BACKSLOT || type == TGSlotType.HANDSLOT;
	}

	/** the slot id an item of this Techguns slot type is worn in */
	public static String slotFor(TGSlotType type) {
		return switch (type) {
			case FACESLOT -> SLOT_FACE;
			case BACKSLOT -> SLOT_BACK;
			case HANDSLOT -> SLOT_HANDS;
			default -> null;
		};
	}

	/** first non-empty stack in a curios slot type */
	public static ItemStack getEquipped(LivingEntity ent, String slot) {
		List<ItemStack> l = getStacks(ent, slot);
		for (ItemStack s : l) {
			if (!s.isEmpty()) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	/** worn Techguns face / back / hand items (only ITGSpecialSlot items of the matching slot type) */
	public static List<ItemStack> getSpecialSlotItems(LivingEntity ent) {
		List<ItemStack> out = new ArrayList<>(3);
		for (String slot : SPECIAL_SLOTS) {
			for (ItemStack s : getStacks(ent, slot)) {
				if (!s.isEmpty() && s.getItem() instanceof ITGSpecialSlot i && slot.equals(slotFor(i.getSlot(s)))) {
					out.add(s);
				}
			}
		}
		return out;
	}

	/** live, modifiable view of a curios slot type's stacks (empty list if the entity doesn't have it) */
	public static List<ItemStack> getStacks(LivingEntity ent, String slot) {
		return CuriosApi.getCuriosInventory(ent).flatMap(inv -> inv.getStacksHandler(slot)).map(TGCurios::view)
				.orElse(Collections.emptyList());
	}

	private static List<ItemStack> view(ICurioStacksHandler handler) {
		IItemHandlerModifiable stacks = handler.getStacks();
		return new AbstractList<>() {
			@Override
			public ItemStack get(int index) {
				return stacks.getStackInSlot(index);
			}

			@Override
			public ItemStack set(int index, ItemStack element) {
				ItemStack old = stacks.getStackInSlot(index);
				stacks.setStackInSlot(index, element);
				return old;
			}

			@Override
			public int size() {
				return stacks.getSlots();
			}
		};
	}
}
