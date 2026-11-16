package com.zyroofficial.data;

import java.util.UUID;

/** A single recorded money movement. Saved to JSON by {@link TransactionStore}. */
public final class Transaction {
	public String id = UUID.randomUUID().toString();

	/** Epoch milliseconds. */
	public long timestamp = System.currentTimeMillis();

	public TransactionType type = TransactionType.RECEIVED;
	public TransactionSource source = TransactionSource.MANUAL;

	/** Always positive; the type decides whether it adds or subtracts. */
	public double amount;

	/** The other player involved, or an empty string when not applicable. */
	public String player = "";

	/** Free-form note. */
	public String note = "";

	public Transaction() {
		// Needed by Gson.
	}

	public Transaction(TransactionType type, TransactionSource source, double amount, String player, String note) {
		this.type = type;
		this.source = source;
		this.amount = Math.abs(amount);
		this.player = player == null ? "" : player;
		this.note = note == null ? "" : note;
	}

	/** Amount with its sign applied: positive for income, negative for expenses. */
	public double signedAmount() {
		return type.isIncome() ? amount : -amount;
	}
}
