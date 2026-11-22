package techguns.items.armors;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import techguns.TGDataComponents;

public interface ICamoChangeable {
	int getCamoCount();

	default int switchCamo(ItemStack item) {
		return this.switchCamo(item, false);
	}

	default int switchCamo(ItemStack item, boolean back) {
		int camoID = getCurrentCamoIndex(item);
		camoID += back ? -1 : 1;
		if (camoID >= getCamoCount()) {
			camoID = 0;
		} else if (camoID < 0) {
			camoID = getCamoCount() - 1;
		}
		item.set(TGDataComponents.CAMO.get(), camoID);
		return camoID;
	}

	default int getCurrentCamoIndex(ItemStack item) {
		return item.getOrDefault(TGDataComponents.CAMO.get(), 0);
	}

	Component getCurrentCamoName(ItemStack item);

	default boolean addBlockCamoChangeRecipes() {
		return true;
	}
}
