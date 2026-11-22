package techguns.capabilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;
import techguns.TGAttachments;
import techguns.api.capabilities.AttackTime;

/**
 * Per-player Techguns state. Was a Forge capability in 1.12.2; now a NeoForge data attachment
 * (registered in {@link TGAttachments}, copied on death).
 */
public class TGExtendedPlayer implements INBTSerializable<CompoundTag> {
	public static final int MAX_RADIATION = 1000;

	public int fireDelayMainhand = 0;
	public int fireDelayOffhand = 0;

	public int loopSoundDelayMainhand = 0;
	public int loopSoundDelayOffhand = 0;

	protected AttackTime[] attackTimes = { new AttackTime(), new AttackTime() };

	public int swingSoundDelay = 0;

	public boolean gotCreativeFlightLastTick = false;
	public boolean wasFlying = false;

	public ItemStack gunMainHand = ItemStack.EMPTY;
	public ItemStack gunOffHand = ItemStack.EMPTY;

	/** Guided missile tracking target. */
	public Entity lockOnEntity;
	public int lockOnTicks;

	/** Server only, not saved. */
	public boolean isJumpkeyPressed = false;
	/** Server only, not saved. */
	public boolean isForwardKeyPressed = false;
	/** Client only, not synced. */
	public boolean isGliding = false;
	/** Synced flag: player is charging a weapon. */
	public boolean isChargingWeapon = false;

	/* Saved fields */
	public boolean enableJetpack = true;
	public boolean enableStepAssist = true;
	public boolean enableNightVision = false;
	public boolean enableSafemode = false;
	public boolean enableHovermode = false;

	public int radlevel = 0;

	public short foodleft = 0;
	public float lastSaturation = 1.0f;

	public boolean showTGHudElements = true;

	/** server only: radlevel last sent to the client */
	public int lastSyncedRadlevel = -1;

	/** adds (or with a negative amount removes) RAD, kept within 0..MAX_RADIATION; adding needs the radiation system enabled */
	public void addRadiation(int amount) {
		if (amount > 0 && !techguns.radiation.TGRadiationSystem.isEnabled()) {
			return;
		}
		this.radlevel = Math.clamp((long) this.radlevel + amount, 0, MAX_RADIATION);
	}

	public static TGExtendedPlayer get(Player ply) {
		return ply.getData(TGAttachments.EXTENDED_PLAYER);
	}

	public AttackTime getAttackTime(boolean offHand) {
		return attackTimes[offHand ? 1 : 0];
	}

	public int getFireDelay(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? fireDelayMainhand : fireDelayOffhand;
	}

	public void setFireDelay(InteractionHand hand, int delay) {
		if (hand == InteractionHand.MAIN_HAND) {
			fireDelayMainhand = delay;
		} else {
			fireDelayOffhand = delay;
		}
	}

	public int getLoopSoundDelay(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? loopSoundDelayMainhand : loopSoundDelayOffhand;
	}

	public void setLoopSoundDelay(InteractionHand hand, int delay) {
		if (hand == InteractionHand.MAIN_HAND) {
			loopSoundDelayMainhand = delay;
		} else {
			loopSoundDelayOffhand = delay;
		}
	}

	public boolean isRecoiling(boolean offHand) {
		return getAttackTime(offHand).isRecoiling();
	}

	public boolean isReloading(boolean offHand) {
		return getAttackTime(offHand).isReloading();
	}

	public void swapAttackTimes() {
		AttackTime a = attackTimes[0];
		attackTimes[0] = attackTimes[1];
		attackTimes[1] = a;
		int i = fireDelayMainhand;
		fireDelayMainhand = fireDelayOffhand;
		fireDelayOffhand = i;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag tags = new CompoundTag();
		tags.putBoolean("enableJetpack", enableJetpack);
		tags.putBoolean("enableNightVision", enableNightVision);
		tags.putBoolean("enableSafemode", enableSafemode);
		tags.putBoolean("enableStepAssist", enableStepAssist);
		tags.putBoolean("showTGHudElements", showTGHudElements);
		tags.putBoolean("enableHovermode", enableHovermode);
		tags.putShort("foodLeft", foodleft);
		tags.putFloat("lastSaturation", lastSaturation);
		tags.putInt("radlevel", radlevel);
		return tags;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tags) {
		enableJetpack = tags.getBoolean("enableJetpack");
		enableNightVision = tags.getBoolean("enableNightVision");
		enableSafemode = tags.getBoolean("enableSafemode");
		enableStepAssist = tags.getBoolean("enableStepAssist");
		showTGHudElements = tags.getBoolean("showTGHudElements");
		enableHovermode = tags.getBoolean("enableHovermode");
		foodleft = tags.getShort("foodLeft");
		lastSaturation = tags.getFloat("lastSaturation");
		radlevel = tags.getInt("radlevel");
	}
}
