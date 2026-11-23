package techguns.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import techguns.TGDataComponents;
import techguns.api.tginventory.ITGSpecialSlot;
import techguns.api.tginventory.TGSlotType;

/**
 * Ammo box (Fabric port AmmoBagItem): a bundle for ammo. It holds ammo items (those that go in the ammo slots, full or
 * empty) up to a number of stacks, and reloading takes ammo from the boxes in the inventory. Use it to empty it; right
 * click with it on a slot or with a stack on it to move ammo in or out, like a bundle.
 */
public class ItemAmmoBox extends Item {
	private static final int BAR_COLOR = Mth.color(0.4F, 0.4F, 1.0F);
	/** a full stack of anything takes this many units */
	public static final int UNITS_PER_STACK = 64;

	/** capacity in units: 64 per stack */
	private final int capacity;

	public ItemAmmoBox(Properties props, int stacks) {
		super(props.stacksTo(1));
		this.capacity = stacks * UNITS_PER_STACK;
	}

	public int getCapacity() {
		return capacity;
	}

	/** what goes in a box: ammo slot items */
	public static boolean isAmmo(ItemStack stack) {
		return !stack.isEmpty() && !(stack.getItem() instanceof ItemAmmoBox) && stack.canFitInsideContainerItems()
				&& stack.getItem() instanceof ITGSpecialSlot s && s.getSlot(stack) == TGSlotType.AMMOSLOT;
	}

	public static BundleContents contents(ItemStack box) {
		return box.getOrDefault(TGDataComponents.AMMO_BOX_CONTENTS.get(), BundleContents.EMPTY);
	}

	private static int units(ItemStack s) {
		return s.getCount() * Math.max(1, UNITS_PER_STACK / s.getMaxStackSize());
	}

	public static int occupancy(ItemStack box) {
		int n = 0;
		for (ItemStack s : contents(box).items()) {
			n += units(s);
		}
		return n;
	}

	private static void set(ItemStack box, List<ItemStack> items) {
		items.removeIf(ItemStack::isEmpty);
		box.set(TGDataComponents.AMMO_BOX_CONTENTS.get(), items.isEmpty() ? BundleContents.EMPTY : new BundleContents(items));
	}

	private static List<ItemStack> items(ItemStack box) {
		List<ItemStack> l = new ArrayList<>();
		contents(box).itemsCopy().forEach(l::add);
		return l;
	}

	/** puts as much of the stack in the box as fits, shrinking it; returns the amount put in */
	public int insert(ItemStack box, ItemStack stack) {
		if (!isAmmo(stack)) {
			return 0;
		}
		int perItem = Math.max(1, UNITS_PER_STACK / stack.getMaxStackSize());
		int amount = Math.min(stack.getCount(), (capacity - occupancy(box)) / perItem);
		if (amount <= 0) {
			return 0;
		}
		List<ItemStack> items = items(box);
		int left = amount;
		for (ItemStack s : items) {
			if (left > 0 && ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) {
				int add = Math.min(left, s.getMaxStackSize() - s.getCount());
				s.grow(add);
				left -= add;
			}
		}
		if (left > 0) {
			items.add(0, stack.copyWithCount(left));
		}
		set(box, items);
		stack.shrink(amount);
		return amount;
	}

	/** takes the first stack out of the box */
	public static Optional<ItemStack> removeFirst(ItemStack box) {
		List<ItemStack> items = items(box);
		if (items.isEmpty()) {
			return Optional.empty();
		}
		ItemStack s = items.remove(0);
		set(box, items);
		return Optional.of(s);
	}

	/** how many items like match the box holds */
	public static int count(ItemStack box, ItemStack match) {
		int n = 0;
		for (ItemStack s : contents(box).items()) {
			if (ItemStack.isSameItemSameComponents(s, match)) {
				n += s.getCount();
			}
		}
		return n;
	}

	/** takes up to amount items like match out of the box; returns the amount taken */
	public static int remove(ItemStack box, ItemStack match, int amount) {
		List<ItemStack> items = items(box);
		int left = amount;
		for (ItemStack s : items) {
			if (left > 0 && ItemStack.isSameItemSameComponents(s, match)) {
				int take = Math.min(left, s.getCount());
				s.shrink(take);
				left -= take;
			}
		}
		if (left != amount) {
			set(box, items);
		}
		return amount - left;
	}

