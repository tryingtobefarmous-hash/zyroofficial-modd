package com.zyroofficial.config;

import com.zyroofficial.ZyroofficialClient;
import com.zyroofficial.util.JsonFiles;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;

/**
 * User settings, saved to config/zyroofficial/config.json.
 * Later phases add fields here (chat detection, popups, HUD layout); unknown or missing
 * fields in an older file simply fall back to the defaults below.
 */
public final class ZyroConfig {
	private static ZyroConfig instance;

	public int configVersion = 1;

	/** Whether the profit HUD is drawn. */
	public boolean hudVisible = true;

	/** Whether popup alerts are shown (used from Phase 4). */
	public boolean popupsEnabled = true;

	/** Transactions below this amount are ignored (used from Phase 2). */
	public double minTransactionAmount = 0.0;

	/** Profit HUD position in scaled GUI pixels from the top-left corner. */
	public int hudX = 6;
	public int hudY = 6;

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("zyroofficial");
	}

	private static Path file() {
		return directory().resolve("config.json");
	}

	public static ZyroConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static void load() {
		ZyroConfig loaded = JsonFiles.read(file(), ZyroConfig.class);
		instance = loaded != null ? loaded : new ZyroConfig();
		instance.sanitize();
	}

	public static void save() {
		try {
			JsonFiles.write(file(), get());
		} catch (IOException e) {
			ZyroofficialClient.LOGGER.error("Could not save Zyroofficial config", e);
		}
	}

	private void sanitize() {
		if (minTransactionAmount < 0 || Double.isNaN(minTransactionAmount)) {
			minTransactionAmount = 0.0;
		}
		if (hudX < 0) {
			hudX = 0;
		}
		if (hudY < 0) {
			hudY = 0;
		}
	}
}
