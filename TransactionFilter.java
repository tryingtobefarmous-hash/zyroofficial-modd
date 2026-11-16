package com.zyroofficial.data;

import com.zyroofficial.util.MoneyFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Search, filter and sort settings for the transaction history. */
public final class TransactionFilter {
	public enum Sort {
		NEWEST("Newest first"),
		OLDEST("Oldest first"),
		LARGEST("Largest first"),
		SMALLEST("Smallest first");

		private final String label;

		Sort(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}

	/** Null means "all types". */
	public TransactionType type;
	/** Null means "all sources". */
	public TransactionSource source;
	/** Matches player name, note, type, source and amount. Empty means no search. */
	public String search = "";
	public Sort sort = Sort.NEWEST;

	public boolean matches(Transaction t) {
		if (type != null && t.type != type) {
			return false;
		}
		if (source != null && t.source != source) {
			return false;
		}
		String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			return true;
		}
		String haystack = (t.player + " " + t.note + " " + t.type.label() + " "
				+ t.source.name() + " " + MoneyFormat.full(t.amount) + " " + t.amount)
				.toLowerCase(Locale.ROOT);
		for (String word : query.split("\\s+")) {
			if (!haystack.contains(word)) {
				return false;
			}
		}
		return true;
	}

	public List<Transaction> apply(List<Transaction> all) {
		List<Transaction> result = new ArrayList<>();
		for (Transaction t : all) {
			if (matches(t)) {
				result.add(t);
			}
		}
		Comparator<Transaction> comparator;
		switch (sort) {
			case OLDEST:
				comparator = Comparator.comparingLong(t -> t.timestamp);
				break;
			case LARGEST:
				comparator = Comparator.comparingDouble((Transaction t) -> t.amount).reversed();
				break;
			case SMALLEST:
				comparator = Comparator.comparingDouble(t -> t.amount);
				break;
			case NEWEST:
			default:
				comparator = Comparator.comparingLong((Transaction t) -> t.timestamp).reversed();
				break;
		}
		result.sort(comparator);
		return result;
	}
}
