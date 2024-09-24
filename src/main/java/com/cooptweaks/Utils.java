package com.cooptweaks;

import com.cooptweaks.advancements.Advancements;

import java.util.HashMap;
import java.util.Map;

public class Utils {
	/** Maps dimension IDs to their names. */
	private static final HashMap<String, String> WORLDS = new HashMap<>(Map.of(
			"minecraft:overworld", "Overworld",
			"minecraft:the_nether", "Nether",
			"minecraft:the_end", "End"
	));

	/**
	 Gets the dimension name of a player.

	 @param player The player's name.
	 @return The dimension name of the player. Example: "Overworld".
	 */
	public static String GetPlayerDimension(String player) {
		String dimensionId = Main.PLAYER_CURRENT_DIMENSION_ID.get(player);
		String dimension = WORLDS.get(dimensionId);
		if (dimension == null) {
			return dimensionId;
		}
		return dimension;
	}

	/**
	 Gets the advancements progress of the server.

	 @return The advancements progress of the server. Example: "10/122".
	 */
	public static String GetAdvancementsProgress() {
		return String.format("%d/%d", Advancements.COMPLETED_ADVANCEMENTS.size(), Advancements.ALL_ADVANCEMENTS.size());
	}
}
