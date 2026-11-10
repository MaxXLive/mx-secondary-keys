package de.ermackov.secondarykeys.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import de.ermackov.secondarykeys.SecondaryKeyStore;
import de.ermackov.secondarykeys.SecondaryKeys;
import de.ermackov.secondarykeys.screen.SecondaryKeyBindsScreen;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.resources.language.I18n;
import org.jspecify.annotations.Nullable;

public class SecondaryKeysClientGameTest implements FabricClientGameTest {
	private static final InputConstants.Key KEY_G = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_G);
	/** Mouse button 4 (the first side button). */
	private static final int MOUSE_4 = InputConstants.MOUSE_BUTTON_4;
	private static final InputConstants.Key MOUSE_4_KEY = InputConstants.Type.MOUSE.getOrCreate(MOUSE_4);

	@Override
	public void runTest(ClientGameTestContext context) {
		TestInput input = context.getInput();

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();

			// A keyboard key as secondary binding for jump.
			setSecondary(context, KEY_G);
			input.holdKey(KEY_G);
			context.waitTick();
			assertJumpDown(context, true, "holding the secondary key G");
			input.releaseKey(KEY_G);
			context.waitTick();
			assertJumpDown(context, false, "after releasing the secondary key G");

			// Releasing one binding keeps the action pressed while the other one is still held, in both orders.
			input.holdKey(options -> options.keyJump);
			input.holdKey(KEY_G);
			context.waitTick();
			input.releaseKey(KEY_G);
			context.waitTick();
			assertJumpDown(context, true, "after releasing G while the primary key is still held");
			input.releaseKey(options -> options.keyJump);
			context.waitTick();
			assertJumpDown(context, false, "after releasing both keys");

			input.holdKey(KEY_G);
			input.holdKey(options -> options.keyJump);
			context.waitTick();
			input.releaseKey(options -> options.keyJump);
			context.waitTick();
			assertJumpDown(context, true, "after releasing the primary key while G is still held");
			input.releaseKey(KEY_G);
			context.waitTick();
			assertJumpDown(context, false, "after releasing both keys");

			// A mouse button as secondary binding.
			setSecondary(context, MOUSE_4_KEY);
			input.holdMouse(MOUSE_4);
			context.waitTick();
			assertJumpDown(context, true, "holding mouse button 4");
			input.releaseMouse(MOUSE_4);
			context.waitTick();
			assertJumpDown(context, false, "after releasing mouse button 4");

			// Secondary bindings are saved to and loaded from the config file.
			Path file = FabricLoader.getInstance().getConfigDir().resolve("mxsecondarykeys.txt");
			context.runOnClient(mc -> SecondaryKeyStore.save());
			String saved = readFile(file);
			String expectedLine = "key.jump:" + MOUSE_4_KEY.getName();
			if (!saved.contains(expectedLine)) {
				throw new AssertionError("Expected " + expectedLine + " in " + file.getFileName() + ", got: " + saved);
			}
			context.runOnClient(mc -> {
				SecondaryKeys.setSecondary(mc.options.keyJump, InputConstants.UNKNOWN);
				SecondaryKeyStore.load();
			});
			boolean loaded = context.computeOnClient(mc -> SecondaryKeys.getSecondary(mc.options.keyJump).equals(MOUSE_4_KEY));
			if (!loaded) {
				throw new AssertionError("Secondary key of jump was not loaded back from " + file.getFileName());
			}

			// K opens the editor in game.
			input.pressKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K));
			context.waitForScreen(SecondaryKeyBindsScreen.class);
			context.takeScreenshot("secondary_keys_editor");
			context.setScreen(() -> null);
			context.waitTick();
		}

		// The controls screen has a button that opens the editor.
		context.setScreen(() -> new ControlsScreen(null, context.computeOnClient(mc -> mc.options)));
		context.waitForScreen(ControlsScreen.class);
		context.takeScreenshot("secondary_keys_controls_button");
		clickButtonInList(context, I18n.get("mxsecondarykeys.button.open"));
		context.waitForScreen(SecondaryKeyBindsScreen.class);
		context.setScreen(() -> null);
	}

	/**
	 * {@code clickScreenButton} only looks at the direct children of a screen, but the button sits inside the
	 * options list, so it is found recursively and clicked with the mouse at its center.
	 */
	private static void clickButtonInList(ClientGameTestContext context, String label) {
		double[] cursor = context.computeOnClient(mc -> {
			AbstractWidget button = findWidget(mc.gui.screen().children(), label);
			if (button == null) {
				throw new AssertionError("Could not find button '" + label + "' in " + mc.gui.screen().getClass().getSimpleName());
			}
			Window window = mc.getWindow();
			double scaleX = (double) window.getScreenWidth() / window.getGuiScaledWidth();
			double scaleY = (double) window.getScreenHeight() / window.getGuiScaledHeight();
			return new double[] {(button.getX() + button.getWidth() / 2.0) * scaleX, (button.getY() + button.getHeight() / 2.0) * scaleY};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
	}

	private static @Nullable AbstractWidget findWidget(List<? extends GuiEventListener> children, String label) {
		for (GuiEventListener child : children) {
			if (child instanceof AbstractWidget widget && widget.getMessage().getString().equals(label)) {
				return widget;
			}
			if (child instanceof ContainerEventHandler container) {
				AbstractWidget found = findWidget(container.children(), label);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static void setSecondary(ClientGameTestContext context, InputConstants.Key key) {
		context.runOnClient(mc -> {
			SecondaryKeys.setSecondary(mc.options.keyJump, key);
			KeyMapping.resetMapping();
		});
	}

	private static void assertJumpDown(ClientGameTestContext context, boolean expected, String situation) {
		boolean down = context.computeOnClient(mc -> mc.options.keyJump.isDown());
		if (down != expected) {
			throw new AssertionError("Jump should be " + (expected ? "down" : "up") + " " + situation);
		}
	}

	private static String readFile(Path file) {
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new AssertionError("Could not read " + file, e);
		}
	}
}
