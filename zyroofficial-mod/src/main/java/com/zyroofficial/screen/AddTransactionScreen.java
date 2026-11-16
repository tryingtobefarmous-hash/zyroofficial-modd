package com.zyroofficial.screen;

import com.zyroofficial.config.ZyroConfig;
import com.zyroofficial.data.Transaction;
import com.zyroofficial.data.TransactionSource;
import com.zyroofficial.data.TransactionStore;
import com.zyroofficial.data.TransactionType;
import com.zyroofficial.util.AmountParser;
import com.zyroofficial.util.MoneyFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Manual entry of a transaction, win or loss. */
public class AddTransactionScreen extends Screen {
	private static final int PANEL_W = 260;

	private final Screen parent;
	private TransactionType type = TransactionType.RECEIVED;

	private Button typeButton;
	private EditBox amountBox;
	private EditBox playerBox;
	private EditBox noteBox;

	private String message = "";
	private int messageColor = Ui.MUTED;

	private int left;
	private int top;
	private int amountLabelY;
	private int playerLabelY;
	private int noteLabelY;
	private int messageY;

	public AddTransactionScreen(Screen parent) {
		super(Component.literal("Add Entry"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.left = (this.width - PANEL_W) / 2;
		int totalH = 14 + 24 + 32 + 32 + 32 + 16 + 24;
		this.top = Math.max(4, (this.height - totalH) / 2);

		int y = this.top + 14;
		this.typeButton = this.addRenderableWidget(Button.builder(typeLabel(), button -> {
					TransactionType[] values = TransactionType.values();
					this.type = values[(this.type.ordinal() + 1) % values.length];
					button.setMessage(typeLabel());
				})
				.bounds(this.left, y, PANEL_W, 20).build());

		y += 24;
		this.amountLabelY = y;
		this.amountBox = new EditBox(this.font, this.left, y + 10, PANEL_W, 20, Component.literal("Amount"));
		this.amountBox.setMaxLength(24);
		this.amountBox.setHint(Component.literal("e.g. 1500, 1,500.50, 2.5K, 3M"));
		this.addRenderableWidget(this.amountBox);

		y += 32;
		this.playerLabelY = y;
		this.playerBox = new EditBox(this.font, this.left, y + 10, PANEL_W, 20, Component.literal("Player"));
		this.playerBox.setMaxLength(32);
		this.playerBox.setHint(Component.literal("Optional"));
		this.addRenderableWidget(this.playerBox);

		y += 32;
		this.noteLabelY = y;
		this.noteBox = new EditBox(this.font, this.left, y + 10, PANEL_W, 20, Component.literal("Note"));
		this.noteBox.setMaxLength(64);
		this.noteBox.setHint(Component.literal("Optional"));
		this.addRenderableWidget(this.noteBox);

		y += 32;
		this.messageY = y + 2;

		y += 16;
		int half = (PANEL_W - 4) / 2;
		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
				.bounds(this.left, y, half, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
				.bounds(this.left + half + 4, y, half, 20).build());
	}

	private Component typeLabel() {
		return Component.literal("Type: " + this.type.label()
				+ (this.type.isIncome() ? "  (adds to profit)" : "  (subtracts from profit)"));
	}

	private void save() {
		double amount = AmountParser.parse(this.amountBox.getValue());
		if (Double.isNaN(amount)) {
			setMessage("Enter a valid amount above zero, like 1500 or 2.5K.", Ui.RED);
			return;
		}

		double minimum = ZyroConfig.get().minTransactionAmount;
		if (amount < minimum) {
			setMessage("Below your minimum (" + MoneyFormat.full(minimum) + ") - not recorded.", Ui.RED);
			return;
		}

		Transaction transaction = new Transaction(this.type, TransactionSource.MANUAL, amount,
				this.playerBox.getValue().trim(), this.noteBox.getValue().trim());
		if (!TransactionStore.get().add(transaction)) {
			setMessage("That entry could not be recorded.", Ui.RED);
			return;
		}

		// Stay open for quick repeated entry; clear the fields.
		this.amountBox.setValue("");
		this.playerBox.setValue("");
		this.noteBox.setValue("");
		setMessage("Saved " + this.type.label() + " " + MoneyFormat.full(amount) + ".", Ui.GREEN);
	}

	private void setMessage(String text, int color) {
		this.message = text;
		this.messageColor = color;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Ui.shadowText(graphics, this.font, "Add Entry", this.left, this.top + 2, Ui.ACCENT);
		Ui.text(graphics, this.font, "Amount", this.left, this.amountLabelY, Ui.MUTED);
		Ui.text(graphics, this.font, "Player", this.left, this.playerLabelY, Ui.MUTED);
		Ui.text(graphics, this.font, "Note", this.left, this.noteLabelY, Ui.MUTED);

		if (!this.message.isEmpty()) {
			Ui.centered(graphics, this.font, Ui.clip(this.font, this.message, PANEL_W),
					this.width / 2, this.messageY, this.messageColor);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
