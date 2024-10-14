package com.cooptweaks.types;

/** A value in the custom configuration map, internally a string. */
public record Value(String value) {
	/** Return the internal string. */
	public String toString() {
		return value;
	}

	/** Convert the {@link Value} to a long. */
	public long toLong() {
		if (value.isEmpty()) {
			return 0L;
		}

		return Long.parseLong(value);
	}

	/** Convert the {@link Value} to a boolean. */
	public boolean toBoolean() {
		if (value.isEmpty()) {
			return false;
		}

		return Boolean.parseBoolean(value);
	}
}