	/** with the box on the cursor, right click a slot: empty slot takes the first stack, ammo goes in, another box gets ours */
	@Override
	public boolean overrideStackedOnOther(ItemStack box, Slot slot, ClickAction action, Player player) {
		if (box.getCount() != 1 || action != ClickAction.SECONDARY) {
			return false;
		}
		ItemStack other = slot.getItem();
		if (other.isEmpty()) {
			removeFirst(box).ifPresent(s -> {
				playRemoveOneSound(player);
				ItemStack rest = slot.safeInsert(s);
				if (!rest.isEmpty()) {
					insert(box, rest);
				}
			});
		} else if (other.getItem() instanceof ItemAmmoBox target && other.getCount() == 1 && slot.allowModification(player)) {
			if (moveAll(box, target, other) > 0) {
				playInsertSound(player);
			}
		} else if (isAmmo(other) && slot.allowModification(player)) {
			ItemStack taken = slot.safeTake(other.getCount(), other.getCount(), player);
			if (insert(box, taken) > 0) {
				playInsertSound(player);
			}
			if (!taken.isEmpty()) {
				slot.safeInsert(taken);
			}
		}
		return true;
	}

	/** moves the stacks of from into to (a box of the item target); returns the number of items moved */
	private static int moveAll(ItemStack from, ItemAmmoBox target, ItemStack to) {
		int moved = 0;
		List<ItemStack> rest = new ArrayList<>();
		for (ItemStack s : items(from)) {
			moved += target.insert(to, s);
			if (!s.isEmpty()) {
				rest.add(s);
			}
		}
		set(from, rest);
		return moved;
	}

	/** right click the box with a stack (puts it in) or an empty cursor (takes the first stack out) */
	@Override
	public boolean overrideOtherStackedOnMe(ItemStack box, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
		if (box.getCount() != 1 || action != ClickAction.SECONDARY || !slot.allowModification(player)) {
			return false;
		}
		if (other.isEmpty()) {
			removeFirst(box).ifPresent(s -> {
				playRemoveOneSound(player);
				access.set(s);
			});
		} else if (insert(box, other) > 0) {
			playInsertSound(player);
		}
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack box = player.getItemInHand(hand);
		List<ItemStack> items = items(box);
		if (items.isEmpty()) {
			return InteractionResultHolder.fail(box);
		}
		set(box, new ArrayList<>());
		if (player instanceof ServerPlayer) {
			items.forEach(s -> player.drop(s, true));
		}
		player.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(box, level.isClientSide());
	}

	@Override
	public boolean isBarVisible(ItemStack box) {
		return occupancy(box) > 0;
	}

	@Override
	public int getBarWidth(ItemStack box) {
		return Math.min(1 + 12 * occupancy(box) / capacity, 13);
	}

	@Override
	public int getBarColor(ItemStack box) {
		return BAR_COLOR;
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack box) {
		BundleContents c = contents(box);
		return c.isEmpty() ? Optional.empty() : Optional.of(new Tooltip(c, occupancy(box) >= capacity));
	}

	@Override
	public void appendHoverText(ItemStack box, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.minecraft.bundle.fullness", occupancy(box), capacity).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public void onDestroyed(ItemEntity entity) {
		List<ItemStack> items = items(entity.getItem());
		if (!items.isEmpty()) {
			set(entity.getItem(), new ArrayList<>());
			ItemUtils.onContainerDestroyed(entity, items);
		}
	}

	private static void playRemoveOneSound(Entity e) {
		e.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + e.level().getRandom().nextFloat() * 0.4F);
	}

	private static void playInsertSound(Entity e) {
		e.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + e.level().getRandom().nextFloat() * 0.4F);
	}

	/** the contents shown in the tooltip like a bundle's; full shows the free slot as blocked */
	public record Tooltip(BundleContents contents, boolean full) implements TooltipComponent {
	}
}
