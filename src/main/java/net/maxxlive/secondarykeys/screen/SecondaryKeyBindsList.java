package net.maxxlive.secondarykeys.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.List;
import net.maxxlive.secondarykeys.SecondaryKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** List with columns: action name | primary key | secondary key | reset. */
public class SecondaryKeyBindsList extends ContainerObjectSelectionList<SecondaryKeyBindsList.Entry> {
	private static final int KEY_BUTTON_WIDTH = 75;
	private static final int RESET_BUTTON_WIDTH = 50;
	private static final int GAP = 5;

	private final SecondaryKeyBindsScreen screen;

	public SecondaryKeyBindsList(SecondaryKeyBindsScreen screen, Minecraft minecraft) {
		super(minecraft, screen.width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 20);
		this.screen = screen;
		this.addEntry(new ColumnHeaderEntry());
		KeyMapping[] keyMappings = minecraft.options.keyMappings.clone();
		Arrays.sort(keyMappings);
		KeyMapping.Category previousCategory = null;
		for (KeyMapping key : keyMappings) {
			KeyMapping.Category category = key.getCategory();
			if (category != previousCategory) {
				previousCategory = category;
				this.addEntry(new CategoryEntry(category));
			}
			this.addEntry(new KeyEntry(key, Component.translatable(key.getName())));
		}
	}

	public void resetMappingAndUpdateButtons() {
		KeyMapping.resetMapping();
		this.refreshEntries();
	}

	public void refreshEntries() {
		this.children().forEach(Entry::refreshEntry);
	}

	@Override
	public int getRowWidth() {
		return Math.min(420, this.width - 24);
	}

	private int resetX() {
		return this.scrollBarX() - RESET_BUTTON_WIDTH - 10;
	}

	private int secondaryX() {
		return this.resetX() - GAP - KEY_BUTTON_WIDTH;
	}

	private int primaryX() {
		return this.secondaryX() - GAP - KEY_BUTTON_WIDTH;
	}

