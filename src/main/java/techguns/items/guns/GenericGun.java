package techguns.items.guns;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import techguns.TGDataComponents;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.api.damagesystem.DamageType;
import techguns.api.guns.GunHandType;
import techguns.api.guns.IGenericGun;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.ShooterValues;
import techguns.client.TGClientState;
import techguns.client.audio.TGSoundCategory;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GenericProjectile;
import techguns.items.GenericItem;
import techguns.items.armors.ArmorBonusUtil;
import techguns.items.armors.ICamoChangeable;
import techguns.items.armors.TGArmorBonus;
import techguns.items.guns.ammo.AmmoType;
import techguns.items.guns.ammo.AmmoTypes;
import techguns.items.guns.ammo.DamageModifier;
import techguns.packets.GunFiredMessage;
import techguns.packets.ReloadStartedMessage;
import techguns.packets.TGPackets;
import techguns.plugins.crafttweaker.EnumGunStat;
import techguns.util.InventoryUtil;
import techguns.util.MathUtil;
import techguns.util.SoundUtil;
import techguns.util.TextUtil;

/**
 * Base gun item. Stats are configured with the builder style setters (see TGuns).
 * Gun state lives in data components: {@link TGDataComponents#AMMO}, {@link TGDataComponents#AMMO_VARIANT},
 * {@link TGDataComponents#CAMO}. A stack without AMMO component counts as fully loaded.
 */
public class GenericGun extends GenericItem implements IGenericGun, ICamoChangeable {

	public static final float SOUND_DISTANCE = 4.0f;

	boolean semiAuto = false;
	int minFiretime = 4;
	int clipsize = 10;
	int reloadtime = 40; // ticks
	float damage = 2.0f;

	// sounds are suppliers since registry objects don't exist yet when guns are defined
	Supplier<SoundEvent> firesound = TGSounds.M4_FIRE;
	Supplier<SoundEvent> reloadsound = TGSounds.M4_RELOAD;
	Supplier<SoundEvent> firesoundStart = TGSounds.M4_FIRE;
	Supplier<SoundEvent> rechamberSound = null;

	int ammoCount; // ammo per reload
	float zoomMult = 1.0f;
	boolean canZoom = false;
	boolean toggleZoom = false;
	boolean fireCenteredZoomed = false;

	int ticksToLive = 40;
	float speed = 2.0f;

	float damageMin = 1.0f;
	float damageDropStart = 20f;
	float damageDropEnd = 40f;

	float penetration = 0.0f;
	AmmoType ammoType = AmmoTypes.PISTOL_ROUNDS;

	boolean shotgun = false;
	boolean burst = false;
	float spread = 0.015f;
	int bulletcount = 7;
	float accuracy = 0.0f;
	float projectileForwardOffset = 0.0f;

	int maxLoopDelay = 0;

	int recoiltime = 5; // ticks
	int muzzleFlashtime = 5;

	boolean silenced = false;
	boolean checkRecoil = false;
	boolean checkMuzzleFlash = false;

	float AI_attackRange = 15.0F;
	int AI_attackTime = 60;
	int AI_burstCount = 0;
	int AI_burstAttackTime = 0;

	int camoCount = 1;

	int lockOnTicks = 0;
	int lockOnPersistTicks = 0;

	EnumCrosshairStyle crossHairStyle = EnumCrosshairStyle.GUN_DYNAMIC;

	public List<ResourceLocation> textures = new ArrayList<>();

	protected ProjectileSelector<? extends GenericProjectile> projectile_selector;

	GunHandType handType = GunHandType.TWO_HANDED;

	int miningAmmoConsumption = 1;

	float meleeDamagePwr = 6.0f;
	float meleeDamageEmpty = 2.0f;
	float digSpeed = 1.0f;

	/** spread multiplier while shooting zoomed */
	float zoombonus = 1.0f;

	float radius = 1.0f;
	double gravity = 0.0f;

	boolean shootWithLeftClick = true;

	public float turretPosOffsetX = 0.0f;
	public float turretPosOffsetY = 0.0f;
	public float turretPosOffsetZ = 0.0f;

	public static final ArrayList<GenericGun> guns = new ArrayList<>();

	/** Lightpulse parameters */
	public int light_lifetime = 2;
	public float light_radius_start = 3.0f;
	public float light_radius_end = 3.0f;
	public float light_r = 1f;
	public float light_g = 0.9f;
	public float light_b = 0.2f;
	protected boolean muzzelight = true;

	boolean hasAimedBowAnim = true;
	boolean hasAmbientEffect = false;

	/** Should bind the texture on rendering? */
	public boolean hasCustomTexture = true;

	protected RangeTooltipType rangeTooltipType = RangeTooltipType.DROP;

	public GenericGun(ProjectileSelector<? extends GenericProjectile> projectileSelector, boolean semiAuto, int minFiretime, int clipsize,
			int reloadtime, float damage, Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy) {
		this(true, projectileSelector, semiAuto, minFiretime, clipsize, reloadtime, damage, firesound, reloadsound, TTL, accuracy);
	}

	public GenericGun(boolean addToGunList, ProjectileSelector<? extends GenericProjectile> projectile_selector, boolean semiAuto, int minFiretime,
			int clipsize, int reloadtime, float damage, Supplier<SoundEvent> firesound, Supplier<SoundEvent> reloadsound, int TTL, float accuracy) {
		super(new Item.Properties().stacksTo(1).setNoRepair());
		this.ammoType = projectile_selector.ammoType;
		this.semiAuto = semiAuto;
		this.minFiretime = minFiretime;
		this.clipsize = clipsize;
		this.reloadtime = reloadtime;
		this.damage = damage;
		this.firesound = firesound;
		this.firesoundStart = firesound;
		this.reloadsound = reloadsound;
		this.ammoCount = 1;
		this.ticksToLive = TTL;
		this.accuracy = accuracy;
		this.damageDropStart = TTL;
		this.damageDropEnd = TTL;
		this.damageMin = damage;
		this.projectile_selector = projectile_selector;
		if (addToGunList) {
			guns.add(this);
		}
	}

