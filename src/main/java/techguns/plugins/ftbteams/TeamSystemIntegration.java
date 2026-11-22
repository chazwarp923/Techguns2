package techguns.plugins.ftbteams;

import java.util.UUID;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;

/**
 * FTB Teams integration (1.12 plugins.ftbl.TeamSystemIntegration). Only loaded when FTB Teams is installed, go
 * through {@link techguns.factions.TGTeams}.
 */
public final class TeamSystemIntegration {

	private TeamSystemIntegration() {
	}

	/** rank of target in the current team of owner, NONE if unknown */
	private static TeamRank rank(UUID owner, UUID target) {
		if (!FTBTeamsAPI.api().isManagerLoaded()) {
			return TeamRank.NONE;
		}
		return FTBTeamsAPI.api().getManager().getTeamForPlayerID(owner).map((Team t) -> t.getRankForPlayer(target)).orElse(TeamRank.NONE);
	}

	/** member or ally of owner's team */
	public static boolean isAllied(UUID owner, UUID target) {
		return owner.equals(target) || rank(owner, target).isAtLeast(TeamRank.ALLY);
	}

	public static boolean isAlliedNoMember(UUID owner, UUID target) {
		return rank(owner, target) == TeamRank.ALLY;
	}

	public static boolean isTeamMember(UUID owner, UUID target) {
		return owner.equals(target) || rank(owner, target).isAtLeast(TeamRank.MEMBER);
	}

	public static boolean isEnemy(UUID owner, UUID target) {
		return !owner.equals(target) && rank(owner, target).isEnemyOrWorse();
	}
}
