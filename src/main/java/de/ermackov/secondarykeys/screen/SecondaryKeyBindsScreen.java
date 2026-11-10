package de.ermackov.secondarykeys.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.ermackov.secondarykeys.SecondaryKeyStore;
import de.ermackov.secondarykeys.SecondaryKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Combined key binds screen: every action shows its primary and its secondary key side by side.
 * Primary keys are stored by vanilla in options.txt, secondary keys in config/mxsecondarykeys.txt.
 */
public class SecondaryKeyBindsScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.translatable("mxsecondarykeys.screen.title");
	public @Nullable KeyMapping selectedKey;
	public boolean selectingSecondary;
	private SecondaryKeyBindsList keyList;
	private Button resetAllButton;

	public SecondaryKeyBindsScreen(@Nullable Screen lastScreen, Options options) {
		super(lastScreen, options, TITLE);
	}

	@Override
	protected void addContents() {
		this.keyList = this.layout.addToContents(new SecondaryKeyBindsList(this, this.minecraft));
	}

	@Override
	protected void addOptions() {
	}

	@Override
	protected void addFooter() {
		this.resetAllButton = Button.builder(Component.translatable("controls.resetAll"), button -> {
			for (KeyMapping key : this.options.keyMappings) {
				key.setKey(key.getDefaultKey());
				SecondaryKeys.setSecondary(key, InputConstants.UNKNOWN);
			}
			this.keyList.resetMappingAndUpdateButtons();
		}).build();
		LinearLayout bottomButtons = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		bottomButtons.addChild(this.resetAllButton);
		bottomButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).build());
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		this.keyList.updateSize(this.width, this.layout);
	}

	public void select(KeyMapping key, boolean secondary) {
		this.selectedKey = key;
		this.selectingSecondary = secondary;
		this.keyList.resetMappingAndUpdateButtons();
	}

	private void assign(InputConstants.Key newKey) {
		if (this.selectingSecondary) {
			SecondaryKeys.setSecondary(this.selectedKey, newKey);
		} else {
			this.selectedKey.setKey(newKey);
		}
		this.selectedKey = null;
		this.keyList.resetMappingAndUpdateButtons();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.selectedKey != null) {
			this.assign(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.selectedKey != null) {
			this.assign(event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		SecondaryKeyStore.save();
		super.removed(); // saves options.txt (primary keys)
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		boolean canReset = false;
		for (KeyMapping key : this.options.keyMappings) {
			if (!key.isDefault() || SecondaryKeys.hasSecondary(key)) {
				canReset = true;
				break;
			}
		}
		this.resetAllButton.active = canReset;
	}
}
