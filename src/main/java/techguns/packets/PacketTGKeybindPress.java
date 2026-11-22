package techguns.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.armors.ArmorBonusUtil;
import techguns.items.armors.TGArmorBonus;
import techguns.items.guns.GenericGun;

/** Client -> server: a Techguns key was pressed (1.12 PacketTGKeybindPress). */
public record PacketTGKeybindPress(byte buttonID, boolean showMsg, boolean offHand) implements CustomPacketPayload {

	public static final byte TOGGLE_NIGHTVISION = 0;
	public static final byte TOGGLE_SAFEMODE = 1;
	public static final byte JETPACK_BOOST_START = 2;
	public static final byte JETPACK_BOOST_STOP = 3;
	public static final byte FORCE_RELOAD = 6;
	public static final byte TOGGLE_JETPACK = 7;
	public static final byte TOGGLE_STEP_ASSIST = 8;

	public static final Type<PacketTGKeybindPress> TYPE = new Type<>(Techguns.rl("keybind_press"));

	public static final StreamCodec<FriendlyByteBuf, PacketTGKeybindPress> CODEC = StreamCodec.of((buf, m) -> {
		buf.writeByte(m.buttonID);
		buf.writeBoolean(m.showMsg);
		buf.writeBoolean(m.offHand);
	}, buf -> new PacketTGKeybindPress(buf.readByte(), buf.readBoolean(), buf.readBoolean()));

	public PacketTGKeybindPress(byte buttonID) {
		this(buttonID, true, false);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PacketTGKeybindPress m, IPayloadContext ctx) {
		ctx.enqueueWork(() -> {
			if (ctx.player() instanceof ServerPlayer ply) {
				handleOnServer(m, ply);
			}
		});
	}

	/** the server side of a key press (separate from handle() so game tests can call it) */
	public static void handleOnServer(PacketTGKeybindPress m, ServerPlayer ply) {
		{
			TGExtendedPlayer props = TGExtendedPlayer.get(ply);
			boolean state;
			switch (m.buttonID) {
				case TOGGLE_NIGHTVISION -> {
					props.enableNightVision = !props.enableNightVision;
					state = props.enableNightVision;
					if (!state) {
						if (ply.hasEffect(MobEffects.NIGHT_VISION)) {
							ply.removeEffect(MobEffects.NIGHT_VISION);
							ply.level().playSound(null, ply.getX(), ply.getEyeY(), ply.getZ(), TGSounds.NIGHTVISION_OFF.get(), SoundSource.PLAYERS, 1f, 1f);
						}
					} else if (ArmorBonusUtil.getArmorBonusForPlayer(ply, TGArmorBonus.NIGHTVISION, false) > 0.0f) {
						ply.level().playSound(null, ply.getX(), ply.getEyeY(), ply.getZ(), TGSounds.NIGHTVISION_ON.get(), SoundSource.PLAYERS, 1f, 1f);
					}
				}
				case TOGGLE_SAFEMODE -> {
					props.enableSafemode = !props.enableSafemode;
					if (!props.enableSafemode && !techguns.TGPermissions.canUseUnsafeMode(ply)) {
						props.enableSafemode = true;
					}
					state = props.enableSafemode;
				}
				case TOGGLE_JETPACK -> {
					if (props.isJumpkeyPressed) {
						return;
					}
					props.enableJetpack = !props.enableJetpack;
					state = props.enableJetpack;
				}
				case JETPACK_BOOST_START, JETPACK_BOOST_STOP -> {
					props.isJumpkeyPressed = m.buttonID == JETPACK_BOOST_START;
					PacketDistributor.sendToPlayersTrackingEntity(ply, new PacketJetpackState(ply.getId(), props.isJumpkeyPressed));
					return;
				}
				case TOGGLE_STEP_ASSIST -> {
					props.enableStepAssist = !props.enableStepAssist;
					state = props.enableStepAssist;
				}
				case FORCE_RELOAD -> {
					InteractionHand hand = m.offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
					ItemStack item = ply.getItemInHand(hand);
					if (item.getItem() instanceof GenericGun gun) {
						gun.tryForcedReload(item, ply.level(), ply, hand);
					}
					return;
				}
				default -> {
					return;
				}
			}
			PacketDistributor.sendToPlayer(ply, PacketTGToggleSync.of(props, m.showMsg ? m.buttonID : -1, state));
		}
	}
}
