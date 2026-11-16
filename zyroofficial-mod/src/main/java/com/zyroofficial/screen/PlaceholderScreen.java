package com.zyroofficial.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Temporary screen for features that are built in a later phase.
 * Each one is replaced by its real screen in its own phase.
 */
public class PlaceholderScreen extends Screen {
	private final String message;
	private final Screen parent;

	public PlaceholderScreen(Component title, String message, Screen parent) {
		super(title);
		this.message = message;
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(this.width / 2 - 50, this.height / 2 + 20, 100, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		String heading = this.title.getString();
		graphics.text(this.font, heading,
				(this.width - this.font.width(heading)) / 2, this.height / 2 - 30, 0xFFFFFFFF, true);
		graphics.text(this.font, this.message,
				(this.width - this.font.width(this.message)) / 2, this.height / 2 - 10, 0xFFB8B8C8, false);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
