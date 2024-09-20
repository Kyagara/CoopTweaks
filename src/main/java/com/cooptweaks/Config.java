package com.cooptweaks;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    // Main config folder.
    public static final Path MAIN = FabricLoader.getInstance().getConfigDir().resolve("cooptweaks");
    // Folder where advancement progress per world(seed) is saved.
    public static final Path SAVES = MAIN.resolve("saves");
    // Discord bot config file.
    public static final Path DISCORD = MAIN.resolve("discord.toml");

    // Verify that the necessary config files exist.
    // Folder "cooptweaks" which should contain:
    // - 'saves' folder
    // - 'discord.toml'
    public static void Verify() {
        if (!Files.exists(MAIN)) {
            try {
                Files.createDirectory(MAIN);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (!Files.exists(SAVES)) {
            try {
                Files.createDirectory(SAVES);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (!Files.exists(DISCORD)) {
            try {
                String defaultConfig = "token = " + System.lineSeparator() + "channel_id = " + System.lineSeparator() + "guild_id = ";
                Files.writeString(DISCORD, defaultConfig, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Simple parser for toml config files.
    public static Map<String, String> Parse(Path path) {
        List<String> lines;

        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> config = new HashMap<>(lines.size());

        for (String line : lines) {
            if (line.contains("#") || line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("=", 2);
            String key = parts[0].trim();
            String value = parts[1].trim();
            config.put(key, value);
        }

        return config;
    }
}
