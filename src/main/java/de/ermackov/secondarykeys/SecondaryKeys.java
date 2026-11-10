package de.ermackov.secondarykeys;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import de.ermackov.secondarykeys.screen.SecondaryKeyBindsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecondaryKeys implements ClientModInitializer {
	public static final String MOD_ID = "mxsecondarykeys";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
	private static KeyMapping openScreenKey;

	/** Mouse buttons currently held (keyboard state is queried from SDL directly). */
	private static final Set<InputConstants.Key> PRESSED_MOUSE = new HashSet<>();

	@Override
	public void onInitializeClient() {
		openScreenKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mxsecondarykeys.open", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));

		// All key mappings (including other mods') exist once the client has started.
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> SecondaryKeyStore.load());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openScreenKey.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new SecondaryKeyBindsScreen(null, client.options));
				}
			}
		});
	}

	public static InputConstants.Key getSecondary(KeyMapping mapping) {
		return ((SecondaryKeyHolder) mapping).mxsecondarykeys$getSecondaryKey();
	}

	public static void setSecondary(KeyMapping mapping, InputConstants.Key key) {
		((SecondaryKeyHolder) mapping).mxsecondarykeys$setSecondaryKey(key);
	}

	public static InputConstants.Key getPrimary(KeyMapping mapping) {
		return ((SecondaryKeyHolder) mapping).mxsecondarykeys$getPrimaryKey();
	}

	public static boolean hasSecondary(KeyMapping mapping) {
		return !getSecondary(mapping).equals(InputConstants.UNKNOWN);
	}

	public static void trackPress(InputConstants.Key key, boolean down) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			if (down) {
				PRESSED_MOUSE.add(key);
			} else {
				PRESSED_MOUSE.remove(key);
			}
		}
	}

	public static boolean isPhysicallyDown(InputConstants.Key key) {
		if (key.equals(InputConstants.UNKNOWN)) {
			return false;
		}
		return switch (key.getType()) {
			case KEYBOARD -> InputConstants.isKeyDown(key.getValue());
			case MOUSE -> PRESSED_MOUSE.contains(key);
			default -> false;
		};
	}

	/**
	 * When {@code released} is let go, is the mapping's other binding (primary vs. secondary) still held?
	 * In that case the action must stay pressed.
	 */
	public static boolean isOtherBindingHeld(KeyMapping mapping, InputConstants.Key released) {
		InputConstants.Key primary = getPrimary(mapping);
		InputConstants.Key secondary = getSecondary(mapping);
		InputConstants.Key other;
		if (released.equals(primary)) {
			other = secondary;
		} else if (released.equals(secondary)) {
			other = primary;
		} else {
			return false;
		}
		return !other.equals(released) && isPhysicallyDown(other);
	}

	public static Minecraft mc() {
		return Minecraft.getInstance();
	}
}
