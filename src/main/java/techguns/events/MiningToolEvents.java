package techguns.events;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import techguns.Techguns;
import techguns.items.guns.MiningDrill;

/** Mining drill: sneaking + ammo = silk touch drops, no XP (was TGEventHandler#onBlockDrops / onBlockBreakEvent). */
@EventBusSubscriber(modid = Techguns.MODID)
public class MiningToolEvents {

	@SubscribeEvent
	public static void onBlockDrops(BlockDropsEvent event) {
		if (!(event.getBreaker() instanceof Player ply) || !ply.isShiftKeyDown()) {
			return;
		}
		ItemStack stack = event.getTool();
		if (stack.isEmpty() || !(stack.getItem() instanceof MiningDrill md) || md.getAmmoLeft(stack) <= 0) {
			return;
		}
		ItemStack silk = stack.copy();
		silk.enchant(event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		List<ItemStack> drops = Block.getDrops(event.getState(), event.getLevel(), event.getPos(), event.getBlockEntity(), ply, silk);
		event.getDrops().clear();
		double x = event.getPos().getX() + 0.5, y = event.getPos().getY() + 0.5, z = event.getPos().getZ() + 0.5;
		for (ItemStack d : drops) {
			ItemEntity ie = new ItemEntity(event.getLevel(), x, y, z, d);
			ie.setDefaultPickUpDelay();
			event.getDrops().add(ie);
		}
		event.setDroppedExperience(0);
	}
}
