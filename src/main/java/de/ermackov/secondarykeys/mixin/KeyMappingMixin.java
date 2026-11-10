package de.ermackov.secondarykeys.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import de.ermackov.secondarykeys.SecondaryKeyHolder;
import de.ermackov.secondarykeys.SecondaryKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements SecondaryKeyHolder {
	@Shadow @Final private static Map<String, KeyMapping> ALL;
	@Shadow @Final private static Map<InputConstants.Key, List<KeyMapping>> MAP;

	@Shadow protected InputConstants.Key key;

	@Unique
	private InputConstants.Key mxsecondarykeys$secondary = InputConstants.UNKNOWN;

	@Override
	public InputConstants.Key mxsecondarykeys$getSecondaryKey() {
		return this.mxsecondarykeys$secondary;
	}

	@Override
	public void mxsecondarykeys$setSecondaryKey(InputConstants.Key key) {
		this.mxsecondarykeys$secondary = key == null ? InputConstants.UNKNOWN : key;
	}

	@Override
	public InputConstants.Key mxsecondarykeys$getPrimaryKey() {
		return this.key;
	}

	/** Register every mapping a second time under its secondary key, so click()/set() reach it. */
	@Inject(method = "resetMapping", at = @At("TAIL"))
	private static void mxsecondarykeys$registerSecondaryKeys(CallbackInfo ci) {
		for (KeyMapping mapping : ALL.values()) {
			InputConstants.Key secondary = SecondaryKeys.getSecondary(mapping);
			if (!secondary.equals(InputConstants.UNKNOWN)) {
				List<KeyMapping> list = MAP.computeIfAbsent(secondary, k -> new ArrayList<>());
				if (!list.contains(mapping)) {
					list.add(mapping);
				}
			}
		}
	}

	/**
	 * Same as vanilla, except that releasing one binding does not release the action
	 * while its other binding (primary or secondary) is still held.
	 */
	@Inject(method = "set", at = @At("HEAD"), cancellable = true)
	private static void mxsecondarykeys$set(InputConstants.Key key, boolean state, CallbackInfo ci) {
		SecondaryKeys.trackPress(key, state);
		ci.cancel();
		List<KeyMapping> mappings = MAP.get(key);
		if (mappings == null) {
			return;
		}
		for (KeyMapping mapping : mappings) {
			if (!state && SecondaryKeys.isOtherBindingHeld(mapping, key)) {
				continue;
			}
			mapping.setDown(state);
		}
	}

	/** After regaining focus, also mark actions whose secondary keyboard key is held as pressed. */
	@Inject(method = "setAll", at = @At("TAIL"))
	private static void mxsecondarykeys$setAllSecondary(CallbackInfo ci) {
		for (KeyMapping mapping : ALL.values()) {
			InputConstants.Key secondary = SecondaryKeys.getSecondary(mapping);
			if (secondary.getType() != InputConstants.Type.KEYBOARD || secondary.equals(InputConstants.UNKNOWN)) {
				continue;
			}
			if (mapping instanceof ToggleKeyMapping toggle && toggle.needsToggle.getAsBoolean()) {
				continue;
			}
			if (InputConstants.isKeyDown(secondary.getValue())) {
				mapping.setDown(true);
			}
		}
	}

	@Inject(method = "matches(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("RETURN"), cancellable = true)
	private void mxsecondarykeys$matchesKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			InputConstants.Key s = this.mxsecondarykeys$secondary;
			if (s.getType() == InputConstants.Type.KEYBOARD && !s.equals(InputConstants.UNKNOWN) && s.getValue() == event.key()) {
				cir.setReturnValue(true);
			}
		}
	}

	@Inject(method = "matchesMouse", at = @At("RETURN"), cancellable = true)
	private void mxsecondarykeys$matchesMouse(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			InputConstants.Key s = this.mxsecondarykeys$secondary;
			if (s.getType() == InputConstants.Type.MOUSE && s.getValue() == event.button()) {
				cir.setReturnValue(true);
			}
		}
	}

	@Inject(method = "matches(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Z", at = @At("RETURN"), cancellable = true)
	private void mxsecondarykeys$matchesInputKey(InputConstants.Key key, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && !this.mxsecondarykeys$secondary.equals(InputConstants.UNKNOWN) && this.mxsecondarykeys$secondary.equals(key)) {
			cir.setReturnValue(true);
		}
	}
}
