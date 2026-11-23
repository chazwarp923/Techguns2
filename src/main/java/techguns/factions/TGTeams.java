package techguns.factions;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import techguns.plugins.ftbteams.TeamSystemIntegration;

/**
 * Player team checks (the player part of 1.12 TGNpcFactions), used by machine security and turret PvP settings.
 * <p>
 * Two team systems count, whichever says yes:
 * <ul>
 * <li><b>Vanilla scoreboard teams</b> ({@code /team}), always available. Players on the same team are team members
 * (and so also allies). Players on two different teams are enemies. Vanilla has no separate "ally" rank.</li>
 * <li><b>FTB Teams</b>, when installed: its members, allies and enemies.</li>
 * </ul>
 * A player on no team, with no FTB team, is only their own team: a neutral to everyone else.
 */
public final class TGTeams {

	public static final String FTB_TEAMS = "ftbteams";

	private static Boolean loaded;

	/** Player UUID to scoreboard name. Game tests swap it, because the test server has no profile cache. */
	private static BiFunction<MinecraftServer, UUID, String> nameLookup = TGTeams::playerName;

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
		return owner.equals(target) || sameVanillaTeam(owner, target) || (isTeamModLoaded() && TeamSystemIntegration.isAllied(owner, target));
	}

	public static boolean isTeamMember(UUID owner, UUID target) {
		return owner.equals(target) || sameVanillaTeam(owner, target) || (isTeamModLoaded() && TeamSystemIntegration.isTeamMember(owner, target));
	}

	public static boolean isEnemy(UUID owner, UUID target) {
		if (owner.equals(target)) {
			return false;
		}
		if (isTeamModLoaded() && TeamSystemIntegration.isEnemy(owner, target)) {
			return true;
		}
		// on two different vanilla teams, unless FTB Teams makes them allies
		PlayerTeam a = vanillaTeam(owner), b = vanillaTeam(target);
		return a != null && b != null && a != b && !isAlliedOrTeamMember(owner, target);
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

	/* ---- vanilla scoreboard teams ---- */

	private static boolean sameVanillaTeam(UUID owner, UUID target) {
		PlayerTeam a = vanillaTeam(owner);
		return a != null && a == vanillaTeam(target);
	}

	/** The /team of a player, online or offline; null without a running server, a known name, or a team. */
	@Nullable
	public static PlayerTeam vanillaTeam(UUID player) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return null;
		}
		String name = nameLookup.apply(server, player);
		return name == null ? null : server.getScoreboard().getPlayersTeam(name);
	}

	/** Name lookup used for teams (swappable in game tests). */
	@Nullable
	public static String lookupName(MinecraftServer server, UUID player) {
		return nameLookup.apply(server, player);
	}

	/** For game tests: replaces the UUID to scoreboard name lookup and returns the previous one. */
	public static BiFunction<MinecraftServer, UUID, String> setNameLookupForTests(BiFunction<MinecraftServer, UUID, String> lookup) {
		BiFunction<MinecraftServer, UUID, String> old = nameLookup;
		nameLookup = lookup;
		return old;
	}

	/** Name of a player, online or offline (profile cache); null if the server has never seen them. */
	@Nullable
	public static String playerName(MinecraftServer server, UUID player) {
		ServerPlayer online = server.getPlayerList().getPlayer(player);
		if (online != null) {
			return online.getScoreboardName();
		}
		var cache = server.getProfileCache();
		if (cache == null) {
			return null;
		}
		Optional<GameProfile> profile = cache.get(player);
		return profile.map(GameProfile::getName).orElse(null);
	}
}