	// ------------------------------------------------------------------ builder setters

	public GenericGun setMuzzleLight(float r, float g, float b) {
		this.light_r = r;
		this.light_g = g;
		this.light_b = b;
		return this;
	}

	public GenericGun setNoMuzzleLight() {
		this.muzzelight = false;
		return this;
	}

	public GenericGun setMuzzleLight(int lifetime, float radius_start, float radius_end, float r, float g, float b) {
		this.light_lifetime = lifetime;
		this.light_radius_start = radius_start;
		this.light_radius_end = radius_end;
		this.setMuzzleLight(r, g, b);
		return this;
	}

	public GenericGun setRangeTooltipType(RangeTooltipType type) {
		this.rangeTooltipType = type;
		return this;
	}

	public GenericGun setHasAmbient() {
		this.hasAmbientEffect = true;
		return this;
	}

	public boolean hasAmbientEffect() {
		return this.hasAmbientEffect;
	}

	/** whether the held ambient effect (Shishkebap flames) should be running: the gun has one and still has ammo */
	public boolean isAmbientEffectActive(ItemStack stack) {
		return this.hasAmbientEffect && this.getAmmoLeft(stack) > 0;
	}

	public GenericGun setNoCustomTexture() {
		this.hasCustomTexture = false;
		return this;
	}

	public GenericGun setNoBowAnim() {
		this.hasAimedBowAnim = false;
		return this;
	}

	public boolean hasBowAnim() {
		return this.hasAimedBowAnim;
	}

	public GenericGun setGravity(double grav) {
		this.gravity = grav;
		return this;
	}

	public GenericGun setAmmoCount(int count) {
		this.ammoCount = count;
		return this;
	}

	public GenericGun setZoom(float mult, boolean toggle, float bonus, boolean fireCenteredWhileZooming) {
		this.canZoom = true;
		this.zoomMult = mult;
		this.toggleZoom = toggle;
		this.zoombonus = bonus;
		this.fireCenteredZoomed = fireCenteredWhileZooming;
		return this;
	}

	public GenericGun setShotgunSpred(int count, float spread) {
		return this.setShotgunSpread(count, spread, false);
	}

	public GenericGun setShotgunSpread(int count, float spread, boolean burst) {
		this.shotgun = true;
		this.spread = spread;
		this.bulletcount = count;
		this.burst = burst;
		return this;
	}

	public GenericGun setForwardOffset(float offset) {
		this.projectileForwardOffset = offset;
		return this;
	}

	public GenericGun setBulletSpeed(float speed) {
		this.speed = speed;
		return this;
	}

	public GenericGun setPenetration(float pen) {
		this.penetration = pen;
		return this;
	}

	public GenericGun setMuzzleFlashTime(int time) {
		this.muzzleFlashtime = time;
		return this;
	}

	public GenericGun setFiresoundStart(Supplier<SoundEvent> firesoundStart) {
		this.firesoundStart = firesoundStart;
		return this;
	}

	public GenericGun setMaxLoopDelay(int maxLoopDelay) {
		this.maxLoopDelay = maxLoopDelay;
		return this;
	}

	public GenericGun setRecoiltime(int recoiltime) {
		this.recoiltime = recoiltime;
		return this;
	}

	public GenericGun setRechamberSound(Supplier<SoundEvent> rechamberSound) {
		this.rechamberSound = rechamberSound;
		return this;
	}

	/**
	 * @param x - Offset sideways, +right -left
	 * @param y - Offset height
	 * @param z - offset forward/backward
	 */
	public GenericGun setTurretPosOffset(float x, float y, float z) {
		this.turretPosOffsetX = x;
		this.turretPosOffsetY = y;
		this.turretPosOffsetZ = z;
		return this;
	}

	protected int getScaledTTL() {
		return (int) Math.ceil(this.ticksToLive / this.speed);
	}

	/**
	 * @param ticks Time required to lock target
	 * @param lockExtraTicks Extra time that is added when the lock is complete
	 */
	public GenericGun setLockOn(int ticks, int lockExtraTicks) {
		this.lockOnTicks = ticks;
		this.lockOnPersistTicks = lockExtraTicks;
		return this;
	}

	public int getLockOnTicks() {
		return lockOnTicks;
	}

	public int getLockOnPersistTicks() {
		return lockOnPersistTicks;
	}

	public GenericGun setCheckRecoil() {
		this.checkRecoil = true;
		return this;
	}

	public GenericGun setCheckMuzzleFlash() {
		this.checkMuzzleFlash = true;
		return this;
	}

	public GenericGun setAIStats(float attackRange, int attackTime, int burstCount, int burstAttackTime) {
		this.AI_attackRange = attackRange;
		this.AI_attackTime = attackTime;
		this.AI_burstCount = burstCount;
		this.AI_burstAttackTime = burstAttackTime;
		return this;
	}

	/** Texture path inside assets/techguns without ".png", e.g. "textures/guns/m4Texture" (lowercased for 1.21). */
	public GenericGun setTexture(String path) {
		return setTextures(path, 1);
	}

	public GenericGun setTextures(String path, int variations) {
		return setTextures(Techguns.rl(path.toLowerCase()), variations);
	}

	public GenericGun setTexture(ResourceLocation path) {
		return setTextures(path, 1);
	}

	public GenericGun setTextures(ResourceLocation path, int variations) {
		this.textures.clear();
		for (int i = 0; i < variations; i++) {
			this.textures.add(ResourceLocation.fromNamespaceAndPath(path.getNamespace(), path.getPath() + (i == 0 ? "" : "_" + i) + ".png"));
		}
		this.camoCount = variations;
		return this;
	}

	public GenericGun setDamageDrop(float start, float end, float minDamage) {
		this.damageDropStart = start;
		this.damageDropEnd = end;
		this.damageMin = minDamage;
		return this;
	}

