package com.zyroofficial.chat;

import com.zyroofficial.data.TransactionType;
import com.zyroofficial.util.AmountParser;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * One user-defined chat detection rule.
 *
 * <p>The pattern is a Java regular expression with two optional placeholders:
 * <ul>
 *   <li><code>{amount}</code> - required; replaced by the amount pattern from the settings</li>
 *   <li><code>{player}</code> - optional; replaced by the player-name pattern from the settings</li>
 * </ul>
 * Matching uses "find", so the pattern may match part of a line; anchor with ^ and $ for exact lines.
 * No rules ship by default: the mod never guesses what a server's messages look like.
 */
public final class ChatRule {
	public String id = UUID.randomUUID().toString();
	public String name = "New rule";
	public boolean enabled = true;
	public TransactionType type = TransactionType.RECEIVED;
	public String pattern = "";
	public boolean ignoreCase = true;

	private transient Pattern compiled;
	private transient String compiledKey;
	private transient String compileError;

	/** What a successful match extracted. */
	public static final class Match {
		public final double amount;
		public final String player;

		Match(double amount, String player) {
			this.amount = amount;
			this.player = player;
		}
	}

	public void copyFrom(ChatRule other) {
		this.name = other.name;
		this.enabled = other.enabled;
		this.type = other.type;
		this.pattern = other.pattern;
		this.ignoreCase = other.ignoreCase;
	}

	public ChatRule copy() {
		ChatRule rule = new ChatRule();
		rule.id = this.id;
		rule.copyFrom(this);
		return rule;
	}

	/** Returns null if the rule is usable, otherwise a short human-readable problem. */
	public String validate() {
		compileIfNeeded();
		return compileError;
	}

	/** Tries to match a cleaned chat line. Returns null when there is no match or the rule is invalid. */
	public Match match(String text) {
		compileIfNeeded();
		if (compiled == null || text == null) {
			return null;
		}
		Matcher matcher = compiled.matcher(text);
		if (!matcher.find()) {
			return null;
		}
		double amount;
		try {
			amount = AmountParser.parse(matcher.group("amount"));
		} catch (IllegalArgumentException | IllegalStateException e) {
			return null;
		}
		if (Double.isNaN(amount)) {
			return null;
		}
		String player = "";
		try {
			String group = matcher.group("player");
			if (group != null) {
				player = group;
			}
		} catch (IllegalArgumentException ignored) {
			// The pattern has no {player} part.
		}
		return new Match(amount, player);
	}

	private void compileIfNeeded() {
		ChatDetectionConfig config = ChatDetectionConfig.get();
		String key = pattern + "\u0000" + ignoreCase + "\u0000" + config.playerPattern + "\u0000" + config.amountPattern;
		if (key.equals(compiledKey)) {
			return;
		}
		compiledKey = key;
		compiled = null;
		compileError = null;

		String raw = pattern == null ? "" : pattern;
		if (raw.isBlank()) {
			compileError = "Pattern is empty.";
			return;
		}
		if (raw.length() > 400) {
			compileError = "Pattern is too long (max 400 characters).";
			return;
		}
		if (!raw.contains("{amount}") && !raw.contains("(?<amount>")) {
			compileError = "Pattern must contain {amount}.";
			return;
		}

		String expanded = raw
				.replace("{player}", "(?<player>" + config.playerPattern + ")")
				.replace("{amount}", "(?<amount>" + config.amountPattern + ")");
		try {
			compiled = Pattern.compile(expanded, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
		} catch (PatternSyntaxException e) {
			String description = e.getDescription();
			compileError = "Invalid pattern: " + (description == null ? "syntax error" : description);
		}
	}
}
