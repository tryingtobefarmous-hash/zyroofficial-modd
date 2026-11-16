package com.zyroofficial.data;

/** Where a transaction came from. */
public enum TransactionSource {
	/** Entered by hand in the menu. */
	MANUAL,
	/** Detected automatically from a chat message. */
	CHAT
}