	public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
		public abstract void refreshEntry();
	}

	/** "Primary" / "Secondary" labels above the two key columns. */
	public class ColumnHeaderEntry extends Entry {
		private final Component primary = Component.translatable("secondarykeys.column.primary").withStyle(ChatFormatting.GRAY);
		private final Component secondary = Component.translatable("secondarykeys.column.secondary").withStyle(ChatFormatting.GRAY);

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			Font font = SecondaryKeyBindsList.this.minecraft.font;
			int y = this.getContentYMiddle() - 9 / 2;
			int px = SecondaryKeyBindsList.this.primaryX() + KEY_BUTTON_WIDTH / 2 - font.width(this.primary) / 2;
			int sx = SecondaryKeyBindsList.this.secondaryX() + KEY_BUTTON_WIDTH / 2 - font.width(this.secondary) / 2;
			graphics.text(font, this.primary, px, y, -1);
			graphics.text(font, this.secondary, sx, y, -1);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}

		@Override
		public void refreshEntry() {
		}
	}

	public class CategoryEntry extends Entry {
		private final FocusableTextWidget categoryName;

		public CategoryEntry(KeyMapping.Category category) {
			this.categoryName = FocusableTextWidget.builder(
					category.label().copy().withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD),
					SecondaryKeyBindsList.this.minecraft.font
				)
				.alwaysShowBorder(false)
				.backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
				.build();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			this.categoryName.setPosition(
				SecondaryKeyBindsList.this.width / 2 - this.categoryName.getWidth() / 2,
				this.getContentBottom() - this.categoryName.getHeight()
			);
			this.categoryName.extractRenderState(graphics, mouseX, mouseY, a);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.categoryName);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.categoryName);
		}

		@Override
		public void refreshEntry() {
		}
	}

	public class KeyEntry extends Entry {
		private final KeyMapping key;
		private final Component name;
		private final Button primaryButton;
		private final Button secondaryButton;
		private final Button resetButton;
		private boolean primaryCollision;
		private boolean secondaryCollision;

		private KeyEntry(KeyMapping key, Component name) {
			this.key = key;
			this.name = name;
			this.primaryButton = Button.builder(name, b -> SecondaryKeyBindsList.this.screen.select(key, false))
				.bounds(0, 0, KEY_BUTTON_WIDTH, 20)
				.createNarration(def -> key.isUnbound()
					? Component.translatable("narrator.controls.unbound", name)
					: Component.translatable("narrator.controls.bound", name, def.get()))
				.build();
			this.secondaryButton = Button.builder(name, b -> SecondaryKeyBindsList.this.screen.select(key, true))
				.bounds(0, 0, KEY_BUTTON_WIDTH, 20)
				.createNarration(def -> !SecondaryKeys.hasSecondary(key)
					? Component.translatable("narrator.controls.unbound", name)
					: Component.translatable("narrator.controls.bound", name, def.get()))
				.build();
			this.resetButton = Button.builder(Component.translatable("controls.reset"), b -> {
					key.setKey(key.getDefaultKey());
					SecondaryKeys.setSecondary(key, InputConstants.UNKNOWN);
					SecondaryKeyBindsList.this.resetMappingAndUpdateButtons();
				})
				.bounds(0, 0, RESET_BUTTON_WIDTH, 20)
				.createNarration(def -> Component.translatable("narrator.controls.reset", name))
				.build();
			this.refreshEntry();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			this.updateMessages();
			int buttonY = this.getContentY() - 2;
			this.resetButton.setPosition(SecondaryKeyBindsList.this.resetX(), buttonY);
			this.secondaryButton.setPosition(SecondaryKeyBindsList.this.secondaryX(), buttonY);
			this.primaryButton.setPosition(SecondaryKeyBindsList.this.primaryX(), buttonY);
			this.resetButton.extractRenderState(graphics, mouseX, mouseY, a);
			this.secondaryButton.extractRenderState(graphics, mouseX, mouseY, a);
			this.primaryButton.extractRenderState(graphics, mouseX, mouseY, a);

			Font font = SecondaryKeyBindsList.this.minecraft.font;
			int maxNameWidth = this.primaryButton.getX() - 8 - this.getContentX();
			Component shown = this.name;
			if (font.width(shown) > maxNameWidth) {
				shown = Component.literal(font.plainSubstrByWidth(shown.getString(), maxNameWidth - font.width("...")) + "...");
			}
			graphics.text(font, shown, this.getContentX(), this.getContentYMiddle() - 9 / 2, -1);

			if (this.primaryCollision) {
				int left = this.primaryButton.getX() - 4;
				graphics.fill(left, this.getContentY() - 1, left + 2, this.getContentBottom(), -256);
			}
			if (this.secondaryCollision) {
				int left = this.secondaryButton.getX() - 4;
				graphics.fill(left, this.getContentY() - 1, left + 2, this.getContentBottom(), -256);
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return ImmutableList.of(this.primaryButton, this.secondaryButton, this.resetButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return ImmutableList.of(this.primaryButton, this.secondaryButton, this.resetButton);
		}

		/** Names of other actions that use {@code bound} as their primary or secondary key. */
		private MutableComponent findConflicts(InputConstants.Key bound, boolean onlyIfChanged) {
			MutableComponent conflicts = null;
			if (bound.equals(InputConstants.UNKNOWN)) {
				return null;
			}
			for (KeyMapping other : SecondaryKeyBindsList.this.minecraft.options.keyMappings) {
				if (other == this.key) {
					continue;
				}
				boolean primaryClash = SecondaryKeys.getPrimary(other).equals(bound)
					// same rule as vanilla: two untouched default bindings don't count as conflict
					&& (!onlyIfChanged || !other.isDefault() || !this.key.isDefault());
				boolean secondaryClash = SecondaryKeys.getSecondary(other).equals(bound);
				if (primaryClash || secondaryClash) {
					if (conflicts == null) {
						conflicts = Component.empty();
					} else {
						conflicts.append(", ");
					}
					conflicts.append(Component.translatable(other.getName()));
				}
			}
			return conflicts;
		}

		@Override
		public void refreshEntry() {
			this.resetButton.active = !this.key.isDefault() || SecondaryKeys.hasSecondary(this.key);

			MutableComponent primaryConflicts = this.findConflicts(SecondaryKeys.getPrimary(this.key), true);
			this.primaryCollision = primaryConflicts != null;
			this.primaryButton.setTooltip(this.primaryCollision
				? Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", primaryConflicts))
				: null);

			MutableComponent secondaryConflicts = this.findConflicts(SecondaryKeys.getSecondary(this.key), false);
			this.secondaryCollision = secondaryConflicts != null;
			this.secondaryButton.setTooltip(this.secondaryCollision
				? Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", secondaryConflicts))
				: null);

			this.updateMessages();
		}

		private void updateMessages() {
			SecondaryKeyBindsScreen s = SecondaryKeyBindsList.this.screen;
			boolean selected = s.selectedKey == this.key;
			this.primaryButton.setMessage(format(
				this.key.getTranslatedKeyMessage(), this.primaryCollision, selected && !s.selectingSecondary));
			this.secondaryButton.setMessage(format(
				SecondaryKeys.getSecondary(this.key).getDisplayName(), this.secondaryCollision, selected && s.selectingSecondary));
		}

		private static Component format(Component message, boolean collision, boolean selected) {
			if (collision) {
				message = Component.literal("[ ").append(message.copy().withStyle(ChatFormatting.WHITE)).append(" ]").withStyle(ChatFormatting.YELLOW);
			}
			if (selected) {
				message = Component.literal("> ")
					.append(message.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <")
					.withStyle(ChatFormatting.YELLOW);
			}
			return message;
		}
	}
}
