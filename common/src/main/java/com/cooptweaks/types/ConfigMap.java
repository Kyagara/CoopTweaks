package com.cooptweaks.types;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/** A custom map for configuration files, which is a collection of key-value pairs where the key is a {@link String} and the value is a custom {@link Value} object. */
public class ConfigMap {
	/** Internal map of key-value pairs. */
	private static final HashMap<String, Value> fields = HashMap.newHashMap(3);

	public ConfigMap(Path file) {
		List<String> lines;

		try {
			lines = Files.readAllLines(file, StandardCharsets.UTF_8);
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
				Value value = new Value("");
				fields.put(key, value);
				continue;
			}

			Value value = new Value(parts[1].trim());
			fields.put(key, value);
		}
	}

	/** Get the {@link Value} of a key. */
	public Value get(String key) {
		if (!fields.containsKey(key)) {
			return new Value("");
		}

		return fields.get(key);
	}
}
