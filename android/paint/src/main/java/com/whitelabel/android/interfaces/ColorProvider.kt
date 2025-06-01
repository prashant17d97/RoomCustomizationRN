package com.whitelabel.android.interfaces

import com.whitelabel.android.data.model.ColorProperty

/**
 * Interface for providing color data to the paint module.
 * This interface is implemented within the paint module (PaintFragment)
 * and does not need to be implemented at the calling site.
 */
interface ColorProvider {
    /**
     * Returns the current color to be used for painting.
     * @return The current ImageMaskColor to be used.
     */
    fun getCurrentColor(): ColorProperty

    /**
     * Updates the current color to be used for painting.
     * This method can be called by the paint module to notify
     * the provider that the color has been updated.
     * @param color The new ImageMaskColor to be used.
     */
    fun updateColor(color: ColorProperty)
}
