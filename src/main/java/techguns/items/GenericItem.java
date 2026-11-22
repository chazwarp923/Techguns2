package techguns.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.api.tginventory.TGSlotType;

/**
 * Base class for simple Techguns items. In 1.12.2 most of these were metadata
 * variants of a single "itemshared" item; since 1.13 every variant is its own item.
 */
public class GenericItem extends Item implements ITGSpecialSlot {
	protected final TGSlotType slotType;
	/** Legacy flag: render this item with its 3D model when held. */
	protected final boolean useRenderHack;

	public GenericItem(Properties properties, TGSlotType slotType, boolean useRenderHack) {
		super(properties);
		this.slotType = slotType;
		this.useRenderHack = useRenderHack;
	}

	public GenericItem(Properties properties) {
		this(properties, TGSlotType.NORMAL, false);
	}

	@Override
	public TGSlotType getSlot(ItemStack item) {
		return slotType;
	}

	public boolean shouldUseRenderHack(ItemStack stack) {
		return useRenderHack;
	}
}
