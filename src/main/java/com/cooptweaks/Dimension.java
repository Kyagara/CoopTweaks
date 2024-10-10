package com.cooptweaks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Dimension {
	private Dimension() {
	}

	/** Maps player names to their current dimension. */
	private static final ConcurrentHashMap<String, String> PLAYER_CURRENT_DIMENSION_ID = new ConcurrentHashMap<>();

	/** Maps dimension IDs to their names. */
	private static final HashMap<String, String> DIMENSIONS = new HashMap<>(Map.of(
			"minecraft:overworld", "Overworld",
			"minecraft:the_nether", "Nether",
			"minecraft:the_end", "End"
	));

	/**
	 Gets the dimension name of a player.

	 @param player The player's name.
	 @return The dimension name of the player. Example: "Overworld".
	 */
	public static String getPlayerDimension(String player) {
		String dimensionId = getPlayerCurrentDimension(player);
		String dimension = DIMENSIONS.get(dimensionId);

		if (dimension == null) {
			return dimensionId;
		}

		return dimension;
	}

	/** Gets the current dimension ID of a player. */
	public static String getPlayerCurrentDimension(String name) {
		return PLAYER_CURRENT_DIMENSION_ID.get(name);
	}

	/** Updates the current dimension ID of a player. */
	public static void updatePlayerCurrentDimension(String name, String dimensionId) {
		PLAYER_CURRENT_DIMENSION_ID.put(name, dimensionId);
	}
}
