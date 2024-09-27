package com.cooptweaks.utils;

public class TimeSince {
	private final long past;

	public TimeSince(long past) {
		this.past = past;
	}

	private static final String AND = " and ";
	private static final String COMMA = ", ";

	/**
	 Returns a formatted string of time.

	 @return "2 days and 16 hours"
	 */
	public String toString() {
		long current = System.currentTimeMillis();
		long elapsed = current - past;

		long seconds = elapsed / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		seconds %= 60;
		minutes %= 60;
		hours %= 24;

		StringBuilder time = new StringBuilder();

		appendTimeUnit(time, formatTimeUnit(days, "day", "days"), true);
		appendTimeUnit(time, formatTimeUnit(hours, "hour", "hours"), days > 0 && (minutes > 0 || seconds > 0));
		appendTimeUnit(time, formatTimeUnit(minutes, "minute", "minutes"), (days > 0 || hours > 0) && seconds > 0);
		appendTimeUnit(time, formatTimeUnit(seconds, "second", "seconds"), false);

		// If no units were added, append 0 seconds
		if (time.isEmpty()) {
			time.append("0 seconds");
		}

		return time.toString();
	}

	/** Format a unit of time. Example: "2 days", "1 hour". */
	private String formatTimeUnit(long value, String singular, String plural) {
		if (value > 0) {
			String unit = value > 1 ? plural : singular;
			return value + " " + unit;
		}

		return "";
	}

	/** Add unit to the string with appropriate conjunctions. */
	private void appendTimeUnit(StringBuilder time, String unit, boolean addComma) {
		if (!unit.isEmpty()) {
			if (!time.isEmpty()) {
				time.append(addComma ? COMMA : AND);
			}

			time.append(unit);
		}
	}
}
