package com.cooptweaks.config;

import com.cooptweaks.types.ConfigMap;

public final class AdvancementsConfig {
	private static boolean ENABLED = false;

	private static boolean DISABLE_VANILLA_ANNOUNCEMENT = false;

	private static final String DEFAULT_CONFIG = """
			# Enables syncing of advancements.
			enabled = true

			# Prevents chat spamming when the syncing by disabling advancement announcement.
			disable_vanilla_announcement = true
			""";

	private AdvancementsConfig() {
	}

	public static String defaultConfig() {
		return DEFAULT_CONFIG;
	}

	public static void load(ConfigMap config) {
		ENABLED = config.get("enabled").toBoolean();

		DISABLE_VANILLA_ANNOUNCEMENT = config.get("disable_vanilla_announcement").toBoolean();
	}

	public static boolean enabled() {
		return ENABLED;
	}

	public static boolean disableVanillaAnnouncement() {
		return DISABLE_VANILLA_ANNOUNCEMENT;
	}
}
