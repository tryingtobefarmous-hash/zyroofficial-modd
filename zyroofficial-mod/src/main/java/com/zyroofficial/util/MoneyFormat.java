package com.zyroofficial.util;

import java.util.Locale;

/** Formats money values for the HUD and menus. */
public final class MoneyFormat {
	private MoneyFormat() {
	}

	/** Full precision with thousands separators, e.g. $1,234,567.50 or -$20.00 */
	public static String full(double amount) {
		String sign = amount < 0 ? "-" : "";
		return sign + "$" + String.format(Locale.US, "%,.2f", Math.abs(amount));
	}

	/** Compact form for tight spaces, e.g. $1.25K, $3.40M, -$2.10B */
	public static String compact(double amount) {
		double abs = Math.abs(amount);
		String sign = amount < 0 ? "-" : "";
		if (abs >= 1_000_000_000_000D) {
			return sign + "$" + trim(abs / 1_000_000_000_000D) + "T";
		}
		if (abs >= 1_000_000_000D) {
			return sign + "$" + trim(abs / 1_000_000_000D) + "B";
		}
		if (abs >= 1_000_000D) {
			return sign + "$" + trim(abs / 1_000_000D) + "M";
		}
		if (abs >= 1_000D) {
			return sign + "$" + trim(abs / 1_000D) + "K";
		}
		return sign + "$" + trim(abs);
	}

	private static String trim(double value) {
		String text = String.format(Locale.US, "%.2f", value);
		if (text.contains(".")) {
			text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return text;
	}
}
