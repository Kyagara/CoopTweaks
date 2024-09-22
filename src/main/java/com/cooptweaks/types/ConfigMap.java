package com.cooptweaks.types;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/** A custom map for configuration files, which is a collection of key-value pairs where the key is a {@link String} and the value is a custom {@link Value} object. */
public class ConfigMap {
	/** Internal map of key-value pairs. */
	private static final HashMap<String, Value> fields = new HashMap<>(5);

	public ConfigMap(Path file) {
		List<String> lines;

		try {
			lines = Files.readAllLines(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (String line : lines) {
			if (line.contains("#") || line.isEmpty()) {
				continue;
			}

			String[] parts = line.split("=", 2);
			String key = parts[0].trim();

			if (parts.length == 1) {
				Value value = new Value(key);
				fields.put(key, value);
				continue;
			}

			Value value = new Value(parts[1].trim());
			fields.put(key, value);
		}
	}

	/** Get the {@link Value} of a key. */
	public Value get(String key) {
		return fields.get(key);
	}
}
