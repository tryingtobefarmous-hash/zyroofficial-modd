package com.zyroofficial.screen;

import com.zyroofficial.ZyroofficialClient;
import com.zyroofficial.config.ZyroConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Phase 1 main menu: a working hub that opens each section and toggles the HUD.
 * Phase 5 replaces this with the full premium interface.
 */
public class MainMenuScreen extends Screen {
	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 6;

	private Button hudButton;

	public MainMenuScreen() {
		super(Component.literal(ZyroofficialClient.MOD_NAME));
	}

	@Override
	protected void init() {
		int x = (this.width - BUTTON_WIDTH) / 2;
		int y = this.height / 2 - 52;

		this.addRenderableWidget(Button.builder(Component.literal("Money Statistics"),
						button -> this.minecraft.gui.setScreen(new MoneyStatsScreen(this)))
				.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		y += BUTTON_HEIGHT + GAP;
		this.addRenderableWidget(Button.builder(Component.literal("Auction Tracker"),
						button -> this.minecraft.gui.setScreen(new PlaceholderScreen(
								Component.literal("Auction Tracker"),
								"The Auction Tracker screen arrives in Phase 3.", this)))
				.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		y += BUTTON_HEIGHT + GAP;
		this.hudButton = this.addRenderableWidget(Button.builder(hudLabel(), button -> {
					ZyroConfig config = ZyroConfig.get();
					config.hudVisible = !config.hudVisible;
					ZyroConfig.save();
					button.setMessage(hudLabel());
				})
				.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		y += BUTTON_HEIGHT + GAP;
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
				.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	private static Component hudLabel() {
		return Component.literal("Profit HUD: " + (ZyroConfig.get().hudVisible ? "ON" : "OFF"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		String heading = ZyroofficialClient.MOD_NAME;
		graphics.text(this.font, heading,
				(this.width - this.font.width(heading)) / 2, this.height / 2 - 78, 0xFF7C5CFF, true);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(null);
	}
}
