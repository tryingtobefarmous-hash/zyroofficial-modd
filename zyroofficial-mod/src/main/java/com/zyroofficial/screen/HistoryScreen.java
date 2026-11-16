package com.zyroofficial.screen;

import com.zyroofficial.data.Transaction;
import com.zyroofficial.data.TransactionFilter;
import com.zyroofficial.data.TransactionSource;
import com.zyroofficial.data.TransactionStore;
import com.zyroofficial.data.TransactionType;
import com.zyroofficial.util.MoneyFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Transaction history with search, type/source filters, sorting and paging. */
public class HistoryScreen extends Screen {
	private static final int ROW_PITCH = 22;
	private static final int MAX_ROWS = 14;
	private static final DateTimeFormatter DATE_FORMAT =
			DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());

	private static final TransactionType[] TYPE_CYCLE = {
			null, TransactionType.RECEIVED, TransactionType.SENT,
			TransactionType.WON, TransactionType.LOST, TransactionType.TAX
	};
	private static final TransactionSource[] SOURCE_CYCLE = {null, TransactionSource.MANUAL, TransactionSource.CHAT};

	private final Screen parent;
	private final TransactionFilter filter = new TransactionFilter();
	private int typeIndex;
	private int sourceIndex;

	private List<Transaction> results = new ArrayList<>();
	private int page;
	private int rows = 1;

	private EditBox searchBox;
	private Button typeButton;
	private Button sourceButton;
	private Button sortButton;
	private Button prevButton;
	private Button nextButton;
	private final List<Button> deleteButtons = new ArrayList<>();

	private int left;
	private int contentW;
	private int top;
	private int listTop;
	private int bottomY;

	public HistoryScreen(Screen parent) {
		super(Component.literal("Transaction History"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.deleteButtons.clear();

		this.contentW = Math.min(this.width - 20, 360);
		this.left = (this.width - this.contentW) / 2;
		this.top = 8;

		this.searchBox = new EditBox(this.font, this.left, this.top + 14, this.contentW, 20, Component.literal("Search"));
		this.searchBox.setMaxLength(48);
		this.searchBox.setHint(Component.literal("Search player, note, type or amount..."));
		this.searchBox.setValue(this.filter.search);
		this.addRenderableWidget(this.searchBox);

		int third = (this.contentW - 8) / 3;
		int filterY = this.top + 38;
		this.typeButton = this.addRenderableWidget(Button.builder(typeLabel(), button -> {
					this.typeIndex = (this.typeIndex + 1) % TYPE_CYCLE.length;
					this.filter.type = TYPE_CYCLE[this.typeIndex];
					this.page = 0;
					button.setMessage(typeLabel());
					refresh();
				})
				.bounds(this.left, filterY, third, 20).build());
		this.sourceButton = this.addRenderableWidget(Button.builder(sourceLabel(), button -> {
					this.sourceIndex = (this.sourceIndex + 1) % SOURCE_CYCLE.length;
					this.filter.source = SOURCE_CYCLE[this.sourceIndex];
					this.page = 0;
					button.setMessage(sourceLabel());
					refresh();
				})
				.bounds(this.left + third + 4, filterY, third, 20).build());
		this.sortButton = this.addRenderableWidget(Button.builder(sortLabel(), button -> {
					TransactionFilter.Sort[] sorts = TransactionFilter.Sort.values();
					this.filter.sort = sorts[(this.filter.sort.ordinal() + 1) % sorts.length];
					this.page = 0;
					button.setMessage(sortLabel());
					refresh();
				})
				.bounds(this.left + (third + 4) * 2, filterY, third, 20).build());

		this.listTop = this.top + 64;
		this.bottomY = this.height - 26;
		this.rows = Math.max(1, Math.min(MAX_ROWS, (this.bottomY - 14 - this.listTop) / ROW_PITCH));

		for (int i = 0; i < this.rows; i++) {
			final int slot = i;
			Button delete = this.addRenderableWidget(Button.builder(Component.literal("X"), button -> deleteSlot(slot))
					.bounds(this.left + this.contentW - 22, this.listTop + i * ROW_PITCH, 22, 20).build());
			this.deleteButtons.add(delete);
		}

		this.prevButton = this.addRenderableWidget(Button.builder(Component.literal("< Prev"), button -> {
					this.page--;
					refresh();
				})
				.bounds(this.left, this.bottomY, 60, 20).build());
		this.nextButton = this.addRenderableWidget(Button.builder(Component.literal("Next >"), button -> {
					this.page++;
					refresh();
				})
				.bounds(this.left + 64, this.bottomY, 60, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(this.left + this.contentW - 70, this.bottomY, 70, 20).build());

		// Attach the search listener last so setValue above does not trigger it during init.
		this.searchBox.setResponder(text -> {
			this.filter.search = text;
			this.page = 0;
			refresh();
		});

		refresh();
	}

	private Component typeLabel() {
		return Component.literal("Type: " + (this.filter.type == null ? "All" : this.filter.type.label()));
	}

	private Component sourceLabel() {
		String name = "All";
		if (this.filter.source == TransactionSource.MANUAL) {
			name = "Manual";
		} else if (this.filter.source == TransactionSource.CHAT) {
			name = "Chat";
		}
		return Component.literal("Source: " + name);
	}

	private Component sortLabel() {
		return Component.literal(this.filter.sort.label());
	}

	private void refresh() {
		this.results = TransactionStore.get().query(this.filter);
		int maxPage = Math.max(0, (this.results.size() - 1) / this.rows);
		this.page = Math.max(0, Math.min(this.page, maxPage));

		int first = this.page * this.rows;
		for (int i = 0; i < this.deleteButtons.size(); i++) {
			boolean present = first + i < this.results.size();
			Button button = this.deleteButtons.get(i);
			button.visible = present;
			button.active = present;
		}
		this.prevButton.active = this.page > 0;
		this.nextButton.active = this.page < maxPage;
	}

	private void deleteSlot(int slot) {
		int index = this.page * this.rows + slot;
		if (index >= 0 && index < this.results.size()) {
			TransactionStore.get().remove(this.results.get(index).id);
			refresh();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Ui.shadowText(graphics, this.font, "Transaction History", this.left, this.top + 2, Ui.ACCENT);
		Ui.rightAligned(graphics, this.font,
				this.results.size() + " shown  |  * = detected from chat",
				this.left + this.contentW, this.top + 2, Ui.MUTED);

		if (this.results.isEmpty()) {
			Ui.centered(graphics, this.font, "No transactions match.", this.width / 2, this.listTop + 10, Ui.MUTED);
		}

		int first = this.page * this.rows;
		int rightEdge = this.left + this.contentW - 26;
		for (int i = 0; i < this.rows; i++) {
			int index = first + i;
			if (index >= this.results.size()) {
				break;
			}
			Transaction t = this.results.get(index);
			int rowY = this.listTop + i * ROW_PITCH;
			int textY = rowY + 6;

			String date = DATE_FORMAT.format(Instant.ofEpochMilli(t.timestamp));
			Ui.text(graphics, this.font, date, this.left + 2, textY, Ui.MUTED);

			String typeText = t.type.label() + (t.source == TransactionSource.CHAT ? "*" : "");
			Ui.text(graphics, this.font, typeText, this.left + 70, textY, t.type.isIncome() ? Ui.GREEN : Ui.RED);

			String amount = (t.type.isIncome() ? "+" : "-") + MoneyFormat.full(t.amount).replace("-", "");
			Ui.text(graphics, this.font, Ui.clip(this.font, amount, 92), this.left + 124, textY,
					t.type.isIncome() ? Ui.GREEN : Ui.RED);

			String detail = !t.player.isEmpty() ? t.player : t.note;
			if (!t.player.isEmpty() && !t.note.isEmpty()) {
				detail = t.player + " - " + t.note;
			}
			int detailX = this.left + 220;
			Ui.text(graphics, this.font, Ui.clip(this.font, detail, rightEdge - detailX), detailX, textY, Ui.TEXT);

			graphics.fill(this.left, rowY + 21, this.left + this.contentW, rowY + 22, Ui.DIVIDER);
		}

		int maxPage = Math.max(0, (this.results.size() - 1) / this.rows);
		Ui.centered(graphics, this.font, "Page " + (this.page + 1) + " / " + (maxPage + 1),
				this.left + this.contentW / 2, this.bottomY + 6, Ui.MUTED);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
