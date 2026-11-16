package com.zyroofficial.hud;

import com.zyroofficial.ZyroofficialClient;
import com.zyroofficial.config.ZyroConfig;
import com.zyroofficial.data.TransactionStore;
import com.zyroofficial.util.MoneyFormat;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Foundation of the profit HUD. Phase 4 extends this into the full configurable,
 * movable HUD; for now it shows the running net profit.
 */
public final class ProfitHud {
	private static final int WIDTH = 130;
	private static final int HEIGHT = 30;

	private static final int BACKGROUND = 0xA0101018;
	private static final int ACCENT = 0xFF7C5CFF;
	private static final int TEXT_MUTED = 0xFFB8B8C8;
	private static final int GREEN = 0xFF55FF7F;
	private static final int RED = 0xFFFF5555;
	private static final int WHITE = 0xFFFFFFFF;

	private ProfitHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(ZyroofficialClient.MOD_ID, "profit_hud"),
				ProfitHud::extract
		);
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		ZyroConfig config = ZyroConfig.get();
		if (!config.hudVisible) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		double net = TransactionStore.get().netProfit();

		int x = config.hudX;
		int y = config.hudY;

		graphics.fill(x, y, x + WIDTH, y + HEIGHT, BACKGROUND);
		graphics.fill(x, y, x + 2, y + HEIGHT, ACCENT);

		graphics.text(client.font, "Net Profit", x + 8, y + 5, TEXT_MUTED, false);
		int color = net > 0 ? GREEN : (net < 0 ? RED : WHITE);
		graphics.text(client.font, MoneyFormat.full(net), x + 8, y + 17, color, true);
	}
}
