package com.cooptweaks;

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

	/** Discord bot configuration file. */
	public static final Path DISCORD_PATH = MAIN_PATH.resolve("discord.toml");

	/**
	 Verify that the necessary config files exist, if not, generate the default config files.
	 <p>
	 The config folder "cooptweaks" should contain:
	 <ul>
	 <li>saves</li>
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

		if (!Files.exists(DISCORD_PATH)) {
			try {
				String config = """
						token =
						application_id =
						channel_id =""";

				Main.LOGGER.info("Creating discord config file.");
				Files.writeString(DISCORD_PATH, config, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
