package com.zyroofficial.chat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory buffer of the most recent system messages, so the user can see the real lines their
 * server sends and turn them into rules. Never written to disk.
 */
public final class RecentMessages {
	private static final int MAX = 100;
	private static final Deque<String> BUFFER = new ArrayDeque<>();

	private RecentMessages() {
	}

	public static synchronized void add(String line) {
		BUFFER.addFirst(line);
		while (BUFFER.size() > MAX) {
			BUFFER.removeLast();
		}
	}

	/** Newest message first. */
	public static synchronized List<String> snapshot() {
		return new ArrayList<>(BUFFER);
	}

	public static synchronized void clear() {
		BUFFER.clear();
	}
}
