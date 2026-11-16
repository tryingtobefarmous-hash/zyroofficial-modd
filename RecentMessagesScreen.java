package com.zyroofficial.screen;

import com.zyroofficial.chat.ChatDetectionConfig;
import com.zyroofficial.chat.ChatRule;
import com.zyroofficial.chat.RecentMessages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the latest server system messages (memory only, never saved) so you can see the exact
 * wording your server uses and turn any line into a rule with one click.
 */
public class RecentMessagesScreen extends Screen {
	private static final int ROW_PITCH = 22;
	private static final int MAX_ROWS = 12;

	private final Screen parent;
	private List<String> lines = new ArrayList<>();
	private int page;
	private int rows = 1;

	private final List<Button> ruleButtons = new ArrayList<>();
	private Button prevButton;
	private Button nextButton;
	private Button captureButton;

	private int left;
	private int contentW;
	private int top;
	private int listTop;
	private int bottomY;

	public RecentMessagesScreen(Screen parent) {
		super(Component.literal("Recent Messages"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.ruleButtons.clear();

		this.contentW = Math.min(this.width - 20, 400);
		this.left = (this.width - this.contentW) / 2;
		this.top = 8;
		this.listTop = this.top + 18;
		this.bottomY = this.height - 26;
		this.rows = Math.max(1, Math.min(MAX_ROWS, (this.bottomY - 14 - this.listTop) / ROW_PITCH));

		for (int i = 0; i < this.rows; i++) {
			final int slot = i;
			this.ruleButtons.add(this.addRenderableWidget(Button.builder(Component.literal("Make Rule"), button -> makeRule(slot))
					.bounds(this.left + this.contentW - 66, this.listTop + i * ROW_PITCH, 66, 20).build()));
		}

		int fifth = (this.contentW - 12) / 5;
		this.prevButton = this.addRenderableWidget(Button.builder(Component.literal("< Prev"), button -> {
					this.page--;
					refresh();
				})
				.bounds(this.left, this.bottomY, fifth, 20).build());
		this.nextButton = this.addRenderableWidget(Button.builder(Component.literal("Next >"), button -> {
					this.page++;
					refresh();
				})
				.bounds(this.left + (fifth + 3), this.bottomY, fifth, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Clear"), button -> {
					RecentMessages.clear();
					refresh();
				})
				.bounds(this.left + (fifth + 3) * 2, this.bottomY, fifth, 20).build());
		this.captureButton = this.addRenderableWidget(Button.builder(captureLabel(), button -> {
					ChatDetectionConfig config = ChatDetectionConfig.get();
					config.captureRecent = !config.captureRecent;
					ChatDetectionConfig.save();
					button.setMessage(captureLabel());
				})
				.bounds(this.left + (fifth + 3) * 3, this.bottomY, fifth, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(this.left + (fifth + 3) * 4, this.bottomY, fifth, 20).build());

		refresh();
	}

	private Component captureLabel() {
		return Component.literal("Capture: " + (ChatDetectionConfig.get().captureRecent ? "ON" : "OFF"));
	}

	private void refresh() {
		this.lines = RecentMessages.snapshot();
		int maxPage = Math.max(0, (this.lines.size() - 1) / this.rows);
		this.page = Math.max(0, Math.min(this.page, maxPage));

		int first = this.page * this.rows;
		for (int i = 0; i < this.ruleButtons.size(); i++) {
			boolean present = first + i < this.lines.size();
			this.ruleButtons.get(i).visible = present;
			this.ruleButtons.get(i).active = present;
		}
		this.prevButton.active = this.page > 0;
		this.nextButton.active = this.page < maxPage;
	}

	private void makeRule(int slot) {
		int index = this.page * this.rows + slot;
		if (index >= 0 && index < this.lines.size()) {
			this.minecraft.gui.setScreen(new RuleEditScreen(this, new ChatRule(), true, this.lines.get(index)));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Ui.shadowText(graphics, this.font, "Recent Server Messages", this.left, this.top + 2, Ui.ACCENT);
		Ui.rightAligned(graphics, this.font, "newest first  |  kept in memory only",
				this.left + this.contentW, this.top + 2, Ui.MUTED);

		if (this.lines.isEmpty()) {
			Ui.centered(graphics, this.font,
					ChatDetectionConfig.get().captureRecent
							? "Nothing captured yet. Play on your server, then reopen this screen."
							: "Capture is OFF. Turn it on, then play on your server.",
					this.width / 2, this.listTop + 10, Ui.MUTED);
		}

		int first = this.page * this.rows;
		for (int i = 0; i < this.rows; i++) {
			int index = first + i;
			if (index >= this.lines.size()) {
				break;
			}
			int rowY = this.listTop + i * ROW_PITCH;
			Ui.text(graphics, this.font, Ui.clip(this.font, this.lines.get(index), this.contentW - 74),
					this.left + 2, rowY + 6, Ui.TEXT);
			graphics.fill(this.left, rowY + 21, this.left + this.contentW, rowY + 22, Ui.DIVIDER);
		}

		int maxPage = Math.max(0, (this.lines.size() - 1) / this.rows);
		Ui.centered(graphics, this.font, "Page " + (this.page + 1) + " / " + (maxPage + 1),
				this.width / 2, this.bottomY - 10, Ui.MUTED);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
