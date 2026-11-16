package com.zyroofficial.screen;

import com.zyroofficial.config.ZyroConfig;
import com.zyroofficial.data.MoneyStats;
import com.zyroofficial.data.TransactionStore;
import com.zyroofficial.util.AmountParser;
import com.zyroofficial.util.MoneyFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Money statistics dashboard with the minimum-amount setting and reset. */
public class MoneyStatsScreen extends Screen {
	private static final int COL_W = 170;
	private static final int GAP = 6;
	private static final int CARD_H = 26;
	private static final int CARD_GAP = 4;
	private static final Component RESET_LABEL = Component.literal("Reset All");

	private final Screen parent;
	private MoneyStats stats = new MoneyStats();

	private EditBox minBox;
	private Button resetButton;
	private long resetArmedUntil;

	private String status = "";
	private int statusColor = Ui.MUTED;

	private int left;
	private int top;
	private int cardsTop;
	private int statusY;

	public MoneyStatsScreen(Screen parent) {
		super(Component.literal("Money Statistics"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.stats = TransactionStore.get().stats();

		int totalW = COL_W * 2 + GAP;
		this.left = (this.width - totalW) / 2;
		int totalH = 14 + 5 * (CARD_H + CARD_GAP) + 4 + 2 * 24 + 12;
		this.top = Math.max(4, (this.height - totalH) / 2);
		this.cardsTop = this.top + 14;

		int y = this.cardsTop + 5 * (CARD_H + CARD_GAP) + 4;
		int bw = (totalW - 9) / 4;

		this.minBox = new EditBox(this.font, this.left, y, bw * 2 + 3, 20, Component.literal("Minimum amount"));
		this.minBox.setMaxLength(24);
		this.minBox.setHint(Component.literal("Minimum amount (e.g. 1000 or 2.5K)"));
		this.minBox.setValue(AmountParser.plain(ZyroConfig.get().minTransactionAmount));
		this.addRenderableWidget(this.minBox);

		this.addRenderableWidget(Button.builder(Component.literal("Set Minimum"), button -> applyMinimum())
				.bounds(this.left + (bw + 3) * 2, y, bw, 20).build());
		this.resetButton = this.addRenderableWidget(Button.builder(RESET_LABEL, button -> onResetPressed())
				.bounds(this.left + (bw + 3) * 3, y, bw, 20).build());

		y += 24;
		this.addRenderableWidget(Button.builder(Component.literal("History"),
						button -> this.minecraft.gui.setScreen(new HistoryScreen(this)))
				.bounds(this.left, y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Add Entry"),
						button -> this.minecraft.gui.setScreen(new AddTransactionScreen(this)))
				.bounds(this.left + (bw + 3), y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Auto-Detect"),
						button -> this.minecraft.gui.setScreen(new ChatDetectionScreen(this)))
				.bounds(this.left + (bw + 3) * 2, y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(this.left + (bw + 3) * 3, y, bw, 20).build());

		this.statusY = y + 26;
	}

	private void applyMinimum() {
		double value = AmountParser.parseAllowZero(this.minBox.getValue());
		if (Double.isNaN(value)) {
			setStatus("Enter a number like 1000, 2.5K or 3M (0 = no minimum).", Ui.RED);
			return;
		}
		ZyroConfig.get().minTransactionAmount = value;
		ZyroConfig.save();
		this.minBox.setValue(AmountParser.plain(value));
		setStatus(value == 0 ? "Minimum removed." : "Minimum set to " + MoneyFormat.full(value) + ".", Ui.GREEN);
	}

	private void onResetPressed() {
		long now = System.currentTimeMillis();
		if (this.resetArmedUntil > now) {
			TransactionStore.get().clear();
			this.stats = TransactionStore.get().stats();
			this.resetArmedUntil = 0;
			this.resetButton.setMessage(RESET_LABEL);
			setStatus("All statistics and history reset to $0.", Ui.GOLD);
		} else {
			this.resetArmedUntil = now + 5000;
			this.resetButton.setMessage(Component.literal("Confirm reset!"));
			setStatus("Click Confirm reset! within 5 seconds to erase everything.", Ui.RED);
		}
	}

	private void setStatus(String message, int color) {
		this.status = message;
		this.statusColor = color;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (this.resetArmedUntil != 0 && System.currentTimeMillis() > this.resetArmedUntil) {
			this.resetArmedUntil = 0;
			this.resetButton.setMessage(RESET_LABEL);
		}

		int totalW = COL_W * 2 + GAP;
		Ui.shadowText(graphics, this.font, "Money Statistics", this.left, this.top + 2, Ui.ACCENT);
		Ui.rightAligned(graphics, this.font,
				"Tax paid " + MoneyFormat.compact(stats.tax) + "  |  " + stats.totalCount + " entries",
				this.left + totalW, this.top + 2, Ui.MUTED);

		MoneyStats s = this.stats;
		drawCard(graphics, 0, 0, "Total Received", MoneyFormat.full(s.received), Ui.GREEN, s.received);
		drawCard(graphics, 1, 0, "Total Sent", MoneyFormat.full(s.sent), Ui.RED, s.sent);
		drawCard(graphics, 0, 1, "Total Won", MoneyFormat.full(s.won), Ui.GREEN, s.won);
		drawCard(graphics, 1, 1, "Total Lost", MoneyFormat.full(s.lost), Ui.RED, s.lost);
		drawCard(graphics, 0, 2, "Net Profit", MoneyFormat.full(s.netProfit), Ui.signColor(s.netProfit), s.netProfit);
		drawCard(graphics, 1, 2, "Transaction Volume", MoneyFormat.full(s.volume), Ui.TEXT, s.volume);
		drawCard(graphics, 0, 3, "Incoming Transactions", String.format(Locale.US, "%,d", s.incomingCount), Ui.TEXT, 1);
		drawCard(graphics, 1, 3, "Outgoing Transactions", String.format(Locale.US, "%,d", s.outgoingCount), Ui.TEXT, 1);
		drawCard(graphics, 0, 4, "Biggest Incoming", MoneyFormat.full(s.biggestIncoming), Ui.GREEN, s.biggestIncoming);
		drawCard(graphics, 1, 4, "Biggest Outgoing", MoneyFormat.full(s.biggestOutgoing), Ui.RED, s.biggestOutgoing);

		String line = this.status;
		int color = this.statusColor;
		if (line.isEmpty()) {
			double min = ZyroConfig.get().minTransactionAmount;
			line = min > 0
					? "Minimum: " + MoneyFormat.full(min) + " - smaller transactions are ignored."
					: "No minimum set - every transaction is recorded.";
			color = Ui.MUTED;
		}
		Ui.centered(graphics, this.font, Ui.clip(this.font, line, totalW), this.width / 2, this.statusY, color);
	}

	private void drawCard(GuiGraphicsExtractor graphics, int column, int row, String label, String value,
			int valueColor, double rawValue) {
		int x = this.left + column * (COL_W + GAP);
		int y = this.cardsTop + row * (CARD_H + CARD_GAP);
		Ui.card(graphics, x, y, COL_W, CARD_H);
		Ui.text(graphics, this.font, label, x + 8, y + 4, Ui.MUTED);

		String shown = value;
		if (this.font.width(shown) > COL_W - 14) {
			shown = MoneyFormat.compact(rawValue);
		}
		int color = rawValue == 0 ? Ui.TEXT : valueColor;
		Ui.shadowText(graphics, this.font, shown, x + 8, y + 15, color);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
