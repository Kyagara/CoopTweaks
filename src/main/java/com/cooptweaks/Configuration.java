package com.cooptweaks;

import com.cooptweaks.types.ConfigMap;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class Configuration {
	/** Main config folder path. */
	private static final Path MAIN_PATH = FabricLoader.getInstance().getConfigDir().resolve("cooptweaks");

	/** Folder where advancement progress per world(seed) is saved. */
	private static final Path ADVANCEMENTS_SAVE_PATH = MAIN_PATH.resolve("saves");

	private static final Path DISCORD_PATH = MAIN_PATH.resolve("discord.toml");

	private static ConfigMap DISCORD_CONFIG = null;

	/**
	 Verify that the necessary config files exist, if not, create them, generating the default config files.
	 <p>
	 The config folder "cooptweaks" which should contain:
	 <ul>
	 <li>saves</li>
	 <li>discord.toml</li>
	 </ul>
	 */
	public Configuration() {
		if (!Files.exists(MAIN_PATH)) {
			try {
				Files.createDirectory(MAIN_PATH);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (!Files.exists(ADVANCEMENTS_SAVE_PATH)) {
			try {
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
				Files.writeString(DISCORD_PATH, config, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public Path getAdvancementsSavePath() {
		return ADVANCEMENTS_SAVE_PATH;
	}

	public ConfigMap getDiscordConfig() {
		if (DISCORD_CONFIG == null) {
			DISCORD_CONFIG = new ConfigMap(DISCORD_PATH);
		}

		return DISCORD_CONFIG;
	}
}
