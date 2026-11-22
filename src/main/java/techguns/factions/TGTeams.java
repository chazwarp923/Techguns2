package techguns.factions;

import java.util.UUID;

import net.neoforged.fml.ModList;
import techguns.plugins.ftbteams.TeamSystemIntegration;

/**
 * Player team checks (the player part of 1.12 TGNpcFactions). With FTB Teams installed the machine security levels
 * 1 (allies) and 2 (team) use the owner's FTB team; without it they behave like level 0, as in 1.12.
 */
public final class TGTeams {

	public static final String FTB_TEAMS = "ftbteams";

	private static Boolean loaded;

	private TGTeams() {
	}

	public static boolean isTeamModLoaded() {
		if (loaded == null) {
			loaded = ModList.get().isLoaded(FTB_TEAMS);
		}
		return loaded;
	}

	/**
	 * Security: 0 anyone, 1 allies or team members, 2 team members, 3 owner only.
	 */
	public static boolean canAccess(UUID owner, UUID target, int security) {
		if (owner.equals(target)) {
			return true;
		}
		return switch (security) {
			case 1 -> isAlliedOrTeamMember(owner, target);
			case 2 -> isTeamMember(owner, target);
			case 3 -> false;
			default -> true;
		};
	}

	public static boolean isAlliedOrTeamMember(UUID owner, UUID target) {
		return !isTeamModLoaded() || TeamSystemIntegration.isAllied(owner, target);
	}

	public static boolean isTeamMember(UUID owner, UUID target) {
		return !isTeamModLoaded() || TeamSystemIntegration.isTeamMember(owner, target);
	}

	public static boolean isEnemy(UUID owner, UUID target) {
		return isTeamModLoaded() && TeamSystemIntegration.isEnemy(owner, target);
	}

	/**
	 * Turret pvp setting (1.12 TGNpcFactions.shouldAttack): 0 no pvp, 1 enemies, 2 everyone not allied, 3 everyone not in
	 * the team, 4 everyone but the owner.
	 */
	public static boolean shouldAttack(UUID owner, UUID target, int pvpsetting) {
		if (owner.equals(target)) {
			return false;
		}
		return switch (pvpsetting) {
			case 1 -> isEnemy(owner, target);
			case 2 -> !isAlliedOrTeamMember(owner, target);
			case 3 -> !isTeamMember(owner, target);
			case 4 -> true;
			default -> false;
		};
	}
}
