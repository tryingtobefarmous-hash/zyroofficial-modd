package com.zyroofficial;

import com.zyroofficial.chat.ChatDetectionConfig;
import com.zyroofficial.chat.ChatDetector;
import com.zyroofficial.config.ZyroConfig;
import com.zyroofficial.data.TransactionStore;
import com.zyroofficial.hud.ProfitHud;
import com.zyroofficial.input.ZyroKeybinds;
import com.zyroofficial.screen.MainMenuScreen;
import com.zyroofficial.screen.MoneyStatsScreen;
import com.zyroofficial.screen.PlaceholderScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint for Zyroofficial Mod. */
public class ZyroofficialClient implements ClientModInitializer {
	public static final String MOD_ID = "zyroofficial";
	public static final String MOD_NAME = "Zyroofficial Mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitializeClient() {
		ZyroConfig.load();
		ChatDetectionConfig.load();
		TransactionStore.get().load();

		ZyroKeybinds.init();
		ProfitHud.register();
		ClientTickEvents.END_CLIENT_TICK.register(ZyroofficialClient::onClientTick);

		// System messages only: player chat is never read, so nobody can fake a payment line.
		ClientReceiveMessageEvents.GAME.register(ChatDetector::onGameMessage);

		LOGGER.info("{} loaded ({} saved transactions, {} chat rules)", MOD_NAME,
				TransactionStore.get().size(), ChatDetectionConfig.get().rules.size());
	}

	private static void onClientTick(Minecraft client) {
		while (ZyroKeybinds.OPEN_MENU.consumeClick()) {
			open(client, new MainMenuScreen());
		}
		while (ZyroKeybinds.OPEN_MONEY.consumeClick()) {
			open(client, new MoneyStatsScreen(null));
		}
		while (ZyroKeybinds.OPEN_AUCTION.consumeClick()) {
			open(client, new PlaceholderScreen(Component.literal("Auction Tracker"),
					"The Auction Tracker screen arrives in Phase 3.", null));
		}
		while (ZyroKeybinds.MOVE_HUD.consumeClick()) {
			open(client, new PlaceholderScreen(Component.literal("Move HUD Elements"),
					"The HUD editor arrives in Phase 4.", null));
		}
		while (ZyroKeybinds.TOGGLE_HUD.consumeClick()) {
			ZyroConfig config = ZyroConfig.get();
			config.hudVisible = !config.hudVisible;
			ZyroConfig.save();
			if (client.player != null) {
				client.player.sendSystemMessage(Component.literal(
						MOD_NAME + ": HUD " + (config.hudVisible ? "shown" : "hidden")));
			}
		}
	}

	/** Opens a screen, but only when no other screen (chat, inventory, menus) is already open. */
	private static void open(Minecraft client, Screen screen) {
		if (client.player == null || client.gui.screen() != null) {
			return;
		}
		client.gui.setScreen(screen);
	}
}
