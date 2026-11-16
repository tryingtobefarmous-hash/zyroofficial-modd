package com.zyroofficial.chat;

import com.zyroofficial.ZyroofficialClient;
import com.zyroofficial.config.ZyroConfig;
import com.zyroofficial.data.TransactionType;
import com.zyroofficial.util.JsonFiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Settings for automatic chat detection, saved to config/zyroofficial/chat-detection.json.
 * The rules list is empty by default: add rules for your server's real messages in the menu.
 */
public final class ChatDetectionConfig {
	public static final String DEFAULT_PLAYER_PATTERN = "[A-Za-z0-9_]{3,16}";
	public static final String DEFAULT_AMOUNT_PATTERN = "\\$?\\s?[0-9][0-9,]*(?:\\.[0-9]+)?\\s?[KkMmBbTt]?";

	private static ChatDetectionConfig instance;

	/** Master switch. */
	public boolean enabled = true;

	/** Per-type switches: a rule whose type is switched off is skipped. */
	public boolean detectReceived = true;
	public boolean detectSent = true;
	public boolean detectWon = true;
	public boolean detectLost = true;
	public boolean detectTax = true;

	/** Ignore identical messages that arrive again within the window. */
	public boolean duplicateFilterEnabled = true;
	public int duplicateWindowMs = 1500;

	/** Ignore action-bar (overlay) messages. */
	public boolean ignoreOverlay = true;

	/** Keep the last 100 system messages in memory (never saved) so rules can be built from real lines. */
	public boolean captureRecent = true;

	/** Regex substituted for {player} in rules. */
	public String playerPattern = DEFAULT_PLAYER_PATTERN;
	/** Regex substituted for {amount} in rules. */
	public String amountPattern = DEFAULT_AMOUNT_PATTERN;

	public List<ChatRule> rules = new ArrayList<>();

	public static ChatDetectionConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	private static Path file() {
		return ZyroConfig.directory().resolve("chat-detection.json");
	}

	public static void load() {
		ChatDetectionConfig loaded = JsonFiles.read(file(), ChatDetectionConfig.class);
		instance = loaded != null ? loaded : new ChatDetectionConfig();
		instance.sanitize();
	}

	public static void save() {
		try {
			JsonFiles.write(file(), get());
		} catch (IOException e) {
			ZyroofficialClient.LOGGER.error("Could not save Zyroofficial chat detection settings", e);
		}
	}

	public boolean typeEnabled(TransactionType type) {
		switch (type) {
			case RECEIVED:
				return detectReceived;
			case SENT:
				return detectSent;
			case WON:
				return detectWon;
			case LOST:
				return detectLost;
			case TAX:
				return detectTax;
			default:
				return false;
		}
	}

	public void setTypeEnabled(TransactionType type, boolean value) {
		switch (type) {
			case RECEIVED:
				detectReceived = value;
				break;
			case SENT:
				detectSent = value;
				break;
			case WON:
				detectWon = value;
				break;
			case LOST:
				detectLost = value;
				break;
			case TAX:
				detectTax = value;
				break;
			default:
				break;
		}
	}

	/** True if the text is a valid regular expression. */
	public static boolean isValidRegex(String regex) {
		if (regex == null || regex.isBlank() || regex.length() > 200) {
			return false;
		}
		try {
			Pattern.compile(regex);
			return true;
		} catch (PatternSyntaxException e) {
			return false;
		}
	}

	private void sanitize() {
		if (rules == null) {
			rules = new ArrayList<>();
		}
		rules.removeIf(rule -> rule == null);
		for (ChatRule rule : rules) {
			if (rule.id == null) {
				rule.id = java.util.UUID.randomUUID().toString();
			}
			if (rule.name == null) {
				rule.name = "Rule";
			}
			if (rule.pattern == null) {
				rule.pattern = "";
			}
			if (rule.type == null) {
				rule.type = TransactionType.RECEIVED;
			}
		}
		if (!isValidRegex(playerPattern)) {
			playerPattern = DEFAULT_PLAYER_PATTERN;
		}
		if (!isValidRegex(amountPattern)) {
			amountPattern = DEFAULT_AMOUNT_PATTERN;
		}
		duplicateWindowMs = Math.max(0, Math.min(60_000, duplicateWindowMs));
	}
}
