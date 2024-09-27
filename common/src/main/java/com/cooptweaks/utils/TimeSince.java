package com.cooptweaks.utils;

public class TimeSince {
	private final long past;

	public TimeSince(long past) {
		this.past = past;
	}

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

		if (days > 0) {
			time.append(days).append(" day").append(days > 1 ? "s" : "");
		}

		if (hours > 0) {
			if (!time.isEmpty()) {
				time.append(days > 0 && (minutes > 0 || seconds > 0) ? ", " : " and ");
			}

			time.append(hours).append(" hour").append(hours > 1 ? "s" : "");
		}

		if (minutes > 0) {
			if (!time.isEmpty()) {
				time.append((days > 0 || hours > 0) && seconds > 0 ? ", " : " and ");
			}

			time.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
		}

		if (seconds > 0 || time.isEmpty()) {
			if (!time.isEmpty()) {
				time.append(" and ");
			}

			time.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
		}

		return time.toString();
	}
}
