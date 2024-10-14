package com.cooptweaks.config;

import com.cooptweaks.types.ConfigMap;

public final class DiscordConfig {
	private static boolean ENABLED = false;

	private static String TOKEN = "";
	private static long APPLICATION_ID = 0;
	private static String CHANNEL_ID = "";

	private static boolean ON_JOIN = false;
	private static boolean ON_LEAVE = false;
	private static boolean ON_MESSAGE = false;
	private static boolean ON_DEATH = false;
	private static boolean ON_CHANGE_DIMENSION = false;
	private static boolean ON_ADVANCEMENT = false;

	private static boolean ON_SERVER_START = false;
	private static boolean ON_SERVER_STOP = false;

	private static boolean ON_DISCORD_MESSAGE = false;

	private static boolean STATUS_COMMAND = false;

	private static final String DEFAULT_CONFIG = """
			# Bot configuration, necessary for the bot to work.
			token =
			application_id =
			channel_id =
			\t
			# Enable sending player events to Discord.
			on_join = true
			on_leave = true
			on_message = true
			on_death = true
			on_change_dimension = true
			on_advancement = true
			\t
			# Enable sending server events to Discord.
			on_server_start = true
			on_server_stop = true
			\t
			# Enable sending Discord events to the server chat.
			on_discord_message = true
			\t
			# Enable Discord commands.
			status_command = true
			""";

	private DiscordConfig() {
	}

	public static String defaultConfig() {
		return DEFAULT_CONFIG;
	}

	public static void load(ConfigMap config) {
		String token = config.get("token").toString();
		String channel_id = config.get("channel_id").toString();
		long application_id = config.get("application_id").toLong();

		if (token.isEmpty() || channel_id.isEmpty() || application_id == 0) {
			return;
		}

		ENABLED = true;

		TOKEN = token;
		APPLICATION_ID = application_id;
		CHANNEL_ID = channel_id;

		// Player events
		ON_JOIN = config.get("on_join").toBoolean();
		ON_LEAVE = config.get("on_leave").toBoolean();
		ON_MESSAGE = config.get("on_message").toBoolean();
		ON_DEATH = config.get("on_death").toBoolean();
		ON_CHANGE_DIMENSION = config.get("on_change_dimension").toBoolean();
		ON_ADVANCEMENT = config.get("on_advancement").toBoolean();

		// Server events
		ON_SERVER_START = config.get("on_server_start").toBoolean();
		ON_SERVER_STOP = config.get("on_server_stop").toBoolean();

		// Discord events
		ON_DISCORD_MESSAGE = config.get("on_discord_message").toBoolean();

		// Commands
		STATUS_COMMAND = config.get("status_command").toBoolean();
	}

	public static boolean enabled() {
		return ENABLED;
	}

	public static String token() {
		return TOKEN;
	}

	public static long applicationId() {
		return APPLICATION_ID;
	}

	public static String channelId() {
		return CHANNEL_ID;
	}

	public static boolean onJoin() {
		return ON_JOIN;
	}

	public static boolean onLeave() {
		return ON_LEAVE;
	}

	public static boolean onMessage() {
		return ON_MESSAGE;
	}

	public static boolean onDeath() {
		return ON_DEATH;
	}

	public static boolean onChangeDimension() {
		return ON_CHANGE_DIMENSION;
	}

	public static boolean onAdvancement() {
		return ON_ADVANCEMENT;
	}

	public static boolean onServerStart() {
		return ON_SERVER_START;
	}

	public static boolean onServerStop() {
		return ON_SERVER_STOP;
	}

	public static boolean onDiscordMessage() {
		return ON_DISCORD_MESSAGE;
	}

	public static boolean statusCommand() {
		return STATUS_COMMAND;
	}
}
