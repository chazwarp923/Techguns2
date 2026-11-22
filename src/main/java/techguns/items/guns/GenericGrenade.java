package techguns.items.guns;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import techguns.api.damagesystem.DamageType;
import techguns.client.audio.TGSoundCategory;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GrenadeProjectile;
import techguns.items.GenericItem;
import techguns.util.SoundUtil;
import techguns.util.TextUtil;

/** Hand grenade: hold right mouse to wind up (longer = farther, flatter throw), release to throw. */
public class GenericGrenade extends GenericItem {

	private final int maxUseDur;
	public float damage = 20.0f;
	public float radius = 3.0f;
	public int maxBounces = 3;
	public float damageMin = 10.0f;
	public float radiusMin = 5.0f;
	public float fullChargeTime = 30.0f;
	public int ticksToLive = 200;
	public float speed = 0.75f;
	public float spread = 0.1f;
	public float penetration = 0f;
	protected Supplier<SoundEvent> startSound = null;

	protected final IGrenadeProjectileFactory<? extends GrenadeProjectile> projectile_factory;

	public GenericGrenade(int stacksize, int maxUseDur, IGrenadeProjectileFactory<? extends GrenadeProjectile> projectile_factory) {
		super(new Properties().stacksTo(stacksize));
		this.maxUseDur = maxUseDur;
		this.projectile_factory = projectile_factory;
	}

	public GenericGrenade setStartSound(Supplier<SoundEvent> sound) {
		this.startSound = sound;
		return this;
	}

	public GenericGrenade setDamageAndRadius(float damage, float radiusMax, float damageMin, float radiusMin) {
		this.damage = damage;
		this.radius = radiusMax;
		this.damageMin = damageMin;
		this.radiusMin = radiusMin;
		return this;
	}

	public GenericGrenade setMaxBounces(int maxBounces) {
		this.maxBounces = maxBounces;
		return this;
	}

	public GenericGrenade setFullChargeTime(float fullChargeTime) {
		this.fullChargeTime = fullChargeTime;
		return this;
	}

	public GenericGrenade setTicksToLive(int ticksToLive) {
		this.ticksToLive = ticksToLive;
		return this;
	}

	public GenericGrenade setSpeed(float speed) {
		this.speed = speed;
		return this;
	}

	public GenericGrenade setSpread(float spread) {
		this.spread = spread;
		return this;
	}

	public GenericGrenade setPenetration(float penetration) {
		this.penetration = penetration;
		return this;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		if (!level.isClientSide && this.startSound != null) {
			SoundUtil.playSoundOnEntityGunPosition(level, player, this.startSound.get(), 1.0f, 1.0f, false, false, true, TGSoundCategory.PLAYER_EFFECT);
		}
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		int j = this.getUseDuration(stack, entity) - timeLeft;
		boolean leftHand = entity.getUsedItemHand() == InteractionHand.OFF_HAND;
		if (entity.getMainArm() == HumanoidArm.LEFT) {
			leftHand = !leftHand;
		}
		EnumBulletFirePos firePos = leftHand ? EnumBulletFirePos.LEFT : EnumBulletFirePos.RIGHT;
		float f = Math.min(1.0f, j / this.fullChargeTime);
		// a released-at-once grenade would get infinite gravity (0.015/charge)
		f = Math.max(f, 0.05f);

		if (!level.isClientSide) {
			spawnGrenade(level, entity, f, firePos);
		}
		if (!(entity instanceof Player ply && ply.getAbilities().instabuild)) {
			stack.shrink(1);
		}
		entity.swing(entity.getUsedItemHand());
	}

	public void spawnGrenade(Level level, LivingEntity entity, float charge, EnumBulletFirePos firePos) {
		float grav = 0.015f / charge;
		GrenadeProjectile proj = this.projectile_factory.createProjectile(level, entity, this.damage, speed, this.ticksToLive, spread, this.radius,
				this.radiusMin, this.damageMin, penetration, GenericGun.getDoBlockDamage(entity), firePos, radiusMin, grav, charge, maxBounces);
		level.addFreshEntity(proj);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return maxUseDur;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.NONE;
	}

	protected DamageType getDamageType() {
		return DamageType.EXPLOSION;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		if (FMLEnvironment.dist.isClient() && net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
			tooltip.add(TextUtil.transTG("gun.tooltip.damageType").append(": ").append(this.getDamageType().getDisplayName()));
			tooltip.add(TextUtil.transTG("gun.tooltip.damage").append(": " + this.damage));
			tooltip.add(TextUtil.transTG("gun.tooltip.radius").append(": " + this.radius));
			tooltip.add(TextUtil.transTG("gun.tooltip.bounces").append(": " + this.maxBounces));
		} else {
			tooltip.add(TextUtil.transTG("gun.tooltip.damage").append(": " + this.damage));
			tooltip.add(TextUtil.transTG("gun.tooltip.shift1").append(" ").append(TextUtil.transTG("gun.tooltip.shift2").withStyle(ChatFormatting.GREEN))
					.append(" ").append(TextUtil.transTG("gun.tooltip.shift3").withStyle(ChatFormatting.GRAY)));
		}
	}
}
