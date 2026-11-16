package com.zyroofficial.data;

import java.util.List;

/**
 * All money statistics, computed from a list of transactions.
 *
 * <ul>
 *   <li>Net profit = received + won - sent - lost - tax</li>
 *   <li>Volume = received + sent + won + lost (tax is not counted as volume)</li>
 *   <li>Incoming transactions / biggest incoming = RECEIVED entries only</li>
 *   <li>Outgoing transactions / biggest outgoing = SENT entries only</li>
 * </ul>
 */
public final class MoneyStats {
	public double received;
	public double sent;
	public double won;
	public double lost;
	public double tax;

	public double netProfit;
	public double volume;

	public int incomingCount;
	public int outgoingCount;
	public int totalCount;

	public double biggestIncoming;
	public double biggestOutgoing;

	public static MoneyStats compute(List<Transaction> transactions) {
		MoneyStats stats = new MoneyStats();
		for (Transaction t : transactions) {
			stats.totalCount++;
			switch (t.type) {
				case RECEIVED:
					stats.received += t.amount;
					stats.incomingCount++;
					stats.biggestIncoming = Math.max(stats.biggestIncoming, t.amount);
					break;
				case SENT:
					stats.sent += t.amount;
					stats.outgoingCount++;
					stats.biggestOutgoing = Math.max(stats.biggestOutgoing, t.amount);
					break;
				case WON:
					stats.won += t.amount;
					break;
				case LOST:
					stats.lost += t.amount;
					break;
				case TAX:
					stats.tax += t.amount;
					break;
				default:
					break;
			}
		}
		stats.netProfit = stats.received + stats.won - stats.sent - stats.lost - stats.tax;
		stats.volume = stats.received + stats.sent + stats.won + stats.lost;
		return stats;
	}
}
