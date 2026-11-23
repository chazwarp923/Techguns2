package techguns.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;
import techguns.damagesystem.TGDamageSource;
import techguns.deatheffects.DeathType;
import techguns.factions.TGTeams;

/**
 * Vanilla /team teams count for machine security and turret PvP settings, and Techguns damage honours a team's
 * friendly-fire setting.
 */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class TeamTests {

	private record P(UUID id, String name) {
	}

	private static P profile(Map<UUID, String> known, String name) {
		P p = new P(UUID.randomUUID(), name);
		known.put(p.id, p.name);
		return p;
	}

	@GameTest(template = "empty")
	public static void vanillaTeamsCount(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		// the test server has no profile cache, so the players are looked up here (online players and the
		// profile cache are what a real server uses)
		Map<UUID, String> known = new HashMap<>();
		var oldLookup = TGTeams.setNameLookupForTests((s, id) -> known.get(id));
		Scoreboard sb = server.getScoreboard();
		String suffix = Long.toHexString(System.nanoTime() & 0xffffff);
		PlayerTeam red = sb.addPlayerTeam("tg_red_" + suffix);
		PlayerTeam blue = sb.addPlayerTeam("tg_blue_" + suffix);
		List<String> problems = new ArrayList<>();
		try {
			P owner = profile(known, "tgOwner" + suffix);
			P mate = profile(known, "tgMate" + suffix);
			P rival = profile(known, "tgRival" + suffix);
			P loner = profile(known, "tgLoner" + suffix);
			sb.addPlayerToTeam(owner.name, red);
			sb.addPlayerToTeam(mate.name, red);
			sb.addPlayerToTeam(rival.name, blue);

			check(problems, "owner's team found", TGTeams.vanillaTeam(owner.id) == red);
			// teammate: member and ally, never shot below "everyone except owner"
			check(problems, "mate is team member", TGTeams.isTeamMember(owner.id, mate.id));
			check(problems, "mate is ally", TGTeams.isAlliedOrTeamMember(owner.id, mate.id));
			check(problems, "mate can use team-security machine", TGTeams.canAccess(owner.id, mate.id, 2));
			check(problems, "mate not attacked by pvp 1-3", !TGTeams.shouldAttack(owner.id, mate.id, 1)
					&& !TGTeams.shouldAttack(owner.id, mate.id, 2) && !TGTeams.shouldAttack(owner.id, mate.id, 3));
			check(problems, "mate attacked by pvp 4", TGTeams.shouldAttack(owner.id, mate.id, 4));
			// other team: enemy
			check(problems, "rival is enemy", TGTeams.isEnemy(owner.id, rival.id));
			check(problems, "rival attacked by pvp 1", TGTeams.shouldAttack(owner.id, rival.id, 1));
			check(problems, "rival can't use allies-security machine", !TGTeams.canAccess(owner.id, rival.id, 1));
			// no team: neutral
			check(problems, "loner is not enemy", !TGTeams.isEnemy(owner.id, loner.id));
			check(problems, "loner not attacked by pvp 1", !TGTeams.shouldAttack(owner.id, loner.id, 1));
			check(problems, "loner attacked by pvp 2", TGTeams.shouldAttack(owner.id, loner.id, 2));
			check(problems, "loner can't use team-security machine", !TGTeams.canAccess(owner.id, loner.id, 2));
			check(problems, "anyone can use public machine", TGTeams.canAccess(owner.id, loner.id, 0));
			// an owner with no team has no team members
			check(problems, "no team, no members", !TGTeams.isTeamMember(loner.id, owner.id));
			// unknown player: no team, no crash
			check(problems, "unknown player has no team", TGTeams.vanillaTeam(UUID.randomUUID()) == null);
		} finally {
			TGTeams.setNameLookupForTests(oldLookup);
			sb.removePlayerTeam(red);
			sb.removePlayerTeam(blue);
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** Techguns damage names the shooting player as attacker, so vanilla's friendly-fire rule applies to it. */
	@GameTest(template = "empty")
	public static void gunDamageHonoursFriendlyFire(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		Scoreboard sb = server.getScoreboard();
		String suffix = Long.toHexString(System.nanoTime() & 0xffffff);
		ServerPlayer shooter = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "tgShooter" + suffix), ClientInformation.createDefault());
		ServerPlayer mate = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "tgMate" + suffix), ClientInformation.createDefault());
		ServerPlayer other = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "tgOther" + suffix), ClientInformation.createDefault());
		PlayerTeam team = sb.addPlayerTeam("tg_ff_" + suffix);
		boolean pvp = server.isPvpAllowed();
		List<String> problems = new ArrayList<>();
		try {
			server.setPvpAllowed(true);
			team.setAllowFriendlyFire(false);
			sb.addPlayerToTeam(shooter.getScoreboardName(), team);
			sb.addPlayerToTeam(mate.getScoreboardName(), team);
			for (TGDamageSource src : List.of(TGDamageSource.causeBulletDamage(shooter, shooter, DeathType.DEFAULT),
					TGDamageSource.causeExplosionDamage(shooter, shooter, DeathType.DEFAULT),
					TGDamageSource.causeEnergyDamage(shooter, shooter, DeathType.DEFAULT))) {
				check(problems, src.getMsgId() + " attacker is the shooter", src.getEntity() == shooter);
			}
			// what ServerPlayer.hurt checks for an attacking player
			check(problems, "teammate protected with friendly fire off", !mate.canHarmPlayer(shooter));
			check(problems, "other player not protected", other.canHarmPlayer(shooter));
			team.setAllowFriendlyFire(true);
			check(problems, "teammate hittable with friendly fire on", mate.canHarmPlayer(shooter));
		} finally {
			server.setPvpAllowed(pvp);
			sb.removePlayerTeam(team);
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	private static void check(List<String> problems, String what, boolean ok) {
		if (!ok) {
			problems.add(what);
		}
	}
}
