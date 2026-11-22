package techguns.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Side-safe access to the client particle system from common code (1.12: Techguns.proxy.createFX...).
 * The client installs its implementation during client setup; on a dedicated server all calls are no-ops.
 * Callers are expected to only call this on the logical client (world.isClientSide).
 */
public final class TGFXHooks {

	public interface Handler {
		void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz);

		void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz, float pitch, float yaw);

		void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz, float scale);

		void createFXOnEntity(String name, Entity ent);

		void createFXOnEntity(String name, Entity ent, float scale);

		void createFXOnEntityWithOffset(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead, EntityCondition condition);

		boolean isClientPlayerAndIn1stPerson(Entity ent);
	}

	private static Handler handler = null;

	public static void setHandler(Handler h) {
		handler = h;
	}

	/** true if ent is the local player viewed in first person (client only) */
	public static boolean isClientPlayerAndIn1stPerson(Entity ent) {
		return handler != null && ent.level().isClientSide && handler.isClientPlayerAndIn1stPerson(ent);
	}

	public static void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz) {
		if (handler != null && world.isClientSide) handler.createFX(name, world, x, y, z, mx, my, mz);
	}

	public static void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz, float pitch, float yaw) {
		if (handler != null && world.isClientSide) handler.createFX(name, world, x, y, z, mx, my, mz, pitch, yaw);
	}

	public static void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz, float scale) {
		if (handler != null && world.isClientSide) handler.createFX(name, world, x, y, z, mx, my, mz, scale);
	}

	public static void createFXOnEntity(String name, Entity ent) {
		if (handler != null && ent.level().isClientSide) handler.createFXOnEntity(name, ent);
	}

	public static void createFXOnEntity(String name, Entity ent, float scale) {
		if (handler != null && ent.level().isClientSide) handler.createFXOnEntity(name, ent, scale);
	}

	public static void createFXOnEntityWithOffset(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead,
			EntityCondition condition) {
		if (handler != null && ent.level().isClientSide) handler.createFXOnEntityWithOffset(name, ent, offsetX, offsetY, offsetZ, attachToHead, condition);
	}
}
