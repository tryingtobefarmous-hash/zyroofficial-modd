package com.zyroofficial.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared colors and small drawing helpers for Zyroofficial screens. */
final class Ui {
	static final int ACCENT = 0xFF7C5CFF;
	static final int CARD = 0xB0141420;
	static final int TEXT = 0xFFFFFFFF;
	static final int MUTED = 0xFFB8B8C8;
	static final int GREEN = 0xFF55FF7F;
	static final int RED = 0xFFFF5555;
	static final int GOLD = 0xFFFFD25A;
	static final int DIVIDER = 0x30FFFFFF;

	private Ui() {
	}

	/** A dark card with an accent stripe on its left edge. */
	static void card(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, CARD);
		graphics.fill(x, y, x + 2, y + h, ACCENT);
	}

	static void text(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.text(font, text, x, y, color, false);
	}

	static void shadowText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.text(font, text, x, y, color, true);
	}

	static void centered(GuiGraphicsExtractor graphics, Font font, String text, int centerX, int y, int color) {
		graphics.text(font, text, centerX - font.width(text) / 2, y, color, false);
	}

	static void rightAligned(GuiGraphicsExtractor graphics, Font font, String text, int rightX, int y, int color) {
		graphics.text(font, text, rightX - font.width(text), y, color, false);
	}

	/** Shortens text with "..." so it fits in maxWidth pixels. */
	static String clip(Font font, String text, int maxWidth) {
		if (maxWidth <= 0) {
			return "";
		}
		if (font.width(text) <= maxWidth) {
			return text;
		}
		String result = text;
		while (!result.isEmpty() && font.width(result + "...") > maxWidth) {
			result = result.substring(0, result.length() - 1);
		}
		return result + "...";
	}

	static int signColor(double value) {
		if (value > 0) {
			return GREEN;
		}
		if (value < 0) {
			return RED;
		}
		return TEXT;
	}
}
