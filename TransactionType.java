package com.zyroofficial.data;

/** What kind of money movement a transaction represents. */
public enum TransactionType {
	/** Money received (incoming payment). */
	RECEIVED("Received", true),
	/** Money sent (outgoing payment). */
	SENT("Sent", false),
	/** Money won (gambling, bets, events). */
	WON("Won", true),
	/** Money lost (gambling, bets, events). */
	LOST("Lost", false),
	/** Tax deducted from a transaction. */
	TAX("Tax", false);

	private final String label;
	private final boolean income;

	TransactionType(String label, boolean income) {
		this.label = label;
		this.income = income;
	}

	public String label() {
		return label;
	}

	/** True if this type adds to net profit, false if it subtracts. */
	public boolean isIncome() {
		return income;
	}
}
