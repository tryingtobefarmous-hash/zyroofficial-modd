package com.zyroofficial.screen;

import com.zyroofficial.chat.ChatDetectionConfig;
import com.zyroofficial.chat.ChatDetector;
import com.zyroofficial.chat.ChatRule;
import com.zyroofficial.util.MoneyFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Create or edit one chat rule, with a live tester: paste a real chat line and see instantly
 * whether the pattern matches and what amount and player it extracts.
 */
public class RuleEditScreen extends Screen {
	private static final int PANEL_W = 340;

	private final Screen parent;
	private final ChatRule target;
	private final boolean isNew;
	private final ChatRule draft;
	private final String samplePrefill;

	private EditBox nameBox;
	private EditBox patternBox;
	private EditBox sampleBox;
	private Button typeButton;
	private Button caseButton;

	private String testText = "";
	private int testColor = Ui.MUTED;
	private String saveError = "";

	private int left;
	private int top;
	private int nameLabelY;
	private int patternLabelY;
	private int sampleLabelY;
	private int resultY;
	private int helpY;

	public RuleEditScreen(Screen parent, ChatRule target, boolean isNew, String samplePrefill) {
		super(Component.literal(isNew ? "New Rule" : "Edit Rule"));
		this.parent = parent;
		this.target = target;
		this.isNew = isNew;
		this.draft = target.copy();
		this.samplePrefill = samplePrefill == null ? "" : samplePrefill;
	}

	@Override
	protected void init() {
		this.left = (this.width - PANEL_W) / 2;
		int totalH = 14 + 32 + 24 + 32 + 32 + 24 + 24 + 24;
		this.top = Math.max(4, (this.height - totalH) / 2);

		int y = this.top + 14;
		this.nameLabelY = y;
		this.nameBox = new EditBox(this.font, this.left, y + 10, PANEL_W, 20, Component.literal("Rule name"));
		this.nameBox.setMaxLength(40);
		this.nameBox.setValue(this.draft.name);
		this.addRenderableWidget(this.nameBox);

		y += 34;
		int half = (PANEL_W - 4) / 2;
		this.typeButton = this.addRenderableWidget(Button.builder(typeLabel(), button -> {
					com.zyroofficial.data.TransactionType[] values = com.zyroofficial.data.TransactionType.values();
					this.draft.type = values[(this.draft.type.ordinal() + 1) % values.length];
					button.setMessage(typeLabel());
					updateTest();
				})
				.bounds(this.left, y, half, 20).build());
		this.caseButton = this.addRenderableWidget(Button.builder(caseLabel(), button -> {
					this.draft.ignoreCase = !this.draft.ignoreCase;
					button.setMessage(caseLabel());
					updateTest();
				})
				.bounds(this.left + half + 4, y, half, 20).build());

		y += 24;
		this.patternLabelY = y;
		this.patternBox = new EditBox(this.font, this.left, y + 10, PANEL_W, 20, Component.literal("Pattern"));
		this.patternBox.setMaxLength(400);
		this.patternBox.setHint(Component.literal("Regex with {amount} and optional {player}"));
		this.patternBox.setValue(this.draft.pattern);
		this.addRenderableWidget(this.patternBox);

		y += 32;
		this.sampleLabelY = y;
		this.sampleBox = new EditBox(this.font, this.left, y + 10, PANEL_W, 20, Component.literal("Sample line"));
		this.sampleBox.setMaxLength(400);
		this.sampleBox.setHint(Component.literal("Paste a real chat line here to test"));
		this.sampleBox.setValue(this.samplePrefill);
		this.addRenderableWidget(this.sampleBox);

		y += 32;
		this.resultY = y;
		y += 12;
		this.helpY = y;

		y += 24;
		this.addRenderableWidget(Button.builder(Component.literal("Save Rule"), button -> save())
				.bounds(this.left, y, half, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
				.bounds(this.left + half + 4, y, half, 20).build());

		// Listeners attach last so the setValue calls above do not trigger them during init.
		this.nameBox.setResponder(text -> this.draft.name = text);
		this.patternBox.setResponder(text -> {
			this.draft.pattern = text;
			updateTest();
		});
		this.sampleBox.setResponder(text -> updateTest());

		updateTest();
	}

	private Component typeLabel() {
		return Component.literal("Type: " + this.draft.type.label());
	}

	private Component caseLabel() {
		return Component.literal("Ignore case: " + (this.draft.ignoreCase ? "YES" : "NO"));
	}

	private void updateTest() {
		this.saveError = "";
		String error = this.draft.validate();
		if (error != null) {
			this.testText = error;
			this.testColor = Ui.RED;
			return;
		}

		String sample = ChatDetector.clean(this.sampleBox.getValue());
		if (sample.isEmpty()) {
			this.testText = "Pattern is valid. Paste a sample line below to test it.";
			this.testColor = Ui.MUTED;
			return;
		}

		ChatRule.Match match = this.draft.match(sample);
		if (match == null) {
			this.testText = "No match on this sample line.";
			this.testColor = Ui.RED;
			return;
		}

		StringBuilder text = new StringBuilder("Match: ")
				.append(this.draft.type.label()).append(' ').append(MoneyFormat.full(match.amount));
		if (!match.player.isEmpty()) {
			text.append("  player ").append(match.player);
		}
		if (!ChatDetectionConfig.get().typeEnabled(this.draft.type)) {
			text.append("  (this type is switched off in Auto-Detect)");
		}
		this.testText = text.toString();
		this.testColor = Ui.GREEN;
	}

	private void save() {
		String name = this.nameBox.getValue().trim();
		if (name.isEmpty()) {
			this.saveError = "Give the rule a name.";
			return;
		}
		String error = this.draft.validate();
		if (error != null) {
			this.saveError = "Fix the pattern first: " + error;
			return;
		}

		this.draft.name = name;
		this.target.copyFrom(this.draft);
		if (this.isNew) {
			ChatDetectionConfig.get().rules.add(this.target);
		}
		ChatDetectionConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Ui.shadowText(graphics, this.font, this.isNew ? "New Rule" : "Edit Rule", this.left, this.top + 2, Ui.ACCENT);
		Ui.text(graphics, this.font, "Rule name", this.left, this.nameLabelY + 1, Ui.MUTED);
		Ui.text(graphics, this.font, "Pattern", this.left, this.patternLabelY, Ui.MUTED);
		Ui.text(graphics, this.font, "Test sample (not saved)", this.left, this.sampleLabelY, Ui.MUTED);

		String shown = this.saveError.isEmpty() ? this.testText : this.saveError;
		int color = this.saveError.isEmpty() ? this.testColor : Ui.RED;
		Ui.text(graphics, this.font, Ui.clip(this.font, shown, PANEL_W), this.left, this.resultY, color);

		Ui.text(graphics, this.font, Ui.clip(this.font,
				"Use {amount} (required) and {player}. Partial match - anchor with ^ and $.", PANEL_W),
				this.left, this.helpY, Ui.MUTED);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
