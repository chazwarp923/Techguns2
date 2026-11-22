package techguns.items.guns;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import techguns.api.guns.GunHandType;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.ShooterValues;
import techguns.client.TGClientState;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GenericProjectile;
import techguns.packets.PacketSpawnParticleOnEntity;
import techguns.packets.TGPackets;
import techguns.util.EntityCondition;
import techguns.util.SoundUtil;
import techguns.client.audio.TGSoundCategory;

/**
 * Gun that charges while right mouse is held (item use) and fires a charged projectile on release.
 * Left click still fires normal shots if canFireWhileCharging or not charging.
 */
public class GenericGunCharge extends GenericGun {

	/** In ticks */
	public float fullChargeTime = 20.0f;
	public int ammoConsumedOnFullCharge = 10;

	protected ChargedProjectileSelector<? extends GenericProjectile> chargedProjectile_selector;

	public boolean hasChargedFireAnim = true;
	public boolean canFireWhileCharging = false;

	Supplier<SoundEvent> startChargeSound = null;
	String chargeFX = null;
	private float chargeFXoffsetX = 0.0f;
	private float chargeFXoffsetY = 0.0f;
	private float chargeFXoffsetZ = 0.0f;

	public GenericGunCharge(ChargedProjectileSelector<? extends GenericProjectile> projectile_selector, boolean semiAuto, int minFiretime,
			int clipsize, int reloadtime, float damage, Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy,
			float fullChargeTime, int ammoConsumedOnFullCharge) {
		super(projectile_selector, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy);
		this.fullChargeTime = fullChargeTime;
		this.ammoConsumedOnFullCharge = ammoConsumedOnFullCharge;
		this.chargedProjectile_selector = projectile_selector;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		TGExtendedPlayer extendedPlayer = TGExtendedPlayer.get(player);
		ItemStack item = player.getItemInHand(hand);

		if (canZoom && player.isShiftKeyDown() && this.toggleZoom) {
			if (level.isClientSide) {
				TGClientState.playerZoom = TGClientState.playerZoom != 1.0f ? 1.0f : this.zoomMult;
			}
			return InteractionResultHolder.pass(item);
		}

		if (this.getCurrentAmmo(item) > 0) {
			if (extendedPlayer.getFireDelay(hand) <= 0) {
				extendedPlayer.setFireDelay(hand, this.minFiretime);
				player.startUsingItem(hand);
				this.startCharge(item, level, player);
				extendedPlayer.isChargingWeapon = true;

				if (this.startChargeSound != null) {
					SoundUtil.playSoundOnEntityGunPosition(level, player, sound(this.startChargeSound), SOUND_DISTANCE, 1.0F, false, true, false,
							TGSoundCategory.GUN_FIRE, EntityCondition.CHARGING_WEAPON);
				}

				if (this.chargeFX != null && !level.isClientSide) {
					float x = player.getMainArm() == HumanoidArm.RIGHT ? this.chargeFXoffsetX : -this.chargeFXoffsetX;
					TGPackets.sendToAllAround(new PacketSpawnParticleOnEntity(this.chargeFX, player, x, this.chargeFXoffsetY, this.chargeFXoffsetZ, true,
							EntityCondition.CHARGING_WEAPON), player, 25.0);
				}
				return InteractionResultHolder.consume(item);
			}
		} else {
			// mag empty: reload
			this.tryForcedReload(item, level, player, hand);
		}
		return InteractionResultHolder.pass(item);
	}

	/** Override for onCharge code. */
	protected void startCharge(ItemStack item, Level level, Player player) {
	}

