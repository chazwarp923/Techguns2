package techguns;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TGTabs {
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Techguns.MODID);

	public static final Supplier<CreativeModeTab> TECHGUNS = TABS.register("techguns", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.techguns"))
			.icon(() -> new ItemStack(TGItems.PISTOL_ROUNDS.get()))
			.withSearchBar()
			.displayItems((params, output) -> TGItems.CREATIVE_TAB_ITEMS.forEach(item -> {
				ItemStack stack = item.get() instanceof ICreativeTabStack c ? c.getCreativeTabStack() : new ItemStack(item.get());
				output.accept(stack);
			}))
			.build());

	/** Items that need a custom stack (e.g. guns with full ammo) in the creative tab. */
	public interface ICreativeTabStack {
		ItemStack getCreativeTabStack();
	}
}
