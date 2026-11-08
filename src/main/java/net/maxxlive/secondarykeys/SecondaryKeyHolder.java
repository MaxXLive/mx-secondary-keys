package net.maxxlive.secondarykeys;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Duck interface injected into {@link net.minecraft.client.KeyMapping} by the mixin.
 */
public interface SecondaryKeyHolder {
	InputConstants.Key secondarykeys$getSecondaryKey();

	void secondarykeys$setSecondaryKey(InputConstants.Key key);

	InputConstants.Key secondarykeys$getPrimaryKey();
}
