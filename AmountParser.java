package com.zyroofficial.util;

/**
 * Parses money amounts typed by the user or captured from chat.
 * Accepts: 1500, 1,500.50, $1500, 2.5K, 3M, 1.2B, 4T (case-insensitive suffix).
 * Rejects signs, exponents, NaN/infinity and anything absurdly large.
 */
public final class AmountParser {
	private static final double MAX_AMOUNT = 1e18;

	private AmountParser() {
	}

	/** Parses a strictly positive amount. Returns NaN if invalid or not above zero. */
	public static double parse(String text) {
		double value = parseAllowZero(text);
		return value > 0 ? value : Double.NaN;
	}

	/** Parses an amount that may be zero. Returns NaN if invalid. */
	public static double parseAllowZero(String text) {
		if (text == null) {
			return Double.NaN;
		}
		String s = text.trim().replace(",", "").replace("$", "").replace("_", "").replace(" ", "");
		if (s.isEmpty()) {
			return Double.NaN;
		}

		double multiplier = 1;
		char last = Character.toUpperCase(s.charAt(s.length() - 1));
		switch (last) {
			case 'K':
				multiplier = 1e3;
				break;
			case 'M':
				multiplier = 1e6;
				break;
			case 'B':
				multiplier = 1e9;
				break;
			case 'T':
				multiplier = 1e12;
				break;
			default:
				break;
		}
		if (multiplier != 1) {
			s = s.substring(0, s.length() - 1);
		}

		if (!s.matches("\\d+(\\.\\d+)?|\\.\\d+")) {
			return Double.NaN;
		}

		double value;
		try {
			value = Double.parseDouble(s) * multiplier;
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
		if (Double.isNaN(value) || Double.isInfinite(value) || value < 0 || value > MAX_AMOUNT) {
			return Double.NaN;
		}
		return value;
	}

	/** Formats a number for an edit box: no trailing zeros, no separators. */
	public static String plain(double value) {
		if (value == Math.floor(value) && Math.abs(value) < 1e15) {
			return Long.toString((long) value);
		}
		return Double.toString(value);
	}
}
