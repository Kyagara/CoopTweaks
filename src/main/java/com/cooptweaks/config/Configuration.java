package com.cooptweaks.config;

import com.cooptweaks.Main;
import com.cooptweaks.types.ConfigMap;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class Configuration {
	private Configuration() {
	}

	/** Main config folder path. */
	private static final Path MAIN_PATH = Platform.getConfigFolder().resolve("cooptweaks");

	/** Folder where advancement progress per world(seed) is saved. */
	public static final Path ADVANCEMENTS_SAVE_PATH = MAIN_PATH.resolve("saves");

	/** Configuration file for the advancement module. */
	private static final Path ADVANCEMENTS_PATH = MAIN_PATH.resolve("advancements.toml");

	/** Discord bot configuration file. */
	private static final Path DISCORD_PATH = MAIN_PATH.resolve("discord.toml");

	/**
	 Verify that the necessary config files exist, if not, generate the default config files.
	 <p>
	 The config folder "cooptweaks" should contain:
	 <ul>
	 <li>saves</li>
	 <li>advancements.toml</li>
	 <li>discord.toml</li>
	 </ul>
	 */
	public static void verify() {
		if (!Files.exists(MAIN_PATH)) {
			try {
				Main.LOGGER.info("Creating config folder.");
				Files.createDirectory(MAIN_PATH);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (!Files.exists(ADVANCEMENTS_SAVE_PATH)) {
			try {
				Main.LOGGER.info("Creating advancements save folder.");
				Files.createDirectory(ADVANCEMENTS_SAVE_PATH);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (!Files.exists(ADVANCEMENTS_PATH)) {
			try {
				Main.LOGGER.info("Creating advancements config file.");
				Files.writeString(ADVANCEMENTS_PATH, AdvancementsConfig.defaultConfig(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (!Files.exists(DISCORD_PATH)) {
			try {
				Main.LOGGER.info("Creating discord config file.");
				Files.writeString(DISCORD_PATH, DiscordConfig.defaultConfig(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/** Parse the configuration files. */
	public static void load() {
		DiscordConfig.load(new ConfigMap(Configuration.DISCORD_PATH));
		AdvancementsConfig.load(new ConfigMap(Configuration.ADVANCEMENTS_PATH));
	}
}
