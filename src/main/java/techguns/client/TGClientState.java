package techguns.client;

/**
 * Plain client state that common code (items) may touch on the logical client
 * (was part of ClientProxy). Contains no client-only types so it is safe to load anywhere.
 */
public class TGClientState {
	public static float playerZoom = 1.0f;

	public static float muzzleFlashJitterX = 0f;
	public static float muzzleFlashJitterY = 0f;
	public static float muzzleFlashJitterScale = 0f;
	public static float muzzleFlashJitterAngle = 0f;
}