	public GenericGun setSilenced(boolean s) {
		this.silenced = s;
		return this;
	}

	public GenericGun setHandType(GunHandType type) {
		this.handType = type;
		return this;
	}

	/** Should this weapon shoot with left click instead of mine, defaults to true */
	public GenericGun setShootWithLeftClick(boolean shootWithLeftClick) {
		this.shootWithLeftClick = shootWithLeftClick;
		return this;
	}

	public GenericGun setMiningAmmoConsumption(int ammo) {
		this.miningAmmoConsumption = ammo;
		return this;
	}

	public GenericGun setCrossHair(EnumCrosshairStyle crosshair) {
		this.crossHairStyle = crosshair;
		return this;
	}

	// ------------------------------------------------------------------ zoom / input

	/** Called only clientside, requires packets for actions other than zoom */
	public boolean gunSecondaryAction(Player player, ItemStack stack) {
		if (player.level().isClientSide && canZoom && this.toggleZoom && !ShooterValues.getPlayerIsReloading(player, false)) {
			if (TGClientState.playerZoom != 1.0f) {
				TGClientState.playerZoom = 1.0f;
			} else {
				TGClientState.playerZoom = this.zoomMult;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
		if (this.shootWithLeftClick) {
			return true;
		}
		return this.getCurrentAmmo(stack) < this.miningAmmoConsumption;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		this.gunSecondaryAction(player, player.getItemInHand(hand));
		return InteractionResultHolder.pass(player.getItemInHand(hand));
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		return !this.shootWithLeftClick;
	}

	@Override
	public boolean isShootWithLeftClick() {
		return this.shootWithLeftClick;
	}

	@Override
	public boolean isSemiAuto() {
		return this.semiAuto;
	}

	@Override
	public boolean isZooming() {
		return TGClientState.playerZoom == this.zoomMult;
	}

	@Override
	public boolean isHoldZoom() {
		return !this.toggleZoom;
	}

	@Override
	public float getZoomMult() {
		return this.zoomMult;
	}

	public boolean canZoom() {
		return canZoom;
	}

	// ------------------------------------------------------------------ shooting

	public ItemStack[] getReloadItem(ItemStack stack) {
		return this.ammoType.getAmmo(this.getCurrentAmmoVariant(stack));
	}

	public int getAmmoCount() {
		return ammoCount;
	}

	public int getAmmoLeftCountTooltip(ItemStack item) {
		int ammo = this.getCurrentAmmo(item);
		return this.burst ? ammo * (this.bulletcount + 1) : ammo;
	}

	public int getClipsizeTooltip() {
		return this.burst ? clipsize * (this.bulletcount + 1) : clipsize;
	}

	public int getAmmoLeftCount(int mags) {
		int count = (this.clipsize / this.ammoCount) * mags;
		return this.burst ? count * (this.bulletcount + 1) : count;
	}

	protected void spawnProjectile(final Level level, final LivingEntity player, final ItemStack itemstack, float spread, float offset,
			float damagebonus, EnumBulletFirePos firePos, @Nullable Entity target) {
		IProjectileFactory<? extends GenericProjectile> projectile = this.projectile_selector
				.getFactoryForType(this.getCurrentAmmoVariantKey(itemstack));

		GenericProjectile proj = projectile.createProjectile(this, level, player, damage * damagebonus, speed, this.getScaledTTL(), spread,
				this.damageDropStart, this.damageDropEnd, this.damageMin * damagebonus, this.penetration, getDoBlockDamage(player), firePos, radius,
				gravity);

		// 1.12 muzzle light pulses (light_* fields) needed the Albedo dynamic light mod; not ported.

		if (silenced) {
			proj.setSilenced();
		}
		if (offset > 0.0f) {
			proj.shiftForward(offset / speed);
		}
		level.addFreshEntity(proj);
	}

	public static boolean getDoBlockDamage(LivingEntity elb) {
		if (elb instanceof Player ply) {
			return !TGExtendedPlayer.get(ply).enableSafemode;
		}
		return false;
	}

	protected static int ticksToMillis(int ticks) {
		return (int) ((ticks / 20.0f) * 1000.0f);
	}

	@Override
	public void shootGunPrimary(ItemStack stack, Level level, Player player, boolean zooming, InteractionHand hand, @Nullable Entity target) {
		int ammo = this.getCurrentAmmo(stack);
		byte ATTACK_TYPE = 0;
		TGExtendedPlayer extendedPlayer = TGExtendedPlayer.get(player);

		if (ammo > 0) {
			int firedelay = extendedPlayer.getFireDelay(hand);
			if (firedelay <= 0) {
				extendedPlayer.setFireDelay(hand, this.minFiretime);

				if (!player.getAbilities().instabuild) {
					this.useAmmo(stack, 1);
				}

				if (!level.isClientSide) {
					// SERVER: create projectile
					float accuracybonus = MathUtil.clamp(1.0f - ArmorBonusUtil.getArmorBonusForPlayer(player, TGArmorBonus.GUN_ACCURACY, false), 0f,
							1f);

					if (hand == InteractionHand.MAIN_HAND && player.isBlocking()) {
						accuracybonus *= this.handType == GunHandType.ONE_HANDED ? 4.0f : 8.0f;
					}

					EnumBulletFirePos firePos;
					if ((hand == InteractionHand.MAIN_HAND && player.getMainArm() == HumanoidArm.RIGHT)
							|| (hand == InteractionHand.OFF_HAND && player.getMainArm() == HumanoidArm.LEFT)) {
						firePos = EnumBulletFirePos.RIGHT;
					} else {
						firePos = EnumBulletFirePos.LEFT;
					}

					if (zooming) {
						accuracybonus *= this.zoombonus;
						if (fireCenteredZoomed) {
							firePos = EnumBulletFirePos.CENTER;
						}
					}
					this.shootGun(level, player, stack, accuracybonus, 1.0f, ATTACK_TYPE, hand, firePos, target);
				} else {
					// CLIENT: effects
					int recoiltime_l = ticksToMillis(recoiltime);
					int muzzleFlashtime_l = ticksToMillis(muzzleFlashtime);
					boolean offHand = hand == InteractionHand.OFF_HAND;
					if (!checkRecoil || !ShooterValues.isStillRecoiling(player, offHand, ATTACK_TYPE)) {
						ShooterValues.setRecoiltime(player, offHand, System.currentTimeMillis() + recoiltime_l, recoiltime_l, ATTACK_TYPE);
					}
					if (!checkMuzzleFlash || ShooterValues.getMuzzleFlashTime(player, offHand) <= System.currentTimeMillis()) {
						ShooterValues.setMuzzleFlashTime(player, offHand, System.currentTimeMillis() + muzzleFlashtime_l, muzzleFlashtime_l);
						var rand = level.random;
						TGClientState.muzzleFlashJitterX = 1.0f - (rand.nextFloat() * 2.0f);
						TGClientState.muzzleFlashJitterY = 1.0f - (rand.nextFloat() * 2.0f);
						TGClientState.muzzleFlashJitterScale = 1.0f - (rand.nextFloat() * 2.0f);
						TGClientState.muzzleFlashJitterAngle = 1.0f - (rand.nextFloat() * 2.0f);
					}
					client_weaponFired();
				}

				// sounds on client & server
				if (maxLoopDelay > 0 && extendedPlayer.getLoopSoundDelay(hand) <= 0) {
					SoundUtil.playSoundOnEntityGunPosition(level, player, sound(this.firesoundStart), SOUND_DISTANCE, 1.0F, false, false,
							TGSoundCategory.GUN_FIRE);
					extendedPlayer.setLoopSoundDelay(hand, this.maxLoopDelay);
				} else {
					SoundUtil.playSoundOnEntityGunPosition(level, player, sound(this.firesound), SOUND_DISTANCE, 1.0F, false, false,
							TGSoundCategory.GUN_FIRE);
					if (this.maxLoopDelay > 0) {
						extendedPlayer.setLoopSoundDelay(hand, this.maxLoopDelay);
					}
				}

				if (rechamberSound != null) {
					SoundUtil.playSoundOnEntityGunPosition(level, player, sound(rechamberSound), 1.0F, 1.0F, false, false, TGSoundCategory.RELOAD);
				}
			}
		} else {
			// empty: try to reload
			if (InventoryUtil.consumeAmmoPlayer(player, this.ammoType.getAmmo(this.getCurrentAmmoVariant(stack)))) {
				giveEmptyMags(level, player);

				if (level.isClientSide) {
					resetToggleZoom();
				}

				extendedPlayer.setFireDelay(hand, this.reloadtime - this.minFiretime);

				if (ammoCount > 1) {
					int i = 1;
					while (i < ammoCount && InventoryUtil.consumeAmmoPlayer(player, this.ammoType.getAmmo(this.getCurrentAmmoVariant(stack)))) {
						i++;
					}
					this.reloadAmmo(stack, i);
				} else {
					this.reloadAmmo(stack);
				}

				startReloadEffects(level, player, hand, ATTACK_TYPE);
			}
		}
	}

	protected void giveEmptyMags(Level level, Player player) {
		for (ItemStack e : this.ammoType.getEmptyMag()) {
			if (!e.isEmpty()) {
				giveOrDrop(level, player, TGItems.newStack(e, 1));
			}
		}
	}

	protected static void giveOrDrop(Level level, Player player, ItemStack stack) {
		int amount = InventoryUtil.addAmmoToPlayerInventory(player, stack);
		if (amount > 0 && !level.isClientSide) {
			level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), TGItems.newStack(stack, amount)));
		}
	}

	protected void resetToggleZoom() {
		if (canZoom && this.toggleZoom && TGClientState.playerZoom != 1.0f) {
			TGClientState.playerZoom = 1.0f;
		}
	}

	protected void startReloadEffects(Level level, Player player, InteractionHand hand, byte attackType) {
		SoundUtil.playReloadSoundOnEntity(level, player, sound(reloadsound), 1.0F, 1.0F, false, true, TGSoundCategory.RELOAD);
		int time = ticksToMillis(reloadtime);
		if (level.isClientSide) {
			ShooterValues.setReloadtime(player, hand == InteractionHand.OFF_HAND, System.currentTimeMillis() + time, time, attackType);
			client_startReload();
		} else {
			TGPackets.sendToAllAround(new ReloadStartedMessage(player, hand, time, attackType), player, 100.0);
		}
	}

	@Nullable
	protected static SoundEvent sound(@Nullable Supplier<SoundEvent> s) {
		return s != null ? s.get() : null;
	}

	/** for extra actions in subclass */
	protected void client_weaponFired() {
	}

	/** for extra actions in subclass */
	protected void client_startReload() {
	}

	protected void shootGun(Level level, LivingEntity player, ItemStack itemstack, float accuracybonus, float damagebonus, int attackType,
			InteractionHand hand, EnumBulletFirePos firePos, @Nullable Entity target) {
		if (!level.isClientSide) {
			TGPackets.sendToAllAround(new GunFiredMessage(player, ticksToMillis(recoiltime), ticksToMillis(muzzleFlashtime), attackType, checkRecoil,
					hand == InteractionHand.OFF_HAND), player, 100.0);
		}

		spawnProjectile(level, player, itemstack, accuracy * accuracybonus, projectileForwardOffset, damagebonus, firePos, target);

		if (shotgun) {
			float offset = 0;
			if (this.burst) {
				offset = this.speed / (this.bulletcount);
			}
			for (int i = 0; i < bulletcount; i++) {
				spawnProjectile(level, player, itemstack, spread * accuracybonus, projectileForwardOffset + offset * (i + 1.0f), damagebonus, firePos,
						target);
			}
		}
	}

	/** Weapon is used by NPC */
	public void fireWeaponFromNPC(LivingEntity shooter, float dmgscale, float accscale) {
		SoundUtil.playSoundOnEntityGunPosition(shooter.level(), shooter, sound(firesound), SOUND_DISTANCE, 1.0F, false, false,
				TGSoundCategory.GUN_FIRE);
		EnumBulletFirePos firePos = isTurret(shooter) ? EnumBulletFirePos.CENTER : EnumBulletFirePos.RIGHT;
		if (!shooter.level().isClientSide) {
			this.shootGun(shooter.level(), shooter, shooter.getMainHandItem(), this.zoombonus * accscale, dmgscale, 0, InteractionHand.MAIN_HAND,
					firePos, null);
		}
	}

	private static boolean isTurret(LivingEntity e) {
		return techguns.damagesystem.DamageSystem.TURRET_CHECK.test(e);
	}

	// ------------------------------------------------------------------ ammo state

	public int getCurrentAmmo(ItemStack stack) {
		return stack.getOrDefault(TGDataComponents.AMMO.get(), this.clipsize);
	}

	public int getCurrentAmmoVariant(ItemStack stack) {
		return this.getAmmoType().getIDforVariantKey(this.getCurrentAmmoVariantKey(stack));
	}

	public String getCurrentAmmoVariantKey(ItemStack stack) {
		String var = stack.get(TGDataComponents.AMMO_VARIANT.get());
		if (var == null || var.isEmpty()) {
			return AmmoTypes.TYPE_DEFAULT;
		}
		return var;
	}

	public void setCurrentAmmoVariantKey(ItemStack stack, String key) {
		if (AmmoTypes.TYPE_DEFAULT.equals(key)) {
			stack.remove(TGDataComponents.AMMO_VARIANT.get());
		} else {
			stack.set(TGDataComponents.AMMO_VARIANT.get(), key);
		}
	}

	/**
	 * Reduces ammo by amount, use positive values! amount=3 means ammo-=3;
	 *
	 * @return actually consumed ammo
	 */
	public int useAmmo(ItemStack stack, int amount) {
		int ammo = this.getCurrentAmmo(stack);
		if (ammo - amount >= 0) {
			stack.set(TGDataComponents.AMMO.get(), ammo - amount);
			return amount;
		} else {
			stack.set(TGDataComponents.AMMO.get(), 0);
			return ammo;
		}
	}

	public void reloadAmmo(ItemStack stack) {
		this.reloadAmmo(stack, this.clipsize);
	}

	public void reloadAmmo(ItemStack stack, int amount) {
		stack.set(TGDataComponents.AMMO.get(), this.getCurrentAmmo(stack) + amount);
	}

	/** An empty gun stack (for recipes that should not give free ammo). */
	public ItemStack getEmptyGun() {
		ItemStack s = new ItemStack(this);
		s.set(TGDataComponents.AMMO.get(), 0);
		return s;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0f * (float) getPercentAmmoLeft(stack));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		float f = Math.max(0.0F, (float) getPercentAmmoLeft(stack));
		return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
	}

	public double getPercentAmmoLeft(ItemStack stack) {
		return ((double) this.getCurrentAmmo(stack)) / ((double) this.clipsize);
	}

	@Override
	public int getAmmoLeft(ItemStack stack) {
		return this.getCurrentAmmo(stack);
	}

	public boolean isFullyLoaded(ItemStack stack) {
		return this.clipsize == this.getCurrentAmmo(stack);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
	}

	@Override
	public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
		return !ItemStack.isSameItem(oldStack, newStack);
	}

	@Override
	public GunHandType getGunHandType() {
		return this.handType;
	}

	@Override
	public ResourceLocation getCurrentTexture(ItemStack stack) {
		int camo = this.getCurrentCamoIndex(stack);
		if (camo < this.textures.size()) {
			return textures.get(camo);
		}
		return this.textures.isEmpty() ? Techguns.rl("textures/guns/missing.png") : this.textures.get(0);
	}

	// ------------------------------------------------------------------ tooltips

	protected DamageModifier getCurrentDamageModifier(ItemStack stack) {
		return this.projectile_selector.getFactoryForType(this.getCurrentAmmoVariantKey(stack)).getDamageModifier();
	}

	protected Component getTooltipTextDmg(ItemStack stack, boolean expanded) {
		DamageModifier mod = getCurrentDamageModifier(stack);
		float dmg = mod.getDamage(this.damage);
		if (dmg == this.damage) {
			return Component.literal("" + this.damage + (this.damageMin < this.damage ? "-" + this.damageMin : ""));
		}
		float dmgmin = mod.getDamage(this.damageMin);
		ChatFormatting prefix = ChatFormatting.GREEN;
		String sgn = "+";
		if (dmg < this.damage) {
			prefix = ChatFormatting.RED;
			sgn = "-";
		}
		String suffix = "";
		if (expanded) {
			if (mod.getDmgMul() != 1f) {
				suffix += " (" + sgn + String.format("%.0f", (mod.getDmgMul() - 1f) * 100f) + "%)";
			}
			if (mod.getDmgAdd() != 0f) {
				float add = mod.getDmgAdd();
				suffix += " (" + (add > 0 ? "+" : "") + String.format("%.1f", add) + ")";
			}
		}
		String sd = String.format("%.1f", dmg);
		String sm = String.format("%.1f", dmgmin);
		return Component.literal(sd + (dmgmin < dmg ? "-" + sm : "") + suffix).withStyle(prefix);
	}

	protected Component getTooltipTextRange(ItemStack stack) {
		DamageModifier mod = getCurrentDamageModifier(stack);
		int ttl = mod.getTTL(this.ticksToLive);
		float rangeStart = mod.getRange(this.damageDropStart);
		float rangeEnd = mod.getRange(this.damageDropEnd);

		ChatFormatting prefix = null;
		String suffix = "";
		if (rangeStart != this.damageDropStart) {
			String sgn = "+";
			if (rangeStart > this.damageDropStart) {
				prefix = ChatFormatting.GREEN;
			} else {
				prefix = ChatFormatting.RED;
				sgn = "-";
			}
			if (mod.getRangeMul() != 1f) {
				suffix += " (" + sgn + String.format("%.0f", (mod.getRangeMul() - 1f) * 100f) + "%)";
			}
			if (mod.getRangeAdd() != 0f) {
				float add = mod.getRangeAdd();
				suffix += " (" + (add > 0 ? "+" : "") + String.format("%.1f", add) + ")";
			}
		}
		String sStart = String.format("%.1f", rangeStart);
		String sEnd = String.format("%.1f", rangeEnd);

		MutableComponent label;
		String value;
		if (this.rangeTooltipType == RangeTooltipType.DROP) {
			label = TextUtil.transTG("gun.tooltip.range");
			value = sStart + "," + sEnd + "," + ttl + suffix;
		} else if (this.rangeTooltipType == RangeTooltipType.NO_DROP) {
			label = TextUtil.transTG("gun.tooltip.range");
			value = sStart + suffix;
		} else {
			label = TextUtil.transTG("gun.tooltip.radius");
			value = sStart + "-" + sEnd + suffix;
		}
		MutableComponent v = Component.literal(value);
		if (prefix != null) {
			v.withStyle(prefix);
		}
		return label.append(": ").append(v);
	}

	protected Component getTooltipTextVelocity(ItemStack stack) {
		DamageModifier mod = getCurrentDamageModifier(stack);
		float velocity = mod.getVelocity(this.speed);
		ChatFormatting prefix = null;
		String suffix = "";
		if (velocity != this.speed) {
			String sgn = "+";
			if (velocity >= this.speed) {
				prefix = ChatFormatting.GREEN;
			} else {
				prefix = ChatFormatting.RED;
				sgn = "";
			}
			if (mod.getVelocityMul() != 1f) {
				suffix += " (" + sgn + String.format("%.0f", (mod.getVelocityMul() - 1f) * 100f) + "%)";
			}
			if (mod.getVelocityAdd() != 0f) {
				float add = mod.getVelocityAdd();
				suffix += " (" + (add > 0 ? "+" : "") + String.format("%.1f", add) + ")";
			}
		}
		MutableComponent v = Component.literal(String.format("%.1f", velocity) + suffix);
		if (prefix != null) {
			v.withStyle(prefix);
		}
		return TextUtil.transTG("gun.tooltip.velocity").append(": ").append(v);
	}

	protected void addMiningTooltip(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag, boolean longTooltip) {
	}

	protected static boolean isShiftDown() {
		return FMLEnvironment.dist.isClient() && net.minecraft.client.gui.screens.Screen.hasShiftDown();
	}

	protected void addAmmoTooltip(ItemStack stack, List<Component> list) {
		for (ItemStack ammo : this.ammoType.getAmmo(this.getCurrentAmmoVariant(stack))) {
			if (ammo.isEmpty()) {
				continue;
			}
			list.add(TextUtil.transTG("gun.tooltip.ammo").append(": " + (this.ammoCount > 1 ? this.ammoCount + "x " : ""))
					.append(ammo.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, context, list, flag);
		String shotgunMult = this.shotgun ? "(x" + (this.bulletcount + 1) + ")" : "";
		if (isShiftDown()) {
			list.add(TextUtil.transTG("gun.tooltip.handtype").append(": ").append(this.getGunHandType().getDisplayName()));
			addAmmoTooltip(stack, list);
			this.addMiningTooltip(stack, context, list, flag, true);
			list.add(TextUtil.transTG("gun.tooltip.damageType").append(": ").append(this.getDamageType(stack).getDisplayName()));
			list.add(TextUtil.transTG("gun.tooltip.damage").append(shotgunMult + ": ").append(getTooltipTextDmg(stack, true)));
			list.add(getTooltipTextRange(stack));
			list.add(getTooltipTextVelocity(stack));
			list.add(TextUtil.transTG("gun.tooltip.spread")
					.append(": " + this.accuracy + (this.zoombonus != 1.0 ? (" Z:" + this.zoombonus * this.accuracy) : "")));
			list.add(TextUtil.transTG("gun.tooltip.clipsize").append(": " + this.clipsize));
			list.add(TextUtil.transTG("gun.tooltip.reloadTime").append(": " + this.reloadtime * 0.05f + "s"));
			if (this.penetration > 0.0f) {
				list.add(TextUtil.transTG("gun.tooltip.armorPen").append(": " + String.format("%.1f", this.penetration)));
			}
			if (this.canZoom) {
				list.add(TextUtil.transTG("gun.tooltip.zoom").append(":(")
						.append(TextUtil.transTG(this.toggleZoom ? "gun.tooltip.zoom.toogle" : "gun.tooltip.zoom.hold")).append(") ")
						.append(TextUtil.transTG("gun.tooltip.zoom.multiplier")).append(":" + this.zoomMult));
			}
		} else {
			addAmmoTooltip(stack, list);
			this.addMiningTooltip(stack, context, list, flag, false);
			list.add(TextUtil.transTG("gun.tooltip.damage").append(shotgunMult + ": ").append(getTooltipTextDmg(stack, false)));
			list.add(TextUtil.transTG("gun.tooltip.shift1").append(" ")
					.append(TextUtil.transTG("gun.tooltip.shift2").withStyle(ChatFormatting.GREEN)).append(" ")
					.append(TextUtil.transTG("gun.tooltip.shift3").withStyle(ChatFormatting.GRAY)));
		}
	}

	public DamageType getDamageType(ItemStack stack) {
		return this.projectile_selector.getFactoryForType(this.getCurrentAmmoVariantKey(stack)).getDamageType();
	}

	@Override
	public int getCamoCount() {
		return this.camoCount;
	}

	@Override
	public Component getCurrentCamoName(ItemStack item) {
		int camoID = this.getCurrentCamoIndex(item);
		if (camoID > 0) {
			return Component.translatable(this.getDescriptionId() + ".camoname." + camoID);
		}
		return TextUtil.transTG("item.defaultcamo");
	}

	// ------------------------------------------------------------------ melee (guns with shootWithLeftClick=false)

	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity targetEntity) {
		if (this.shootWithLeftClick) {
			return true;
		}
		if (player.level().isClientSide) {
			int time = ticksToMillis(this.recoiltime);
			ShooterValues.setRecoiltime(player, false, System.currentTimeMillis() + time, time, (byte) 0);
		}
		this.meleeAttack(stack, player, targetEntity);
		return true;
	}

	/** Condensed port of Player#attack using the gun's melee damage source. */
	protected void meleeAttack(ItemStack stack, Player player, Entity targetEntity) {
		if (!targetEntity.isAttackable() || !GenericProjectile.BULLET_TARGETS.test(targetEntity) || targetEntity.skipAttackInteraction(player)) {
			return;
		}
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			player.resetAttackStrengthTicker();
			return;
		}
		TGDamageSource src = getMeleeDamageSource(player, stack);
		float f = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float f1 = EnchantmentHelper.modifyDamage(serverLevel, stack, targetEntity, src, f) - f;
		float f2 = player.getAttackStrengthScale(0.5F);
		f *= 0.2F + f2 * f2 * 0.8F;
		f1 *= f2;
		player.resetAttackStrengthTicker();
		if (f <= 0.0F && f1 <= 0.0F) {
			return;
		}
		boolean strong = f2 > 0.9F;
		float knockback = (float) player.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
		knockback = EnchantmentHelper.modifyKnockback(serverLevel, stack, targetEntity, src, knockback);
		boolean sprintHit = false;
		if (player.isSprinting() && strong) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, player.getSoundSource(),
					1.0F, 1.0F);
			knockback++;
			sprintHit = true;
		}
		boolean crit = strong && player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() && !player.isInWater()
				&& !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && !player.isPassenger() && targetEntity instanceof LivingEntity
				&& !player.isSprinting();
		if (crit) {
			f *= 1.5F;
		}
		f += f1;

		boolean sweep = false;
		double walked = player.walkDist - player.walkDistO;
		if (strong && !crit && !sprintHit && player.onGround() && walked < (double) player.getSpeed()) {
			sweep = this.hasSwordSweep() && this.getAmmoLeft(stack) > 0;
		}

		float healthBefore = targetEntity instanceof LivingEntity le ? le.getHealth() : 0f;
		var oldMotion = targetEntity.getDeltaMovement();

		if (targetEntity.hurt(src, f)) {
			this.consumeAmmoOnMeleeHit(player, stack);
			if (knockback > 0) {
				float yaw = player.getYRot() * ((float) Math.PI / 180F);
				if (targetEntity instanceof LivingEntity le) {
					le.knockback(knockback * 0.5F, Mth.sin(yaw), -Mth.cos(yaw));
				} else {
					targetEntity.push(-Mth.sin(yaw) * knockback * 0.5F, 0.1D, Mth.cos(yaw) * knockback * 0.5F);
				}
				player.setDeltaMovement(player.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
				player.setSprinting(false);
			}

			if (sweep) {
				float f3 = 1.0F + (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * f;
				for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, targetEntity.getBoundingBox().inflate(1.0D, 0.25D, 1.0D))) {
					if (e != player && e != targetEntity && !player.isAlliedTo(e) && player.distanceToSqr(e) < 9.0D) {
						float yaw = player.getYRot() * ((float) Math.PI / 180F);
						e.knockback(0.4F, Mth.sin(yaw), -Mth.cos(yaw));
						if (e.hurt(getMeleeDamageSource(player, stack), f3)) {
							this.onMeleeHitTarget(stack, e);
						}
					}
				}
				this.doSweepAttackEffect(player);
			}

			this.onMeleeHitTarget(stack, targetEntity);

			if (targetEntity instanceof net.minecraft.server.level.ServerPlayer sp && targetEntity.hurtMarked) {
				sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(targetEntity));
				targetEntity.hurtMarked = false;
				targetEntity.setDeltaMovement(oldMotion);
			}

			if (crit) {
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, player.getSoundSource(), 1.0F,
						1.0F);
				player.crit(targetEntity);
			}
			if (!crit && !sweep) {
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
						strong ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK, player.getSoundSource(), 1.0F, 1.0F);
			}
			if (f1 > 0.0F) {
				player.magicCrit(targetEntity);
			}
			player.setLastHurtMob(targetEntity);
			EnchantmentHelper.doPostAttackEffects(serverLevel, targetEntity, src);

			if (targetEntity instanceof LivingEntity le) {
				float dealt = healthBefore - le.getHealth();
				player.awardStat(net.minecraft.stats.Stats.DAMAGE_DEALT, Math.round(dealt * 10.0F));
				if (dealt > 2.0F) {
					int k = (int) (dealt * 0.5D);
					serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, targetEntity.getX(),
							targetEntity.getY(0.5D), targetEntity.getZ(), k, 0.1D, 0.0D, 0.1D, 0.2D);
				}
			}
			player.causeFoodExhaustion(0.1F);
		} else {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, player.getSoundSource(), 1.0F,
					1.0F);
		}
	}

	protected void onMeleeHitTarget(ItemStack stack, Entity target) {
	}

	protected void consumeAmmoOnMeleeHit(LivingEntity elb, ItemStack stack) {
		if (elb instanceof Player ply && ply.getAbilities().instabuild) {
			return;
		}
		this.useAmmo(stack, 1);
	}

	protected void doSweepAttackEffect(Player player) {
		if (!player.level().isClientSide) {
			double d0 = -Mth.sin(player.getYRot() * 0.017453292F);
			double d1 = Mth.cos(player.getYRot() * 0.017453292F);
			double x = player.getX() + d0;
			double y = player.getY() + player.getBbHeight() * 0.8d;
			double z = player.getZ() + d1;
			this.spawnSweepParticle(player.level(), x, y, z, d0, 0, d1);
			this.playSweepSoundEffect(player);
		}
	}

	protected void playSweepSoundEffect(Player player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F,
				1.0F);
	}

	protected void spawnSweepParticle(Level w, double x, double y, double z, double motionX, double motionY, double motionZ) {
	}

	/** Damage source used for player melee attacks, only relevant if "shootWithLeftClick" is not set */
	protected TGDamageSource getMeleeDamageSource(Player player, ItemStack stack) {
		return TGDamageSource.causePlayerMeleeDamage(player, DamageType.PHYSICAL, DeathType.GORE);
	}

	protected boolean hasSwordSweep() {
		return true;
	}

	public int getMiningAmmoConsumption() {
		return miningAmmoConsumption;
	}

	// ------------------------------------------------------------------ misc

	public AmmoType getAmmoType() {
		return ammoType;
	}

	public float getAI_attackRange() {
		return AI_attackRange;
	}

	public int getAI_attackTime() {
		return AI_attackTime;
	}

	public int getAI_burstCount() {
		return AI_burstCount;
	}

	public int getAI_burstAttackTime() {
		return AI_burstAttackTime;
	}

	public boolean hasRightClickAction() {
		return this.getGunHandType() == GunHandType.TWO_HANDED && this.canZoom;
	}

	/** Get all ammo and magazines the gun currently holds */
	public List<ItemStack> getAmmoOnUnload(ItemStack stack) {
		List<ItemStack> items = new ArrayList<>();
		int ammo = this.getCurrentAmmo(stack);
		int variant = this.getCurrentAmmoVariant(stack);
		if (this.ammoCount > 1 && ammo > 0) {
			for (ItemStack s : this.getAmmoType().getBullet(variant)) {
				items.add(TGItems.newStack(s, ammo));
			}
		} else if (!this.isFullyLoaded(stack)) {
			int amount = this.ammoType.getEmptyMag().length;
			for (int i = 0; i < amount; i++) {
				int bulletsBack = (int) Math.floor(ammo / this.ammoType.getShotsPerBullet(clipsize, ammo));
				if (bulletsBack > 0) {
					items.add(TGItems.newStack(this.getAmmoType().getBullet(variant)[i], bulletsBack));
				}
				if (!this.ammoType.getEmptyMag()[i].isEmpty()) {
					items.add(TGItems.newStack(this.ammoType.getEmptyMag()[i], 1));
				}
			}
		} else {
			int amount = this.ammoType.getEmptyMag().length;
			for (int i = 0; i < amount; i++) {
				items.add(TGItems.newStack(this.ammoType.getAmmo(variant)[i], 1));
			}
		}
		return items;
	}

	/** try to force reload the gun, might lose some ammo */
	public void tryForcedReload(ItemStack item, Level level, Player player, InteractionHand hand) {
		TGExtendedPlayer extendedPlayer = TGExtendedPlayer.get(player);
		if (extendedPlayer.getFireDelay(hand) <= 0 && !this.isFullyLoaded(item)) {
			int oldAmmo = this.getCurrentAmmo(item);
			int variant = this.getCurrentAmmoVariant(item);
			if (InventoryUtil.consumeAmmoPlayer(player, this.getReloadItem(item))) {
				if (ammoCount <= 1) {
					this.useAmmo(item, oldAmmo);
				}
				int ammos = this.getAmmoType().getEmptyMag().length;
				for (int i = 0; i < ammos; i++) {
					if (!this.ammoType.getEmptyMag()[i].isEmpty()) {
						giveOrDrop(level, player, TGItems.newStack(this.ammoType.getEmptyMag()[i], 1));
						int bulletsBack = (int) Math.floor(oldAmmo / this.ammoType.getShotsPerBullet(clipsize, oldAmmo));
						if (bulletsBack > 0) {
							giveOrDrop(level, player, TGItems.newStack(this.ammoType.getBullet(variant)[i], bulletsBack));
						}
					}
				}

				if (level.isClientSide) {
					resetToggleZoom();
				}

				extendedPlayer.setFireDelay(hand, this.reloadtime - this.minFiretime);

				if (ammoCount > 1) {
					int i = 1;
					while (i < (ammoCount - oldAmmo) && InventoryUtil.consumeAmmoPlayer(player, this.ammoType.getAmmo(variant))) {
						i++;
					}
					this.reloadAmmo(item, i);
				} else {
					this.reloadAmmo(item);
				}

				startReloadEffects(level, player, hand, (byte) 0);
			}
		}
	}

	public int getClipsize() {
		return clipsize;
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
		return this.handType != GunHandType.TWO_HANDED;
	}

	public boolean setGunStat(EnumGunStat stat, float value) {
		switch (stat) {
			case DAMAGE:
				this.damage = value;
				return true;
			case DAMAGE_MIN:
				this.damageMin = value;
				return true;
			case DAMAGE_DROP_START:
				this.damageDropStart = value;
				return true;
			case DAMAGE_DROP_END:
				this.damageDropEnd = value;
				return true;
			case BULLET_SPEED:
				this.speed = value;
				return true;
			case BULLET_DISTANCE:
				this.ticksToLive = (int) value;
				return true;
			case GRAVITY:
				this.gravity = value;
				return true;
			case SPREAD:
				this.spread = value;
				return true;
			default:
				return false;
		}
	}

	public float getSpread() {
		return spread;
	}

	public GunHandType getHandType() {
		return handType;
	}

	public float getZoombonus() {
		return zoombonus;
	}

	public EnumCrosshairStyle getCrossHairStyle() {
		return this.crossHairStyle;
	}

	public int getMinFiretime() {
		return minFiretime;
	}

	public int getRecoiltime() {
		return recoiltime;
	}

	public int getReloadtime() {
		return reloadtime;
	}
}
