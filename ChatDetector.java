package com.zyroofficial.chat;

import com.zyroofficial.data.Transaction;
import com.zyroofficial.data.TransactionSource;
import com.zyroofficial.data.TransactionStore;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Turns incoming system messages into transactions using the user's rules.
 *
 * <p>Only server system messages are inspected. Player chat is deliberately ignored, because any
 * player could otherwise type a fake "payment" line and corrupt your statistics.
 */
public final class ChatDetector {
	private static final int MAX_LINE_LENGTH = 400;
	private static final Map<String, Long> RECENT_LINES = new HashMap<>();

	private ChatDetector() {
	}

	/** Registered with ClientReceiveMessageEvents.GAME. */
	public static void onGameMessage(Component message, boolean overlay) {
		ChatDetectionConfig config = ChatDetectionConfig.get();
		if (overlay && config.ignoreOverlay) {
			return;
		}

		String text = clean(message.getString());
		if (text.isEmpty()) {
			return;
		}

		if (config.captureRecent) {
			RecentMessages.add(text);
		}
		if (!config.enabled || text.length() > MAX_LINE_LENGTH) {
			return;
		}

		for (ChatRule rule : config.rules) {
			if (!rule.enabled || !config.typeEnabled(rule.type)) {
				continue;
			}
			ChatRule.Match match = rule.match(text);
			if (match == null) {
				continue;
			}
			if (isDuplicate(text, config)) {
				return;
			}
			TransactionStore.get().add(new Transaction(
					rule.type, TransactionSource.CHAT, match.amount, match.player, rule.name));
			return; // first matching rule wins
		}
	}

	/** Strips formatting codes and surrounding whitespace so rules see plain text. */
	public static String clean(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replaceAll("§.", "").strip();
	}

	private static synchronized boolean isDuplicate(String text, ChatDetectionConfig config) {
		if (!config.duplicateFilterEnabled || config.duplicateWindowMs <= 0) {
			return false;
		}
		long now = System.currentTimeMillis();

		Iterator<Map.Entry<String, Long>> iterator = RECENT_LINES.entrySet().iterator();
		while (iterator.hasNext()) {
			if (now - iterator.next().getValue() > config.duplicateWindowMs) {
				iterator.remove();
			}
		}

		Long previous = RECENT_LINES.get(text);
		if (previous != null) {
			return true;
		}
		RECENT_LINES.put(text, now);
		return false;
	}
}
