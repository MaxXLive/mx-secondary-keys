package de.ermackov.secondarykeys.mixin;

import de.ermackov.secondarykeys.screen.SecondaryKeyBindsScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlsScreen.class)
public abstract class ControlsScreenMixin extends OptionsSubScreen {
	private ControlsScreenMixin(Screen lastScreen, Options options, Component title) {
		super(lastScreen, options, title);
	}

	/** Adds a full-width "Secondary Keybinds..." button directly below the "Mouse Settings / Key Binds" row. */
	@Inject(
		method = "addOptions",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/OptionsList;addSmall(Lnet/minecraft/client/gui/components/AbstractWidget;Lnet/minecraft/client/gui/components/AbstractWidget;)V",
			shift = At.Shift.AFTER,
			ordinal = 0
		)
	)
	private void mxsecondarykeys$addButton(CallbackInfo ci) {
		if (this.list != null) {
			this.list.addBig(Button.builder(
				Component.translatable("mxsecondarykeys.button.open"),
				button -> this.minecraft.gui.setScreen(new SecondaryKeyBindsScreen(this, this.options))
			).build());
		}
	}
}
