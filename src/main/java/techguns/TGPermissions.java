package techguns;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/** 1.12 TGPermissions: who may switch guns out of safe mode. */
@EventBusSubscriber(modid = Techguns.MODID)
public class TGPermissions {

	/** "techguns.allowunsafemode", granted to everyone by default (as in 1.12) */
	public static final PermissionNode<Boolean> ALLOW_UNSAFE_MODE = new PermissionNode<>(Techguns.MODID, "allowunsafemode", PermissionTypes.BOOLEAN,
			(player, uuid, context) -> true);

	@SubscribeEvent
	public static void registerNodes(PermissionGatherEvent.Nodes event) {
		ALLOW_UNSAFE_MODE.setInformation(net.minecraft.network.chat.Component.literal("Allow unsafe mode"),
				net.minecraft.network.chat.Component.literal("Allow player to use the unsafe mode of weapons."));
		event.addNodes(ALLOW_UNSAFE_MODE);
	}

	/** config RestrictUnsafeModeToOP overrides the permission node */
	public static boolean canUseUnsafeMode(ServerPlayer ply) {
		if (TGConfig.limitUnsafeModeToOP) {
			return isPlayerOp(ply);
		}
		return PermissionAPI.getPermission(ply, ALLOW_UNSAFE_MODE);
	}

	public static boolean isPlayerOp(ServerPlayer ply) {
		return ply.server.getPlayerList().isOp(ply.getGameProfile());
	}
}
