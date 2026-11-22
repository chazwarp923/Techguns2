package techguns.items.additionalslots;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.api.tginventory.TGSlotType;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.armors.TGArmorBonus;
import techguns.packets.PacketSpawnParticle;
import techguns.packets.PacketSpawnParticleOnEntity;
import techguns.packets.TGPackets;
import techguns.util.TextUtil;

/** Back slot: jump boost and fall protection (jetpack toggle), reloads with compressed air tanks. */
public class ItemJumpPack extends ItemTGSpecialSlot {

	private static final int DUR_PER_USE = 1;
	private static final float JUMPBOOST = 0.8f;
	private static final float FREEHEIGHT = 8.0f;
	private static final float FALLDMG = 0.2f;

	public ItemJumpPack(int camoCount, int dur) {
		super(TGSlotType.BACKSLOT, camoCount, dur, TGItems.COMPRESSED_AIR_TANK, TGItems.COMPRESSED_AIR_TANK_EMPTY);
	}

	private static boolean canUse(ItemStack stack) {
		return stack.getDamageValue() + DUR_PER_USE <= stack.getMaxDamage();
	}

	@Override
	public void onPlayerTick(ItemStack item, Player ply) {
		if (!canUse(item)) {
			this.tryReloadAndRepair(item, ply);
		}
	}

	@Override
	public float getBonus(TGArmorBonus type, ItemStack stack, boolean consume, Player player) {
		if (type == TGArmorBonus.JUMP) {
			if (consume) {
				if (TGExtendedPlayer.get(player).enableJetpack && canUse(stack)) {
					useCharge(stack, DUR_PER_USE);
					doJumpEffect(player);
					return JUMPBOOST;
				}
			} else if (canUse(stack)) {
				return JUMPBOOST;
			}
		} else if (type == TGArmorBonus.FREEHEIGHT) {
			if (hasCharge(stack)) {
				return FREEHEIGHT;
			}
		} else if (type == TGArmorBonus.FALLDMG) {
			if (consume) {
				if (TGExtendedPlayer.get(player).enableJetpack && canUse(stack)) {
					useCharge(stack, DUR_PER_USE);
					doFallEffect(player);
					return FALLDMG;
				}
			} else if (hasCharge(stack)) {
				return FALLDMG;
			}
		}
		return 0;
	}

	public void doJumpEffect(Player ply) {
		if (!ply.level().isClientSide) {
			TGPackets.sendToAllAround(new PacketSpawnParticleOnEntity("JumpPackBoost", ply), ply, 50);
			ply.level().playSound(null, ply.getX(), ply.getY(), ply.getZ(), TGSounds.STEAM_JUMP_2.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		}
	}

	public void doFallEffect(Player ply) {
		if (!ply.level().isClientSide) {
			TGPackets.sendToAllAround(new PacketSpawnParticle("JumpPackFall", ply.getX(), ply.getY(), ply.getZ()), ply, 50);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(TextUtil.transTG("armorTooltip.jumpheight").append(": +" + JUMPBOOST));
		tooltip.add(TextUtil.transTG("armorTooltip.falldamage").append(": -" + FALLDMG * 100.0f + "%"));
		tooltip.add(TextUtil.transTG("armorTooltip.fallheight").append(": -" + FREEHEIGHT));
	}
}
