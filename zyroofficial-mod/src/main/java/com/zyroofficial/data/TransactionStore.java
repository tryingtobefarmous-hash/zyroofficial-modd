package com.zyroofficial.data;

import com.zyroofficial.ZyroofficialClient;
import com.zyroofficial.config.ZyroConfig;
import com.zyroofficial.util.JsonFiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds every recorded transaction and persists them to config/zyroofficial/transactions.json.
 * Phase 2 builds the statistics, search and filtering on top of this.
 */
public final class TransactionStore {
	private static final TransactionStore INSTANCE = new TransactionStore();

	/** On-disk format. */
	private static final class FileData {
		int version = 1;
		List<Transaction> transactions = new ArrayList<>();
	}

	private final List<Transaction> transactions = new ArrayList<>();

	private TransactionStore() {
	}

	public static TransactionStore get() {
		return INSTANCE;
	}

	private static Path file() {
		return ZyroConfig.directory().resolve("transactions.json");
	}

	public synchronized void load() {
		transactions.clear();
		FileData data = JsonFiles.read(file(), FileData.class);
		if (data != null && data.transactions != null) {
			for (Transaction transaction : data.transactions) {
				if (transaction != null && transaction.type != null) {
					if (transaction.source == null) {
						transaction.source = TransactionSource.MANUAL;
					}
					transactions.add(transaction);
				}
			}
		}
	}

	public synchronized void save() {
		FileData data = new FileData();
		data.transactions = new ArrayList<>(transactions);
		try {
			JsonFiles.write(file(), data);
		} catch (IOException e) {
			ZyroofficialClient.LOGGER.error("Could not save Zyroofficial transactions", e);
		}
	}

	/**
	 * Adds a transaction unless it is below the user's minimum amount.
	 *
	 * @return true if it was recorded, false if it was ignored
	 */
	public synchronized boolean add(Transaction transaction) {
		if (transaction.amount <= 0 || Double.isNaN(transaction.amount) || Double.isInfinite(transaction.amount)) {
			return false;
		}
		if (transaction.amount < ZyroConfig.get().minTransactionAmount) {
			return false;
		}
		transactions.add(transaction);
		save();
		return true;
	}

	public synchronized boolean remove(String id) {
		boolean removed = transactions.removeIf(t -> t.id.equals(id));
		if (removed) {
			save();
		}
		return removed;
	}

	/** Deletes every transaction, resetting all statistics to $0. */
	public synchronized void clear() {
		transactions.clear();
		save();
	}

	/** A snapshot copy, oldest first. Safe to iterate while new transactions arrive. */
	public synchronized List<Transaction> snapshot() {
		return Collections.unmodifiableList(new ArrayList<>(transactions));
	}

	public synchronized int size() {
		return transactions.size();
	}

	/** Computes every statistic from the current transactions. */
	public synchronized MoneyStats stats() {
		return MoneyStats.compute(transactions);
	}

	/** Returns the transactions matching the filter, sorted as the filter asks. */
	public synchronized List<Transaction> query(TransactionFilter filter) {
		return filter.apply(transactions);
	}

	/** Net profit: income (received, won) minus expenses (sent, lost, tax). */
	public synchronized double netProfit() {
		double net = 0;
		for (Transaction transaction : transactions) {
			net += transaction.signedAmount();
		}
		return net;
	}
}
