package net.maxxlive.secondarykeys;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Persists secondary bindings in {@code config/secondarykeys.txt} as {@code <mapping name>:<key name>} lines.
 */
public final class SecondaryKeyStore {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("secondarykeys.txt");

	private SecondaryKeyStore() {
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				// Key names never contain ':' but mapping names might, so split at the last one.
				int sep = line.lastIndexOf(':');
				if (sep <= 0) {
					continue;
				}
				String name = line.substring(0, sep);
				String keyName = line.substring(sep + 1);
				KeyMapping mapping = KeyMapping.get(name);
				if (mapping == null) {
					SecondaryKeys.LOGGER.warn("Unknown key mapping '{}' in {}, skipping", name, FILE.getFileName());
					continue;
				}
				try {
					SecondaryKeys.setSecondary(mapping, InputConstants.getKey(keyName));
				} catch (IllegalArgumentException e) {
					SecondaryKeys.LOGGER.warn("Unknown key '{}' for '{}', skipping", keyName, name);
				}
			}
		} catch (IOException e) {
			SecondaryKeys.LOGGER.error("Could not read {}", FILE, e);
		}
		KeyMapping.resetMapping();
	}

	public static void save() {
		List<String> lines = new ArrayList<>();
		lines.add("# Secondary key bindings (edit in-game: Options > Controls > Secondary Keybinds)");
		for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
			if (SecondaryKeys.hasSecondary(mapping)) {
				lines.add(mapping.getName() + ":" + SecondaryKeys.getSecondary(mapping).getName());
			}
		}
		try {
			Files.createDirectories(FILE.getParent());
			Files.write(FILE, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			SecondaryKeys.LOGGER.error("Could not write {}", FILE, e);
		}
	}
}
