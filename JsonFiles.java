package com.zyroofficial.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Small helper for reading and atomically writing JSON files.
 * Writes go to a temporary file first so a crash never leaves a half-written save.
 */
public final class JsonFiles {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private JsonFiles() {
	}

	/** Reads a JSON file, or returns null if it does not exist or cannot be parsed. */
	public static <T> T read(Path path, Class<T> type) {
		if (!Files.isRegularFile(path)) {
			return null;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, type);
		} catch (Exception e) {
			// Keep the broken file around so the user does not lose data silently.
			try {
				Files.move(path, path.resolveSibling(path.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				// Nothing more we can do.
			}
			return null;
		}
	}

	/** Atomically writes the object as pretty-printed JSON. */
	public static void write(Path path, Object value) throws IOException {
		Files.createDirectories(path.getParent());
		Path temp = path.resolveSibling(path.getFileName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
			GSON.toJson(value, writer);
		}
		try {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException atomicFailed) {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
