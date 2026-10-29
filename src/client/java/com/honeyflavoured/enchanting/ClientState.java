package com.honeyflavoured.enchanting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;


final class ClientState {

	static boolean panelOpen;
	static boolean craftableOnlyToggle;
	private static boolean loaded;

	private ClientState() {}

	private static Path file() {

		return FabricLoader.getInstance().getConfigDir().resolve("honeyflavoured-client.properties");
	}

	static void load() {

		if (loaded) return;

		loaded = true;
		Path f = file();

		if (Files.exists(f)) {

			try (InputStream in = Files.newInputStream(f)) {

				Properties p = new Properties();
				p.load(in);
				panelOpen = Boolean.parseBoolean(p.getProperty("panelOpen", "false"));
				craftableOnlyToggle = Boolean.parseBoolean(p.getProperty("craftableOnlyToggle", "false"));
			}
			catch (IOException ignored) {
				// keep defaults
			}
		}
	}

	static void save() {

		Properties p = new Properties();

		p.setProperty("panelOpen", Boolean.toString(panelOpen));
		p.setProperty("craftableOnlyToggle", Boolean.toString(craftableOnlyToggle));

		try (OutputStream out = Files.newOutputStream(file())) {
			
			p.store(out, "Craft Enchanting client settings");
		}
		catch (IOException ignored) {
			// not fatal
		}
	}
}