package com.example.craftenchanting.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Remembers the knowledge book settings between sessions (config/craftenchanting-client.properties). */
final class ClientState {
	/** Knowledge book panel left open. */
	static boolean panelOpen;
	/** "Show only craftable" toggle. */
	static boolean craftableOnly;

	private static boolean loaded;

	private ClientState() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("craftenchanting-client.properties");
	}

	static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		Path f = file();
		if (Files.exists(f)) {
			try (InputStream in = Files.newInputStream(f)) {
				Properties p = new Properties();
				p.load(in);
				panelOpen = Boolean.parseBoolean(p.getProperty("panelOpen", "false"));
				craftableOnly = Boolean.parseBoolean(p.getProperty("craftableOnly", "false"));
			} catch (IOException ignored) {
				// keep defaults
			}
		}
	}

	static void save() {
		Properties p = new Properties();
		p.setProperty("panelOpen", Boolean.toString(panelOpen));
		p.setProperty("craftableOnly", Boolean.toString(craftableOnly));
		try (OutputStream out = Files.newOutputStream(file())) {
			p.store(out, "Craft Enchanting client settings");
		} catch (IOException ignored) {
			// not fatal
		}
	}
}