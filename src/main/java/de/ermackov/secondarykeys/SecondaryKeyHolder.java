package de.ermackov.secondarykeys;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Duck interface injected into {@link net.minecraft.client.KeyMapping} by the mixin.
 */
public interface SecondaryKeyHolder {
	InputConstants.Key mxsecondarykeys$getSecondaryKey();

	void mxsecondarykeys$setSecondaryKey(InputConstants.Key key);

	InputConstants.Key mxsecondarykeys$getPrimaryKey();
}
