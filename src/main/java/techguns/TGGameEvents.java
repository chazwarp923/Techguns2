package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Vibrations (sculk sensors, Wardens). Gunfire has its own event because vanilla's projectile_shoot is in
 * {@code #minecraft:ignore_vibrations_sneaking}: a sneaking archer is quiet, a sneaking gunner is not. The event is in
 * {@code #minecraft:vibrations} and {@code #minecraft:warden_can_listen}, with frequency 3 (like projectile_shoot) in
 * the {@code neoforge:vibration_frequencies} data map.
 * <p>
 * The rest uses vanilla events: reloads item_interact_finish (hidden by sneaking), explosions explode, bullet impacts
 * projectile_land and thrown grenades projectile_shoot.
 */
public final class TGGameEvents {

	public static final DeferredRegister<GameEvent> GAME_EVENTS = DeferredRegister.create(Registries.GAME_EVENT, Techguns.MODID);

	/** a gun firing, at the shooter; 16 blocks like vanilla events (sculk sensors still only listen 8 blocks) */
	public static final DeferredHolder<GameEvent, GameEvent> GUNFIRE = GAME_EVENTS.register("gunfire", () -> new GameEvent(16));

	private TGGameEvents() {
	}
}