	@Override
	public void shootGunPrimary(ItemStack stack, Level level, Player player, boolean zooming, InteractionHand hand, Entity target) {
		if (this.canFireWhileCharging || player.getUseItem() != stack) {
			super.shootGunPrimary(stack, level, player, zooming, hand, target);
		}
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.NONE;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);
		// charging can end without releaseUsing (slot change, death, ...)
		if (selected && entity instanceof Player player && !player.isUsingItem()) {
			TGExtendedPlayer txp = TGExtendedPlayer.get(player);
			if (txp.isChargingWeapon) {
				txp.isChargingWeapon = false;
			}
		}
	}

	@Override
	public void releaseUsing(ItemStack item, Level level, LivingEntity entityLiving, int timeLeft) {
		if (!(entityLiving instanceof Player player)) {
			return;
		}
		TGExtendedPlayer.get(player).isChargingWeapon = false;

		int j = this.getUseDuration(item, player) - timeLeft;
		float f = Math.min(1.0f, j / this.fullChargeTime);

		int ammoConsumed = this.consumeAmmoCharge(item, f, player.getAbilities().instabuild);

		// reduce charge value if ammo is low
		if (ammoConsumed < (int) Math.ceil(f * this.ammoConsumedOnFullCharge)) {
			f = (float) ammoConsumed / (float) this.ammoConsumedOnFullCharge;
		}

		if (!level.isClientSide) {
			EnumBulletFirePos firePos = player.getMainArm() == HumanoidArm.RIGHT ? EnumBulletFirePos.RIGHT : EnumBulletFirePos.LEFT;
			// charged shot is always from the main hand
			spawnChargedProjectile(level, player, item, accuracy, f, ammoConsumed, firePos);
			if (shotgun) {
				for (int i = 0; i < bulletcount; i++) {
					spawnChargedProjectile(level, player, item, spread, f, ammoConsumed, firePos);
				}
			}
			if (this.hasChargedFireAnim) {
				this.playChargedFiresound(item, level, player, f);
			}
		} else {
			if (this.hasChargedFireAnim) {
				int recoiltime_l = getRecoilTime(f);
				int muzzleFlashtime_l = getMuzzleFlashTime(f);
				boolean offHand = player.getUsedItemHand() == InteractionHand.OFF_HAND;
				ShooterValues.setRecoiltime(player, offHand, System.currentTimeMillis() + recoiltime_l, recoiltime_l, (byte) 1, f);
				ShooterValues.setMuzzleFlashTime(player, offHand, System.currentTimeMillis() + muzzleFlashtime_l, muzzleFlashtime_l);
			}
			client_weaponFired();
		}
	}

	protected void playChargedFiresound(ItemStack item, Level level, Player player, float chargeProgress) {
		SoundUtil.playSoundOnEntityGunPosition(level, player, sound(firesound), SOUND_DISTANCE, 1.0f, false, false, true, TGSoundCategory.GUN_FIRE);
		if (rechamberSound != null) {
			SoundUtil.playSoundOnEntityGunPosition(level, player, sound(rechamberSound), 1.0f, 1.0f, false, false, true, TGSoundCategory.GUN_FIRE);
		}
	}

	public int getRecoilTime(float charge) {
		return ticksToMillis(recoiltime);
	}

	public int getMuzzleFlashTime(float charge) {
		return ticksToMillis(muzzleFlashtime);
	}

	/**
	 * consume ammo
	 * @param f charge amount
	 * @return amount of ammo actually consumed
	 */
	public int consumeAmmoCharge(ItemStack item, float f, boolean creative) {
		int amount = (int) Math.ceil(f * this.ammoConsumedOnFullCharge);
		if (!creative) {
			amount = this.useAmmo(item, amount);
		}
		return amount;
	}

	public void spawnChargedProjectile(Level level, LivingEntity player, ItemStack itemStack, float spread, float charge, int ammoConsumed,
			EnumBulletFirePos firePos) {
		IChargedProjectileFactory<? extends GenericProjectile> fact = this.chargedProjectile_selector
				.getFactoryForType(this.getCurrentAmmoVariantKey(itemStack));
		GenericProjectile proj = fact.createChargedProjectile(level, player, damage, speed, this.getScaledTTL(), spread, this.damageDropStart,
				damageDropEnd, this.damageMin, penetration, getDoBlockDamage(player), firePos, radius, gravity, charge, ammoConsumed);
		if (proj != null) {
			level.addFreshEntity(proj);
		}
	}

	@Override
	public boolean canCharge() {
		return true;
	}

	public GenericGunCharge setChargeFireAnims(boolean hasAnims) {
		this.hasChargedFireAnim = hasAnims;
		return this;
	}

	public GenericGunCharge setFireWhileCharging(boolean canFire) {
		this.canFireWhileCharging = canFire;
		return this;
	}

	@Override
	public boolean hasRightClickAction() {
		return this.getGunHandType() == GunHandType.TWO_HANDED;
	}

	public GenericGunCharge setChargeSound(Supplier<SoundEvent> startChargeSound) {
		this.startChargeSound = startChargeSound;
		return this;
	}

	public GenericGunCharge setChargeFX(String fx, float offsetX, float offsetY, float offsetZ) {
		this.chargeFX = fx;
		this.chargeFXoffsetX = offsetX;
		this.chargeFXoffsetY = offsetY;
		this.chargeFXoffsetZ = offsetZ;
		return this;
	}
}
