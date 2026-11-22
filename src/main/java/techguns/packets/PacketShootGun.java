package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.Techguns;
import techguns.api.guns.IGenericGun;

/**
 * Client -> server: the player wants to shoot with the gun in the given hand. entityId is the
 * lock-on target (was PacketShootGunTarget), or -1.
 */
public record PacketShootGun(boolean isZooming, boolean offHand, int entityId) implements CustomPacketPayload {

	public static final Type<PacketShootGun> TYPE = new Type<>(Techguns.rl("shoot_gun"));

	public static final StreamCodec<FriendlyByteBuf, PacketShootGun> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeBoolean(m.isZooming);
		buf.writeBoolean(m.offHand);
		buf.writeVarInt(m.entityId + 1);
	}, buf -> new PacketShootGun(buf.readBoolean(), buf.readBoolean(), buf.readVarInt() - 1));

	public PacketShootGun(boolean isZooming, InteractionHand hand) {
		this(isZooming, hand == InteractionHand.OFF_HAND, -1);
	}

	public PacketShootGun(boolean isZooming, InteractionHand hand, Entity target) {
		this(isZooming, hand == InteractionHand.OFF_HAND, target != null ? target.getId() : -1);
	}

	public InteractionHand getHand() {
		return offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketShootGun m, IPayloadContext ctx) {
		Player ply = ctx.player();
		ItemStack stack = ply.getItemInHand(m.getHand());
		if (!stack.isEmpty() && stack.getItem() instanceof IGenericGun gun) {
			Entity target = m.entityId >= 0 ? ply.level().getEntity(m.entityId) : null;
			gun.shootGunPrimary(stack, ply.level(), ply, m.isZooming, m.getHand(), target);
		}
	}
}
