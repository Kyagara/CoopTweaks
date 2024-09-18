package com.cooptweaks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Simple parser for toml config files.
public class Config {
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
