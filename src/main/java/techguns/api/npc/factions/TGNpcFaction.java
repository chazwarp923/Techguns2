package techguns.api.npc.factions;

public enum TGNpcFaction {
	HOSTILE,
	TURRET,
	NEUTRAL;

	/** 1.12 TGNpcFactions hostility table: hostile NPCs fight turrets and neutrals, nobody fights their own faction */
	public static boolean isHostile(TGNpcFaction a, TGNpcFaction b) {
		if (a == b) {
			return false;
		}
		return a == HOSTILE || b == HOSTILE;
	}
}
