package com.cooptweaks.types;

/** A value in the custom configuration map, internally a string. */
public record Value(String value) {
	/** Return the internal string. */
	public String toString() {
		return value;
	}

	/** Convert the value to a long. */
	public Long toLong() {
		if (value.isEmpty()) {
			return 0L;
		}

		return Long.parseLong(value);
	}
}
