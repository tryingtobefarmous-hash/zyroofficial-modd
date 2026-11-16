package com.zyroofficial.screen;

import com.zyroofficial.chat.ChatDetectionConfig;
import com.zyroofficial.data.TransactionType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Settings for automatic chat detection: switches, duplicate filter and name/amount patterns. */
public class ChatDetectionScreen extends Screen {
	private static final int PANEL_W = 300;

	private final Screen parent;

	private EditBox windowBox;
	private EditBox playerBox;
	private EditBox amountBox;

	private String patternError = "";
	private int left;
	private int top;
	private int playerLabelY;
	private int amountLabelY;
	private int errorY;

	public ChatDetectionScreen(Screen parent) {
		super(Component.literal("Auto-Detect"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ChatDetectionConfig config = ChatDetectionConfig.get();
		this.left = (this.width - PANEL_W) / 2;
		int totalH = 14 + 24 + 48 + 24 + 34 + 34 + 12 + 24 + 24;
		this.top = Math.max(4, (this.height - totalH) / 2);

		int y = this.top + 14;
		this.addRenderableWidget(Button.builder(onOff("Auto-detection", config.enabled), button -> {
					config.enabled = !config.enabled;
					ChatDetectionConfig.save();
					button.setMessage(onOff("Auto-detection", config.enabled));
				})
				.bounds(this.left, y, PANEL_W, 20).build());

		// Per-type switches: three on the first row, two on the second.
		y += 24;
		TransactionType[] types = TransactionType.values();
		int third = (PANEL_W - 8) / 3;
		for (int i = 0; i < types.length; i++) {
			final TransactionType type = types[i];
			int column = i % 3;
			int row = i / 3;
			this.addRenderableWidget(Button.builder(onOff(type.label(), config.typeEnabled(type)), button -> {
						config.setTypeEnabled(type, !config.typeEnabled(type));
						ChatDetectionConfig.save();
						button.setMessage(onOff(type.label(), config.typeEnabled(type)));
					})
					.bounds(this.left + column * (third + 4), y + row * 24, third, 20).build());
		}

		y += 48;
		int half = (PANEL_W - 4) / 2;
		this.addRenderableWidget(Button.builder(onOff("Duplicate filter", config.duplicateFilterEnabled), button -> {
					config.duplicateFilterEnabled = !config.duplicateFilterEnabled;
					ChatDetectionConfig.save();
					button.setMessage(onOff("Duplicate filter", config.duplicateFilterEnabled));
				})
				.bounds(this.left, y, half, 20).build());
		this.windowBox = new EditBox(this.font, this.left + half + 4, y, half, 20, Component.literal("Window ms"));
		this.windowBox.setMaxLength(5);
		this.windowBox.setHint(Component.literal("Window (ms)"));
		this.windowBox.setValue(Integer.toString(config.duplicateWindowMs));
		this.addRenderableWidget(this.windowBox);

		y += 24;
		this.playerLabelY = y;
		this.playerBox = new EditBox(this.font, this.left, y + 10, PANEL_W, 20, Component.literal("Player pattern"));
		this.playerBox.setMaxLength(120);
		this.playerBox.setValue(config.playerPattern);
		this.addRenderableWidget(this.playerBox);

		y += 34;
		this.amountLabelY = y;
		this.amountBox = new EditBox(this.font, this.left, y + 10, PANEL_W, 20, Component.literal("Amount pattern"));
		this.amountBox.setMaxLength(120);
		this.amountBox.setValue(config.amountPattern);
		this.addRenderableWidget(this.amountBox);

		y += 34;
		this.errorY = y;

		y += 12;
		this.addRenderableWidget(Button.builder(Component.literal("Manage Rules (" + config.rules.size() + ")"),
						button -> this.minecraft.gui.setScreen(new RuleListScreen(this)))
				.bounds(this.left, y, half, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Recent Messages"),
						button -> this.minecraft.gui.setScreen(new RecentMessagesScreen(this)))
				.bounds(this.left + half + 4, y, half, 20).build());

		y += 24;
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
				.bounds(this.left, y, PANEL_W, 20).build());

		// Listeners attach last so the setValue calls above do not trigger them during init.
		this.windowBox.setResponder(text -> applyWindow());
		this.playerBox.setResponder(text -> applyPatterns());
		this.amountBox.setResponder(text -> applyPatterns());
	}

	private static Component onOff(String label, boolean value) {
		return Component.literal(label + ": " + (value ? "ON" : "OFF"));
	}

	private void applyWindow() {
		try {
			int value = Integer.parseInt(this.windowBox.getValue().trim());
			ChatDetectionConfig.get().duplicateWindowMs = Math.max(0, Math.min(60_000, value));
		} catch (NumberFormatException ignored) {
			// Keep the previous value until the box holds a valid number.
		}
	}

	private void applyPatterns() {
		ChatDetectionConfig config = ChatDetectionConfig.get();
		String player = this.playerBox.getValue();
		String amount = this.amountBox.getValue();

		boolean playerOk = ChatDetectionConfig.isValidRegex(player);
		boolean amountOk = ChatDetectionConfig.isValidRegex(amount);
		if (playerOk) {
			config.playerPattern = player;
		}
		if (amountOk) {
			config.amountPattern = amount;
		}

		if (!playerOk) {
			this.patternError = "Player pattern is not a valid regular expression - not applied.";
		} else if (!amountOk) {
			this.patternError = "Amount pattern is not a valid regular expression - not applied.";
		} else {
			this.patternError = "";
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Ui.shadowText(graphics, this.font, "Auto-Detect Settings", this.left, this.top + 2, Ui.ACCENT);
		Ui.text(graphics, this.font, "Player name pattern (used for {player})", this.left, this.playerLabelY, Ui.MUTED);
		Ui.text(graphics, this.font, "Amount pattern (used for {amount})", this.left, this.amountLabelY, Ui.MUTED);

		if (!this.patternError.isEmpty()) {
			Ui.text(graphics, this.font, Ui.clip(this.font, this.patternError, PANEL_W), this.left, this.errorY, Ui.RED);
		} else if (ChatDetectionConfig.get().rules.isEmpty()) {
			Ui.text(graphics, this.font, "No rules yet - add one under Manage Rules.", this.left, this.errorY, Ui.GOLD);
		}
	}

	@Override
	public void onClose() {
		applyWindow();
		applyPatterns();
		ChatDetectionConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}
}
