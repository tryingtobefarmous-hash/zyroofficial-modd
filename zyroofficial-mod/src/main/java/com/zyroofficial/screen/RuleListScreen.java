package com.zyroofficial.screen;

import com.zyroofficial.chat.ChatDetectionConfig;
import com.zyroofficial.chat.ChatRule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Lists chat detection rules: enable/disable, edit, delete, add. */
public class RuleListScreen extends Screen {
	private static final int ROW_PITCH = 24;
	private static final int MAX_ROWS = 10;

	private final Screen parent;
	private int page;
	private int rows = 1;
	private String pendingDeleteId = "";

	private final List<Button> toggleButtons = new ArrayList<>();
	private final List<Button> editButtons = new ArrayList<>();
	private final List<Button> deleteButtons = new ArrayList<>();
	private Button prevButton;
	private Button nextButton;

	private int left;
	private int contentW;
	private int top;
	private int listTop;
	private int bottomY;

	public RuleListScreen(Screen parent) {
		super(Component.literal("Chat Rules"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.toggleButtons.clear();
		this.editButtons.clear();
		this.deleteButtons.clear();

		this.contentW = Math.min(this.width - 20, 380);
		this.left = (this.width - this.contentW) / 2;
		this.top = 8;
		this.listTop = this.top + 18;
		this.bottomY = this.height - 26;
		this.rows = Math.max(1, Math.min(MAX_ROWS, (this.bottomY - 14 - this.listTop) / ROW_PITCH));

		for (int i = 0; i < this.rows; i++) {
			final int slot = i;
			int y = this.listTop + i * ROW_PITCH;
			this.toggleButtons.add(this.addRenderableWidget(Button.builder(Component.literal("ON"), button -> toggleSlot(slot))
					.bounds(this.left, y, 36, 20).build()));
			this.editButtons.add(this.addRenderableWidget(Button.builder(Component.literal("Edit"), button -> editSlot(slot))
					.bounds(this.left + this.contentW - 84, y, 40, 20).build()));
			this.deleteButtons.add(this.addRenderableWidget(Button.builder(Component.literal("X"), button -> deleteSlot(slot))
					.bounds(this.left + this.contentW - 40, y, 40, 20).build()));
		}

		int quarter = (this.contentW - 9) / 4;
		this.addRenderableWidget(Button.builder(Component.literal("Add Rule"),
						button -> this.minecraft.gui.setScreen(new RuleEditScreen(this, new ChatRule(), true, "")))
				.bounds(this.left, this.bottomY, quarter, 20).build());
		this.prevButton = this.addRenderableWidget(Button.builder(Component.literal("< Prev"), button -> {
					this.page--;
					this.pendingDeleteId = "";
					refresh();
				})
				.bounds(this.left + (quarter + 3), this.bottomY, quarter, 20).build());
		this.nextButton = this.addRenderableWidget(Button.builder(Component.literal("Next >"), button -> {
					this.page++;
					this.pendingDeleteId = "";
					refresh();
				})
				.bounds(this.left + (quarter + 3) * 2, this.bottomY, quarter, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(this.left + (quarter + 3) * 3, this.bottomY, quarter, 20).build());

		refresh();
	}

	private List<ChatRule> rules() {
		return ChatDetectionConfig.get().rules;
	}

	private ChatRule ruleAt(int slot) {
		int index = this.page * this.rows + slot;
		return index >= 0 && index < rules().size() ? rules().get(index) : null;
	}

	private void refresh() {
		int count = rules().size();
		int maxPage = Math.max(0, (count - 1) / this.rows);
		this.page = Math.max(0, Math.min(this.page, maxPage));

		for (int i = 0; i < this.rows; i++) {
			ChatRule rule = ruleAt(i);
			boolean present = rule != null;
			this.toggleButtons.get(i).visible = present;
			this.toggleButtons.get(i).active = present;
			this.editButtons.get(i).visible = present;
			this.editButtons.get(i).active = present;
			this.deleteButtons.get(i).visible = present;
			this.deleteButtons.get(i).active = present;
			if (present) {
				this.toggleButtons.get(i).setMessage(Component.literal(rule.enabled ? "ON" : "OFF"));
				this.deleteButtons.get(i).setMessage(Component.literal(rule.id.equals(this.pendingDeleteId) ? "Sure?" : "X"));
			}
		}
		this.prevButton.active = this.page > 0;
		this.nextButton.active = this.page < maxPage;
	}

	private void toggleSlot(int slot) {
		ChatRule rule = ruleAt(slot);
		if (rule != null) {
			rule.enabled = !rule.enabled;
			ChatDetectionConfig.save();
			this.pendingDeleteId = "";
			refresh();
		}
	}

	private void editSlot(int slot) {
		ChatRule rule = ruleAt(slot);
		if (rule != null) {
			this.minecraft.gui.setScreen(new RuleEditScreen(this, rule, false, ""));
		}
	}

	private void deleteSlot(int slot) {
		ChatRule rule = ruleAt(slot);
		if (rule == null) {
			return;
		}
		if (rule.id.equals(this.pendingDeleteId)) {
			rules().remove(rule);
			ChatDetectionConfig.save();
			this.pendingDeleteId = "";
		} else {
			this.pendingDeleteId = rule.id;
		}
		refresh();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Ui.shadowText(graphics, this.font, "Chat Rules", this.left, this.top + 2, Ui.ACCENT);
		Ui.rightAligned(graphics, this.font, rules().size() + " rules  |  first match wins",
				this.left + this.contentW, this.top + 2, Ui.MUTED);

		if (rules().isEmpty()) {
			Ui.centered(graphics, this.font, "No rules yet.", this.width / 2, this.listTop + 6, Ui.MUTED);
			Ui.centered(graphics, this.font, "Open Recent Messages to turn a real server line into a rule,",
					this.width / 2, this.listTop + 20, Ui.MUTED);
			Ui.centered(graphics, this.font, "or press Add Rule to write one by hand.",
					this.width / 2, this.listTop + 32, Ui.MUTED);
		}

		for (int i = 0; i < this.rows; i++) {
			ChatRule rule = ruleAt(i);
			if (rule == null) {
				break;
			}
			int y = this.listTop + i * ROW_PITCH + 6;
			int regionLeft = this.left + 42;
			int regionRight = this.left + this.contentW - 90;

			String typeText = rule.type.label();
			int typeWidth = this.font.width(typeText);
			Ui.rightAligned(graphics, this.font, typeText, regionRight, y,
					rule.type.isIncome() ? Ui.GREEN : Ui.RED);

			boolean invalid = rule.validate() != null;
			String name = (invalid ? "(!) " : "") + rule.name;
			int color = invalid ? Ui.RED : (rule.enabled ? Ui.TEXT : Ui.MUTED);
			Ui.text(graphics, this.font, Ui.clip(this.font, name, regionRight - regionLeft - typeWidth - 8),
					regionLeft, y, color);
		}

		int maxPage = Math.max(0, (rules().size() - 1) / this.rows);
		Ui.centered(graphics, this.font, "Page " + (this.page + 1) + " / " + (maxPage + 1),
				this.width / 2, this.bottomY - 10, Ui.MUTED);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
